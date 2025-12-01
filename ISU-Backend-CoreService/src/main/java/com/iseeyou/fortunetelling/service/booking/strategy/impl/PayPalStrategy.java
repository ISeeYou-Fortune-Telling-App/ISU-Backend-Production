package com.iseeyou.fortunetelling.service.booking.strategy.impl;

import com.iseeyou.fortunetelling.entity.booking.Booking;
import com.iseeyou.fortunetelling.entity.booking.BookingPayment;
import com.iseeyou.fortunetelling.repository.booking.BookingPaymentRepository;
import com.iseeyou.fortunetelling.service.booking.strategy.PaymentStrategy;
import com.iseeyou.fortunetelling.service.booking.strategy.gateway.PayPalGateway;
import com.iseeyou.fortunetelling.service.notification.NotificationMicroservice;
import com.iseeyou.fortunetelling.service.report.ReportMicroservice;
import com.iseeyou.fortunetelling.util.Constants;
import com.paypal.api.payments.DetailedRefund;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.RelatedResources;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PayPalStrategy implements PaymentStrategy {

    private final PayPalGateway payPalGateway;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final ReportMicroservice reportMicroservice;
    private final NotificationMicroservice notificationMicroservice;

    // USD conversion rate - 1 VND = 0.0395 USD
    private static final double VND_TO_USD = 0.0395;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingPayment pay(Booking booking) throws PayPalRESTException {
        double amountInVND = booking.getServicePackage().getPrice();
        double amountInUSD = amountInVND * VND_TO_USD;
        try {
            Payment payment = payPalGateway.createPaymentWithPayPal(
                    amountInUSD,
                    String.valueOf(booking.getId())
            );

            // Get approval URL to redirect user
            String redirectUrl = payment.getLinks().stream()
                    .filter(link -> "approval_url".equals(link.getRel()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No approval URL found"))
                    .getHref();

            BookingPayment bookingPayment = new BookingPayment();
            bookingPayment.setBooking(booking);
            bookingPayment.setAmount(amountInVND);
            bookingPayment.setStatus(Constants.PaymentStatusEnum.PENDING);
            bookingPayment.setPaymentMethod(Constants.PaymentMethodEnum.PAYPAL);
            bookingPayment.setPaymentType(Constants.PaymentTypeEnum.PAID_PACKAGE);
            bookingPayment.setApprovalUrl(redirectUrl);
            bookingPayment.setExtraInfo(payment.getId());

            return bookingPaymentRepository.save(bookingPayment);

        } catch (PayPalRESTException e) {
            log.error("PayPal payment creation failed: {}", e.getMessage());

            BookingPayment bookingPayment = new BookingPayment();
            bookingPayment.setBooking(booking);
            bookingPayment.setAmount(amountInVND);
            bookingPayment.setStatus(Constants.PaymentStatusEnum.FAILED);
            bookingPayment.setPaymentMethod(Constants.PaymentMethodEnum.PAYPAL);
            bookingPayment.setPaymentType(Constants.PaymentTypeEnum.PAID_PACKAGE);
            bookingPayment.setFailureReason(e.getMessage());

            return bookingPaymentRepository.save(bookingPayment);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingPayment executePayment(Map<String, Object> paymentParams) {
        String paymentId = (String) paymentParams.get("paymentId");
        String payerId = (String) paymentParams.get("PayerID");

        BookingPayment bookingPayment = bookingPaymentRepository.findByExtraInfo(paymentId);

        try {

            if (bookingPayment == null) {
                log.error("BookingPayment not found for paymentId: {}", paymentId);
                return null;
            }

            Payment executedPayment = payPalGateway.executePayment(paymentId, payerId);

            String transactionId = null;
            List<Transaction> transactions = executedPayment.getTransactions();

            if (transactions != null && !transactions.isEmpty()) {
                Transaction transaction = transactions.get(0); // Lấy giao dịch đầu tiên
                List<RelatedResources> relatedResources = transaction.getRelatedResources();

                if (relatedResources != null && !relatedResources.isEmpty()) {
                    for (RelatedResources resource : relatedResources) {
                        if (resource.getSale() != null) {
                            transactionId = resource.getSale().getId(); // Lấy ID của sale
                            log.info("Extracted PayPal Sale ID: {} from payment execution", transactionId);
                            break;
                        }
                    }
                }
            }

            if (transactionId == null || transactionId.trim().isEmpty()) {
                log.error("Failed to extract transaction ID from PayPal payment execution. Payment ID: {}", paymentId);
                throw new PayPalRESTException("Could not extract transaction ID from PayPal payment");
            }
            
            // Validate transaction ID format before saving
            if (!isValidPayPalTransactionId(transactionId)) {
                log.error("Invalid PayPal transaction ID format extracted: {}", transactionId);
                throw new PayPalRESTException("Invalid transaction ID format received from PayPal: " + transactionId);
            }

            bookingPayment.setTransactionId(transactionId);
            log.info("Successfully set transaction ID {} for payment {}", transactionId, bookingPayment.getId());

            // Update payment status based on PayPal response
            if ("approved".equals(executedPayment.getState())) {
                bookingPayment.setStatus(Constants.PaymentStatusEnum.COMPLETED);

                // Record customer spending action to Report Service
                try {
                    UUID customerId = bookingPayment.getBooking().getCustomer().getId();
                    boolean spendingSuccess = reportMicroservice.customerAction(
                        customerId.toString(),
                        Constants.CustomerAction.SPENDING,
                        java.math.BigDecimal.valueOf(bookingPayment.getAmount())
                    );
                    if (!spendingSuccess) {
                        log.warn("Failed to record customer SPENDING action to Report Service for customer {}", customerId);
                    }
                } catch (Exception e) {
                    log.error("Error calling Report Service for customer SPENDING action: {}", e.getMessage());
                    // Don't throw - continue with business logic
                }

                // Record seer received booking action to Report Service
                try {
                    UUID seerId = bookingPayment.getBooking().getServicePackage().getSeer().getId();
                    boolean receivedSuccess = reportMicroservice.seerAction(
                        seerId.toString(),
                        Constants.SeerAction.RECEIVED_BOOKING,
                        java.math.BigDecimal.valueOf(bookingPayment.getAmount())
                    );
                    if (!receivedSuccess) {
                        log.warn("Failed to record seer RECEIVED_BOOKING action to Report Service for seer {}", seerId);
                    }
                } catch (Exception e) {
                    log.error("Error calling Report Service for seer RECEIVED_BOOKING action: {}", e.getMessage());
                    // Don't throw - continue with business logic
                }

                // Send notification to customer about successful payment
                try {
                    var customer = bookingPayment.getBooking().getCustomer();
                    String customerFcmToken = customer.getFcmToken() != null ? customer.getFcmToken() : "";
                    notificationMicroservice.sendNotification(
                        customer.getId().toString(),
                        "Thanh toán thành công",
                        "Thanh toán thành công cho booking #" + bookingPayment.getBooking().getId(),
                        Constants.TargetType.BOOKING,
                        bookingPayment.getBooking().getId().toString(),
                        null,
                        java.util.Map.of(
                            "bookingId", bookingPayment.getBooking().getId().toString(),
                            "amount", String.valueOf(bookingPayment.getAmount())
                        )
                    );
                } catch (Exception e) {
                    log.error("Error sending notification to customer about payment success: {}", e.getMessage());
                }

                // Send notification to seer about new booking
                try {
                    var seer = bookingPayment.getBooking().getServicePackage().getSeer();
                    var customer = bookingPayment.getBooking().getCustomer();
                    String seerFcmToken = seer.getFcmToken() != null ? seer.getFcmToken() : "";
                    notificationMicroservice.sendNotification(
                        seer.getId().toString(),
                        "Booking mới",
                        "Bạn vừa nhận được 1 booking mới từ " + customer.getFullName(),
                        Constants.TargetType.BOOKING,
                        bookingPayment.getBooking().getId().toString(),
                        null,
                        java.util.Map.of(
                            "bookingId", bookingPayment.getBooking().getId().toString(),
                            "customerId", customer.getId().toString(),
                            "customerName", customer.getFullName()
                        )
                    );
                } catch (Exception e) {
                    log.error("Error sending notification to seer about new booking: {}", e.getMessage());
                }
//                if (orderPayment.getOrder().getUser() != null) {
//                    notificationService.createNotification(
//                            NotificationCreateRequestDTO.builder()
//                                    .notificationType(Constants.NotificationTypeEnum.PAYMENT)
//                                    .notificationContent("Thanh toán thành công cho đơn hàng " + orderPayment.getOrder().getId())
//                                    .senderId(null)
//                                    .receiverId(orderPayment.getOrder().getUser().getId())
//                                    .isRead(false)
//                                    .build()
//                    );
//                }
            } else {
                bookingPayment.setStatus(Constants.PaymentStatusEnum.FAILED);
                bookingPayment.setFailureReason("Payment not approved: " + executedPayment.getState());

                // Send notification about payment failure
                try {
                    var customer = bookingPayment.getBooking().getCustomer();
                    String customerFcmToken = customer.getFcmToken() != null ? customer.getFcmToken() : "";
                    notificationMicroservice.sendNotification(
                        customer.getId().toString(),
                        "Thanh toán thất bại",
                        "Thanh toán cho booking #" + bookingPayment.getBooking().getId() + " thất bại. Vui lòng thử lại.",
                        Constants.TargetType.BOOKING,
                        bookingPayment.getBooking().getId().toString(),
                        null,
                        java.util.Map.of("bookingId", bookingPayment.getBooking().getId().toString())
                    );
                } catch (Exception ex) {
                    log.error("Error sending notification about payment failure: {}", ex.getMessage());
                }
            }

            return bookingPaymentRepository.save(bookingPayment);

        } catch (PayPalRESTException e) {
            log.error("PayPal payment execution failed: {}", e.getMessage());
            bookingPayment.setStatus(Constants.PaymentStatusEnum.FAILED);
            bookingPayment.setFailureReason(e.getMessage());

            // Send notification about payment failure
            try {
                var customer = bookingPayment.getBooking().getCustomer();
                String customerFcmToken = customer.getFcmToken() != null ? customer.getFcmToken() : "";
                notificationMicroservice.sendNotification(
                    customer.getId().toString(),
                    "Thanh toán thất bại",
                    "Thanh toán cho booking #" + bookingPayment.getBooking().getId() + " thất bại. Vui lòng thử lại.",
                    Constants.TargetType.BOOKING,
                    bookingPayment.getBooking().getId().toString(),
                    null,
                    java.util.Map.of("bookingId", bookingPayment.getBooking().getId().toString())
                );
            } catch (Exception ex) {
                log.error("Error sending notification about payment failure: {}", ex.getMessage());
            }

            return bookingPaymentRepository.save(bookingPayment);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingPayment refund(UUID bookingId, BookingPayment payment) throws PayPalRESTException {
        log.info("Starting refund process for booking {} with payment {}", bookingId, payment.getId());
        log.debug("Payment details - ID: {}, Method: {}, Status: {}, Amount: {}, TransactionID: {}", 
                payment.getId(), payment.getPaymentMethod(), payment.getStatus(), 
                payment.getAmount(), payment.getTransactionId());
        
        // Validate payment method
        if (!payment.getPaymentMethod().equals(Constants.PaymentMethodEnum.PAYPAL)) {
            log.error("Invalid payment method for refund: {}. Expected PAYPAL", payment.getPaymentMethod());
            throw new IllegalArgumentException("Payment method is not PayPal. Cannot refund payment with method: " + payment.getPaymentMethod());
        }
        
        // Check if already refunded FIRST (before checking COMPLETED status)
        if (payment.getStatus().equals(Constants.PaymentStatusEnum.REFUNDED)) {
            log.warn("Payment {} is already refunded", payment.getId());
            throw new IllegalArgumentException("Payment has already been refunded");
        }
        
        // Validate payment status - must be COMPLETED to refund
        if (!payment.getStatus().equals(Constants.PaymentStatusEnum.COMPLETED)) {
            log.error("Cannot refund payment with status: {}. Expected COMPLETED", payment.getStatus());
            throw new IllegalArgumentException("Only COMPLETED payments can be refunded. Current status: " + payment.getStatus());
        }
        
        // Validate transaction ID exists
        String transactionId = payment.getTransactionId();
        if (transactionId == null || transactionId.trim().isEmpty()) {
            log.error("No transaction ID found for payment {}", payment.getId());
            throw new IllegalArgumentException("No transaction ID found for this payment. Cannot process refund.");
        }
        
        // Validate transaction ID format - PayPal Sale IDs should not contain invalid characters
        if (!isValidPayPalTransactionId(transactionId)) {
            log.error("Invalid PayPal transaction ID format: {}. This may not be a valid PayPal Sale ID", transactionId);
            throw new IllegalArgumentException(
                "Invalid transaction ID format: '" + transactionId + "'. " +
                "This doesn't appear to be a valid PayPal Sale ID. " +
                "The payment may have been created with a different payment method or contains invalid data. " +
                "Please contact support for manual refund."
            );
        }
        
        try {
            // Extract Sale ID from transaction ID
            // TransactionId can be in format "SALE-xxx" or just "xxx"
            String saleId = extractSaleId(transactionId);
            log.info("Extracted Sale ID: {} from transaction ID: {}", saleId, transactionId);
            
            // Convert amount back to USD for PayPal refund
            double amountInVND = payment.getAmount();
            double amountInUSD = amountInVND * VND_TO_USD;
            
            // Call PayPal API to refund
            DetailedRefund refund = payPalGateway.refundSale(saleId, amountInUSD);
            
            // Update payment status and info
            payment.setStatus(Constants.PaymentStatusEnum.REFUNDED);
            payment.setExtraInfo(buildRefundInfo(refund, payment.getExtraInfo()));
            
            BookingPayment savedPayment = bookingPaymentRepository.save(payment);
            
            log.info("Successfully refunded payment {} for booking {}. Refund ID: {}, State: {}", 
                    payment.getId(), bookingId, refund.getId(), refund.getState());
            
            // Send notification to customer about successful refund
            try {
                var customer = payment.getBooking().getCustomer();
                String customerFcmToken = customer.getFcmToken() != null ? customer.getFcmToken() : "";
                notificationMicroservice.sendNotification(
                    customer.getId().toString(),
                    "Hoàn tiền thành công",
                    "Tiền đã được hoàn lại cho booking #" + bookingId,
                    Constants.TargetType.BOOKING,
                    bookingId.toString(),
                    null,
                    java.util.Map.of(
                        "bookingId", bookingId.toString(),
                        "amount", String.valueOf(payment.getAmount())
                    )
                );
            } catch (Exception ex) {
                log.error("Error sending notification about refund success: {}", ex.getMessage());
            }

            return savedPayment;
            
        } catch (PayPalRESTException e) {
            log.error("PayPal refund failed for payment {} (booking {}): {} - {}", 
                    payment.getId(), bookingId, e.getMessage(), e.getDetails());
            
            // Update payment with failure info but don't change status
            payment.setFailureReason("Refund failed: " + e.getMessage());
            bookingPaymentRepository.save(payment);
            
            throw e;
        }
    }
    
    /**
     * Extract Sale ID from transaction ID
     * Transaction ID can be in various formats:
     * - "SALE-xxx" -> return "SALE-xxx"
     * - "PAY-xxx#SALE-yyy" -> return "SALE-yyy"
     * - Just "xxx" -> return "xxx"
     */
    private String extractSaleId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is null or empty");
        }
        
        String trimmed = transactionId.trim();
        
        // If contains #SALE-, extract the part after #
        if (trimmed.contains("#SALE-")) {
            String[] parts = trimmed.split("#");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        
        // If already starts with SALE-, return as is
        if (trimmed.startsWith("SALE-")) {
            return trimmed;
        }
        
        // Otherwise, assume it's a direct sale ID
        return trimmed;
    }
    
    /**
     * Build refund info string to store in extraInfo field
     */
    private String buildRefundInfo(DetailedRefund refund, String existingInfo) {
        StringBuilder info = new StringBuilder();
        
        if (existingInfo != null && !existingInfo.isEmpty()) {
            info.append(existingInfo).append(" | ");
        }
        
        info.append("Refund ID: ").append(refund.getId())
            .append(", State: ").append(refund.getState())
            .append(", Create Time: ").append(refund.getCreateTime());
        
        if (refund.getAmount() != null) {
            info.append(", Amount: ").append(refund.getAmount().getTotal())
                .append(" ").append(refund.getAmount().getCurrency());
        }
        
        return info.toString();
    }
    
    /**
     * Validate if transaction ID is a valid PayPal format
     * PayPal Sale IDs typically:
     * - Start with alphanumeric characters
     * - May contain hyphens and underscores
     * - Should NOT contain payment method prefixes like "MOMO_", "VNPAY_", etc.
     * - Common formats: "SALE-xxxxx", "PAY-xxxxx#SALE-xxxxx", or alphanumeric IDs
     */
    private boolean isValidPayPalTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = transactionId.trim();
        
        // Check for invalid prefixes that indicate wrong payment method
        String[] invalidPrefixes = {"MOMO_", "VNPAY_", "ZALOPAY_", "BANKING_"};
        for (String prefix : invalidPrefixes) {
            if (trimmed.toUpperCase().startsWith(prefix)) {
                log.warn("Transaction ID contains invalid prefix '{}': {}", prefix, trimmed);
                return false;
            }
        }
        
        // PayPal transaction IDs should be reasonable length (not too short, not too long)
        if (trimmed.length() < 5 || trimmed.length() > 100) {
            log.warn("Transaction ID length is invalid: {} characters", trimmed.length());
            return false;
        }
        
        // Check if contains only valid characters for PayPal IDs
        // Allow: alphanumeric, hyphens, underscores, and # (for combined PAY-xxx#SALE-xxx format)
        if (!trimmed.matches("^[A-Za-z0-9\\-_#]+$")) {
            log.warn("Transaction ID contains invalid characters: {}", trimmed);
            return false;
        }
        
        return true;
    }
}

package com.iseeyou.fortunetelling.config.payment;

import com.iseeyou.fortunetelling.service.booking.strategy.PaymentStrategy;
import com.iseeyou.fortunetelling.service.booking.strategy.impl.PayPalStrategy;
import com.iseeyou.fortunetelling.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PaymentConfig {
    @Bean
    public Map<Constants.PaymentMethodEnum, PaymentStrategy> paymentStrategies(
            PayPalStrategy paypalPaymentStrategy) {
        Map<Constants.PaymentMethodEnum, PaymentStrategy> strategies = new HashMap<>();
        strategies.put(Constants.PaymentMethodEnum.PAYPAL, paypalPaymentStrategy);
        return strategies;
    }
}

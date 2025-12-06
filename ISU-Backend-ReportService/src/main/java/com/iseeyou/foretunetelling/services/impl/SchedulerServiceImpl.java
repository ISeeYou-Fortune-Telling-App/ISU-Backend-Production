package com.iseeyou.foretunetelling.services.impl;

import com.iseeyou.foretunetelling.events.ReportEventPublisher;
import com.iseeyou.foretunetelling.events.dto.SeerNewRatingEvent;
import com.iseeyou.foretunetelling.models.CustomerPotential;
import com.iseeyou.foretunetelling.models.SeerPerformance;
import com.iseeyou.foretunetelling.repositories.CustomerPotentialRepository;
import com.iseeyou.foretunetelling.repositories.SeerPerformanceRepository;
import com.iseeyou.foretunetelling.services.SchedulerService;
import com.iseeyou.foretunetelling.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {
    private final CustomerPotentialRepository customerPotentialRepository;
    private final SeerPerformanceRepository seerPerformanceRepository;
    private final ReportEventPublisher reportEventPublisher;

    @Override
    @Transactional
//     Run every day at 00:00 AM (midnight)
    @Scheduled(cron = "0 0 0 * * ?")
//     Run every minute for debugging
//    @Scheduled(cron = "0 * * * * ?")
    public boolean autoCalculateSeer() {
        List<SeerPerformance> seerPerformanceList = seerPerformanceRepository.findAll();
        for (SeerPerformance seerPerformance : seerPerformanceList) {
            if (!calculateSeerPoint(seerPerformance)) {
                return false;
            }
        }

        seerPerformanceRepository.saveAll(seerPerformanceList);

        int month = LocalDate.now().getMonthValue();
        int  year = LocalDate.now().getYear();
        return calculateSeerRanking(month, year);
    }

    @Override
    @Transactional
    // Run every day at 00:00 AM (midnight)
    @Scheduled(cron = "0 0 0 * * ?")
    // Run every minute for debugging
//    @Scheduled(cron = "0 * * * * ?")
    public boolean autoCalculateCustomer() {

        List<CustomerPotential> customerPotentialList = customerPotentialRepository.findAll();
        for (CustomerPotential customerPotential : customerPotentialList) {
            if (!calculateCustomerPoint(customerPotential)) {
                return false;
            }
        }

        customerPotentialRepository.saveAll(customerPotentialList);

        int month = LocalDate.now().getMonthValue();
        int  year = LocalDate.now().getYear();
        return calculateCustomerRanking(month, year);
    }

    private boolean calculateSeerPoint(SeerPerformance seerPerformance) {
        // Get tier bonus based on last month's tier
        Constants.SeerTier lastSeerTier = getSeerLastTier(seerPerformance);
        int tierBonus = getTierBonus(lastSeerTier.getMinPoint());

        // Engagement Score: Each package approved got 20 points
        int engagementScore = seerPerformance.getTotalPackages() * 20;

        // Rating Score
        int ratingScore = seerPerformance.getAvgRating().intValue() * 20;
        int confidentBoost = Math.min(seerPerformance.getTotalRates() * 2, 20);
        int finalRatingScore = confidentBoost + ratingScore;

        // Completion Score
        double completionRate = (double) seerPerformance.getCompletedBookings() / seerPerformance.getTotalBookings();
        int completionScore = (int) (completionRate * 100);

        // Reliability Score
        double cancellationRate = (double) seerPerformance.getCancelledBySeer() / seerPerformance.getTotalBookings();
        int reliabilityScore = (int) ((1 - cancellationRate) * 100);

        // Earnings Score
        int earningScore = seerPerformance.getTotalRevenue().multiply(BigDecimal.valueOf(10))
                .divide(BigDecimal.valueOf(500000), 2, RoundingMode.HALF_UP).intValue();

        // Calculate base point from current month's metrics
        int calculatedPoint = (int) (
                0.3 * engagementScore +
                        0.25 * finalRatingScore +
                        0.2 * completionScore +
                        0.15 * reliabilityScore +
                        0.1 * earningScore
        );

        // Add tier bonus and cap at 100
        int currentSeerPoint = Math.min(calculatedPoint + tierBonus, 100);

        seerPerformance.setPerformancePoint(currentSeerPoint);
        updateSeerTier(seerPerformance);

        // Publish for new data
        try {
            SeerNewRatingEvent seerNewRatingEvent = new SeerNewRatingEvent();
            seerNewRatingEvent.setAvgRating(seerPerformance.getAvgRating());
            seerNewRatingEvent.setSeerTier(seerPerformance.getPerformanceTier().getValue());
            seerNewRatingEvent.setSeerId(seerPerformance.getSeerId());
            seerNewRatingEvent.setTotalRates(seerPerformance.getTotalRates());

            reportEventPublisher.publishNewSeerRatingEvent(seerNewRatingEvent);
        } catch (Exception e) {
            log.error("Publish event for seer rating error {}", e.getMessage());
        }

        return true;
    }

    private boolean calculateCustomerPoint(CustomerPotential customerPotential) {
        // Get tier bonus based on last month's tier
        Constants.CustomerTier lastCustomerTier = getCustomerLastTier(customerPotential);
        int tierBonus = getTierBonus(lastCustomerTier.getMinPoint());

        // Loyalty Score: Each booking request got 10 points
        int loyaltyScore = customerPotential.getTotalBookingRequests() * 10;

        // Value Score: Average spending per booking
        // Each 100k average spending -> 10 points
        BigDecimal avgSpending = customerPotential.getTotalSpending()
                .divide(BigDecimal.valueOf(customerPotential.getTotalBookingRequests()), 2, RoundingMode.HALF_UP);
        int valueScore = avgSpending.multiply(BigDecimal.valueOf(10))
                .divide(BigDecimal.valueOf(100000), 2, RoundingMode.HALF_UP).intValue();

        // Customer Reliability Score
        double customerCancellationRate = (double) customerPotential.getCancelledByCustomer() / customerPotential.getTotalBookingRequests();
        int customerReliabilityScore = (int) ((1 - customerCancellationRate) * 100);

        // Calculate customer potential with weighted formula
        int calculatedPotential = (int) (
                0.4 * loyaltyScore +
                        0.35 * valueScore +
                        0.25 * customerReliabilityScore
        );

        // Add tier bonus and cap at 100
        int currentCustomerPoint = Math.min(calculatedPotential + tierBonus, 100);
        customerPotential.setPotentialPoint(currentCustomerPoint);

        updateCustomerTier(customerPotential);
        return true;
    }

    private Constants.SeerTier getSeerLastTier(SeerPerformance seerPerformance) {
        // Calculate previous month and year
        int lastMonth = seerPerformance.getMonth() - 1;
        int lastYear = seerPerformance.getYear();
        if (lastMonth == 0) {
            lastMonth = 12;
            lastYear--;
        }

        SeerPerformance lastMonthPerformance = seerPerformanceRepository.findBySeerIdAndMonthAndYear(
                seerPerformance.getSeerId(), lastMonth, lastYear
        );
        if (lastMonthPerformance == null)
            return Constants.SeerTier.APPRENTICE;
        else
            return lastMonthPerformance.getPerformanceTier();
    }

    private Constants.CustomerTier getCustomerLastTier(CustomerPotential customerPotential) {
        // Calculate previous month and year
        int lastMonth = customerPotential.getMonth() - 1;
        int lastYear = customerPotential.getYear();
        if (lastMonth == 0) {
            lastMonth = 12;
            lastYear--;
        }

        CustomerPotential lastMonthPotential = customerPotentialRepository.findByCustomerIdAndMonthAndYear(
                customerPotential.getCustomerId(), lastMonth, lastYear
        );
        if (lastMonthPotential == null)
            return Constants.CustomerTier.CASUAL;
        else
            return lastMonthPotential.getPotentialTier();
    }

    private int getTierBonus(int tier) {
        // Tier bonus system: CASUAL/APPRENTICE=0, STANDARD/PROFESSIONAL=10, PREMIUM/EXPERT=20, VIP/MASTER=30
        return switch (tier) {
            case 0 -> 0;  // CASUAL or APPRENTICE
            case 50 -> 10; // STANDARD or PROFESSIONAL
            case 70 -> 20; // PREMIUM or EXPERT
            case 85 -> 30; // VIP or MASTER
            default -> 0;
        };
    }

    private void updateCustomerTier(CustomerPotential customerPotential) {
        int currentCustomerPotential = customerPotential.getPotentialPoint();
        if (currentCustomerPotential >= Constants.CustomerTier.VIP.getMinPoint())
            customerPotential.setPotentialTier(Constants.CustomerTier.VIP);
        else if (currentCustomerPotential >= Constants.CustomerTier.PREMIUM.getMinPoint())
            customerPotential.setPotentialTier(Constants.CustomerTier.PREMIUM);
        else if (currentCustomerPotential >= Constants.CustomerTier.STANDARD.getMinPoint())
            customerPotential.setPotentialTier(Constants.CustomerTier.STANDARD);
        else
            customerPotential.setPotentialTier(Constants.CustomerTier.CASUAL);
    }

    private void updateSeerTier(SeerPerformance seerPerformance) {
        int currentPerformancePoint = seerPerformance.getPerformancePoint();
        if (currentPerformancePoint >= Constants.SeerTier.MASTER.getMinPoint())
            seerPerformance.setPerformanceTier(Constants.SeerTier.MASTER);
        else if (currentPerformancePoint >= Constants.SeerTier.EXPERT.getMinPoint())
            seerPerformance.setPerformanceTier(Constants.SeerTier.EXPERT);
        else if (currentPerformancePoint >= Constants.SeerTier.PROFESSIONAL.getMinPoint())
            seerPerformance.setPerformanceTier(Constants.SeerTier.PROFESSIONAL);
        else
            seerPerformance.setPerformanceTier(Constants.SeerTier.APPRENTICE);
    }


    private boolean calculateCustomerRanking(Integer month, Integer year) {
        try {
            List<CustomerPotential> customerPotentialMonth = customerPotentialRepository.findAllByMonthAndYear(month, year);

            // Sort by potentialPoint descending
            customerPotentialMonth.sort((c1, c2) -> c2.getPotentialPoint().compareTo(c1.getPotentialPoint()));

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (CustomerPotential customerPotential : customerPotentialMonth) {
                if (previousPoint != null && !previousPoint.equals(customerPotential.getPotentialPoint())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                customerPotential.setRanking(ranking);
                previousPoint = customerPotential.getPotentialPoint();
                currentPosition++;
            }

            // Save all
            customerPotentialRepository.saveAll(customerPotentialMonth);

            return true;
        } catch (Exception e) {
            log.error("calculateCustomerRanking - {}", e.getMessage());
            return false;
        }
    }

    private boolean calculateSeerRanking(Integer month, Integer year) {
        try {
            List<SeerPerformance> seerPerformanceMonth = seerPerformanceRepository.findAllByMonthAndYear(month, year);

            // Sort by performancePoint descending
            seerPerformanceMonth.sort((s1, s2) -> s2.getPerformancePoint().compareTo(s1.getPerformancePoint()));

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (SeerPerformance seerPerformance : seerPerformanceMonth) {
                if (previousPoint != null && !previousPoint.equals(seerPerformance.getPerformancePoint())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                seerPerformance.setRanking(ranking);
                previousPoint = seerPerformance.getPerformancePoint();
                currentPosition++;
            }

            // Save all
            seerPerformanceRepository.saveAll(seerPerformanceMonth);

            return true;
        } catch (Exception e) {
            log.error("calculateSeerRanking - {}", e.getMessage());
            return false;
        }
    }
}

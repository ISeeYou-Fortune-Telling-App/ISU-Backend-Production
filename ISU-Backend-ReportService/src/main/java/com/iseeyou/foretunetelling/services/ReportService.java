package com.iseeyou.foretunetelling.services;

import com.iseeyou.foretunetelling.dtos.FinanceStatistic;
import com.iseeyou.foretunetelling.dtos.chart.ChartDto;
import com.iseeyou.foretunetelling.events.dto.UserChangeEvent;
import com.iseeyou.foretunetelling.models.CustomerPotential;
import com.iseeyou.foretunetelling.models.SeerPerformance;
import com.iseeyou.foretunetelling.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ReportService {
    boolean createCustomerReport(String customerId, Integer month, Integer year);
    boolean createSeerPerformanceReport(String seerId, Integer month, Integer year);
    boolean createListCustomerReport(List<String> customerIds, Integer month, Integer year);
    boolean createListSeerPerformanceReport(List<String> seerIds, Integer month, Integer year);
    CustomerPotential getCustomerPotential(String customerId, Integer month, Integer year);
    CustomerPotential getMyCustomerPotential(Integer month, Integer year);
    Page<CustomerPotential> getAllCustomerPotential(
            Pageable pageable,
            Integer month,
            Integer year,
            Integer minPotentialPoint,
            Integer maxPotentialPoint,
            Constants.CustomerTier potentialTier,
            Integer minRanking,
            Integer maxRanking,
            Integer minTotalBookingRequests,
            Integer maxTotalBookingRequests,
            BigDecimal minTotalSpending,
            BigDecimal maxTotalSpending,
            Integer minCancelledByCustomer,
            Integer maxCancelledByCustomer
    );
    Page<SeerPerformance> getAllSeerPerformance(
            Pageable pageable,
            Integer month,
            Integer year,
            Integer minPerformancePoint,
            Integer maxPerformancePoint,
            Constants.SeerTier performanceTier,
            Integer minRanking,
            Integer maxRanking,
            Integer minTotalPackages,
            Integer maxTotalPackages,
            Integer minTotalRates,
            Integer maxTotalRates,
            Double minAvgRating,
            Double maxAvgRating,
            Integer minTotalBookings,
            Integer maxTotalBookings,
            Integer minCompletedBookings,
            Integer maxCompletedBookings,
            Integer minCancelledBySeer,
            Integer maxCancelledBySeer,
            BigDecimal minTotalRevenue,
            BigDecimal maxTotalRevenue,
            BigDecimal minBonus,
            BigDecimal maxBonus
    );
    SeerPerformance getSeerPerformance(String seerId, Integer month, Integer year);
    SeerPerformance getMySeerPerformance(Integer month, Integer year);

    boolean customerAction(String customerId, Constants.CustomerAction action, BigDecimal amount);
    boolean seerAction(String seerId, Constants.SeerAction action, BigDecimal amount);
    boolean customerChange(UserChangeEvent event);
    boolean seerChange(UserChangeEvent event);

    // Chart and stats
    FinanceStatistic getFinanceStatistic();
    ChartDto<?> getChartDto(Constants.ChartTypeDto chartType, Integer month, Integer year);
}

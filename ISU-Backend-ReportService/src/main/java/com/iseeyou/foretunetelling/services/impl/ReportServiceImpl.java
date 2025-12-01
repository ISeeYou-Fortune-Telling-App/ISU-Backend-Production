package com.iseeyou.foretunetelling.services.impl;

import com.iseeyou.foretunetelling.dtos.FinanceStatistic;
import com.iseeyou.foretunetelling.dtos.chart.ChartDto;
import com.iseeyou.foretunetelling.events.dto.UserChangeEvent;
import com.iseeyou.foretunetelling.models.CustomerPotential;
import com.iseeyou.foretunetelling.models.SeerPerformance;
import com.iseeyou.foretunetelling.repositories.CustomerPotentialRepository;
import com.iseeyou.foretunetelling.repositories.SeerPerformanceRepository;
import com.iseeyou.foretunetelling.services.AuthService;
import com.iseeyou.foretunetelling.services.ReportService;
import com.iseeyou.foretunetelling.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final CustomerPotentialRepository customerPotentialRepository;
    private final SeerPerformanceRepository seerPerformanceRepository;
    private final AuthService authService;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public boolean createCustomerReport(String customerId, Integer month, Integer year) {
        try {
            CustomerPotential customerPotential = new CustomerPotential();

            customerPotential.setCustomerId(customerId);
            customerPotential.setMonth(month);
            customerPotential.setYear(year);
            customerPotential.setTotalBookingRequests(0);
            customerPotential.setTotalSpending(BigDecimal.ZERO);
            customerPotential.setCancelledByCustomer(0);
            customerPotential.setRanking(0);

            if (month == 1) {
                month = 12;
                year--;
            }
            CustomerPotential lastCustomerPotential = getCustomerPotential(customerId, month - 1, year);
            if (lastCustomerPotential != null) {
                customerPotential.setPotentialPoint(lastCustomerPotential.getPotentialTier().getMinPoint());
                customerPotential.setPotentialTier(lastCustomerPotential.getPotentialTier().getPreviousTier());
            } else {
                customerPotential.setPotentialPoint(0);
                customerPotential.setPotentialTier(Constants.CustomerTier.CASUAL);
            }
            
            customerPotentialRepository.save(customerPotential);

            return true;
        } catch (Exception e) {
            log.error("createCustomerReport - {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean createSeerPerformanceReport(String seerId, Integer month, Integer year) {
        try {
            SeerPerformance seerPerformance = new SeerPerformance();

            seerPerformance.setSeerId(seerId);
            seerPerformance.setMonth(month);
            seerPerformance.setYear(year);
            seerPerformance.setTotalPackages(0);
            seerPerformance.setTotalRates(0);
            seerPerformance.setAvgRating(0.0);
            seerPerformance.setTotalBookings(0);
            seerPerformance.setCompletedBookings(0);
            seerPerformance.setCancelledBySeer(0);
            seerPerformance.setTotalRevenue(BigDecimal.ZERO);
            seerPerformance.setBonus(BigDecimal.ZERO);
            seerPerformance.setRanking(0);

            if (month == 1) {
                month = 12;
                year--;
            }
            SeerPerformance lastSeerPerformance = getSeerPerformance(seerId, month - 1, year);
            if (lastSeerPerformance != null) {
                seerPerformance.setPerformancePoint(lastSeerPerformance.getPerformanceTier().getMinPoint());
                seerPerformance.setPerformanceTier(lastSeerPerformance.getPerformanceTier().getPreviousTier());
            } else {
                seerPerformance.setPerformancePoint(0);
                seerPerformance.setPerformanceTier(Constants.SeerTier.APPRENTICE);
            }

            seerPerformanceRepository.save(seerPerformance);

            return true;
        } catch (Exception e) {
            log.error("createSeerPerformanceReport - {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean createListCustomerReport(List<String> customerIds, Integer month, Integer year) {
        try {
            for (String customerId : customerIds) {
                if (!createCustomerReport(customerId, month, year)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("createListCustomerReport - {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean createListSeerPerformanceReport(List<String> seerIds, Integer month, Integer year) {
        try {
            for (String seerId : seerIds) {
                if (!createSeerPerformanceReport(seerId, month, year)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("createListSeerPerformanceReport - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public CustomerPotential getCustomerPotential(String customerId, Integer month, Integer year) {
        return customerPotentialRepository.findByCustomerIdAndMonthAndYear(customerId, month, year);
    }

    @Override
    public CustomerPotential getMyCustomerPotential(Integer month, Integer year) {
        return getCustomerPotential(authService.getCurrentUserId().toString(), month, year);
    }

    @Override
    public Page<CustomerPotential> getAllCustomerPotential(
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
    ) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Month and Year filter
        if (month != null) {
            criteriaList.add(Criteria.where("month").is(month));
        }
        if (year != null) {
            criteriaList.add(Criteria.where("year").is(year));
        }

        // Potential Point filter
        if (minPotentialPoint != null) {
            criteriaList.add(Criteria.where("potential_point").gte(minPotentialPoint));
        }
        if (maxPotentialPoint != null) {
            criteriaList.add(Criteria.where("potential_point").lte(maxPotentialPoint));
        }

        // Potential Tier filter
        if (potentialTier != null) {
            criteriaList.add(Criteria.where("potential_tier").is(potentialTier.ordinal()));
        }

        // Ranking filter
        if (minRanking != null) {
            criteriaList.add(Criteria.where("ranking").gte(minRanking));
        }
        if (maxRanking != null) {
            criteriaList.add(Criteria.where("ranking").lte(maxRanking));
        }

        // Total Booking Requests filter
        if (minTotalBookingRequests != null) {
            criteriaList.add(Criteria.where("total_booking_requests").gte(minTotalBookingRequests));
        }
        if (maxTotalBookingRequests != null) {
            criteriaList.add(Criteria.where("total_booking_requests").lte(maxTotalBookingRequests));
        }

        // Total Spending filter
        if (minTotalSpending != null) {
            criteriaList.add(Criteria.where("total_spending").gte(minTotalSpending));
        }
        if (maxTotalSpending != null) {
            criteriaList.add(Criteria.where("total_spending").lte(maxTotalSpending));
        }

        // Cancelled By Customer filter
        if (minCancelledByCustomer != null) {
            criteriaList.add(Criteria.where("cancelled_by_customer").gte(minCancelledByCustomer));
        }
        if (maxCancelledByCustomer != null) {
            criteriaList.add(Criteria.where("cancelled_by_customer").lte(maxCancelledByCustomer));
        }

        if (!criteriaList.isEmpty()) {
            Criteria criteria = new Criteria();
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
            query.addCriteria(criteria);
        }

        query.with(pageable);

        long total = mongoTemplate.count(query, CustomerPotential.class);
        List<CustomerPotential> customerPotentials = mongoTemplate.find(query, CustomerPotential.class);

        return new PageImpl<>(customerPotentials, pageable, total);
    }

    @Override
    public Page<SeerPerformance> getAllSeerPerformance(
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
    ) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Month and Year filter
        if (month != null) {
            criteriaList.add(Criteria.where("month").is(month));
        }
        if (year != null) {
            criteriaList.add(Criteria.where("year").is(year));
        }

        // Performance Point filter
        if (minPerformancePoint != null) {
            criteriaList.add(Criteria.where("performance_point").gte(minPerformancePoint));
        }
        if (maxPerformancePoint != null) {
            criteriaList.add(Criteria.where("performance_point").lte(maxPerformancePoint));
        }

        // Performance Tier filter
        if (performanceTier != null) {
            criteriaList.add(Criteria.where("performance_tier").is(performanceTier.ordinal()));
        }

        // Ranking filter
        if (minRanking != null) {
            criteriaList.add(Criteria.where("ranking").gte(minRanking));
        }
        if (maxRanking != null) {
            criteriaList.add(Criteria.where("ranking").lte(maxRanking));
        }

        // Total Packages filter
        if (minTotalPackages != null) {
            criteriaList.add(Criteria.where("total_packages").gte(minTotalPackages));
        }
        if (maxTotalPackages != null) {
            criteriaList.add(Criteria.where("total_packages").lte(maxTotalPackages));
        }

        // Total Rates filter
        if (minTotalRates != null) {
            criteriaList.add(Criteria.where("total_rates").gte(minTotalRates));
        }
        if (maxTotalRates != null) {
            criteriaList.add(Criteria.where("total_rates").lte(maxTotalRates));
        }

        // Average Rating filter
        if (minAvgRating != null) {
            criteriaList.add(Criteria.where("avg_rating").gte(minAvgRating));
        }
        if (maxAvgRating != null) {
            criteriaList.add(Criteria.where("avg_rating").lte(maxAvgRating));
        }

        // Total Bookings filter
        if (minTotalBookings != null) {
            criteriaList.add(Criteria.where("total_bookings").gte(minTotalBookings));
        }
        if (maxTotalBookings != null) {
            criteriaList.add(Criteria.where("total_bookings").lte(maxTotalBookings));
        }

        // Completed Bookings filter
        if (minCompletedBookings != null) {
            criteriaList.add(Criteria.where("completed_bookings").gte(minCompletedBookings));
        }
        if (maxCompletedBookings != null) {
            criteriaList.add(Criteria.where("completed_bookings").lte(maxCompletedBookings));
        }

        // Cancelled By Seer filter
        if (minCancelledBySeer != null) {
            criteriaList.add(Criteria.where("cancelled_by_seer").gte(minCancelledBySeer));
        }
        if (maxCancelledBySeer != null) {
            criteriaList.add(Criteria.where("cancelled_by_seer").lte(maxCancelledBySeer));
        }

        // Total Revenue filter
        if (minTotalRevenue != null) {
            criteriaList.add(Criteria.where("total_revenue").gte(minTotalRevenue));
        }
        if (maxTotalRevenue != null) {
            criteriaList.add(Criteria.where("total_revenue").lte(maxTotalRevenue));
        }

        // Bonus filter
        if (minBonus != null) {
            criteriaList.add(Criteria.where("bonus").gte(minBonus));
        }
        if (maxBonus != null) {
            criteriaList.add(Criteria.where("bonus").lte(maxBonus));
        }

        if (!criteriaList.isEmpty()) {
            Criteria criteria = new Criteria();
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
            query.addCriteria(criteria);
        }

        query.with(pageable);

        long total = mongoTemplate.count(query, SeerPerformance.class);
        List<SeerPerformance> seerPerformances = mongoTemplate.find(query, SeerPerformance.class);

        return new PageImpl<>(seerPerformances, pageable, total);
    }

    @Override
    public SeerPerformance getSeerPerformance(String seerId, Integer month, Integer year) {
        return seerPerformanceRepository.findBySeerIdAndMonthAndYear(seerId, month, year);
    }

    @Override
    public SeerPerformance getMySeerPerformance(Integer month, Integer year) {
        return getSeerPerformance(authService.getCurrentUserId().toString(), month, year);
    }

    @Override
    public boolean customerAction(String customerId, Constants.CustomerAction action, BigDecimal amount) {
        try {
            int month = LocalDate.now().getMonthValue();
            int year = LocalDate.now().getYear();

            CustomerPotential customerPotential = getCustomerPotential(customerId, month, year);
            if (customerPotential == null) {
                log.error("Customer potential not found for customerId: {}, month: {}, year: {}", customerId, month, year);
                return false;
            }

            switch (action) {
                case BOOKING, SPENDING -> {
                    customerPotential.setTotalBookingRequests(customerPotential.getTotalBookingRequests() + 1);
                    customerPotential.setTotalSpending(customerPotential.getTotalSpending().add(amount));
                }
                case CANCELLING -> {
                    customerPotential.setCancelledByCustomer(customerPotential.getCancelledByCustomer() + 1);
                }
            }
            customerPotentialRepository.save(customerPotential);

            return true;
        } catch (Exception e) {
            log.error("customerAction - {}", e.getMessage());
            return false;
        }
    }


    @Override
    public boolean seerAction(String seerId, Constants.SeerAction action, BigDecimal amount) {
        try {
            int month = LocalDate.now().getMonthValue();
            int year = LocalDate.now().getYear();

            SeerPerformance seerPerformance = getSeerPerformance(seerId, month, year);
            if (seerPerformance == null) {
                log.error("Seer performance not found for seerId: {}, month: {}, year: {}", seerId, month, year);
                return false;
            }

            switch (action) {
                case RATED -> {
                    double newAvgRating = seerPerformance.getAvgRating();
                    int totalRates = seerPerformance.getTotalRates();
                    newAvgRating = (newAvgRating * totalRates + amount.doubleValue()) / (totalRates + 1);

                    seerPerformance.setTotalRates(totalRates + 1);
                    seerPerformance.setAvgRating(newAvgRating);
                }
                case EARNING -> {
                    BigDecimal currentRevenue = seerPerformance.getTotalRevenue();
                    if (currentRevenue == null) {
                        currentRevenue = BigDecimal.ZERO;
                    }
                    seerPerformance.setTotalRevenue(currentRevenue.add(amount));
                }
                case BONUS_GAINED -> {
                    BigDecimal currentBonus = seerPerformance.getBonus();
                    if (currentBonus == null) {
                        currentBonus = BigDecimal.ZERO;
                    }
                    seerPerformance.setBonus(currentBonus.add(amount));
                }
                case RECEIVED_BOOKING -> {
                    BigDecimal totalRevenue = seerPerformance.getTotalRevenue().add(amount);
                    seerPerformance.setTotalRevenue(totalRevenue);
                    seerPerformance.setTotalBookings(seerPerformance.getTotalBookings() + 1);
                }
                case CANCELLING -> {
                    seerPerformance.setCancelledBySeer(seerPerformance.getCancelledBySeer() + 1);
                }
                case COMPLETED_BOOKING -> {
                    seerPerformance.setCompletedBookings(seerPerformance.getCompletedBookings() + 1);
                }
                case CREATE_PACKAGE -> {
                    seerPerformance.setTotalPackages(seerPerformance.getTotalPackages() + 1);
                }
                default ->  {
                    log.error("seerAction not valid - {}", seerId);
                    return false;
                }
            }

            seerPerformanceRepository.save(seerPerformance);
            log.info("Saved seer performance for seerId: {}", seerId);

            return true;
        } catch (Exception e) {
            log.error("seerAction - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean customerChange(UserChangeEvent event) {
        try {
            List<CustomerPotential> customerPotentials = customerPotentialRepository.findAllByCustomerId(event.getUserId());
            for (CustomerPotential customerPotential : customerPotentials) {
                if (event.getFullName() != null && !event.getFullName().equals(""))
                    customerPotential.setFullName(event.getFullName());
                if (event.getAvatarUrl() != null && !event.getAvatarUrl().equals(""))
                    customerPotential.setAvatarUrl(event.getAvatarUrl());
            }
            customerPotentialRepository.saveAll(customerPotentials);
            return true;
        }
        catch (Exception e) {
            log.error("customer change action - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean seerChange(UserChangeEvent event) {
        try {
            List<SeerPerformance> seerPerformances = seerPerformanceRepository.findAllBySeerId(event.getUserId());
            for (SeerPerformance seerPerformance : seerPerformances) {
                if (event.getFullName() != null && !event.getFullName().equals(""))
                    seerPerformance.setFullName(event.getFullName());
                if (event.getAvatarUrl() != null && !event.getAvatarUrl().equals(""))
                    seerPerformance.setAvatarUrl(event.getAvatarUrl());
            }

            seerPerformanceRepository.saveAll(seerPerformances);
            return true;
        }
        catch (Exception e) {
            log.error("seer change action - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public FinanceStatistic getFinanceStatistic() {
        // Get current month and year
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        // Calculate previous month and year
        int previousMonth = currentMonth - 1;
        int previousYear = currentYear;
        if (previousMonth == 0) {
            previousMonth = 12;
            previousYear--;
        }

        // Calculate total revenue for current month using repository query
        BigDecimal totalRevenue = seerPerformanceRepository.findAllByMonthAndYear(currentMonth, currentYear).stream()
                .map(SeerPerformance::getTotalRevenue)
                .filter(revenue -> revenue != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total revenue for previous month using repository query
        BigDecimal previousMonthRevenue = seerPerformanceRepository.findAllByMonthAndYear(previousMonth, previousYear).stream()
                .map(SeerPerformance::getTotalRevenue)
                .filter(revenue -> revenue != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percent change compared to previous month
        Double percentChangeTotalRevenue = 0.0;
        if (previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = totalRevenue.subtract(previousMonthRevenue);
            percentChangeTotalRevenue = change.multiply(BigDecimal.valueOf(100))
                    .divide(previousMonthRevenue, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        } else if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            percentChangeTotalRevenue = 100.0;
        }

        // Calculate tax (7% of total revenue)
        BigDecimal taxRate = BigDecimal.valueOf(0.07);
        BigDecimal totalTax = totalRevenue.multiply(taxRate);

        // Calculate net (total revenue - tax)
        BigDecimal totalNet = totalRevenue.subtract(totalTax);

        // Calculate previous month net
        BigDecimal previousMonthTax = previousMonthRevenue.multiply(taxRate);
        BigDecimal previousMonthNet = previousMonthRevenue.subtract(previousMonthTax);

        // Calculate percent change for net
        Double percentChangeTotalNet = 0.0;
        if (previousMonthNet.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netChange = totalNet.subtract(previousMonthNet);
            percentChangeTotalNet = netChange.multiply(BigDecimal.valueOf(100))
                    .divide(previousMonthNet, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        } else if (totalNet.compareTo(BigDecimal.ZERO) > 0) {
            percentChangeTotalNet = 100.0;
        }

        return FinanceStatistic.builder()
                .totalRevenue(totalRevenue)
                .percentChangeTotalRevenue(percentChangeTotalRevenue)
                .totalNet(totalNet)
                .percentChangeTotalNet(percentChangeTotalNet)
                .totalTax(totalTax)
                .percentChangeTotalTax(0.0)  // Fixed at 0%
                .totalRevenueDay(BigDecimal.ZERO)  // Fixed at 0
                .percentChangeTotalRevenueDay(0.0)  // Fixed at 0%
                .build();
    }

    @Override
    public ChartDto<?> getChartDto(Constants.ChartTypeDto chartType, Integer month, Integer year) {
        // Special handling for tier contributions - returns data by tier
        if (chartType == Constants.ChartTypeDto.SEER_TIER_CONTRIBUTIONS) {
            return getSeerChartDataByTier(month, year);
        }
        if (chartType == Constants.ChartTypeDto.CUSTOMER_TIER_CONTRIBUTIONS) {
            return getCustomerChartDataByTier(month, year);
        }

        // Regular time-based charts
        if (month != null && year != null) {
            return getChartDataByDay(chartType, month, year);
        }
        else if (year != null) {
            return getChartDataByMonth(chartType, year);
        }
        else {
            return getChartDataByYear(chartType);
        }
    }

    private ChartDto<String> getSeerChartDataByTier(Integer month, Integer year) {
        Map<String, BigDecimal> dataMap = new HashMap<>();

        // Initialize all tiers with 0
        for (Constants.SeerTier tier : Constants.SeerTier.values()) {
            dataMap.put(tier.getValue(), BigDecimal.ZERO);
        }

        List<SeerPerformance> seerPerformances;

        // Get data based on provided parameters
        if (month != null && year != null) {
            seerPerformances = seerPerformanceRepository.findAllByMonthAndYear(month, year);
        } else if (year != null) {
            seerPerformances = seerPerformanceRepository.findAllByYear(year);
        } else {
            // Get all data from 2023 onwards
            int currentYear = LocalDate.now().getYear();
            seerPerformances = new ArrayList<>();
            for (int y = 2023; y <= currentYear; y++) {
                seerPerformances.addAll(seerPerformanceRepository.findAllByYear(y));
            }
        }

        // Calculate total revenue contribution by tier
        Map<Constants.SeerTier, BigDecimal> tierRevenue = seerPerformances.stream()
                .filter(sp -> sp.getPerformanceTier() != null && sp.getTotalRevenue() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        SeerPerformance::getPerformanceTier,
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO,
                                SeerPerformance::getTotalRevenue,
                                BigDecimal::add
                        )
                ));

        // Convert to string keys
        tierRevenue.forEach((tier, revenue) -> dataMap.put(tier.getValue(), revenue));

        return ChartDto.<String>builder()
                .month(month)
                .year(year)
                .data(dataMap)
                .build();
    }

    private ChartDto<String> getCustomerChartDataByTier(Integer month, Integer year) {
        Map<String, BigDecimal> dataMap = new HashMap<>();

        // Initialize all tiers with 0
        for (Constants.CustomerTier tier : Constants.CustomerTier.values()) {
            dataMap.put(tier.getValue(), BigDecimal.ZERO);
        }

        List<CustomerPotential> customerPotentials;

        // Get data based on provided parameters
        if (month != null && year != null) {
            customerPotentials = customerPotentialRepository.findAllByMonthAndYear(month, year);
        } else if (year != null) {
            customerPotentials = customerPotentialRepository.findAllByYear(year);
        } else {
            // Get all data from 2023 onwards
            int currentYear = LocalDate.now().getYear();
            customerPotentials = new ArrayList<>();
            for (int y = 2023; y <= currentYear; y++) {
                customerPotentials.addAll(customerPotentialRepository.findAllByYear(y));
            }
        }

        // Calculate total spending contribution by tier
        Map<Constants.CustomerTier, BigDecimal> tierSpending = customerPotentials.stream()
                .filter(cp -> cp.getPotentialTier() != null && cp.getTotalSpending() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        CustomerPotential::getPotentialTier,
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO,
                                CustomerPotential::getTotalSpending,
                                BigDecimal::add
                        )
                ));

        // Convert to string keys
        tierSpending.forEach((tier, spending) -> dataMap.put(tier.getValue(), spending));

        return ChartDto.<String>builder()
                .month(month)
                .year(year)
                .data(dataMap)
                .build();
    }

    private ChartDto<Integer> getChartDataByDay(Constants.ChartTypeDto chartType, Integer month, Integer year) {
        Map<Integer, BigDecimal> dataMap = new HashMap<>();

        // Get number of days in the month
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();

        // Initialize all days with 0
        for (int day = 1; day <= daysInMonth; day++) {
            dataMap.put(day, BigDecimal.ZERO);
        }

        // Get all data for the specified month and year using repository queries
        List<SeerPerformance> seerPerformances = seerPerformanceRepository.findAllByMonthAndYear(month, year);
        List<CustomerPotential> customerPotentials = customerPotentialRepository.findAllByMonthAndYear(month, year);

        // For daily data, we aggregate all data for each day (simplified - showing total for the month divided by days)
        BigDecimal totalValue = BigDecimal.ZERO;

        switch (chartType) {
            case TOTAL_REVENUE -> {
                totalValue = seerPerformances.stream()
                        .map(SeerPerformance::getTotalRevenue)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case TOTAL_BOOKING_COMPLETED -> {
                totalValue = seerPerformances.stream()
                        .map(sp -> BigDecimal.valueOf(sp.getCompletedBookings() != null ? sp.getCompletedBookings() : 0))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case TOTAL_PACKAGES -> {
                totalValue = seerPerformances.stream()
                        .map(sp -> BigDecimal.valueOf(sp.getTotalPackages() != null ? sp.getTotalPackages() : 0))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case TOTAL_BOOKING_REQUESTS -> {
                totalValue = customerPotentials.stream()
                        .map(cp -> BigDecimal.valueOf(cp.getTotalBookingRequests() != null ? cp.getTotalBookingRequests() : 0))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case AVG_SEER_REVENUE -> {
                // Calculate average revenue per seer
                BigDecimal totalRevenue = seerPerformances.stream()
                        .map(SeerPerformance::getTotalRevenue)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int seerCount = seerPerformances.size();
                totalValue = seerCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(seerCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            }
            case AVG_RATING_SEER -> {
                // Calculate average rating across all seers
                double avgRating = seerPerformances.stream()
                        .filter(sp -> sp.getAvgRating() != null)
                        .mapToDouble(SeerPerformance::getAvgRating)
                        .average()
                        .orElse(0.0);
                totalValue = BigDecimal.valueOf(avgRating);
            }
            case AVG_PERFORMANCE_POINTS_SEER -> {
                // Calculate average performance points per seer
                double avgPoints = seerPerformances.stream()
                        .filter(sp -> sp.getPerformancePoint() != null)
                        .mapToInt(SeerPerformance::getPerformancePoint)
                        .average()
                        .orElse(0.0);
                totalValue = BigDecimal.valueOf(avgPoints);
            }
            case AVG_CUSTOMER_SPENDING -> {
                // Calculate average spending per customer
                BigDecimal totalSpending = customerPotentials.stream()
                        .map(CustomerPotential::getTotalSpending)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int customerCount = customerPotentials.size();
                totalValue = customerCount > 0 ? totalSpending.divide(BigDecimal.valueOf(customerCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            }
            case AVG_CUSTOMER_POTENTIAL_POINTS -> {
                // Calculate average potential points per customer
                double avgPoints = customerPotentials.stream()
                        .filter(cp -> cp.getPotentialPoint() != null)
                        .mapToInt(CustomerPotential::getPotentialPoint)
                        .average()
                        .orElse(0.0);
                totalValue = BigDecimal.valueOf(avgPoints);
            }
        }

        // Distribute evenly across days (simplified approach)
        BigDecimal dailyAverage = daysInMonth > 0 ? totalValue.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        for (int day = 1; day <= daysInMonth; day++) {
            dataMap.put(day, dailyAverage);
        }

        return ChartDto.<Integer>builder()
                .month(month)
                .year(year)
                .data(dataMap)
                .build();
    }

    private ChartDto<Integer> getChartDataByMonth(Constants.ChartTypeDto chartType, Integer year) {
        Map<Integer, BigDecimal> dataMap = new HashMap<>();

        // Initialize all 12 months with 0
        for (int month = 1; month <= 12; month++) {
            dataMap.put(month, BigDecimal.ZERO);
        }

        // Get all data for the specified year using repository queries
        List<SeerPerformance> seerPerformances = seerPerformanceRepository.findAllByYear(year);
        List<CustomerPotential> customerPotentials = customerPotentialRepository.findAllByYear(year);

        // Aggregate data by month
        for (int month = 1; month <= 12; month++) {
            final int currentMonth = month;
            BigDecimal monthValue = BigDecimal.ZERO;

            switch (chartType) {
                case TOTAL_REVENUE -> {
                    monthValue = seerPerformances.stream()
                            .filter(sp -> sp.getMonth() != null && sp.getMonth().equals(currentMonth))
                            .map(SeerPerformance::getTotalRevenue)
                            .filter(revenue -> revenue != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case TOTAL_BOOKING_COMPLETED -> {
                    monthValue = seerPerformances.stream()
                            .filter(sp -> sp.getMonth() != null && sp.getMonth().equals(currentMonth))
                            .map(sp -> BigDecimal.valueOf(sp.getCompletedBookings() != null ? sp.getCompletedBookings() : 0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case TOTAL_PACKAGES -> {
                    monthValue = seerPerformances.stream()
                            .filter(sp -> sp.getMonth() != null && sp.getMonth().equals(currentMonth))
                            .map(sp -> BigDecimal.valueOf(sp.getTotalPackages() != null ? sp.getTotalPackages() : 0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case TOTAL_BOOKING_REQUESTS -> {
                    monthValue = customerPotentials.stream()
                            .filter(cp -> cp.getMonth() != null && cp.getMonth().equals(currentMonth))
                            .map(cp -> BigDecimal.valueOf(cp.getTotalBookingRequests() != null ? cp.getTotalBookingRequests() : 0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case AVG_SEER_REVENUE -> {
                    List<SeerPerformance> monthPerformances = seerPerformances.stream()
                            .filter(sp -> sp.getMonth() != null && sp.getMonth().equals(currentMonth))
                            .toList();
                    BigDecimal totalRevenue = monthPerformances.stream()
                            .map(SeerPerformance::getTotalRevenue)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int seerCount = monthPerformances.size();
                    monthValue = seerCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(seerCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                }
                case AVG_RATING_SEER -> {
                    double avgRating = seerPerformances.stream()
                            .filter(sp -> sp.getMonth() != null && sp.getMonth().equals(currentMonth))
                            .filter(sp -> sp.getAvgRating() != null)
                            .mapToDouble(SeerPerformance::getAvgRating)
                            .average()
                            .orElse(0.0);
                    monthValue = BigDecimal.valueOf(avgRating);
                }
                case AVG_PERFORMANCE_POINTS_SEER -> {
                    double avgPoints = seerPerformances.stream()
                            .filter(sp -> sp.getMonth() != null && sp.getMonth().equals(currentMonth))
                            .filter(sp -> sp.getPerformancePoint() != null)
                            .mapToInt(SeerPerformance::getPerformancePoint)
                            .average()
                            .orElse(0.0);
                    monthValue = BigDecimal.valueOf(avgPoints);
                }
                case AVG_CUSTOMER_SPENDING -> {
                    List<CustomerPotential> monthPotentials = customerPotentials.stream()
                            .filter(cp -> cp.getMonth() != null && cp.getMonth().equals(currentMonth))
                            .toList();
                    BigDecimal totalSpending = monthPotentials.stream()
                            .map(CustomerPotential::getTotalSpending)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int customerCount = monthPotentials.size();
                    monthValue = customerCount > 0 ? totalSpending.divide(BigDecimal.valueOf(customerCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                }
                case AVG_CUSTOMER_POTENTIAL_POINTS -> {
                    double avgPoints = customerPotentials.stream()
                            .filter(cp -> cp.getMonth() != null && cp.getMonth().equals(currentMonth))
                            .filter(cp -> cp.getPotentialPoint() != null)
                            .mapToInt(CustomerPotential::getPotentialPoint)
                            .average()
                            .orElse(0.0);
                    monthValue = BigDecimal.valueOf(avgPoints);
                }
            }

            dataMap.put(month, monthValue);
        }

        return ChartDto.<Integer>builder()
                .year(year)
                .data(dataMap)
                .build();
    }

    private ChartDto<Integer> getChartDataByYear(Constants.ChartTypeDto chartType) {
        Map<Integer, BigDecimal> dataMap = new HashMap<>();

        int currentYear = LocalDate.now().getYear();

        // Initialize from 2023 to current year with 0
        for (int year = 2023; year <= currentYear; year++) {
            dataMap.put(year, BigDecimal.ZERO);
        }

        // Get all data
        List<SeerPerformance> seerPerformances = seerPerformanceRepository.findAll();
        List<CustomerPotential> customerPotentials = customerPotentialRepository.findAll();

        // Aggregate data by year
        for (int year = 2023; year <= currentYear; year++) {
            final int currentYearIter = year;
            BigDecimal yearValue = BigDecimal.ZERO;

            switch (chartType) {
                case TOTAL_REVENUE -> {
                    yearValue = seerPerformances.stream()
                            .filter(sp -> sp.getYear() != null && sp.getYear().equals(currentYearIter))
                            .map(SeerPerformance::getTotalRevenue)
                            .filter(revenue -> revenue != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case TOTAL_BOOKING_COMPLETED -> {
                    yearValue = seerPerformances.stream()
                            .filter(sp -> sp.getYear() != null && sp.getYear().equals(currentYearIter))
                            .map(sp -> BigDecimal.valueOf(sp.getCompletedBookings() != null ? sp.getCompletedBookings() : 0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case TOTAL_PACKAGES -> {
                    yearValue = seerPerformances.stream()
                            .filter(sp -> sp.getYear() != null && sp.getYear().equals(currentYearIter))
                            .map(sp -> BigDecimal.valueOf(sp.getTotalPackages() != null ? sp.getTotalPackages() : 0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case TOTAL_BOOKING_REQUESTS -> {
                    yearValue = customerPotentials.stream()
                            .filter(cp -> cp.getYear() != null && cp.getYear().equals(currentYearIter))
                            .map(cp -> BigDecimal.valueOf(cp.getTotalBookingRequests() != null ? cp.getTotalBookingRequests() : 0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                case AVG_SEER_REVENUE -> {
                    List<SeerPerformance> yearPerformances = seerPerformances.stream()
                            .filter(sp -> sp.getYear() != null && sp.getYear().equals(currentYearIter))
                            .toList();
                    BigDecimal totalRevenue = yearPerformances.stream()
                            .map(SeerPerformance::getTotalRevenue)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int seerCount = yearPerformances.size();
                    yearValue = seerCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(seerCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                }
                case AVG_RATING_SEER -> {
                    double avgRating = seerPerformances.stream()
                            .filter(sp -> sp.getYear() != null && sp.getYear().equals(currentYearIter))
                            .filter(sp -> sp.getAvgRating() != null)
                            .mapToDouble(SeerPerformance::getAvgRating)
                            .average()
                            .orElse(0.0);
                    yearValue = BigDecimal.valueOf(avgRating);
                }
                case AVG_PERFORMANCE_POINTS_SEER -> {
                    double avgPoints = seerPerformances.stream()
                            .filter(sp -> sp.getYear() != null && sp.getYear().equals(currentYearIter))
                            .filter(sp -> sp.getPerformancePoint() != null)
                            .mapToInt(SeerPerformance::getPerformancePoint)
                            .average()
                            .orElse(0.0);
                    yearValue = BigDecimal.valueOf(avgPoints);
                }
                case AVG_CUSTOMER_SPENDING -> {
                    List<CustomerPotential> yearPotentials = customerPotentials.stream()
                            .filter(cp -> cp.getYear() != null && cp.getYear().equals(currentYearIter))
                            .toList();
                    BigDecimal totalSpending = yearPotentials.stream()
                            .map(CustomerPotential::getTotalSpending)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int customerCount = yearPotentials.size();
                    yearValue = customerCount > 0 ? totalSpending.divide(BigDecimal.valueOf(customerCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                }
                case AVG_CUSTOMER_POTENTIAL_POINTS -> {
                    double avgPoints = customerPotentials.stream()
                            .filter(cp -> cp.getYear() != null && cp.getYear().equals(currentYearIter))
                            .filter(cp -> cp.getPotentialPoint() != null)
                            .mapToInt(CustomerPotential::getPotentialPoint)
                            .average()
                            .orElse(0.0);
                    yearValue = BigDecimal.valueOf(avgPoints);
                }
            }

            dataMap.put(year, yearValue);
        }

        return ChartDto.<Integer>builder()
                .data(dataMap)
                .build();
    }


}

package com.iseeyou.foretunetelling.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeerPerformanceDto {
    private String id;
    private String seerId;
    private Integer month;
    private Integer year;
    private String performanceTier;
    private Integer performancePoint;
    private Integer ranking;
    private Integer totalPackages;
    private Integer totalRates;
    private Double avgRating;
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer cancelledBySeer;
    private Double totalRevenue;
    private Double bonus;
    private Date createdAt;
    private Date updatedAt;
    private String fullName;
    private String avatarUrl;
}


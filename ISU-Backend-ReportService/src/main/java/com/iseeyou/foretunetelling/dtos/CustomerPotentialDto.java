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
public class CustomerPotentialDto {
    private String id;
    private String customerId;
    private Integer month;
    private Integer year;
    private Integer potentialPoint;
    private String potentialTier;
    private Integer ranking;
    private Integer totalBookingRequests;
    private Double totalSpending;
    private Integer cancelledByCustomer;
    private Date createdAt;
    private Date updatedAt;
    private String fullName;
    private String avatarUrl;
}


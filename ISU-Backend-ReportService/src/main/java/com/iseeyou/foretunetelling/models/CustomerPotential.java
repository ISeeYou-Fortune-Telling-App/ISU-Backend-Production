package com.iseeyou.foretunetelling.models;

import com.iseeyou.foretunetelling.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "customer_potentials")
@Getter
@Setter
@NoArgsConstructor
public class CustomerPotential {
    @Id
    private String id;

    @Field("customer_id")
    private String customerId;

    @Field("full_name")
    private String fullName;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("month")
    private Integer month;

    @Field("year")
    private Integer year;

    @Field("potential_point")
    private Integer potentialPoint;

    @Field("potential_tier")
    private Constants.CustomerTier potentialTier;

    @Field("ranking")
    private Integer ranking;

    @Field("total_booking_requests")
    private Integer totalBookingRequests;

    @Field("total_spending")
    private BigDecimal totalSpending;

    @Field("cancelled_by_customer")
    private Integer cancelledByCustomer;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

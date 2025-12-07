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

@Document(collection = "seer_performances")
@Getter
@Setter
@NoArgsConstructor
public class SeerPerformance {
    @Id
    private String id;

    @Field("seer_id")
    private String seerId;

    @Field("full_name")
    private String fullName;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("month")
    private Integer month;

    @Field("year")
    private Integer year;

    @Field("performance_tier")
    private Constants.SeerTier performanceTier;

    @Field("performance_point")
    private Integer performancePoint;

    @Field("ranking")
    private Integer ranking;

    @Field("total_packages")
    private Integer totalPackages;

    @Field("total_rates")
    private Integer totalRates;

    @Field("avg_rating")
    private Double avgRating;

    @Field("total_bookings")
    private Integer totalBookings;

    @Field("completed_bookings")
    private Integer completedBookings;

    @Field("cancelled_by_seer")
    private Integer cancelledBySeer;

    @Field("total_revenue")
    private BigDecimal totalRevenue;

    @Field("bonus")
    private BigDecimal bonus;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}

package com.iseeyou.foretunetelling.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

public final class Constants {
    @Getter
    public enum CustomerTier {
        CASUAL("CASUAL", 0),
        STANDARD("STANDARD", 50),
        PREMIUM("PREMIUM", 70),
        VIP("VIP", 85);

        private final String value;
        private final Integer minPoint;
        private CustomerTier nextTier;
        private CustomerTier previousTier;

        CustomerTier(String value, Integer minPoint) {
            this.value = value;
            this.minPoint = minPoint;
        }

        static {
            CASUAL.nextTier = STANDARD;
            CASUAL.previousTier = CASUAL;

            STANDARD.nextTier = PREMIUM;
            STANDARD.previousTier = CASUAL;

            PREMIUM.nextTier = VIP;
            PREMIUM.previousTier = STANDARD;

            VIP.nextTier = VIP;
            VIP.previousTier = PREMIUM;
        }
    }

    @Getter
    public enum SeerTier {
        APPRENTICE("APPRENTICE", 0),
        PROFESSIONAL("PROFESSIONAL", 50),
        EXPERT("EXPERT", 70),
        MASTER("MASTER", 85);

        private final String value;
        private final Integer minPoint;
        private SeerTier nextTier;
        private SeerTier previousTier;

        SeerTier(String value, Integer minPoint) {
            this.value = value;
            this.minPoint = minPoint;
        }

        static {
            APPRENTICE.nextTier = PROFESSIONAL;
            APPRENTICE.previousTier = APPRENTICE;

            PROFESSIONAL.nextTier = EXPERT;
            PROFESSIONAL.previousTier = APPRENTICE;

            EXPERT.nextTier = MASTER;
            EXPERT.previousTier = PROFESSIONAL;

            MASTER.nextTier = MASTER;
            MASTER.previousTier = EXPERT;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum CustomerAction {
        BOOKING("BOOKING"),
        SPENDING("SPENDING"),
        CANCELLING("CANCELLING");

        private final String value;
    }

    @Getter
    @AllArgsConstructor
    public enum SeerAction {
        CREATE_PACKAGE("CREATE_PACKAGE"),
        RATED("RATED"),
        RECEIVED_BOOKING("RECEIVED_BOOKING"),
        COMPLETED_BOOKING("COMPLETED_BOOKING"),
        CANCELLING("CANCELLING"),
        EARNING("EARNING"),
        BONUS_GAINED("BONUS_GAINED"),;

        private final String value;
    }

    @Getter
    @AllArgsConstructor
    public enum ChartTypeDto {
        TOTAL_REVENUE("TOTAL_REVENUE"),
        TOTAL_BOOKING_REQUESTS("TOTAL_BOOKING_REQUESTS"),
        TOTAL_BOOKING_COMPLETED("TOTAL_TRANSACTIONS"),
        TOTAL_PACKAGES("TOTAL_PACKAGES"),
        AVG_SEER_REVENUE("AVG_SEER_REVENUE"),
        AVG_RATING_SEER("AVG_RATING_SEER"),
        SEER_TIER_CONTRIBUTIONS("SEER_TIER_CONTRIBUTIONS"),
        AVG_PERFORMANCE_POINTS_SEER("AVG_PERFORMANCE_POINTS_SEER"),
        AVG_CUSTOMER_SPENDING("AVG_CUSTOMER_SPENDING"),
        AVG_CUSTOMER_POTENTIAL_POINTS("AVG_CUSTOMER_POTENTIAL_POINTS"),
        CUSTOMER_TIER_CONTRIBUTIONS("CUSTOMER_TIER_CONTRIBUTIONS");

        private final String value;
    }
}

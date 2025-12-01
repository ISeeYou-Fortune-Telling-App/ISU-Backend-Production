package com.iseeyou.fortunetelling.service.report;

import com.iseeyou.fortunetelling.envent.dto.SeerNewRatingEvent;
import com.iseeyou.fortunetelling.util.Constants;

import java.math.BigDecimal;

public interface ReportMicroservice {
    SeerNewRatingEvent getSeerSimpleRating(String seerId, Integer month, Integer year);

    boolean customerAction(String customerId, Constants.CustomerAction action, BigDecimal amount);
    boolean seerAction(String seerId, Constants.SeerAction action, BigDecimal amount);
    boolean customerChange(String customerId, String fullName, String avatarUrl);
    boolean seerChange(String seerId, String fullName, String avatarUrl);

}


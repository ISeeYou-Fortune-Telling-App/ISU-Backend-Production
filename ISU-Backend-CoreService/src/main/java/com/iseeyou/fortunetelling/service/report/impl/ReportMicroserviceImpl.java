package com.iseeyou.fortunetelling.service.report.impl;

import com.iseeyou.fortunetelling.envent.dto.SeerNewRatingEvent;
import com.iseeyou.fortunetelling.envent.ReportEventPublisher;
import com.iseeyou.fortunetelling.envent.dto.UserActionEvent;
import com.iseeyou.fortunetelling.envent.dto.UserChangeEvent;
import com.iseeyou.fortunetelling.service.report.ReportMicroservice;
import com.iseeyou.fortunetelling.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportMicroserviceImpl implements ReportMicroservice {

    private final ReportEventPublisher reportEventPublisher;

    @Override
    public SeerNewRatingEvent getSeerSimpleRating(String seerId, Integer month, Integer year) {
        return null;
    }

    @Override
    public boolean customerAction(String customerId, Constants.CustomerAction action, BigDecimal amount) {
        try {
            UserActionEvent userActionEvent = new UserActionEvent();
            userActionEvent.setUserId(customerId);
            userActionEvent.setRole("USER");
            userActionEvent.setAction(action.toString());
            userActionEvent.setAmount(amount);
            reportEventPublisher.userActionEvent(userActionEvent);
            return true;
        } catch (Exception e) {
            log.error("Customer Action Error{}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean seerAction(String seerId, Constants.SeerAction action, BigDecimal amount) {
        try {
            UserActionEvent userActionEvent = new UserActionEvent();
            userActionEvent.setUserId(seerId);
            userActionEvent.setRole("SEER");
            userActionEvent.setAction(action.toString());
            userActionEvent.setAmount(amount);
            reportEventPublisher.userActionEvent(userActionEvent);
            return true;
        } catch (Exception e) {
            log.error("Seer Action Error{}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean customerChange(String customerId, String fullName, String avatarUrl) {
        try {
            UserChangeEvent userChangeEvent = new UserChangeEvent();
            userChangeEvent.setUserId(customerId);
            userChangeEvent.setRole("CUSTOMER");
            userChangeEvent.setFullName(fullName);
            userChangeEvent.setAvatarUrl(avatarUrl);
            reportEventPublisher.userChangeEvent(userChangeEvent);

            return true;
        } catch (Exception e) {
            log.error("Customer Change Error{}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean seerChange(String seerId, String fullName, String avatarUrl) {
        try {
            UserChangeEvent userChangeEvent = new UserChangeEvent();
            userChangeEvent.setUserId(seerId);
            userChangeEvent.setRole("SEER");
            userChangeEvent.setFullName(fullName);
            userChangeEvent.setAvatarUrl(avatarUrl);
            reportEventPublisher.userChangeEvent(userChangeEvent);
            return true;
        } catch (Exception e) {
            log.error("Seer Change Error{}", e.getMessage());
            return false;
        }
    }
}

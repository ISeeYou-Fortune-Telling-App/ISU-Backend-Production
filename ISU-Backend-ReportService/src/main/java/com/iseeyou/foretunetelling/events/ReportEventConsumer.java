package com.iseeyou.foretunetelling.events;

import com.iseeyou.foretunetelling.events.dto.UserActionEvent;
import com.iseeyou.foretunetelling.events.dto.UserChangeEvent;
import com.iseeyou.foretunetelling.services.ReportService;
import com.iseeyou.foretunetelling.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportEventConsumer {
    private final ReportService reportService;

    @RabbitListener(queues = "user.change.queue")
    public void handleUserChangeEvent(UserChangeEvent event) {
        try {
            log.info("Processing user change event: {}", event.getEventId());
            if (Objects.equals(event.getRole(), "CUSTOMER")) {
                reportService.customerChange(event);
            } else
                reportService.seerChange(event);
        } catch (Exception e) {
            log.error("Error processing user change event: {}", event.getEventId(), e);
        }
    }

    @RabbitListener(queues = "user.action.queue")
    public void handleUserActionEvent(UserActionEvent event) {
        try {
            log.info("Processing user action event: {}", event.getEventId());
            if (Objects.equals(event.getRole(), "CUSTOMER")) {
                reportService.customerAction(
                        event.getUserId(),
                        Constants.CustomerAction.valueOf(event.getAction()),
                        event.getAmount()
                );
            } else {
                reportService.seerAction(
                        event.getUserId(),
                        Constants.SeerAction.valueOf(event.getAction()),
                        event.getAmount()
                );
            }
        } catch (Exception e) {
            log.error("Error processing user change event: {}", event.getEventId(), e);
        }
    }
}

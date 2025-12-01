package com.iseeyou.fortunetelling.envent;

import com.iseeyou.fortunetelling.envent.dto.UserActionEvent;
import com.iseeyou.fortunetelling.envent.dto.UserChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void userChangeEvent(UserChangeEvent event) {
        try {
            rabbitTemplate.convertAndSend("report.exchange", "user.change", event);
        } catch (Exception e) {
            log.error("Failed to publish user change event: {}", e.getMessage(), e);
        }
    }

    public void userActionEvent(UserActionEvent event) {
        try {
            rabbitTemplate.convertAndSend("report.exchange", "user.action", event);
        } catch (Exception e) {
            log.error("Failed to publish user action event: {}", e.getMessage(), e);
        }
    }
}

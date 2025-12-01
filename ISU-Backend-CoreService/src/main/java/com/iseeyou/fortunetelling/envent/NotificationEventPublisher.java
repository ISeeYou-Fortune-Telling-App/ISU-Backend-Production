package com.iseeyou.fortunetelling.envent;

import com.iseeyou.fortunetelling.envent.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:notification.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key:notification.send}")
    private String routingKey;

    public void publishNotificationEvent(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info("Published notification event: {} for recipient: {}",
                    event.getTargetType().getValue(),
                    event.getRecipientId() != null ? event.getRecipientId() : event.getFcmToken());
        } catch (Exception e) {
            log.error("Failed to publish notification event: {}", e.getMessage(), e);
        }
    }
}
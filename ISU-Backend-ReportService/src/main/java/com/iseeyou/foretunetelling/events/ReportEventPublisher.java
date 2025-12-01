package com.iseeyou.foretunetelling.events;

import com.iseeyou.foretunetelling.events.dto.SeerNewRatingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishNewSeerRatingEvent(SeerNewRatingEvent event) {
        try {
            rabbitTemplate.convertAndSend("report.exchange", "seer.rating", event);
            log.info("Published new seer rating event: {} for seer: {}",
                    event.getEventId(),
                    event.getSeerId());
        } catch (Exception e) {
            log.error("Failed to publish new seer rating event: {}", e.getMessage(), e);
        }
    }
}

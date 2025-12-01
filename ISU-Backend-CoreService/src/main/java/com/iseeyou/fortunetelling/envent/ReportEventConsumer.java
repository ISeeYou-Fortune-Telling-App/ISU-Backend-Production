package com.iseeyou.fortunetelling.envent;

import com.iseeyou.fortunetelling.envent.dto.SeerNewRatingEvent;
import com.iseeyou.fortunetelling.service.user.UserService;
import com.iseeyou.fortunetelling.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportEventConsumer {

    private final UserService userService;

    @RabbitListener(queues = "seer.rating.queue")
    public void handleSeerNewRatingEvent(SeerNewRatingEvent event) {
        try {
            if (event == null) {
                log.error("Received null SeerNewRatingEvent");
                return;
            }
            if (event.getSeerId() == null || event.getTotalRates() == null || event.getAvgRating() == null || event.getSeerTier() == null) {
                log.error("SeerNewRatingEvent has null fields: {}", event);
                return;
            }
            log.info("Processing new seer rating change event: {}", event.getEventId());
               userService.updateSeerProfile(
                       UUID.fromString(event.getSeerId()),
                       event.getTotalRates(),
                       event.getAvgRating(),
                       Constants.SeerTier.get(event.getSeerTier())
               );
        } catch (Exception e) {
            log.error("Error processing new seer rating change event: {}", event != null ? event.getEventId() : "null event", e);
        }
    }
}

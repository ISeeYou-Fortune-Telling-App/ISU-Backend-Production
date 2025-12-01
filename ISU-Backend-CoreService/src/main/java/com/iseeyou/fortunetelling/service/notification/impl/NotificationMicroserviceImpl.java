package com.iseeyou.fortunetelling.service.notification.impl;

import com.iseeyou.fortunetelling.envent.dto.NotificationEvent;
import com.iseeyou.fortunetelling.envent.NotificationEventPublisher;
import com.iseeyou.fortunetelling.service.notification.NotificationMicroservice;
import com.iseeyou.fortunetelling.service.user.UserService;
import com.iseeyou.fortunetelling.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationMicroserviceImpl implements NotificationMicroservice {

    private final UserService userService;
    private final NotificationEventPublisher eventPublisher;

    @Override
    public boolean sendNotification(
            String recipientId,
            String notificationTitle,
            String notificationBody,
            Constants.TargetType targetType,
            String targetId,
            String imageUrl,
            Map<String, String> metaData
    ) {
        try {
            NotificationEvent notificationEvent = new NotificationEvent();
            notificationEvent.setRecipientId(recipientId);
            notificationEvent.setFcmToken(userService.findById(UUID.fromString(recipientId)).getFcmToken());
            notificationEvent.setNotificationTitle(notificationTitle);
            notificationEvent.setNotificationBody(notificationBody);
            notificationEvent.setTargetType(targetType);
            notificationEvent.setTargetId(targetId);
            notificationEvent.setImageUrl(imageUrl);
            notificationEvent.setMetaData(metaData);

            eventPublisher.publishNotificationEvent(notificationEvent);

            return true;
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}


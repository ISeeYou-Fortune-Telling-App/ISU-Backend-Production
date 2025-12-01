package com.iseeyou.fortunetelling.service.notification;

import com.iseeyou.fortunetelling.util.Constants;

import java.util.Map;

public interface NotificationMicroservice {
    /**
     * Send notification to a user via Notification Microservice
     * @param recipientId ID of the recipient user
     * @param notificationTitle Title of the notification
     * @param notificationBody Body of the notification
     * @param targetType Type of the target (BOOKING, REPORT, USER, ACCOUNT)
     * @param targetId ID of the target
     * @param imageUrl Optional image URL
     * @param metaData Optional metadata
     * @return true if notification sent successfully, false otherwise
     */
    boolean sendNotification(
            String recipientId,
            String notificationTitle,
            String notificationBody,
            Constants.TargetType targetType,
            String targetId,
            String imageUrl,
            Map<String, String> metaData
    );
}


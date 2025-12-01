package com.iseeyou.fortunetelling.envent;

import com.iseeyou.fortunetelling.envent.dto.UserDeleteEvent;
import com.iseeyou.fortunetelling.envent.dto.UserLoginEvent;
import com.iseeyou.fortunetelling.envent.dto.UserLogoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service để publish các events liên quan đến User lifecycle
 * Các events này sẽ được Push Notification microservice lắng nghe để quản lý FCM tokens
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String USER_EXCHANGE = "user.exchange";
    private static final String LOGIN_ROUTING_KEY = "user.login";
    private static final String LOGOUT_ROUTING_KEY = "user.logout";
    private static final String DELETE_ROUTING_KEY = "user.delete";

    /**
     * Publish event khi user login thành công
     * Push Notification service sẽ lưu FCM token cho user
     *
     * @param userId ID của user
     * @param fcmToken FCM token từ thiết bị
     */
    public void publishLoginEvent(String userId, String fcmToken) {
        try {
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(userId)
                    .fcmToken(fcmToken)
                    .build();

            rabbitTemplate.convertAndSend(USER_EXCHANGE, LOGIN_ROUTING_KEY, event);
            log.info("Published user login event for userId: {} with FCM token", userId);
        } catch (Exception e) {
            log.error("Failed to publish login event for userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Publish event khi user logout
     * Push Notification service sẽ xóa FCM token tương ứng
     *
     * @param userId ID của user
     * @param fcmToken FCM token cần xóa
     */
    public void publishLogoutEvent(String userId, String fcmToken) {
        try {
            UserLogoutEvent event = UserLogoutEvent.builder()
                    .userId(userId)
                    .fcmToken(fcmToken)
                    .build();

            rabbitTemplate.convertAndSend(USER_EXCHANGE, LOGOUT_ROUTING_KEY, event);
            log.info("Published user logout event for userId: {} with FCM token", userId);
        } catch (Exception e) {
            log.error("Failed to publish logout event for userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Publish event khi user bị xóa
     * Push Notification service sẽ xóa user và tất cả FCM tokens liên quan
     *
     * @param userId ID của user bị xóa
     */
    public void publishUserDeleteEvent(String userId) {
        try {
            UserDeleteEvent event = UserDeleteEvent.builder()
                    .userId(userId)
                    .build();

            rabbitTemplate.convertAndSend(USER_EXCHANGE, DELETE_ROUTING_KEY, event);
            log.info("Published user delete event for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish user delete event for userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }
}


package com.iseeyou.fortunetelling.envent.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Event được publish khi user login thành công
 * Push Notification service sẽ lắng nghe và lưu FCM token
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID của user đăng nhập
     */
    private String userId;

    /**
     * FCM Token để gửi push notification
     */
    private String fcmToken;
}


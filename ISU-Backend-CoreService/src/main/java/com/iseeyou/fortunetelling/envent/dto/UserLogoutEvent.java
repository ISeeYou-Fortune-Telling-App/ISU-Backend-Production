package com.iseeyou.fortunetelling.envent.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Event được publish khi user logout
 * Push Notification service sẽ lắng nghe và xóa FCM token tương ứng
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLogoutEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID của user đăng xuất
     */
    private String userId;

    /**
     * FCM Token cần xóa
     */
    private String fcmToken;
}


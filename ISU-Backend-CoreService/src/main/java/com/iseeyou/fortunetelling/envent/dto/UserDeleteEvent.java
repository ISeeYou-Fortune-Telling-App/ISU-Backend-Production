package com.iseeyou.fortunetelling.envent.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Event được publish khi user bị xóa
 * Push Notification service sẽ lắng nghe và xóa user cùng tất cả FCM tokens liên quan
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeleteEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID của user bị xóa
     */
    private String userId;
}


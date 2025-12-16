package com.iseeyou.fortunetelling.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Field("fcm_tokens")
    private List<String> fcmTokens = new ArrayList<>();

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    public void addFcmToken(String token) {
        if (this.fcmTokens == null) {
            this.fcmTokens = new ArrayList<>();
        }
        if (!this.fcmTokens.contains(token)) {
            this.fcmTokens.add(token);
        }
    }

    public void removeFcmToken(String token) {
        if (this.fcmTokens != null) {
            this.fcmTokens.remove(token);
        }
    }
}

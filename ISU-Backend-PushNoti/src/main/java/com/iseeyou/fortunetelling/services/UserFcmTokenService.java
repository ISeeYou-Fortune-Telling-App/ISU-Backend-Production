package com.iseeyou.fortunetelling.services;

import java.util.List;

public interface UserFcmTokenService {
    void addFcmToken(String userId, String fcmToken);
    void removeFcmToken(String userId, String fcmToken);
    void deleteUser(String userId);
    List<String> getFcmTokensByUserId(String userId);
}


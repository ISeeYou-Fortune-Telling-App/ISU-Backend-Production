package com.iseeyou.fortunetelling.services.impl;

import com.iseeyou.fortunetelling.models.User;
import com.iseeyou.fortunetelling.repositories.UserRepository;
import com.iseeyou.fortunetelling.services.UserFcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFcmTokenServiceImpl implements UserFcmTokenService {
    private final UserRepository userRepository;

    @Override
    public void addFcmToken(String userId, String fcmToken) {
        log.info("Adding FCM token for user: {}", userId);

        // TODO: QUESTION: Database may have duplicate userId records from previous bug.
        // Current: Uses findFirstByUserId which returns first match.
        // Consider: Run cleanup script to merge/remove duplicates.
        User user = userRepository.findFirstByUserId(userId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUserId(userId);
                    log.info("Creating new user record for userId: {}", userId);
                    return newUser;
                });

        user.addFcmToken(fcmToken);
        User savedUser = userRepository.save(user);

        log.info("FCM token added successfully. User {} (dbId: {}) now has {} token(s): {}",
                userId, savedUser.getId(), savedUser.getFcmTokens().size(), savedUser.getFcmTokens());

    }

    @Override
    public void removeFcmToken(String userId, String fcmToken) {
        log.info("Removing FCM token for user: {}", userId);

        userRepository.findFirstByUserId(userId)
                .map(user -> {
                    user.removeFcmToken(fcmToken);
                    User savedUser = userRepository.save(user);
                    log.info("FCM token removed successfully. User {} (dbId: {}) now has {} token(s): {}",
                            userId, savedUser.getId(), savedUser.getFcmTokens().size(), savedUser.getFcmTokens());
                    return savedUser;
                })
                .orElseGet(() -> {
                    log.warn("User {} not found, cannot remove FCM token", userId);
                    return null;
                });
    }

    @Override
    @Async
    public void deleteUser(String userId) {
        log.info("Deleting user and all FCM tokens: {}", userId);

        userRepository.findFirstByUserId(userId)
                .ifPresentOrElse(
                        user -> {
                            userRepository.delete(user);
                            log.info("User {} and all associated FCM tokens deleted successfully", userId);
                        },
                        () -> log.warn("User {} not found, cannot delete", userId));
    }

    @Override
    public List<String> getFcmTokensByUserId(String userId) {
        List<String> tokens = userRepository.findFirstByUserId(userId)
                .map(user -> {
                    log.info("Retrieved FCM tokens for user {} (dbId: {}): {} token(s): {}",
                            userId, user.getId(), user.getFcmTokens().size(), user.getFcmTokens());
                    return user.getFcmTokens();
                })
                .orElseGet(() -> {
                    log.warn("No user record found for userId: {}, returning empty token list", userId);
                    return Collections.emptyList();
                });

        log.info("getFcmTokensByUserId for {}: returning {} tokens", userId, tokens.size());
        return tokens;
    }
}

package com.iseeyou.fortunetelling.service.auth.impl;

import com.iseeyou.fortunetelling.exception.AccountBannedException;
import com.iseeyou.fortunetelling.exception.EmailNotVerifiedException;
import com.iseeyou.fortunetelling.service.email.EmailVerificationService;
import com.iseeyou.fortunetelling.envent.UserEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.iseeyou.fortunetelling.dto.response.auth.TokenResponse;
import com.iseeyou.fortunetelling.entity.auth.JwtToken;
import com.iseeyou.fortunetelling.entity.user.User;
import com.iseeyou.fortunetelling.exception.NotFoundException;
import com.iseeyou.fortunetelling.exception.RefreshTokenExpiredException;
import com.iseeyou.fortunetelling.security.JwtTokenProvider;
import com.iseeyou.fortunetelling.security.JwtUserDetails;
import com.iseeyou.fortunetelling.service.MessageSourceService;
import com.iseeyou.fortunetelling.service.auth.AuthService;
import com.iseeyou.fortunetelling.service.auth.JwtTokenService;
import com.iseeyou.fortunetelling.service.user.UserService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.iseeyou.fortunetelling.util.Constants.TOKEN_HEADER;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserService userService;

    private final JwtTokenService jwtTokenService;

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final HttpServletRequest httpServletRequest;

    private final MessageSourceService messageSourceService;

    private final EmailVerificationService emailVerificationService;

    private final UserEventPublisher userEventPublisher;

    /**
     * Authenticate user.
     *
     * @param email      String
     * @param password   String
     * @param rememberMe Boolean
     * @return TokenResponse
     */
    @Override
    @Transactional
    public TokenResponse login(String email, final String password, String fcmToken, final Boolean rememberMe) {
        log.info("Login request received: {}", email);

        String badCredentialsMessage = messageSourceService.get("Unauthorized");

        User user;
        try {
            user = userService.findByEmail(email);
            email = user.getEmail();
        } catch (NotFoundException e) {
            log.error("User not found with email: {}", email);
            throw new AuthenticationCredentialsNotFoundException(badCredentialsMessage);
        }

        // Kiểm tra tài khoản bị ban vĩnh viễn
        if (user.getIsBanned() != null && user.getIsBanned()) {
            log.warn("Banned user attempted to login: {}", email);
            String banReason = user.getBanReason() != null ? user.getBanReason() : "Tài khoản bị khóa vĩnh viễn";
            throw new AccountBannedException("Tài khoản của bạn đã bị khóa vĩnh viễn", banReason);
        }

        // Kiểm tra tài khoản bị tạm khóa
        if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(LocalDateTime.now())) {
            log.warn("Suspended user attempted to login: {}", email);
            String suspensionReason = user.getSuspensionReason() != null ? user.getSuspensionReason() : "Tài khoản bị tạm khóa";
            String message = String.format("Tài khoản của bạn bị tạm khóa đến %s",
                user.getSuspendedUntil().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            throw new AccountBannedException(message, suspensionReason);
        }

        // Kiểm tra email đã được xác thực chưa
        if (!user.getIsActive()) {
            log.warn("User attempted to login with unverified email: {}", email);
            // Gửi OTP xác thực
            emailVerificationService.sendVerificationEmail(email);
            throw new EmailNotVerifiedException("Email chưa được xác thực. Vui lòng kiểm tra email để lấy mã OTP xác thực.");
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(email, password);
        
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        JwtUserDetails jwtUserDetails = jwtTokenProvider.getPrincipal(authentication);

        // Active the user
        userService.activateUserByEmail(email);

        if (fcmToken != null && !fcmToken.isEmpty()) {
            userService.setFcmTokenByEmail(email, fcmToken);

            // Publish login event để Push Notification service lưu FCM token
            userEventPublisher.publishLoginEvent(user.getId().toString(), fcmToken);
            log.info("Published login event for user: {} with FCM token", user.getId());
        }

        return generateTokens(UUID.fromString(jwtUserDetails.getId()), rememberMe);
    }

    /**
     * Refresh from bearer string.
     *
     * @param bearer String
     * @return TokenResponse
     */
    @Override
    @Transactional
    public TokenResponse refreshFromBearerString(final String bearer) {
        return refresh(jwtTokenProvider.extractJwtFromBearerString(bearer));
    }

    /**
     * Logout from bearer string by user.
     *
     * @param user   User
     * @param bearer String
     */
    @Override
    @Transactional
    public void logout(User user, final String bearer) {
        JwtToken jwtToken = jwtTokenService.findByTokenOrRefreshToken(
                jwtTokenProvider.extractJwtFromBearerString(bearer), "");

        if (!user.getId().equals(jwtToken.getUserId())) {
            log.error("User id: {} is not equal to token user id: {}", user.getId(), jwtToken.getUserId());
            throw new AuthenticationCredentialsNotFoundException(messageSourceService.get("bad_credentials"));
        }

        // Lấy FCM token trước khi logout để publish event
        String fcmToken = user.getFcmToken();

        jwtTokenService.delete(jwtToken);

        // Publish logout event để Push Notification service xóa FCM token
        if (fcmToken != null && !fcmToken.isEmpty()) {
            userEventPublisher.publishLogoutEvent(user.getId().toString(), fcmToken);
            log.info("Published logout event for user: {} with FCM token", user.getId());
        }
    }

    @Override
    @Transactional
    public void logout(User user) {
        logout(user, httpServletRequest.getHeader(TOKEN_HEADER));
    }

    @Override
    @Transactional
    public TokenResponse refresh(final String refreshToken) {
        log.info("Refresh request received: {}", refreshToken);

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.error("Refresh token is expired.");
            throw new RefreshTokenExpiredException();
        }

        User user = jwtTokenProvider.getUserFromToken(refreshToken);
        JwtToken oldToken = jwtTokenService.findByUserIdAndRefreshToken(user.getId(), refreshToken);

        boolean rememberMe = false;
        if (oldToken != null) {
            jwtTokenService.delete(oldToken);
        }

        return generateTokens(user.getId(), rememberMe); // Tạo và trả về token mới cho người dùng
    }

    /**
     * Đây là hàm dùng để sinh ra access token và refresh token mới cho người dùng, đồng thời lưu thông tin token vào database.
     */
    @Override
    @Transactional
    public TokenResponse generateTokens(final UUID id, final Boolean rememberMe) {
        String token = jwtTokenProvider.generateJwt(id.toString());
        String refreshToken = jwtTokenProvider.generateRefresh(id.toString());

        jwtTokenService.save(JwtToken.builder()
                .userId(id)
                .token(token)
                .refreshToken(refreshToken)
                .tokenTimeToLive(jwtTokenProvider.getRefreshTokenExpiresIn())
                .build());
        log.info("Token generated for user: {}", id);
        log.info("Token id: {}", id);


        User user = userService.findById(id);
        String role = user.getRole().getValue();

        return TokenResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(String.valueOf(id))
                .role(role)
                .build();
    }
}
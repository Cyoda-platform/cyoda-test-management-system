package com.java_template.application.auth;

import com.java_template.application.entity.UserEntity;
import com.java_template.application.service.UserService;
import com.java_template.common.dto.EntityWithMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String ACTIVE_STATE = "ACTIVE";

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(JwtTokenProvider tokenProvider, UserService userService) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse authenticate(String username, String password) {
        EntityWithMetadata<UserEntity> userRecord;
        try {
            userRecord = userService.findByUsername(username).orElse(null);
        } catch (Exception e) {
            log.warn("User lookup failed for '{}': {}", username, e.getMessage());
            return null;
        }

        if (userRecord == null) {
            return null;
        }

        if (!ACTIVE_STATE.equals(userRecord.getState())) {
            log.debug("Login rejected for '{}': account state is '{}'", username, userRecord.getState());
            return null;
        }

        UserEntity user = userRecord.entity();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return null;
        }

        String userId = userRecord.getId().toString();
        String role = user.getRoles().isEmpty() ? "" : user.getRoles().get(0);
        String token = tokenProvider.generateToken(userId, role);
        long expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000L);

        return new LoginResponse(token, username, role, expiresAt);
    }

    public static class LoginResponse {
        public String token;
        public String username;
        public String role;
        public long expiresAt;

        public LoginResponse(String token, String username, String role, long expiresAt) {
            this.token = token;
            this.username = username;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }
}

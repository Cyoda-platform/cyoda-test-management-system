package com.java_template.application.auth;

import org.springframework.stereotype.Component;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple JWT-like token provider using Base64 encoding
 * Hardcoded secret key and 24-hour expiration
 */
@Component
public class JwtTokenProvider {
    private static final String SECRET = "java-template-secret-key-12345";
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours
    private final Map<String, TokenData> tokenStore = new HashMap<>();

    /**
     * Generate a token for the given username and role
     */
    public String generateToken(String username, String role) {
        long issuedAt = System.currentTimeMillis();
        long expiresAt = issuedAt + EXPIRATION_TIME;

        String payload = username + "|" + role + "|" + issuedAt + "|" + expiresAt;
        String token = Base64.getEncoder().encodeToString(payload.getBytes());

        tokenStore.put(token, new TokenData(username, role, issuedAt, expiresAt));
        return token;
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        TokenData data = tokenStore.get(token);
        if (data == null) {
            return false;
        }
        return data.expiresAt > System.currentTimeMillis();
    }

    /**
     * Get username from token
     * Falls back to decoding from token if not in store (e.g., after server restart)
     */
    public String getUsernameFromToken(String token) {
        // Try to get from store first
        TokenData data = tokenStore.get(token);
        if (data != null && data.expiresAt > System.currentTimeMillis()) {
            return data.username;
        }

        // Fallback: decode from token (Base64 format: username|role|issuedAt|expiresAt)
        try {
            String payload = new String(Base64.getDecoder().decode(token));
            String[] parts = payload.split("\\|");
            if (parts.length >= 2) {
                long expiresAt = Long.parseLong(parts[3]);
                if (expiresAt > System.currentTimeMillis()) {
                    return parts[0]; // Return username
                }
            }
        } catch (Exception e) {
            // Token is not valid Base64 or wrong format
        }

        return null;
    }

    /**
     * Get role from token
     * Falls back to decoding from token if not in store (e.g., after server restart)
     */
    public String getRoleFromToken(String token) {
        // Try to get from store first
        TokenData data = tokenStore.get(token);
        if (data != null && data.expiresAt > System.currentTimeMillis()) {
            return data.role;
        }

        // Fallback: decode from token (Base64 format: username|role|issuedAt|expiresAt)
        try {
            String payload = new String(Base64.getDecoder().decode(token));
            String[] parts = payload.split("\\|");
            if (parts.length >= 2) {
                long expiresAt = Long.parseLong(parts[3]);
                if (expiresAt > System.currentTimeMillis()) {
                    return parts[1]; // Return role
                }
            }
        } catch (Exception e) {
            // Token is not valid Base64 or wrong format
        }

        return null;
    }

    private static class TokenData {
        String username;
        String role;
        long issuedAt;
        long expiresAt;

        TokenData(String username, String role, long issuedAt, long expiresAt) {
            this.username = username;
            this.role = role;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }
    }
}


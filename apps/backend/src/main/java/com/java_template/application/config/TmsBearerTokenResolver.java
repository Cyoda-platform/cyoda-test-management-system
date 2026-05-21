package com.java_template.application.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves a bearer token from the auth-token httpOnly cookie (preferred)
 * or the Authorization: Bearer header (fallback for API clients / Swagger UI).
 */
@Component
public class TmsBearerTokenResolver implements BearerTokenResolver {

    private static final String COOKIE_NAME = "auth-token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public String resolve(HttpServletRequest request) {
        String fromCookie = extractFromCookie(request);
        if (fromCookie != null) {
            return fromCookie;
        }
        return extractFromHeader(request);
    }

    private String extractFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extractFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

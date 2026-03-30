package com.java_template.application.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Authorization filter for API endpoints.
 * Accepts tokens via:
 *   1. httpOnly cookie "auth-token" (preferred — set by /auth/login)
 *   2. Authorization: Bearer <token> header (kept for backwards compatibility / Swagger UI)
 */
public class AuthorizationFilter implements Filter {

    private static final String COOKIE_NAME = "auth-token";

    private final JwtTokenProvider tokenProvider;

    public AuthorizationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Allow public endpoints without a token
        if (isPublicEndpoint(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. Try httpOnly cookie first
        String token = extractFromCookie(httpRequest);

        // 2. Fall back to Authorization: Bearer header (Swagger UI / API clients)
        if (token == null) {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            unauthorized(httpResponse, "No authentication token provided");
            return;
        }

        if (!tokenProvider.validateToken(token)) {
            unauthorized(httpResponse, "Invalid or expired token");
            return;
        }

        // Attach user context to request attributes for use in controllers
        httpRequest.setAttribute("username", tokenProvider.getUsernameFromToken(token));
        httpRequest.setAttribute("role", tokenProvider.getRoleFromToken(token));

        chain.doFilter(request, response);
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

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/auth/login") ||
               path.contains("/auth/logout") ||
               path.contains("/actuator") ||
               path.contains("/health") ||
               path.contains("/swagger") ||
               path.contains("/api-docs") ||
               path.contains("/webjars") ||
               path.equals("/api") ||
               path.equals("/api/");
    }
}

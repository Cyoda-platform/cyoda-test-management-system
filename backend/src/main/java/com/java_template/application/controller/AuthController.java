package com.java_template.application.controller;

import com.java_template.application.auth.AuthService;
import com.java_template.application.dto.LoginRequest;
import com.java_template.application.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * REST controller for Authentication operations
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    static final String COOKIE_NAME = "auth-token";
    static final int COOKIE_MAX_AGE = 24 * 60 * 60; // 24 hours

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password",
               description = "Authenticates user and sets an httpOnly cookie. Returns username and role.")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {

        AuthService.LoginResponse authResponse = authService.authenticate(
                request.getUsername(), request.getPassword());

        if (authResponse == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        // Set httpOnly cookie — JS cannot read the token
        Cookie cookie = new Cookie(COOKIE_NAME, authResponse.token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");  // Allow cookie in cross-site requests (for Vite proxy)
        httpResponse.addCookie(cookie);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(authResponse.expiresAt),
                ZoneId.systemDefault());

        // Return only non-sensitive user info in body — not the token
        return ResponseEntity.ok(new LoginResponse(
                null,
                authResponse.username,
                authResponse.role,
                expiresAt));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Clears the auth cookie.")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // delete immediately
        cookie.setAttribute("SameSite", "Lax");  // Match login cookie settings
        httpResponse.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user",
               description = "Returns username and role for the authenticated session.")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        String role = (String) request.getAttribute("role");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(Map.of("username", username, "role", role));
    }
}

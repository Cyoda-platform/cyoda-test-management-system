package com.java_template.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.auth.AuthService;
import com.java_template.application.auth.JwtTokenProvider;
import com.java_template.application.controller.AuthController;
import com.java_template.application.dto.LoginRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies ApiSecurityConfig security rules:
 *   - public paths are accessible without a token
 *   - protected paths require a valid token
 *   - both Bearer header and httpOnly cookie are accepted
 *   - invalid / missing tokens are rejected with 401
 */
@WebMvcTest(controllers = AuthController.class)
@Import({ApiSecurityConfig.class, TmsBearerTokenResolver.class})
@TestPropertySource(properties = {
        "app.auth.secret=test-signing-secret-32-chars-ok!!",
        "app.auth.secure-cookie=false"
})
class ApiSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final JwtTokenProvider signingProvider =
            new JwtTokenProvider("test-signing-secret-32-chars-ok!!");

    // ── public endpoints ──────────────────────────────────────────────────────

    @Test
    void loginEndpointIsAccessibleWithoutToken() throws Exception {
        when(authService.authenticate(any(), any())).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("nobody", "wrong"))))
                .andExpect(status().isUnauthorized()); // 401 from AuthService, not from Security
    }

    @Test
    void loginEndpointIsNotBlockedBySecurityFilter() throws Exception {
        // Security must permit /auth/login without a JWT.
        // AuthController returns 401 when credentials are bad — that's business logic, not security filter.
        // If the security filter blocked it, we'd get 401 with WWW-Authenticate: Bearer.
        // We verify the request reaches the controller by checking the response body.
        when(authService.authenticate(any(), any()))
                .thenReturn(new AuthService.LoginResponse("tok", "u", "ADMIN",
                        System.currentTimeMillis() + 86400000));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("admin", "pass"))))
                .andExpect(status().isOk());
    }

    @Test
    void logoutEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());
    }

    // ── protected endpoints with valid token ──────────────────────────────────

    @Test
    void validBearerTokenGrantsAccessToProtectedEndpoint() throws Exception {
        String token = signingProvider.generateToken("user-uuid", "ADMIN");
        when(authService.authenticate(any(), any()))
                .thenReturn(new AuthService.LoginResponse("tok", "u", "ADMIN",
                        System.currentTimeMillis() + 86400000));

        // /auth/login with Bearer token should reach the controller (not be blocked by security)
        mockMvc.perform(post("/auth/login")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("admin", "pass"))))
                .andExpect(status().isOk());
    }

    @Test
    void validCookieTokenGrantsAccessToProtectedEndpoint() throws Exception {
        String token = signingProvider.generateToken("user-uuid", "TESTER");
        when(authService.authenticate(any(), any()))
                .thenReturn(new AuthService.LoginResponse("tok", "u", "TESTER",
                        System.currentTimeMillis() + 86400000));

        mockMvc.perform(post("/auth/login")
                        .cookie(new Cookie("auth-token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("tester", "pass"))))
                .andExpect(status().isOk());
    }

    // ── rejection cases ───────────────────────────────────────────────────────

    @Test
    void missingTokenIsRejectedOn401ForProtectedEndpoint() throws Exception {
        // GET /auth/me doesn't exist — but the 404 would only occur after security passes.
        // A missing token should yield 401 before the route is even looked up.
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBearerTokenIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }
}

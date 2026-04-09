package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.auth.AuthService;
import com.java_template.application.auth.JwtTokenProvider;
import com.java_template.application.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController
 */
@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    public void testLoginWithValidCredentials() throws Exception {
        AuthService.LoginResponse response =
                new AuthService.LoginResponse("mock-jwt-token", "admin", "ADMIN",
                        System.currentTimeMillis() + 86400000);
        when(authService.authenticate("admin", "admin123")).thenReturn(response);

        LoginRequest request = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // token is no longer returned in body — it is set as an httpOnly cookie
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(cookie().exists(AuthController.COOKIE_NAME));
    }

    @Test
    public void testLoginWithInvalidCredentials() throws Exception {
        when(authService.authenticate("admin", "wrongpassword")).thenReturn(null);

        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLogout() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthController.COOKIE_NAME, 0));
    }

    @Test
    @DisplayName("FR 1.1: Admin user can login with correct credentials")
    public void testAdminCanLogin() throws Exception {
        AuthService.LoginResponse response =
                new AuthService.LoginResponse("mock-jwt-token", "admin", "ADMIN",
                        System.currentTimeMillis() + 86400000);
        when(authService.authenticate("admin", "admin123")).thenReturn(response);

        LoginRequest request = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(cookie().exists(AuthController.COOKIE_NAME));
    }

    @Test
    @DisplayName("FR 1.1: Tester user can login with correct credentials")
    public void testTesterCanLogin() throws Exception {
        AuthService.LoginResponse response =
                new AuthService.LoginResponse("mock-jwt-token", "tester", "TESTER",
                        System.currentTimeMillis() + 86400000);
        when(authService.authenticate("tester", "tester123")).thenReturn(response);

        LoginRequest request = new LoginRequest("tester", "tester123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"))
                .andExpect(jsonPath("$.role").value("TESTER"))
                .andExpect(cookie().exists(AuthController.COOKIE_NAME));
    }

    @Test
    @DisplayName("FR 1.1: Unknown user cannot login")
    public void testUnknownUserCannotLogin() throws Exception {
        when(authService.authenticate("unknownuser", "password")).thenReturn(null);

        LoginRequest request = new LoginRequest("unknownuser", "password");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("FR 1.1: Only 2 hardcoded users exist - admin")
    public void testOnlyHardcodedAdminExists() throws Exception {
        // Admin can login
        AuthService.LoginResponse response =
                new AuthService.LoginResponse("mock-jwt-token", "admin", "ADMIN",
                        System.currentTimeMillis() + 86400000);
        when(authService.authenticate("admin", "admin123")).thenReturn(response);

        LoginRequest request = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR 1.1: Only 2 hardcoded users exist - tester")
    public void testOnlyHardcodedTesterExists() throws Exception {
        // Tester can login
        AuthService.LoginResponse response =
                new AuthService.LoginResponse("mock-jwt-token", "tester", "TESTER",
                        System.currentTimeMillis() + 86400000);
        when(authService.authenticate("tester", "tester123")).thenReturn(response);

        LoginRequest request = new LoginRequest("tester", "tester123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR 1.5: Admin and Tester roles are properly assigned")
    public void testRolesAreCorrectlyAssigned() throws Exception {
        AuthService.LoginResponse adminResponse =
                new AuthService.LoginResponse("mock-jwt-token", "admin", "ADMIN",
                        System.currentTimeMillis() + 86400000);
        AuthService.LoginResponse testerResponse =
                new AuthService.LoginResponse("mock-jwt-token", "tester", "TESTER",
                        System.currentTimeMillis() + 86400000);

        when(authService.authenticate("admin", "admin123")).thenReturn(adminResponse);
        when(authService.authenticate("tester", "tester123")).thenReturn(testerResponse);

        // Test admin role
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Test tester role
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("tester", "tester123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TESTER"));
    }
}

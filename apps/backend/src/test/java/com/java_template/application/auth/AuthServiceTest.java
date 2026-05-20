package com.java_template.application.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Test
    void authenticatesWithConfiguredAdminCredentials() {
        when(tokenProvider.generateToken(any(), any())).thenReturn("jwt-token");
        AuthService service = new AuthService(tokenProvider, propsWithTwoUsers("my-admin", "my-secret", "my-tester", "tester-secret"));

        AuthService.LoginResponse response = service.authenticate("my-admin", "my-secret");

        assertThat(response).isNotNull();
        assertThat(response.username).isEqualTo("my-admin");
        assertThat(response.role).isEqualTo("ADMIN");
    }

    @Test
    void authenticatesWithConfiguredTesterCredentials() {
        when(tokenProvider.generateToken(any(), any())).thenReturn("jwt-token");
        AuthService service = new AuthService(tokenProvider, propsWithTwoUsers("my-admin", "my-secret", "my-tester", "tester-secret"));

        AuthService.LoginResponse response = service.authenticate("my-tester", "tester-secret");

        assertThat(response).isNotNull();
        assertThat(response.username).isEqualTo("my-tester");
        assertThat(response.role).isEqualTo("TESTER");
    }

    @Test
    void supportsMultipleTesters() {
        when(tokenProvider.generateToken(any(), any())).thenReturn("jwt-token");
        AuthUsersProperties props = new AuthUsersProperties();
        props.setUsers(List.of(
                userConfig("admin", "admin-pass", "ADMIN"),
                userConfig("tester1", "pass1", "TESTER"),
                userConfig("tester2", "pass2", "TESTER")
        ));
        AuthService service = new AuthService(tokenProvider, props);

        assertThat(service.authenticate("tester1", "pass1")).isNotNull();
        assertThat(service.authenticate("tester2", "pass2")).isNotNull();
        assertThat(service.authenticate("tester1", "pass2")).isNull();
    }

    @Test
    void rejectsOldHardcodedCredentialsWhenCustomOnesConfigured() {
        AuthService service = new AuthService(tokenProvider, propsWithTwoUsers("my-admin", "my-secret", "my-tester", "tester-secret"));

        assertThat(service.authenticate("admin", "admin123")).isNull();
        assertThat(service.authenticate("tester", "tester123")).isNull();
    }

    @Test
    void rejectsWrongPassword() {
        AuthService service = new AuthService(tokenProvider, propsWithTwoUsers("my-admin", "my-secret", "my-tester", "tester-secret"));

        assertThat(service.authenticate("my-admin", "wrong-password")).isNull();
    }

    private AuthUsersProperties propsWithTwoUsers(String adminUsername, String adminPassword,
                                                   String testerUsername, String testerPassword) {
        AuthUsersProperties props = new AuthUsersProperties();
        props.setUsers(List.of(
                userConfig(adminUsername, adminPassword, "ADMIN"),
                userConfig(testerUsername, testerPassword, "TESTER")
        ));
        return props;
    }

    private AuthUsersProperties.UserConfig userConfig(String username, String password, String role) {
        AuthUsersProperties.UserConfig config = new AuthUsersProperties.UserConfig();
        config.setUsername(username);
        config.setPassword(password);
        config.setRole(role);
        return config;
    }
}

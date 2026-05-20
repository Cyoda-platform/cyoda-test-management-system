package com.java_template.application.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AuthUsersPropertiesTest {

    @Test
    void failsAtStartupWhenUsersListIsEmpty() {
        AuthUsersProperties props = new AuthUsersProperties();
        props.setUsers(List.of());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_USERS_0_USERNAME");
    }

    @Test
    void failsAtStartupWhenPasswordIsBlank() {
        AuthUsersProperties props = new AuthUsersProperties();
        props.setUsers(List.of(userConfig("admin", "", "ADMIN")));

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_USERS_0_PASSWORD");
    }

    @Test
    void failsAtStartupWhenRoleIsInvalid() {
        AuthUsersProperties props = new AuthUsersProperties();
        props.setUsers(List.of(userConfig("admin", "secret", "SUPERUSER")));

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_USERS_0_ROLE")
                .hasMessageContaining("ADMIN or TESTER");
    }

    @Test
    void passesValidationWithCorrectConfig() {
        AuthUsersProperties props = new AuthUsersProperties();
        props.setUsers(List.of(
                userConfig("admin", "secret", "ADMIN"),
                userConfig("tester1", "pass1", "TESTER"),
                userConfig("tester2", "pass2", "TESTER")
        ));

        assertThatNoException().isThrownBy(props::validate);
    }

    @Test
    void reportsCorrectIndexInErrorMessage() {
        AuthUsersProperties props = new AuthUsersProperties();
        props.setUsers(List.of(
                userConfig("admin", "secret", "ADMIN"),
                userConfig("tester", "", "TESTER")
        ));

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_USERS_1_PASSWORD");
    }

    private AuthUsersProperties.UserConfig userConfig(String username, String password, String role) {
        AuthUsersProperties.UserConfig config = new AuthUsersProperties.UserConfig();
        config.setUsername(username);
        config.setPassword(password);
        config.setRole(role);
        return config;
    }
}

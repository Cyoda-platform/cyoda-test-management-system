package com.java_template.application.auth;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AuthUsersProperties {

    private List<UserConfig> users = new ArrayList<>();

    @PostConstruct
    public void validate() {
        if (users.isEmpty()) {
            throw new IllegalStateException(
                    "No users configured. Add at least one user in your environment:\n" +
                    "  APP_USERS_0_USERNAME=admin\n" +
                    "  APP_USERS_0_PASSWORD=your-password\n" +
                    "  APP_USERS_0_ROLE=ADMIN"
            );
        }
        for (int i = 0; i < users.size(); i++) {
            UserConfig user = users.get(i);
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalStateException(
                        "User at index " + i + " ('" + user.getUsername() + "') has no password configured.\n" +
                        "Set APP_USERS_" + i + "_PASSWORD in your environment."
                );
            }
            if (user.getRole() == null || (!user.getRole().equals("ADMIN") && !user.getRole().equals("TESTER"))) {
                throw new IllegalStateException(
                        "User at index " + i + " ('" + user.getUsername() + "') has invalid role '" + user.getRole() + "'.\n" +
                        "Set APP_USERS_" + i + "_ROLE to ADMIN or TESTER."
                );
            }
        }
    }

    public List<UserConfig> getUsers() {
        return users;
    }

    public void setUsers(List<UserConfig> users) {
        this.users = users;
    }

    @Data
    public static class UserConfig {
        private String username;
        private String password;
        private String role;
    }
}

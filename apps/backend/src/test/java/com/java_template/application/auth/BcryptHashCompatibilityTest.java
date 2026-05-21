package com.java_template.application.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the $2y$ hashes in users-seed.yml (test resources) are accepted
 * by Spring's BCryptPasswordEncoder. $2y$ is bcrypt variant — Spring treats it as $2a$.
 */
class BcryptHashCompatibilityTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void admin123MatchesTestSeedHash() {
        assertThat(encoder.matches("admin123",
                "$2y$12$8cWM1Z0rokPGvrlg2NRCieHActYs5YUI6m2q2UQ9sZ.Y6yqsiUQhK"))
                .isTrue();
    }

    @Test
    void tester123MatchesTestSeedHash() {
        assertThat(encoder.matches("tester123",
                "$2y$12$4WbaSTCBjR3Hq7pNSjQ7kuFvS4z1q3Emdgd/IHvAyTnLLfyeLvWYC"))
                .isTrue();
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        assertThat(encoder.matches("wrongpassword",
                "$2y$12$8cWM1Z0rokPGvrlg2NRCieHActYs5YUI6m2q2UQ9sZ.Y6yqsiUQhK"))
                .isFalse();
    }
}

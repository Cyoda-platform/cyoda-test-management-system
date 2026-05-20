package com.java_template.application.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * SG-06: ssl-trust-all=true must be rejected at startup in non-dev profiles.
 */
class SslTrustAllGuardTest {

    @Test
    void trustedAllSslIsAllowedInLocalProfile() {
        // Local/dev profile: ssl-trust-all=true is acceptable
        assertDoesNotThrow(() -> new SslTrustAllGuard(true, new String[]{"local"}));
    }

    @Test
    void trustedAllSslIsAllowedInTestProfile() {
        assertDoesNotThrow(() -> new SslTrustAllGuard(true, new String[]{"test"}));
    }

    @Test
    void trustedAllSslIsRejectedWithNoActiveProfile() {
        assertThatThrownBy(() -> {
            SslTrustAllGuard guard = new SslTrustAllGuard(true, new String[]{});
            guard.rejectIfUnsafe();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("ssl-trust-all");
    }

    @Test
    void trustedAllSslIsRejectedInProductionProfile() {
        assertThatThrownBy(() -> {
            SslTrustAllGuard guard = new SslTrustAllGuard(true, new String[]{"prod"});
            guard.rejectIfUnsafe();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("ssl-trust-all");
    }

    @Test
    void trustedAllSslFalseIsAlwaysAllowed() {
        assertDoesNotThrow(() -> {
            SslTrustAllGuard guard = new SslTrustAllGuard(false, new String[]{"prod"});
            guard.rejectIfUnsafe();
        });
    }
}

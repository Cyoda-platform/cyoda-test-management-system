package com.java_template.application.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * SG-06: Refuses to start if ssl-trust-all=true is set outside a local/test profile.
 * This flag disables SSL certificate verification and must never reach production.
 */
@Component
public class SslTrustAllGuard {

    private static final Logger log = LoggerFactory.getLogger(SslTrustAllGuard.class);
    private static final Set<String> SAFE_PROFILES = Set.of("local", "test", "cucumber", "dev");

    private final boolean sslTrustAll;
    private final String[] activeProfiles;

    @Autowired
    public SslTrustAllGuard(
            @Value("${app.config.ssl-trust-all:false}") boolean sslTrustAll,
            @Value("${spring.profiles.active:}") String activeProfilesRaw) {
        this.sslTrustAll = sslTrustAll;
        this.activeProfiles = activeProfilesRaw.isBlank()
                ? new String[0]
                : activeProfilesRaw.split(",");
    }

    // Package-private constructor for unit tests
    SslTrustAllGuard(boolean sslTrustAll, String[] activeProfiles) {
        this.sslTrustAll = sslTrustAll;
        this.activeProfiles = activeProfiles;
    }

    @PostConstruct
    public void rejectIfUnsafe() {
        if (!sslTrustAll) {
            return;
        }
        boolean isSafeProfile = Arrays.stream(activeProfiles)
                .anyMatch(p -> SAFE_PROFILES.contains(p.trim().toLowerCase()));
        if (!isSafeProfile) {
            throw new IllegalStateException(
                    "ssl-trust-all=true is not allowed outside local/test profiles " +
                    "(active profiles: " + Arrays.toString(activeProfiles) + "). " +
                    "Set SSL_TRUST_ALL=false or switch to the 'local' profile.");
        }
        log.warn("ssl-trust-all=true — SSL certificate verification is disabled. " +
                 "Only acceptable in profiles: {}", SAFE_PROFILES);
    }
}

package com.java_template.application.auth;

import com.java_template.application.entity.UserEntity;
import com.java_template.application.service.UserService;
import com.java_template.common.dto.EntityWithMetadata;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserService userService;

    private AuthService authService;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new AuthService(tokenProvider, userService);
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    void authenticatesActiveUserWithCorrectPassword() {
        String rawPassword = "correct-secret";
        when(tokenProvider.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(activeUser("alice", rawPassword, "ADMIN")));

        AuthService.LoginResponse response = authService.authenticate("alice", rawPassword);

        assertThat(response).isNotNull();
        assertThat(response.username).isEqualTo("alice");
        assertThat(response.role).isEqualTo("ADMIN");
    }

    @Test
    void tokenSubIsTheCyodaUuidAndDisplayNameClaimIsUsername() {
        UUID userId = UUID.randomUUID();
        String rawPassword = "secret";
        when(tokenProvider.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(activeUser("alice", rawPassword, "TESTER", userId)));

        authService.authenticate("alice", rawPassword);

        // generateToken must be called with (uuid, displayName, role) — not just (uuid, role)
        org.mockito.Mockito.verify(tokenProvider).generateToken(userId.toString(), "alice", "TESTER");
    }

    // ── rejection cases ───────────────────────────────────────────────────────

    @Test
    void rejectsWrongPassword() {
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(activeUser("alice", "real-password", "TESTER")));

        assertThat(authService.authenticate("alice", "wrong-password")).isNull();
    }

    @Test
    void rejectsUnknownUser() {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(authService.authenticate("ghost", "any-password")).isNull();
    }

    @Test
    void rejectsInactiveUser() {
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(userWithState("alice", "correct", "TESTER", "INACTIVE")));

        assertThat(authService.authenticate("alice", "correct")).isNull();
    }

    @Test
    void rejectsLockedUser() {
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(userWithState("alice", "correct", "TESTER", "LOCKED")));

        assertThat(authService.authenticate("alice", "correct")).isNull();
    }

    @Test
    void rejectsSubmittingTheHashItselfAsPassword() {
        String hash = ENCODER.encode("real-secret");
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(activeUser("alice", "real-secret", "ADMIN")));

        // Submitting the stored hash directly must not authenticate
        assertThat(authService.authenticate("alice", hash)).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private EntityWithMetadata<UserEntity> activeUser(String username, String rawPassword, String role) {
        return activeUser(username, rawPassword, role, UUID.randomUUID());
    }

    private EntityWithMetadata<UserEntity> activeUser(String username, String rawPassword,
                                                       String role, UUID id) {
        return userWithState(username, rawPassword, role, id, "ACTIVE");
    }

    private EntityWithMetadata<UserEntity> userWithState(String username, String rawPassword,
                                                          String role, String state) {
        return userWithState(username, rawPassword, role, UUID.randomUUID(), state);
    }

    private EntityWithMetadata<UserEntity> userWithState(String username, String rawPassword,
                                                          String role, UUID id, String state) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPasswordHash(ENCODER.encode(rawPassword));
        entity.setRoles(List.of(role));
        entity.setCreatedAt(Instant.now().toString());

        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        meta.setState(state);

        return new EntityWithMetadata<>(entity, meta);
    }
}

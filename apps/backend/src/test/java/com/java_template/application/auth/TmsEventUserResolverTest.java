package com.java_template.application.auth;

import com.java_template.application.entity.UserEntity;
import com.java_template.application.service.UserService;
import com.java_template.common.auth.CloudEventAuthContext;
import com.java_template.common.auth.EventUserIdentity;
import com.java_template.common.auth.EventUserResolutionException;
import com.java_template.common.dto.EntityWithMetadata;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmsEventUserResolverTest {

    @Mock
    private UserService userService;

    private TmsEventUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TmsEventUserResolver(userService);
    }

    @Test
    void resolvesActiveUserSuccessfully() {
        UUID userId = UUID.randomUUID();
        EntityWithMetadata<UserEntity> user = activeUser(userId, List.of("TESTER"));
        when(userService.findById(userId)).thenReturn(Optional.of(user));

        EventUserIdentity identity = resolver.resolve(authContext(userId.toString(), "{\"roles\":[\"TESTER\"]}"));

        assertThat(identity.userId()).isEqualTo(userId.toString());
        assertThat(identity.roles()).contains("TESTER");
    }

    @Test
    void throwsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(authContext(userId.toString(), null)))
                .isInstanceOf(EventUserResolutionException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void throwsWhenUserIsInactive() {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(Optional.of(userWithState(userId, "INACTIVE")));

        assertThatThrownBy(() -> resolver.resolve(authContext(userId.toString(), null)))
                .isInstanceOf(EventUserResolutionException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void throwsWhenUserIsLocked() {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(Optional.of(userWithState(userId, "LOCKED")));

        assertThatThrownBy(() -> resolver.resolve(authContext(userId.toString(), null)))
                .isInstanceOf(EventUserResolutionException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void throwsWhenAuthIdIsNotAValidUuid() {
        assertThatThrownBy(() -> resolver.resolve(authContext("not-a-uuid", null)))
                .isInstanceOf(EventUserResolutionException.class)
                .hasMessageContaining("not-a-uuid");
    }

    @Test
    void throwsWhenAuthIdIsBlank() {
        assertThatThrownBy(() -> resolver.resolve(authContext("", null)))
                .isInstanceOf(EventUserResolutionException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CloudEventAuthContext authContext(String authId, String authClaimsJson) {
        return new CloudEventAuthContext("user", authId, authClaimsJson);
    }

    private EntityWithMetadata<UserEntity> activeUser(UUID id, List<String> roles) {
        UserEntity u = new UserEntity();
        u.setUsername("some-user");
        u.setRoles(roles);
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        meta.setState("ACTIVE");
        return new EntityWithMetadata<>(u, meta);
    }

    private EntityWithMetadata<UserEntity> userWithState(UUID id, String state) {
        UserEntity u = new UserEntity();
        u.setUsername("some-user");
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        meta.setState(state);
        return new EntityWithMetadata<>(u, meta);
    }
}

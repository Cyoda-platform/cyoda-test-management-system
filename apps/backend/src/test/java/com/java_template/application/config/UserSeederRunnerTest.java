package com.java_template.application.config;

import com.java_template.application.entity.UserEntity;
import com.java_template.application.service.UserService;
import com.java_template.common.dto.EntityWithMetadata;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSeederRunnerTest {

    @Mock
    private UserService userService;

    @Test
    void createsUserFromSeedWhenNotAlreadyPresent() throws Exception {
        when(userService.findByUsername(anyString())).thenReturn(Optional.empty());
        doReturn(entityWith("admin")).when(userService).createUser(any());

        UserSeederRunner runner = runnerWithInlineSeed(List.of(
                seedEntry("admin", "$2b$12$testhash1234567890abcdefghijklmnopqrstuvwxyz01", "ADMIN")
        ));
        runner.run();

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userService).createUser(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        assertThat(captor.getValue().getRoles()).contains("ADMIN");
        assertThat(captor.getValue().getPasswordHash()).startsWith("$2b$");
    }

    @Test
    void skipsUserThatAlreadyExists() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(entityWith("admin")));

        UserSeederRunner runner = runnerWithInlineSeed(List.of(
                seedEntry("admin", "$2b$12$testhash1234567890abcdefghijklmnopqrstuvwxyz01", "ADMIN")
        ));
        runner.run();

        verify(userService, never()).createUser(any());
    }

    @Test
    void createsOnlyMissingUsersFromSeed() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(entityWith("admin")));
        when(userService.findByUsername("tester")).thenReturn(Optional.empty());
        doReturn(entityWith("tester")).when(userService).createUser(any());

        UserSeederRunner runner = runnerWithInlineSeed(List.of(
                seedEntry("admin",  "$2b$12$testhash1234567890abcdefghijklmnopqrstuvwxyz01", "ADMIN"),
                seedEntry("tester", "$2b$12$testhash1234567890abcdefghijklmnopqrstuvwxyz02", "TESTER")
        ));
        runner.run();

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userService, times(1)).createUser(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("tester");
    }

    @Test
    void doesNotFailWhenSeedListIsEmpty() {
        UserSeederRunner runner = runnerWithInlineSeed(List.of());

        assertThatNoException().isThrownBy(runner::run);
        verifyNoInteractions(userService);
    }

    @Test
    void setsCreatedAtOnNewUser() throws Exception {
        when(userService.findByUsername(anyString())).thenReturn(Optional.empty());
        doReturn(entityWith("carol")).when(userService).createUser(any());

        UserSeederRunner runner = runnerWithInlineSeed(List.of(
                seedEntry("carol", "$2b$12$testhash1234567890abcdefghijklmnopqrstuvwxyz01", "TESTER")
        ));
        runner.run();

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userService).createUser(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotBlank();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserSeederRunner runnerWithInlineSeed(List<UserSeederRunner.SeedEntry> entries) {
        return new UserSeederRunner(userService, entries);
    }

    private UserSeederRunner.SeedEntry seedEntry(String username, String hash, String role) {
        UserSeederRunner.SeedEntry e = new UserSeederRunner.SeedEntry();
        e.setUsername(username);
        e.setPasswordHash(hash);
        e.setRoles(List.of(role));
        return e;
    }

    private EntityWithMetadata<UserEntity> entityWith(String username) {
        UserEntity u = new UserEntity();
        u.setUsername(username);
        EntityMetadata meta = new EntityMetadata();
        meta.setId(UUID.randomUUID());
        return new EntityWithMetadata<>(u, meta);
    }
}

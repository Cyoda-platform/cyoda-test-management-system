package com.java_template.application.service;

import com.java_template.application.entity.UserEntity;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.service.EntityService;
import com.java_template.common.dto.PageResult;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private EntityService entityService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(entityService);
    }

    @Test
    void findByUsernameReturnsEmptyWhenNoUserFound() {
        when(entityService.search(any(), any(GroupConditionDto.class), eq(UserEntity.class)))
                .thenReturn(emptyPage());

        Optional<EntityWithMetadata<UserEntity>> result = userService.findByUsername("nobody");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUsernameReturnsFirstMatchWhenFound() {
        EntityWithMetadata<UserEntity> match = entityWithUser("alice");
        when(entityService.search(any(), any(GroupConditionDto.class), eq(UserEntity.class)))
                .thenReturn(PageResult.of(null, List.of(match), 0, 20, 1));

        Optional<EntityWithMetadata<UserEntity>> result = userService.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get().entity().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByUsernameSearchesOnUsernameField() {
        when(entityService.search(any(), any(GroupConditionDto.class), eq(UserEntity.class)))
                .thenReturn(emptyPage());
        ArgumentCaptor<GroupConditionDto> conditionCaptor = ArgumentCaptor.forClass(GroupConditionDto.class);

        userService.findByUsername("alice");

        verify(entityService).search(any(), conditionCaptor.capture(), eq(UserEntity.class));
        // The condition must target the username field
        String conditionJson = conditionCaptor.getValue().toString();
        assertThat(conditionJson).containsIgnoringCase("username");
    }

    @Test
    void createUserCallsEntityServiceCreate() {
        UserEntity user = new UserEntity();
        user.setUsername("bob");
        user.setPasswordHash("$2b$12$hash");
        user.setRoles(List.of("TESTER"));
        EntityWithMetadata<UserEntity> created = entityWithUser("bob");
        doReturn(created).when(entityService).create(user);

        EntityWithMetadata<UserEntity> result = userService.createUser(user);

        verify(entityService).create(user);
        assertThat(result.entity().getUsername()).isEqualTo("bob");
    }

    @Test
    void createUserSetsCreatedAtTimestampIfBlank() {
        UserEntity user = new UserEntity();
        user.setUsername("carol");
        user.setPasswordHash("$2b$12$hash");
        user.setRoles(List.of("ADMIN"));
        doReturn(entityWithUser("carol")).when(entityService).create(any(UserEntity.class));

        userService.createUser(user);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(entityService).create(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotBlank();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PageResult<EntityWithMetadata<UserEntity>> emptyPage() {
        return PageResult.of(null, List.of(), 0, 20, 0);
    }

    private EntityWithMetadata<UserEntity> entityWithUser(String username) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPasswordHash("$2b$12$testhash");
        entity.setRoles(List.of("TESTER"));
        entity.setCreatedAt(Instant.now().toString());
        EntityMetadata meta = new EntityMetadata();
        meta.setId(UUID.randomUUID());
        return new EntityWithMetadata<>(entity, meta);
    }
}

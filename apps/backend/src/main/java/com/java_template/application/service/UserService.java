package com.java_template.application.service;

import com.java_template.application.entity.UserEntity;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.service.EntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.common.model.GroupOperatorDto;
import org.cyoda.cloud.api.common.model.OperatorTypeDto;
import org.cyoda.cloud.api.common.model.QueryConditionTypeDto;
import org.cyoda.cloud.api.common.model.SimpleConditionDto;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(UserEntity.ENTITY_NAME).withVersion(UserEntity.ENTITY_VERSION);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EntityService entityService;

    public UserService(EntityService entityService) {
        this.entityService = entityService;
    }

    public Optional<EntityWithMetadata<UserEntity>> findById(UUID userId) {
        try {
            return Optional.of(entityService.getById(userId, MODEL_SPEC, UserEntity.class));
        } catch (Exception ex) {
            log.warn("Could not find user by id '{}': {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<EntityWithMetadata<UserEntity>> findByUsername(String username) {
        try {
            GroupConditionDto condition = conditionByUsername(username);
            return entityService.search(MODEL_SPEC, condition, UserEntity.class)
                    .data().stream()
                    .findFirst();
        } catch (Exception ex) {
            log.warn("Could not search for user '{}': {}", username, ex.getMessage());
            return Optional.empty();
        }
    }

    public EntityWithMetadata<UserEntity> createUser(UserEntity user) {
        if (user.getCreatedAt() == null || user.getCreatedAt().isBlank()) {
            user.setCreatedAt(Instant.now().toString());
        }
        return entityService.create(user);
    }

    private GroupConditionDto conditionByUsername(String username) {
        SimpleConditionDto condition = new SimpleConditionDto()
                .jsonPath("$.username")
                .operation(OperatorTypeDto.EQUALS)
                .value(MAPPER.valueToTree(username));
        condition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(condition));
        group.setType(QueryConditionTypeDto.GROUP);
        return group;
    }
}

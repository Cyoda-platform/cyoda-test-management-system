package com.java_template.application.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.common.workflow.CyodaEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void hasCorrectEntityNameAndVersion() {
        assertThat(UserEntity.ENTITY_NAME).isEqualTo("User");
        assertThat(UserEntity.ENTITY_VERSION).isEqualTo(1);
    }

    @Test
    void implementsCyodaEntity() {
        assertThat(UserEntity.class).isAssignableTo(CyodaEntity.class);
    }

    @Test
    void getModelKeyReturnsEntitySpec() {
        UserEntity entity = fullEntity();

        assertThat(entity.getModelKey()).isNotNull();
    }

    @Test
    void serializesAndDeserializesAllFields() throws Exception {
        UserEntity original = fullEntity();

        String json = mapper.writeValueAsString(original);
        UserEntity restored = mapper.readValue(json, UserEntity.class);

        assertThat(restored.getUsername()).isEqualTo(original.getUsername());
        assertThat(restored.getEmail()).isEqualTo(original.getEmail());
        assertThat(restored.getPasswordHash()).isEqualTo(original.getPasswordHash());
        assertThat(restored.getRoles()).isEqualTo(original.getRoles());
        assertThat(restored.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }

    @Test
    void passwordHashIsIncludedInSerializedJson() throws Exception {
        // UserEntity is a Cyoda store entity — passwordHash must round-trip through Jackson
        // so Cyoda can persist and retrieve it. API responses use a separate DTO.
        UserEntity entity = fullEntity();

        String json = mapper.writeValueAsString(entity);

        assertThat(json).contains("passwordHash");
    }

    @Test
    void usernameIsRequired() {
        UserEntity entity = fullEntity();
        entity.setUsername("");

        Set<ConstraintViolation<UserEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void rolesDefaultsToEmptyList() {
        UserEntity entity = new UserEntity();

        assertThat(entity.getRoles()).isNotNull().isEmpty();
    }

    @Test
    void lastLoginAtIsNullableByDefault() {
        UserEntity entity = fullEntity();
        entity.setLastLoginAt(null);

        Set<ConstraintViolation<UserEntity>> violations = validator.validate(entity);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("lastLoginAt"));
    }

    private UserEntity fullEntity() {
        UserEntity e = new UserEntity();
        e.setUsername("alice");
        e.setEmail("alice@example.com");
        e.setPasswordHash("$2b$12$somehashvalue");
        e.setRoles(List.of("TESTER"));
        e.setCreatedAt("2026-01-01T00:00:00Z");
        e.setLastLoginAt(null);
        return e;
    }
}

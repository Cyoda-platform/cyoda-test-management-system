package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.common.model.GroupOperatorDto;
import org.cyoda.cloud.api.common.model.OperatorTypeDto;
import org.cyoda.cloud.api.common.model.QueryConditionTypeDto;
import org.cyoda.cloud.api.common.model.SimpleConditionDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Test Step operations
 */
@Service
public class TestStepService {
    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(TestStepDTO.ENTITY_NAME).withVersion(TestStepDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ObjectMapper objectMapper;

    public TestStepService(EntityService entityService, ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
    }

    private GroupConditionDto conditionByField(String fieldName, Object value) {
        SimpleConditionDto condition = new SimpleConditionDto()
                .jsonPath("$." + fieldName)
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(value));
        condition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(condition));
        group.setType(QueryConditionTypeDto.GROUP);
        return group;
    }

    private TestStepDTO withId(EntityWithMetadata<TestStepDTO> result) {
        TestStepDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    /**
     * Creates a new test step
     */
    public TestStepDTO createTestStep(TestStepDTO testStep) {
        return withId(entityService.create(testStep));
    }

    /**
     * Retrieves a test step by ID
     */
    public Optional<TestStepDTO> getTestStepById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestStepDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves all test steps for a specific test case
     */
    public List<TestStepDTO> getTestStepsByTestCaseId(UUID testCaseId) {
        GroupConditionDto condition = conditionByField("testCaseId", testCaseId.toString());
        return entityService.search(MODEL_SPEC, condition, TestStepDTO.class).data()
                .stream()
                .map(this::withId)
                .sorted(Comparator.comparing(TestStepDTO::getStepNumber, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    /**
     * Updates an existing test step
     */
    public TestStepDTO updateTestStep(UUID id, TestStepDTO testStep) {
        return withId(entityService.update(id, testStep, null));
    }

    /**
     * Deletes a test step by ID
     */
    public boolean deleteTestStep(UUID id) {
        try {
            entityService.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


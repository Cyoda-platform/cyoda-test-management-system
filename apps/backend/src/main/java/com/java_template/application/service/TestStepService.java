package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.StepReplaceDTO;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        testStep.setDeleted(false);
        return withId(entityService.create(testStep));
    }

    /**
     * Retrieves a test step by ID (excludes soft-deleted steps)
     */
    public Optional<TestStepDTO> getTestStepById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestStepDTO.class)))
                    .filter(ts -> !ts.isDeleted());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves all non-deleted test steps for a specific test case, ordered by stepNumber
     */
    public List<TestStepDTO> getTestStepsByTestCaseId(UUID testCaseId) {
        SimpleConditionDto caseCondition = new SimpleConditionDto()
                .jsonPath("$.testCaseId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(testCaseId.toString()));
        caseCondition.setType(QueryConditionTypeDto.SIMPLE);
        SimpleConditionDto deletedCondition = new SimpleConditionDto()
                .jsonPath("$.deleted")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(false));
        deletedCondition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto condition = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(caseCondition, deletedCondition));
        condition.setType(QueryConditionTypeDto.GROUP);
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
     * Soft-deletes a test step by setting deleted=true.
     * Preserves the record so existing TestRunStep snapshots remain intact.
     */
    public boolean deleteTestStep(UUID id) {
        return getTestStepById(id).map(ts -> {
            ts.setDeleted(true);
            entityService.update(id, ts, null);
            return true;
        }).orElse(false);
    }

    /**
     * Returns ALL test steps for a given test case including soft-deleted ones.
     * Used internally for cascade-delete operations when a project is removed.
     */
    public List<TestStepDTO> getAllTestStepsByTestCaseIdIncludingDeleted(UUID testCaseId) {
        GroupConditionDto condition = conditionByField("testCaseId", testCaseId.toString());
        return entityService.search(MODEL_SPEC, condition, TestStepDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    /**
     * Hard-deletes a test step entity by ID.
     * Used ONLY during cascade deletion of a parent project — ordinary deletion
     * must go through {@link #deleteTestStep(UUID)} which applies the soft-delete flag.
     */
    public void hardDeleteTestStep(UUID id) {
        entityService.deleteById(id);
    }

    /**
     * Replaces all steps for a test case atomically:
     * soft-deletes existing steps in parallel, then creates new ones in parallel.
     */
    public List<TestStepDTO> replaceSteps(UUID caseId, List<StepReplaceDTO> newSteps) {
        List<TestStepDTO> existing = getTestStepsByTestCaseId(caseId);
        if (!existing.isEmpty()) {
            List<CompletableFuture<Void>> deleteFutures = existing.stream()
                    .map(s -> CompletableFuture.runAsync(() -> {
                        s.setDeleted(true);
                        entityService.update(s.getId(), s, null);
                    }))
                    .toList();
            CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();
        }

        if (newSteps == null || newSteps.isEmpty()) return List.of();

        TestStepDTO[] results = new TestStepDTO[newSteps.size()];
        List<CompletableFuture<Void>> createFutures = new ArrayList<>(newSteps.size());
        for (int i = 0; i < newSteps.size(); i++) {
            final int idx = i;
            final StepReplaceDTO dto = newSteps.get(i);
            createFutures.add(CompletableFuture.runAsync(() -> {
                TestStepDTO step = new TestStepDTO();
                step.setTestCaseId(caseId);
                step.setStepNumber(dto.getStepNumber() != null ? dto.getStepNumber() : idx + 1);
                step.setAction(dto.getAction() != null ? dto.getAction() : "");
                step.setExpectedResult(dto.getExpectedResult() != null ? dto.getExpectedResult() : "");
                step.setDeleted(false);
                results[idx] = withId(entityService.create(step));
            }));
        }
        CompletableFuture.allOf(createFutures.toArray(new CompletableFuture[0])).join();
        return Arrays.asList(results);
    }
}


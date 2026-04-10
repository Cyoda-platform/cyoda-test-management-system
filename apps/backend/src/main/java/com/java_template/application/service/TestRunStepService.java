package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.common.model.GroupOperatorDto;
import org.cyoda.cloud.api.common.model.OperatorTypeDto;
import org.cyoda.cloud.api.common.model.QueryConditionTypeDto;
import org.cyoda.cloud.api.common.model.SimpleConditionDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Test Run Step operations
 */
@Service
public class TestRunStepService {

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(TestRunStepDTO.ENTITY_NAME).withVersion(TestRunStepDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ObjectMapper objectMapper;

    public TestRunStepService(EntityService entityService, ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
    }

    private GroupConditionDto conditionByField(String jsonPath, String value) {
        SimpleConditionDto condition = new SimpleConditionDto()
                .jsonPath("$." + jsonPath)
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(value));
        condition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(condition));
        group.setType(QueryConditionTypeDto.GROUP);
        return group;
    }

    private TestRunStepDTO withId(EntityWithMetadata<TestRunStepDTO> result) {
        TestRunStepDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<TestRunStepDTO> toPage(PageResult<EntityWithMetadata<TestRunStepDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }

    public TestRunStepDTO createTestRunStep(TestRunStepDTO testRunStep) {
        testRunStep.setStatus("UNTESTED");
        return withId(entityService.create(testRunStep));
    }

    public Optional<TestRunStepDTO> getTestRunStepById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestRunStepDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public PageResult<TestRunStepDTO> getTestRunStepsByTestRunCaseId(UUID testRunCaseId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        GroupConditionDto condition = conditionByField("testRunCaseId", testRunCaseId.toString());
        PageResult<EntityWithMetadata<TestRunStepDTO>> result =
                entityService.search(MODEL_SPEC, condition, TestRunStepDTO.class, params);
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                page, size, result.totalElements());
    }

    /**
     * Returns ALL TestRunStep records for a given run-case without pagination.
     * Used internally for cascade-delete operations.
     */
    public List<TestRunStepDTO> getAllTestRunStepsByTestRunCaseId(UUID testRunCaseId) {
        GroupConditionDto condition = conditionByField("testRunCaseId", testRunCaseId.toString());
        return entityService.search(MODEL_SPEC, condition, TestRunStepDTO.class)
                .data().stream().map(this::withId).toList();
    }

    /**
     * Hard-deletes a TestRunStep entity by ID.
     * Used during cascade deletion of parent entities.
     */
    public void deleteTestRunStep(UUID id) {
        entityService.deleteById(id);
    }

    public Optional<TestRunStepDTO> updateTestRunStepStatus(UUID id, String status) {
        return getTestRunStepById(id).map(trs -> {
            trs.setStatus(status);
            return withId(entityService.update(id, trs, null));
        });
    }


}


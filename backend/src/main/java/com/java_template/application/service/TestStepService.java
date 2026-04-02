package com.java_template.application.service;

import com.java_template.application.dto.TestStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
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

    public TestStepService(EntityService entityService) {
        this.entityService = entityService;
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
        if (testStep.getStatus() == null || testStep.getStatus().isBlank()) {
            testStep.setStatus("untested");
        }
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
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(0).pageSize(1000).build();

        PageResult<EntityWithMetadata<TestStepDTO>> allTestSteps = entityService.findAll(
                MODEL_SPEC,
                TestStepDTO.class,
                params);

        return allTestSteps.data().stream()
                .map(this::withId)
                .filter(testStep -> testCaseId.equals(testStep.getTestCaseId()))
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


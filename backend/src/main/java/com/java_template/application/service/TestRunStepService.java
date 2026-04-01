package com.java_template.application.service;

import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
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

    public TestRunStepService(EntityService entityService) {
        this.entityService = entityService;
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
        List<TestRunStepDTO> all = entityService.findAll(MODEL_SPEC, TestRunStepDTO.class)
                .data().stream()
                .map(this::withId)
                .filter(s -> testRunCaseId.equals(s.getTestRunCaseId()))
                .toList();
        int from = page * size;
        int to = Math.min(from + size, all.size());
        List<TestRunStepDTO> pageData = from < all.size() ? all.subList(from, to) : List.of();
        return PageResult.of(null, pageData, page, size, (long) all.size());
    }

    public Optional<TestRunStepDTO> updateTestRunStepStatus(UUID id, String status) {
        return getTestRunStepById(id).map(trs -> {
            trs.setStatus(status);
            return withId(entityService.update(id, trs, null));
        });
    }


}


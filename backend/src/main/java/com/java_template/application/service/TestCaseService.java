package com.java_template.application.service;

import com.java_template.application.dto.TestCaseDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Test Case operations
 */
@Service
public class TestCaseService {

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(TestCaseDTO.ENTITY_NAME).withVersion(TestCaseDTO.ENTITY_VERSION);

    private final EntityService entityService;

    public TestCaseService(EntityService entityService) {
        this.entityService = entityService;
    }

    private TestCaseDTO withId(EntityWithMetadata<TestCaseDTO> result) {
        TestCaseDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<TestCaseDTO> toPage(PageResult<EntityWithMetadata<TestCaseDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }

    /**
     * Creates a new test case with ACTIVE status
     */
    public TestCaseDTO createTestCase(TestCaseDTO testCase) {
        testCase.setStatus("ACTIVE");
        testCase.setDeleted(false);
        return withId(entityService.create(testCase));
    }

    public Optional<TestCaseDTO> getTestCaseById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestCaseDTO.class)))
                    .filter(testCase -> !testCase.isDeleted());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public PageResult<TestCaseDTO> getTestCasesBySuiteId(UUID suiteId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(0).pageSize(1000).build();

        PageResult<EntityWithMetadata<TestCaseDTO>> allTestCases = entityService.findAll(
                MODEL_SPEC,
                TestCaseDTO.class,
                params);

        List<TestCaseDTO> filtered = allTestCases.data().stream()
                .map(this::withId)
                .filter(testCase -> suiteId.equals(testCase.getSuiteId()))
                .filter(testCase -> !testCase.isDeleted())
                .skip((long) page * size)
                .limit(size)
                .toList();

        long total = allTestCases.data().stream()
                .map(this::withId)
                .filter(testCase -> suiteId.equals(testCase.getSuiteId()))
                .filter(testCase -> !testCase.isDeleted())
                .count();

        return PageResult.of(allTestCases.searchId(), filtered, page, size, total);
    }

    public List<TestCaseDTO> getAllTestCases() {
        return entityService.findAll(MODEL_SPEC, TestCaseDTO.class).data()
                .stream()
                .map(this::withId)
                .filter(testCase -> !testCase.isDeleted())
                .toList();
    }

    public TestCaseDTO updateTestCase(UUID id, TestCaseDTO testCase) {
        return withId(entityService.update(id, testCase, null));
    }

    public boolean deleteTestCase(UUID id) {
        return softDeleteTestCase(id);
    }

    public boolean testCaseExists(UUID id) {
        return getTestCaseById(id).isPresent();
    }

    public boolean softDeleteTestCase(UUID id) {
        return getTestCaseById(id).map(tc -> {
            tc.setDeleted(true);
            entityService.update(id, tc, null);
            return true;
        }).orElse(false);
    }
}


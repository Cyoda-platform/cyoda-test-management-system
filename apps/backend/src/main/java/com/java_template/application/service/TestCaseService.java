package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.BatchImportCaseDTO;
import com.java_template.application.dto.ReorderItemDTO;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import lombok.extern.slf4j.Slf4j;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.common.model.GroupOperatorDto;
import org.cyoda.cloud.api.common.model.OperatorTypeDto;
import org.cyoda.cloud.api.common.model.QueryConditionTypeDto;
import org.cyoda.cloud.api.common.model.SimpleConditionDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for Test Case operations
 */
@Slf4j
@Service
public class TestCaseService {

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(TestCaseDTO.ENTITY_NAME).withVersion(TestCaseDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ProjectCounterService projectCounterService;
    private final TestStepService testStepService;
    private final ObjectMapper objectMapper;

    public TestCaseService(EntityService entityService,
                           ProjectCounterService projectCounterService,
                           TestStepService testStepService,
                           ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.projectCounterService = projectCounterService;
        this.testStepService = testStepService;
        this.objectMapper = objectMapper;
    }

    private GroupConditionDto conditionByField(String fieldName, Object value) {
        SimpleConditionDto condition = new SimpleConditionDto()
                .jsonPath("$." + fieldName)
                .operation(OperatorTypeDto.EQUALS)
                .value(com.fasterxml.jackson.databind.node.TextNode.valueOf(value.toString()));
        condition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(condition));
        group.setType(QueryConditionTypeDto.GROUP);
        return group;
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
     * Creates a new test case.
     * Always generates a server-side display ID using the project counter so IDs
     * are unique across the project, independent of suite names, and never reused.
     *
     * Note: Cyoda's create-then-reload cycle may not return the displayId in the
     * reloaded payload.  We therefore persist it explicitly via a follow-up update
     * so that subsequent reads always see the correct TC-X value.
     */
    @CacheEvict(value = "casesBySuite", allEntries = true)
    public TestCaseDTO createTestCase(TestCaseDTO testCase) {
        testCase.setDeleted(false);
        // Always override any client-supplied displayId with a server-generated one
        String displayId = projectCounterService.nextDisplayId(testCase.getProjectId());
        testCase.setDisplayId(displayId);
        // Initialize timestamps
        String now = Instant.now().toString();
        testCase.setCreatedAt(now);
        testCase.setUpdatedAt(now);

        TestCaseDTO created = withId(entityService.create(testCase));
        // Cyoda's create reload may strip the displayId; persist it explicitly
        created.setDisplayId(displayId);
        entityService.update(created.getId(), created, null);
        return created;
    }

    public Optional<TestCaseDTO> getTestCaseById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestCaseDTO.class)))
                    .filter(testCase -> !testCase.isDeleted());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves test cases for a suite, ordered by sortOrder (nulls last).
     */
    @Cacheable(value = "casesBySuite", key = "#suiteId + ':' + #page + ':' + #size")
    public PageResult<TestCaseDTO> getTestCasesBySuiteId(UUID suiteId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        SimpleConditionDto suiteCondition = new SimpleConditionDto()
                .jsonPath("$.suiteId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(suiteId.toString()));
        suiteCondition.setType(QueryConditionTypeDto.SIMPLE);
        SimpleConditionDto deletedCondition = new SimpleConditionDto()
                .jsonPath("$.deleted")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(false));
        deletedCondition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto condition = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(suiteCondition, deletedCondition));
        condition.setType(QueryConditionTypeDto.GROUP);
        PageResult<EntityWithMetadata<TestCaseDTO>> result =
                entityService.search(MODEL_SPEC, condition, TestCaseDTO.class, params);
        List<TestCaseDTO> sorted = result.data().stream()
                .map(this::withId)
                .sorted(Comparator.comparing(TestCaseDTO::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return PageResult.of(result.searchId(), sorted, page, size, result.totalElements());
    }

    /**
     * Retrieves all non-deleted test cases for an entire project in one Cyoda call.
     * Used by the repository aggregate endpoint to avoid per-suite fetches.
     */
    public List<TestCaseDTO> getCasesByProjectId(UUID projectId) {
        SimpleConditionDto projectCondition = new SimpleConditionDto()
                .jsonPath("$.projectId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(projectId.toString()));
        projectCondition.setType(QueryConditionTypeDto.SIMPLE);
        SimpleConditionDto deletedCondition = new SimpleConditionDto()
                .jsonPath("$.deleted")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(false));
        deletedCondition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto condition = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(projectCondition, deletedCondition));
        condition.setType(QueryConditionTypeDto.GROUP);
        return entityService.search(MODEL_SPEC, condition, TestCaseDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    public List<TestCaseDTO> getAllTestCases() {
        GroupConditionDto condition = conditionByField("deleted", false);
        return entityService.search(MODEL_SPEC, condition, TestCaseDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    @CacheEvict(value = "casesBySuite", allEntries = true)
    public TestCaseDTO updateTestCase(UUID id, TestCaseDTO testCase) {
        // Protect displayId from being overwritten by client updates
        // If the client doesn't send displayId (or sends null), preserve the existing one
        TestCaseDTO existing = getTestCaseById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found: " + id));

        if (testCase.getDisplayId() == null || testCase.getDisplayId().isEmpty()) {
            testCase.setDisplayId(existing.getDisplayId());
        }

        return withId(entityService.update(id, testCase, null));
    }

    public boolean deleteTestCase(UUID id) {
        return softDeleteTestCase(id);
    }

    public boolean testCaseExists(UUID id) {
        return getTestCaseById(id).isPresent();
    }

    @CacheEvict(value = "casesBySuite", allEntries = true)
    public boolean softDeleteTestCase(UUID id) {
        return getTestCaseById(id).map(tc -> {
            tc.setDeleted(true);
            entityService.update(id, tc, null);
            return true;
        }).orElse(false);
    }

    /**
     * Hard-deletes a test case entity by ID, bypassing soft-delete logic.
     * Used ONLY during project cascade deletion — ordinary deletion must go through
     * {@link #deleteTestCase(UUID)} which applies the soft-delete flag.
     */
    public void hardDeleteTestCase(UUID id) {
        entityService.deleteById(id);
    }

    /**
     * Returns ALL test cases for a project (including soft-deleted ones) without pagination.
     * Used internally for cascade hard-delete when a project is being removed.
     */
    public List<TestCaseDTO> getAllCasesByProjectIdIncludingDeleted(UUID projectId) {
        GroupConditionDto condition = conditionByField("projectId", projectId.toString());
        return entityService.search(MODEL_SPEC, condition, TestCaseDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    /**
     * Returns all non-deleted test cases for a suite without pagination.
     * Used internally for cascade soft-delete when a suite is being removed.
     */
    public List<TestCaseDTO> getAllTestCasesBySuiteId(UUID suiteId) {
        SimpleConditionDto suiteCondition = new SimpleConditionDto()
                .jsonPath("$.suiteId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(suiteId.toString()));
        suiteCondition.setType(QueryConditionTypeDto.SIMPLE);
        SimpleConditionDto deletedCondition = new SimpleConditionDto()
                .jsonPath("$.deleted")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(false));
        deletedCondition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto condition = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(suiteCondition, deletedCondition));
        condition.setType(QueryConditionTypeDto.GROUP);
        return entityService.search(MODEL_SPEC, condition, TestCaseDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    /**
     * Bulk-updates the sortOrder for a list of test cases within a suite.
     * Each item carries the case UUID and its new 0-based position.
     * Unknown or deleted IDs are silently skipped.
     * Uses {@code entityService.updateAll()} to avoid N individual update round-trips.
     */
    @CacheEvict(value = "casesBySuite", allEntries = true)
    public void reorderTestCases(List<ReorderItemDTO> items) {
        var orderMap = new java.util.HashMap<UUID, Integer>(items.size() * 2);
        items.forEach(item -> orderMap.put(item.id(), item.sortOrder()));

        List<TestCaseDTO> toUpdate = items.stream()
                .map(item -> getTestCaseById(item.id()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(tc -> tc.setSortOrder(orderMap.get(tc.getId())))
                .toList();

        if (!toUpdate.isEmpty()) {
            entityService.updateAll(toUpdate, null);
        }
    }

    /**
     * Moves a test case to a different suite and assigns it a new sort position.
     * The caller is responsible for re-indexing the remaining cases in both
     * the source and destination suites.
     *
     * @param id           UUID of the test case to move
     * @param targetSuiteId UUID of the destination suite
     * @param sortOrder    0-based position within the destination suite
     * @return the updated TestCaseDTO
     * @throws IllegalArgumentException if the test case does not exist
     */
    @CacheEvict(value = "casesBySuite", allEntries = true)
    public TestCaseDTO moveTestCase(UUID id, UUID targetSuiteId, Integer sortOrder) {
        return getTestCaseById(id).map(tc -> {
            tc.setSuiteId(targetSuiteId);
            tc.setSortOrder(sortOrder);
            return updateTestCase(id, tc);
        }).orElseThrow(() -> new IllegalArgumentException("Test case not found: " + id));
    }

    /**
     * Batch-creates multiple test cases (with optional steps) in a single suite.
     *
     * Performance: all display IDs are reserved with ONE counter round-trip via
     * {@link ProjectCounterService#nextDisplayIdBatch}, eliminating the N individual
     * counter reads that made per-row import slow.
     *
     * @param projectId project UUID (path param)
     * @param suiteId   suite UUID  (path param)
     * @param items     ordered list of cases to import; each may carry embedded steps
     * @return the list of created test cases (without their steps)
     */
    @CacheEvict(value = "casesBySuite", allEntries = true)
    public List<TestCaseDTO> batchCreateTestCases(UUID projectId, UUID suiteId,
                                                  List<BatchImportCaseDTO> items) {
        if (items == null || items.isEmpty()) return java.util.Collections.emptyList();

        List<String> displayIds = projectCounterService.nextDisplayIdBatch(projectId, items.size());

        TestCaseDTO[] results = new TestCaseDTO[items.size()];
        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>(items.size());

        for (int i = 0; i < items.size(); i++) {
            final int idx = i;
            final BatchImportCaseDTO item = items.get(i);
            final String displayId = displayIds.get(i);

            futures.add(CompletableFuture.runAsync(() -> {
                TestCaseDTO tc = new TestCaseDTO();
                tc.setProjectId(projectId);
                tc.setSuiteId(suiteId);
                tc.setTitle(item.getTitle());
                tc.setDescription(item.getDescription() != null ? item.getDescription() : "");
                tc.setPreconditions(item.getPreconditions() != null ? item.getPreconditions() : "");
                tc.setPriority(item.getPriority() != null ? item.getPriority()
                        : com.java_template.application.dto.Priority.MEDIUM);
                tc.setDeleted(false);
                tc.setDisplayId(displayId);

                TestCaseDTO saved = withId(entityService.create(tc));
                saved.setDisplayId(displayId);
                entityService.update(saved.getId(), saved, null);

                if (item.getSteps() != null && !item.getSteps().isEmpty()) {
                    int stepNum = 1;
                    for (BatchImportCaseDTO.StepDTO s : item.getSteps()) {
                        TestStepDTO step = new TestStepDTO();
                        step.setTestCaseId(saved.getId());
                        step.setStepNumber(s.getStepNumber() != null ? s.getStepNumber() : stepNum);
                        step.setAction(s.getAction() != null ? s.getAction() : "");
                        step.setExpectedResult(s.getExpectedResult() != null ? s.getExpectedResult() : "");
                        testStepService.createTestStep(step);
                        stepNum++;
                    }
                }
                results[idx] = saved;
            }));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (java.util.concurrent.CompletionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException("Batch import failed: " + cause.getMessage(), cause);
        }

        return java.util.Arrays.asList(results);
    }
}

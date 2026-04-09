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
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.common.condition.GroupCondition;
import org.cyoda.cloud.api.event.common.condition.Operation;
import org.cyoda.cloud.api.event.common.condition.SimpleCondition;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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

    private GroupCondition conditionByField(String fieldName, Object value) {
        SimpleCondition condition = new SimpleCondition()
                .withJsonPath("$." + fieldName)
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(value));
        return new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(condition));
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
     */
    public TestCaseDTO createTestCase(TestCaseDTO testCase) {
        testCase.setDeleted(false);
        // Always override any client-supplied displayId with a server-generated one
        testCase.setDisplayId(projectCounterService.nextDisplayId(testCase.getProjectId()));
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

    /**
     * Retrieves test cases for a suite, ordered by sortOrder (nulls last).
     */
    public PageResult<TestCaseDTO> getTestCasesBySuiteId(UUID suiteId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        SimpleCondition suiteCondition = new SimpleCondition()
                .withJsonPath("$.suiteId")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(suiteId.toString()));
        SimpleCondition deletedCondition = new SimpleCondition()
                .withJsonPath("$.deleted")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(false));
        GroupCondition condition = new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(suiteCondition, deletedCondition));
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
        SimpleCondition projectCondition = new SimpleCondition()
                .withJsonPath("$.projectId")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(projectId.toString()));
        SimpleCondition deletedCondition = new SimpleCondition()
                .withJsonPath("$.deleted")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(false));
        GroupCondition condition = new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(projectCondition, deletedCondition));
        return entityService.search(MODEL_SPEC, condition, TestCaseDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    public List<TestCaseDTO> getAllTestCases() {
        GroupCondition condition = conditionByField("deleted", false);
        return entityService.search(MODEL_SPEC, condition, TestCaseDTO.class).data()
                .stream()
                .map(this::withId)
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

    /**
     * Bulk-updates the sortOrder for a list of test cases within a suite.
     * Each item carries the case UUID and its new 0-based position.
     * Unknown or deleted IDs are silently skipped.
     * Uses {@code entityService.updateAll()} to avoid N individual update round-trips.
     */
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
    public List<TestCaseDTO> batchCreateTestCases(UUID projectId, UUID suiteId,
                                                  List<BatchImportCaseDTO> items) {
        if (items == null || items.isEmpty()) return java.util.Collections.emptyList();

        // Pre-allocate ALL display IDs in one counter update
        List<String> displayIds = projectCounterService.nextDisplayIdBatch(projectId, items.size());

        List<TestCaseDTO> created = new java.util.ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            BatchImportCaseDTO item = items.get(i);

            TestCaseDTO tc = new TestCaseDTO();
            tc.setProjectId(projectId);
            tc.setSuiteId(suiteId);
            tc.setTitle(item.getTitle());
            tc.setDescription(item.getDescription() != null ? item.getDescription() : "");
            tc.setPreconditions(item.getPreconditions() != null ? item.getPreconditions() : "");
            tc.setPriority(item.getPriority() != null ? item.getPriority()
                    : com.java_template.application.dto.Priority.MEDIUM);
            tc.setDeleted(false);
            tc.setDisplayId(displayIds.get(i));

            TestCaseDTO saved = withId(entityService.create(tc));

            // Create steps inline if provided
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
            created.add(saved);
        }
        return created;
    }
}

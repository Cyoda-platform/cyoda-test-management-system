package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.common.condition.GroupCondition;
import org.cyoda.cloud.api.event.common.condition.Operation;
import org.cyoda.cloud.api.event.common.condition.SimpleCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Test Run Case operations
 */
@Service
public class TestRunCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestRunCaseService.class);

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(TestRunCaseDTO.ENTITY_NAME).withVersion(TestRunCaseDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ObjectMapper objectMapper;
    private final TestRunStepService testRunStepService;

    public TestRunCaseService(EntityService entityService,
                              ObjectMapper objectMapper,
                              TestRunStepService testRunStepService) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
        this.testRunStepService = testRunStepService;
    }

    private TestRunCaseDTO withId(EntityWithMetadata<TestRunCaseDTO> result) {
        TestRunCaseDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<TestRunCaseDTO> toPage(PageResult<EntityWithMetadata<TestRunCaseDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
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

    /**
     * Creates a {@link TestRunCaseDTO} with upsert semantics.
     *
     * <p>If a {@code TestRunCase} record already exists for the given
     * {@code (testRunId, testCaseId)} pair, the existing record is returned
     * immediately without creating a duplicate. This prevents the "two pending
     * /cases" deadlock that occurred when the frontend fired concurrent
     * {@code POST /cases} requests for the same case (e.g. from the auto-fail
     * modal trigger and the background resolution of {@code handleBugClick}
     * running in parallel).</p>
     *
     * <p>Without this guard the Cyoda entity service received two simultaneous
     * create requests for the same logical record. Because each create requires
     * a workflow lock, the second request blocked indefinitely waiting for the
     * first — keeping {@code /cases} in {@code (pending)} state in the Network
     * tab and causing the defect-creation modal to hang.</p>
     */
    public TestRunCaseDTO createTestRunCase(TestRunCaseDTO testRunCase) {
        // Upsert guard: check for an existing record with the same run+case pair.
        if (testRunCase.getTestRunId() != null && testRunCase.getTestCaseId() != null) {
            try {
                List<TestRunCaseDTO> existing = getTestRunCasesByTestRunIdAndTestCaseId(
                        testRunCase.getTestRunId(), testRunCase.getTestCaseId());
                if (!existing.isEmpty()) {
                    log.debug("TestRunCase already exists for run={} case={} — returning existing id={}",
                            testRunCase.getTestRunId(), testRunCase.getTestCaseId(), existing.get(0).getId());
                    return existing.get(0);
                }
            } catch (Exception e) {
                // If the existence check itself fails (e.g. CYODA search error), proceed
                // with creation — a duplicate is safer than a missing record.
                log.warn("Upsert existence check failed for TestRunCase run={} case={}: {}",
                        testRunCase.getTestRunId(), testRunCase.getTestCaseId(), e.getMessage());
            }
        }

        testRunCase.setStatus("UNTESTED");
        return withId(entityService.create(testRunCase));
    }

    /**
     * Finds existing {@link TestRunCaseDTO} records for a specific run+case combination.
     * Used by the upsert guard in {@link #createTestRunCase}.
     */
    private List<TestRunCaseDTO> getTestRunCasesByTestRunIdAndTestCaseId(UUID testRunId, UUID testCaseId) {
        SimpleCondition runCondition = new SimpleCondition()
                .withJsonPath("$.testRunId")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(testRunId.toString()));
        SimpleCondition caseCondition = new SimpleCondition()
                .withJsonPath("$.testCaseId")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(testCaseId.toString()));
        GroupCondition condition = new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(runCondition, caseCondition));
        return entityService.search(MODEL_SPEC, condition, TestRunCaseDTO.class)
                .data().stream()
                .map(this::withId)
                .toList();
    }

    /**
     * Atomically creates a {@link TestRunCaseDTO} together with all of its
     * {@link TestRunStepDTO} records in a single logical operation.
     *
     * <p>Because CYODA's {@code EntityService} does not expose a cross-entity
     * transaction boundary, this method implements a <em>compensating-transaction</em>
     * pattern: if any step creation fails, every successfully created step and the
     * parent case are deleted before the exception is re-thrown, preventing the
     * "case-without-steps" partial-write state that caused the 'No steps defined'
     * symptom.</p>
     *
     * @param testRunCase the case to create (status will be overwritten to UNTESTED)
     * @param testStepIds ordered list of original {@code TestStep.id} values to snapshot
     * @return the persisted {@link TestRunCaseDTO} with its assigned {@code id}
     * @throws RuntimeException if step creation fails (case creation is rolled back)
     */
    public TestRunCaseDTO createTestRunCaseWithSteps(TestRunCaseDTO testRunCase,
                                                     List<UUID> testStepIds) {
        // 1. Persist the case record.
        TestRunCaseDTO created = createTestRunCase(testRunCase);
        UUID runCaseId = created.getId();

        // 2. Persist each step. Track created IDs so we can compensate on failure.
        List<UUID> createdStepIds = new ArrayList<>();
        try {
            for (UUID testStepId : testStepIds) {
                TestRunStepDTO step = new TestRunStepDTO();
                step.setTestRunCaseId(runCaseId);
                step.setTestStepId(testStepId);
                TestRunStepDTO createdStep = testRunStepService.createTestRunStep(step);
                createdStepIds.add(createdStep.getId());
            }
        } catch (Exception stepException) {
            // Compensating transaction: delete all steps created so far, then the case.
            log.error("Step creation failed for TestRunCase {}. Rolling back {} step(s) and the case.",
                    runCaseId, createdStepIds.size(), stepException);
            createdStepIds.forEach(stepId -> {
                try {
                    entityService.deleteById(stepId);
                } catch (Exception ignored) {
                    log.warn("Cleanup failed for TestRunStep {}", stepId);
                }
            });
            try {
                entityService.deleteById(runCaseId);
            } catch (Exception ignored) {
                log.warn("Cleanup failed for TestRunCase {}", runCaseId);
            }
            throw new RuntimeException(
                    "Failed to initialise TestRunCase " + runCaseId +
                    ": step creation failed. The partial write has been rolled back.", stepException);
        }

        return created;
    }

    public Optional<TestRunCaseDTO> getTestRunCaseById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestRunCaseDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public PageResult<TestRunCaseDTO> getTestRunCasesByTestRunId(UUID testRunId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        return toPage(entityService.search(MODEL_SPEC, conditionByField("testRunId", testRunId.toString()),
                TestRunCaseDTO.class, params));
    }

    public Optional<TestRunCaseDTO> updateTestRunCaseStatus(UUID id, String status) {
        return getTestRunCaseById(id).map(trc -> {
            trc.setStatus(status);
            return withId(entityService.update(id, trc, null));
        });
    }

    public Optional<TestRunCaseDTO> linkBug(UUID id, String bugUrl) {
        return getTestRunCaseById(id).map(trc -> {
            trc.setBugUrl(bugUrl);
            return withId(entityService.update(id, trc, null));
        });
    }
}


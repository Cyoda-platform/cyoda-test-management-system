package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunCaseDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(value = "runCasesByRun", allEntries = true)
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
        SimpleConditionDto runCondition = new SimpleConditionDto()
                .jsonPath("$.testRunId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(testRunId.toString()));
        runCondition.setType(QueryConditionTypeDto.SIMPLE);
        SimpleConditionDto caseCondition = new SimpleConditionDto()
                .jsonPath("$.testCaseId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(testCaseId.toString()));
        caseCondition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto condition = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(runCondition, caseCondition));
        condition.setType(QueryConditionTypeDto.GROUP);
        return entityService.search(MODEL_SPEC, condition, TestRunCaseDTO.class)
                .data().stream()
                .map(this::withId)
                .toList();
    }

    /**
     * Creates a {@link TestRunCaseDTO} together with all of its {@link TestRunStepDTO}
     * records, copying snapshot content from the case's embedded steps.
     *
     * <p>Steps are now embedded directly in {@link TestCaseDTO} — there are no separate
     * TestStep entities. This method snapshots the step data at run-creation time so
     * the run remains self-contained even if the original case is updated later.</p>
     *
     * <p>Uses a compensating-transaction pattern: if any step creation fails, all
     * successfully created steps and the parent case are deleted before re-throwing.</p>
     *
     * @param testRunCase the case to create (snapshot fields must already be set by caller)
     * @param steps       embedded steps from {@link TestCaseDTO#getSteps()}
     * @return the persisted {@link TestRunCaseDTO} with its assigned {@code id}
     */
    @CacheEvict(value = "runCasesByRun", allEntries = true)
    public TestRunCaseDTO createTestRunCaseWithStepSnapshots(TestRunCaseDTO testRunCase,
                                                              List<TestCaseDTO.StepDTO> steps) {
        TestRunCaseDTO created = createTestRunCase(testRunCase);
        UUID runCaseId = created.getId();

        List<UUID> createdStepIds = new ArrayList<>();
        try {
            for (TestCaseDTO.StepDTO step : steps) {
                TestRunStepDTO runStep = new TestRunStepDTO();
                runStep.setTestRunCaseId(runCaseId);
                runStep.setStepNumber(step.getStepNumber());
                runStep.setAction(step.getAction());
                runStep.setExpectedResult(step.getExpectedResult());
                TestRunStepDTO createdStep = testRunStepService.createTestRunStep(runStep);
                createdStepIds.add(createdStep.getId());
            }
        } catch (Exception stepException) {
            log.error("Step snapshot creation failed for TestRunCase {}. Rolling back {} step(s) and the case.",
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
                    ": step snapshot creation failed. The partial write has been rolled back.", stepException);
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

    /**
     * Returns ALL TestRunCase records for a given project without pagination.
     * Used internally for cascade-delete operations.
     * Requires {@code projectId} to be stored on the entity (populated at creation time).
     */
    public List<TestRunCaseDTO> getAllTestRunCasesByProjectId(UUID projectId) {
        return entityService.search(MODEL_SPEC,
                        conditionByField("projectId", projectId.toString()),
                        TestRunCaseDTO.class)
                .data().stream()
                .map(this::withId)
                .toList();
    }

    /**
     * Returns ALL TestRunCase records for a given test run without pagination.
     * Used internally for cascade-delete operations.
     */
    @Cacheable(value = "runCasesByRun", key = "#testRunId")
    public List<TestRunCaseDTO> getAllTestRunCasesByTestRunId(UUID testRunId) {
        return entityService.search(MODEL_SPEC,
                        conditionByField("testRunId", testRunId.toString()),
                        TestRunCaseDTO.class)
                .data().stream()
                .map(this::withId)
                .toList();
    }

    /**
     * Hard-deletes a TestRunCase entity by ID.
     * Used during cascade deletion of parent entities.
     */
    @CacheEvict(value = "runCasesByRun", allEntries = true)
    public void deleteTestRunCase(UUID id) {
        entityService.deleteById(id);
    }

    @CacheEvict(value = "runCasesByRun", allEntries = true)
    public Optional<TestRunCaseDTO> updateTestRunCaseStatus(UUID id, String status) {
        return getTestRunCaseById(id).map(trc -> {
            trc.setStatus(status);
            return withId(entityService.update(id, trc, null));
        });
    }

    @CacheEvict(value = "runCasesByRun", allEntries = true)
    public Optional<TestRunCaseDTO> linkBug(UUID id, String bugUrl) {
        return getTestRunCaseById(id).map(trc -> {
            trc.setBugUrl(bugUrl);
            return withId(entityService.update(id, trc, null));
        });
    }
}


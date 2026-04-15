package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.common.model.GroupOperatorDto;
import org.cyoda.cloud.api.common.model.OperatorTypeDto;
import org.cyoda.cloud.api.common.model.QueryConditionTypeDto;
import org.cyoda.cloud.api.common.model.SimpleConditionDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Test Run operations
 */
@Service
public class TestRunService {

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(TestRunDTO.ENTITY_NAME).withVersion(TestRunDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ProjectCounterService projectCounterService;
    private final ObjectMapper objectMapper;

    public TestRunService(EntityService entityService,
                          ProjectCounterService projectCounterService,
                          ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.projectCounterService = projectCounterService;
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

    private TestRunDTO withId(EntityWithMetadata<TestRunDTO> result) {
        TestRunDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<TestRunDTO> toPage(PageResult<EntityWithMetadata<TestRunDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }

    public TestRunDTO createTestRun(TestRunDTO testRun) {
        String now = Instant.now().toString();
        testRun.setCreatedAt(now);
        testRun.setStartedAt(now);
        // Always override any client-supplied displayId with a server-generated TR-N
        String displayId = projectCounterService.nextRunDisplayId(testRun.getProjectId());
        testRun.setDisplayId(displayId);
        TestRunDTO created = withId(entityService.create(testRun));
        // Persist displayId explicitly — Cyoda's create reload may not return it
        created.setDisplayId(displayId);
        entityService.update(created.getId(), created, null);
        return created;
    }

    public Optional<TestRunDTO> getTestRunById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestRunDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public PageResult<TestRunDTO> getTestRunsByProjectId(UUID projectId, int page, int size) {
        // Filter in-memory: the Cyoda entity model for TestRun was registered before
        // projectId was added, so $.projectId is not a valid search path in the schema.
        // findAll + client-side filter is safe given the expected number of runs per project.
        List<TestRunDTO> all = entityService.findAll(MODEL_SPEC, TestRunDTO.class)
                .data().stream()
                .map(this::withId)
                .filter(r -> projectId.equals(r.getProjectId()))
                .toList();
        int from = page * size;
        int to = Math.min(from + size, all.size());
        List<TestRunDTO> pageData = from < all.size() ? all.subList(from, to) : List.of();
        return PageResult.of(null, pageData, page, size, (long) all.size());
    }

    public List<TestRunDTO> getTestRunsByStatus(String status) {
        GroupConditionDto condition = conditionByField("status", status);
        return entityService.search(MODEL_SPEC, condition, TestRunDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    public List<TestRunDTO> getAllTestRuns() {
        return entityService.findAll(MODEL_SPEC, TestRunDTO.class).data()
                .stream().map(this::withId).toList();
    }

    /**
     * Retrieves all test runs for a project without pagination.
     * Used internally for cascade-delete operations.
     */
    public List<TestRunDTO> getAllTestRunsByProjectId(UUID projectId) {
        return entityService.findAll(MODEL_SPEC, TestRunDTO.class).data()
                .stream()
                .map(this::withId)
                .filter(r -> projectId.equals(r.getProjectId()))
                .toList();
    }

    /**
     * Updates a test run, guarding against accidental caseIds / stepStatuses erasure.
     *
     * <p>The frontend sends a full-entity replacement body on every step-status click.
     * If the incoming payload omits {@code caseIds} (e.g. due to a stale ref during a
     * rapid update sequence), the stored snapshot would be wiped, causing all run cases
     * to vanish on the next page refresh. This guard fetches the existing record and
     * re-applies its {@code caseIds} whenever the caller sends none.</p>
     *
     * <p>Similarly, {@code stepStatuses} is never allowed to shrink — the server merges
     * the incoming map on top of the stored one so that a concurrent update from another
     * tab cannot silently discard progress already saved by the first tab.</p>
     */
    public TestRunDTO updateTestRun(UUID id, TestRunDTO testRun) {
        boolean needsCaseIdsMerge   = testRun.getCaseIds() == null || testRun.getCaseIds().isEmpty();
        boolean needsStatusMerge    = testRun.getStepStatuses() == null;

        if (needsCaseIdsMerge || needsStatusMerge) {
            getTestRunById(id).ifPresent(existing -> {
                if (needsCaseIdsMerge
                        && existing.getCaseIds() != null
                        && !existing.getCaseIds().isEmpty()) {
                    testRun.setCaseIds(existing.getCaseIds());
                }
                if (needsStatusMerge && existing.getStepStatuses() != null) {
                    testRun.setStepStatuses(existing.getStepStatuses());
                } else if (!needsStatusMerge
                        && existing.getStepStatuses() != null
                        && !existing.getStepStatuses().isEmpty()) {
                    // Merge: keep any keys from the stored map that the caller did not send.
                    // This prevents a concurrent save from the second browser tab from wiping
                    // step progress saved by the first tab.
                    java.util.Map<String, String> merged = new java.util.LinkedHashMap<>(existing.getStepStatuses());
                    merged.putAll(testRun.getStepStatuses());
                    testRun.setStepStatuses(merged);
                }
            });
        }

        return withId(entityService.update(id, testRun, null));
    }

    public Optional<TestRunDTO> completeTestRun(UUID id) {
        return getTestRunById(id).map(run -> {
            run.setCompletedAt(Instant.now().toString());
            return withId(entityService.update(id, run, "complete_run"));
        });
    }

    public Optional<TestRunDTO> unlockTestRun(UUID id) {
        return getTestRunById(id).map(run ->
                withId(entityService.update(id, run, "unlock_run"))
        );
    }

    public boolean testRunExists(UUID id) {
        return getTestRunById(id).isPresent();
    }

    public boolean deleteTestRun(UUID id) {
        entityService.deleteById(id);
        return true;
    }
}


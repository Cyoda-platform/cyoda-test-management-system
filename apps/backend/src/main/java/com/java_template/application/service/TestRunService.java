package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
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
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Test Run operations
 */
@Slf4j
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

    private Map<String, String> parseStepStatuses(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse stepStatuses JSON: {}", json, e);
            return new HashMap<>();
        }
    }

    private String serializeStepStatuses(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map != null ? map : new HashMap<>());
        } catch (Exception e) {
            log.warn("Failed to serialize stepStatuses map", e);
            return "{}";
        }
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

    @Caching(evict = {
        @CacheEvict(value = "testRunsByProject",    allEntries = true),
        @CacheEvict(value = "allTestRunsByProject", allEntries = true)
    })
    public TestRunDTO createTestRun(TestRunDTO testRun) {
        String now = Instant.now().toString();
        testRun.setCreatedAt(now);
        testRun.setStartedAt(now);
        // Always override any client-supplied displayId with a server-generated TR-N
        String displayId = projectCounterService.nextRunDisplayId(testRun.getProjectId());
        testRun.setDisplayId(displayId);

        // Save caseIds and stepStatuses temporarily — cleared before create,
        // restored in a follow-up update to avoid Cyoda schema validation errors on create.
        String savedCaseIds = testRun.getCaseIds();
        String savedStepStatuses = testRun.getStepStatuses();

        testRun.setCaseIds(null);
        testRun.setStepStatuses(null);

        TestRunDTO created = withId(entityService.create(testRun));
        // Persist displayId explicitly — Cyoda's create reload may not return it
        created.setDisplayId(displayId);

        // Restore caseIds and stepStatuses on the follow-up update
        if ((savedCaseIds != null && !savedCaseIds.isBlank()) ||
            (savedStepStatuses != null && !savedStepStatuses.isEmpty() && !savedStepStatuses.equals("{}"))) {
            created.setCaseIds(savedCaseIds);
            created.setStepStatuses(savedStepStatuses);
        }

        // Mark as 'initial' so updateTestRun can detect it and send initialize_run
        // on the first status update (when the user starts executing).
        created.setStatus("initial");
        try {
            entityService.update(created.getId(), created, null);
        } catch (Exception e) {
            log.error("createTestRun: follow-up update failed for {}, attempting cleanup", created.getId(), e);
            try {
                entityService.deleteById(created.getId());
            } catch (Exception cleanup) {
                log.error("createTestRun: cleanup also failed for {}, entity may be orphaned",
                        created.getId(), cleanup);
            }
            throw e;
        }
        return created;
    }

    public Optional<TestRunDTO> getTestRunById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, TestRunDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Cacheable(value = "testRunsByProject", key = "#projectId + ':' + #page + ':' + #size")
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
    @Cacheable(value = "allTestRunsByProject", key = "#projectId")
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
     * tab cannot silently discard progress already saved by the first tab.
     * Note: stepStatuses is stored as JSON string, so merging happens via Map conversion.</p>
     */
    @Caching(evict = {
        @CacheEvict(value = "testRunsByProject",    allEntries = true),
        @CacheEvict(value = "allTestRunsByProject", allEntries = true)
    })
    public Optional<TestRunDTO> updateTestRun(UUID id, TestRunDTO testRun) {
        boolean needsCaseIdsMerge = testRun.getCaseIds() == null || testRun.getCaseIds().isBlank();
        boolean needsStatusMerge  = testRun.getStepStatuses() == null
                || testRun.getStepStatuses().isEmpty()
                || testRun.getStepStatuses().equals("{}");

        final String[] operationName = new String[]{null};

        // Single fetch — reused for BOTH transition detection AND merge logic.
        // Previously two separate getTestRunById() calls caused concurrent threads to
        // each see status=initial and each send initialize_run, flooding Cyoda with
        // conflicting transactions that timed out after 60 s.
        Optional<TestRunDTO> existingOpt = getTestRunById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        existingOpt.ifPresent(existing -> {
            if ("initial".equals(existing.getStatus())) {
                operationName[0] = "initialize_run";
            }
        });

        if (needsCaseIdsMerge || needsStatusMerge) {
            existingOpt.ifPresent(existing -> {
                if (needsCaseIdsMerge
                        && existing.getCaseIds() != null
                        && !existing.getCaseIds().isBlank()) {
                    testRun.setCaseIds(existing.getCaseIds());
                }
                if (needsStatusMerge
                        && existing.getStepStatuses() != null
                        && !existing.getStepStatuses().equals("{}")) {
                    testRun.setStepStatuses(existing.getStepStatuses());
                } else if (!needsStatusMerge
                        && existing.getStepStatuses() != null
                        && !existing.getStepStatuses().equals("{}")) {
                    Map<String, String> existingMap = parseStepStatuses(existing.getStepStatuses());
                    Map<String, String> incomingMap = parseStepStatuses(testRun.getStepStatuses());
                    existingMap.putAll(incomingMap);
                    testRun.setStepStatuses(serializeStepStatuses(existingMap));
                }
            });
        }

        try {
            return Optional.of(withId(entityService.update(id, testRun, operationName[0])));
        } catch (Exception e) {
            // If initialize_run fails because another concurrent request already sent it,
            // Cyoda returns "State machine failed". Retry as a plain update so the
            // step-status data is still persisted.
            if ("initialize_run".equals(operationName[0])
                    && e.getMessage() != null
                    && e.getMessage().contains("State machine failed")) {
                log.info("Concurrent initialize_run detected for {}, retrying as plain update", id);
                return Optional.of(withId(entityService.update(id, testRun, null)));
            }
            throw e;
        }
    }

    public Optional<TestRunDTO> completeTestRun(UUID id) {
        return getTestRunById(id).map(run -> {
            run.setCompletedAt(Instant.now().toString());
            run.setStatus("completed");  // Explicitly set status to 'completed'
            // Do NOT clear stepStatuses or caseIds here — they must be preserved
            // for historical reporting and audit trails. The "complete_run" operation
            // in the workflow will set the run to read-only state.
            return withId(entityService.update(id, run, "complete_run"));
        });
    }

    public Optional<TestRunDTO> unlockTestRun(UUID id) {
        return getTestRunById(id).map(run -> {
            run.setStatus("active");  // Explicitly set status back to 'active'
            // Preserve all data when unlocking — the "unlock_run" operation will
            // restore edit permissions while keeping historical step statuses intact.
            return withId(entityService.update(id, run, "unlock_run"));
        });
    }

    public boolean testRunExists(UUID id) {
        return getTestRunById(id).isPresent();
    }

    @Caching(evict = {
        @CacheEvict(value = "testRunsByProject",    allEntries = true),
        @CacheEvict(value = "allTestRunsByProject", allEntries = true)
    })
    public boolean deleteTestRun(UUID id) {
        if (getTestRunById(id).isEmpty()) {
            return false;
        }
        entityService.deleteById(id);
        return true;
    }
}


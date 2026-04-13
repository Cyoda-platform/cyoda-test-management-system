package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ReorderItemDTO;
import com.java_template.application.dto.SuiteDTO;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Test Suite operations
 */
@Service
public class SuiteService {

    private static final Logger log = LoggerFactory.getLogger(SuiteService.class);
    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(SuiteDTO.ENTITY_NAME).withVersion(SuiteDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ObjectMapper objectMapper;
    private final TestCaseService testCaseService;

    public SuiteService(EntityService entityService,
                        ObjectMapper objectMapper,
                        @Lazy TestCaseService testCaseService) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
        this.testCaseService = testCaseService;
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

    private SuiteDTO withId(EntityWithMetadata<SuiteDTO> result) {
        SuiteDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<SuiteDTO> toPage(PageResult<EntityWithMetadata<SuiteDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }

    /**
     * Creates a new suite.
     * Auto-assigns a sortOrder equal to the current suite count so that new
     * suites always have a non-null position. This prevents the Cyoda platform's
     * default updated_at ordering from leaking through the stable Java sort when
     * all sortOrder values are null — which would cause suites to jump whenever a
     * child test-case is created and the suite's updated_at timestamp changes.
     */
    public SuiteDTO createSuite(SuiteDTO suite) {
        if (suite.getSortOrder() == null) {
            int count = getSuitesByProjectId(suite.getProjectId(), 0, 1000).data().size();
            suite.setSortOrder(count);
        }
        return withId(entityService.create(suite));
    }

    /**
     * Retrieves a suite by ID
     */
    public Optional<SuiteDTO> getSuiteById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, SuiteDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves all suites for a specific project, ordered by sortOrder (nulls last).
     */
    public PageResult<SuiteDTO> getSuitesByProjectId(UUID projectId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        GroupConditionDto condition = conditionByField("projectId", projectId.toString());
        PageResult<EntityWithMetadata<SuiteDTO>> result =
                entityService.search(MODEL_SPEC, condition, SuiteDTO.class, params);
        List<SuiteDTO> sorted = result.data().stream()
                .map(this::withId)
                .sorted(Comparator.comparing(SuiteDTO::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        // Secondary stable sort by id string so that suites with the
                        // same (or null) sortOrder always arrive in a deterministic order
                        // regardless of how Cyoda orders them internally (e.g. updated_at).
                        .thenComparing(s -> s.getId() != null ? s.getId().toString() : ""))
                .toList();
        return PageResult.of(result.searchId(), sorted, page, size, result.totalElements());
    }

    /**
     * Retrieves all suites
     */
    public List<SuiteDTO> getAllSuites() {
        return entityService.findAll(MODEL_SPEC, SuiteDTO.class).data()
                .stream().map(this::withId).toList();
    }

    /**
     * Retrieves all suites for a project without pagination.
     * Used internally for cascade-delete operations.
     */
    public List<SuiteDTO> getAllSuitesByProjectId(UUID projectId) {
        GroupConditionDto condition = conditionByField("projectId", projectId.toString());
        return entityService.search(MODEL_SPEC, condition, SuiteDTO.class)
                .data().stream().map(this::withId).toList();
    }

    /**
     * Updates an existing suite
     */
    public SuiteDTO updateSuite(UUID id, SuiteDTO suite) {
        return withId(entityService.update(id, suite, null));
    }

    /**
     * Cascade-deletes a suite and soft-deletes all its test cases.
     *
     * <p>Test cases are soft-deleted (not hard-deleted) so that completed
     * {@code TestRun} records retain their execution history: {@code TestRunCase}
     * and {@code TestRunStep} records continue to reference the soft-deleted
     * {@code TestCase} and {@code TestStep} entities, preserving PASSED/FAILED
     * results for auditing purposes.</p>
     *
     * <p>TestStep records are intentionally left untouched: they remain accessible
     * via {@code testCaseId} and are still referenced by {@code TestRunStep.testStepId},
     * keeping the run history intact.</p>
     */
    public boolean deleteSuite(UUID id) {
        var cases = testCaseService.getAllTestCasesBySuiteId(id);
        log.info("[Suite] Cascade soft-delete: {} TestCase(s) for suite {}", cases.size(), id);
        cases.forEach(tc -> testCaseService.softDeleteTestCase(tc.getId()));

        entityService.deleteById(id);
        log.info("[Suite] Deleted suite: {}", id);
        return true;
    }

    /**
     * Checks if a suite exists by ID
     */
    public boolean suiteExists(UUID id) {
        return getSuiteById(id).isPresent();
    }

    /**
     * Bulk-updates the sortOrder for a list of suites in a single batch call.
     * Each item carries the suite UUID and its new 0-based position.
     * Unknown IDs are silently skipped to guard against stale client state.
     * Uses {@code entityService.updateAll()} to avoid N individual update round-trips.
     */
    public void reorderSuites(List<ReorderItemDTO> items) {
        // Build a map of id → new sortOrder for O(1) lookups
        var orderMap = new java.util.HashMap<UUID, Integer>(items.size() * 2);
        items.forEach(item -> orderMap.put(item.id(), item.sortOrder()));

        // Fetch each suite individually (reads are unavoidable without a bulk-get API),
        // patch the sortOrder in memory, then flush all updates in one batch call.
        List<SuiteDTO> toUpdate = items.stream()
                .map(item -> getSuiteById(item.id()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(suite -> suite.setSortOrder(orderMap.get(suite.getId())))
                .toList();

        if (!toUpdate.isEmpty()) {
            entityService.updateAll(toUpdate, null);
        }
    }
}

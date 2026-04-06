package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ReorderItemDTO;
import com.java_template.application.dto.SuiteDTO;
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
 * Service for Test Suite operations
 */
@Service
public class SuiteService {

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(SuiteDTO.ENTITY_NAME).withVersion(SuiteDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ObjectMapper objectMapper;

    public SuiteService(EntityService entityService, ObjectMapper objectMapper) {
        this.entityService = entityService;
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
     * Creates a new suite with ACTIVE status
     */
    public SuiteDTO createSuite(SuiteDTO suite) {
        suite.setStatus("ACTIVE");
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
        GroupCondition condition = conditionByField("projectId", projectId.toString());
        PageResult<EntityWithMetadata<SuiteDTO>> result =
                entityService.search(MODEL_SPEC, condition, SuiteDTO.class, params);
        List<SuiteDTO> sorted = result.data().stream()
                .map(this::withId)
                .sorted(Comparator.comparing(SuiteDTO::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
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
     * Updates an existing suite
     */
    public SuiteDTO updateSuite(UUID id, SuiteDTO suite) {
        return withId(entityService.update(id, suite, null));
    }

    /**
     * Deletes a suite by ID
     */
    public boolean deleteSuite(UUID id) {
        entityService.deleteById(id);
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

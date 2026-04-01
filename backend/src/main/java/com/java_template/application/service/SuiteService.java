package com.java_template.application.service;

import com.java_template.application.dto.SuiteDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.springframework.stereotype.Service;

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

    public SuiteService(EntityService entityService) {
        this.entityService = entityService;
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
     * Retrieves all suites for a specific project
     */
    public PageResult<SuiteDTO> getSuitesByProjectId(UUID projectId, int page, int size) {
        List<SuiteDTO> all = entityService.findAll(MODEL_SPEC, SuiteDTO.class)
                .data().stream()
                .map(this::withId)
                .filter(s -> projectId.equals(s.getProjectId()))
                .toList();
        int from = page * size;
        int to = Math.min(from + size, all.size());
        List<SuiteDTO> pageData = from < all.size() ? all.subList(from, to) : List.of();
        return PageResult.of(null, pageData, page, size, (long) all.size());
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
}


package com.java_template.application.service;

import com.java_template.application.dto.ReportDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Report operations
 */
@Service
public class ReportService {

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(ReportDTO.ENTITY_NAME).withVersion(ReportDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ProjectCounterService projectCounterService;

    public ReportService(EntityService entityService, ProjectCounterService projectCounterService) {
        this.entityService = entityService;
        this.projectCounterService = projectCounterService;
    }

    private ReportDTO withId(EntityWithMetadata<ReportDTO> result) {
        ReportDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<ReportDTO> toPage(PageResult<EntityWithMetadata<ReportDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }

    public ReportDTO createReport(ReportDTO report) {
        String now = Instant.now().toString();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        // Always override any client-supplied displayId with a server-generated REP-N
        String displayId = projectCounterService.nextReportDisplayId(report.getProjectId());
        report.setDisplayId(displayId);
        ReportDTO created = withId(entityService.create(report));
        // Persist displayId explicitly — Cyoda's create reload may not return it
        created.setDisplayId(displayId);
        entityService.update(created.getId(), created, null);
        return created;
    }

    public Optional<ReportDTO> getReportById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, ReportDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Returns ALL reports for a project without pagination.
     * Used internally for cascade-delete operations.
     */
    public List<ReportDTO> getAllReportsByProjectId(UUID projectId) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(0).pageSize(Integer.MAX_VALUE).build();
        return entityService.findAll(MODEL_SPEC, ReportDTO.class, params)
                .data().stream()
                .filter(r -> r.entity().getProjectId() != null && r.entity().getProjectId().equals(projectId))
                .map(this::withId)
                .toList();
    }

    public PageResult<ReportDTO> getReportsByProjectId(UUID projectId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        PageResult<EntityWithMetadata<ReportDTO>> allReports =
                entityService.findAll(MODEL_SPEC, ReportDTO.class, params);

        var filtered = allReports.data().stream()
                .filter(r -> r.entity().getProjectId() != null && r.entity().getProjectId().equals(projectId))
                .toList();

        return PageResult.of(allReports.searchId(),
                filtered.stream().map(this::withId).toList(),
                page, size, filtered.size());
    }

    public ReportDTO updateReport(UUID id, ReportDTO report) {
        report.setUpdatedAt(Instant.now().toString());
        return withId(entityService.update(id, report, null));
    }

    public boolean deleteReport(UUID id) {
        entityService.deleteById(id);
        return true;
    }

    public boolean reportExists(UUID id) {
        return getReportById(id).isPresent();
    }
}

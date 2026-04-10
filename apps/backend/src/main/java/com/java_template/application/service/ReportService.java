package com.java_template.application.service;

import com.java_template.application.dto.ReportDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public ReportService(EntityService entityService) {
        this.entityService = entityService;
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
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return withId(entityService.create(report));
    }

    public Optional<ReportDTO> getReportById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, ReportDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
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
        report.setUpdatedAt(LocalDateTime.now());
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

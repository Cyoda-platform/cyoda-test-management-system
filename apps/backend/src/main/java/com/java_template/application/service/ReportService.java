package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ReportDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.common.model.GroupOperatorDto;
import org.cyoda.cloud.api.common.model.OperatorTypeDto;
import org.cyoda.cloud.api.common.model.QueryConditionTypeDto;
import org.cyoda.cloud.api.common.model.SimpleConditionDto;
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
    private final ObjectMapper objectMapper;

    public ReportService(EntityService entityService, ProjectCounterService projectCounterService,
                         ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.projectCounterService = projectCounterService;
        this.objectMapper = objectMapper;
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

    private GroupConditionDto conditionByField(String jsonPath, String value) {
        SimpleConditionDto simple = new SimpleConditionDto()
                .jsonPath("$." + jsonPath)
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(value));
        simple.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto();
        group.setOperator(GroupOperatorDto.AND);
        group.setConditions(List.of(simple));
        return group;
    }

    public ReportDTO createReport(ReportDTO report) {
        String now = Instant.now().toString();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        String displayId = projectCounterService.nextReportDisplayId(report.getProjectId());
        report.setDisplayId(displayId);
        ReportDTO created = withId(entityService.create(report));
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
        GroupConditionDto condition = conditionByField("projectId", projectId.toString());
        return toPage(entityService.search(MODEL_SPEC, condition, ReportDTO.class, params));
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

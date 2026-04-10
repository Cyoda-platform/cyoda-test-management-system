package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Defect operations
 */
@Service
public class DefectService {

    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(DefectDTO.ENTITY_NAME).withVersion(DefectDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ObjectMapper objectMapper;

    public DefectService(EntityService entityService, ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
    }

    private GroupConditionDto conditionByField(String jsonPath, String value) {
        SimpleConditionDto condition = new SimpleConditionDto()
                .jsonPath("$." + jsonPath)
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(value));
        condition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(condition));
        group.setType(QueryConditionTypeDto.GROUP);
        return group;
    }

    private DefectDTO withId(EntityWithMetadata<DefectDTO> result) {
        DefectDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<DefectDTO> toPage(PageResult<EntityWithMetadata<DefectDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }



    public DefectDTO createDefect(DefectDTO defect) {
        defect.setStatus("Open");
        defect.setCreatedAt(LocalDateTime.now());
        defect.setUpdatedAt(LocalDateTime.now());
        return withId(entityService.create(defect));
    }

    public Optional<DefectDTO> getDefectById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, DefectDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public PageResult<DefectDTO> getDefectsByProjectId(UUID projectId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        GroupConditionDto condition = conditionByField("projectId", projectId.toString());
        PageResult<EntityWithMetadata<DefectDTO>> result =
                entityService.search(MODEL_SPEC, condition, DefectDTO.class, params);
        return toPage(result);
    }

    public List<DefectDTO> getDefectsByStatus(UUID projectId, String status) {
        SimpleConditionDto projectCondition = new SimpleConditionDto()
                .jsonPath("$.projectId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(projectId.toString()));
        projectCondition.setType(QueryConditionTypeDto.SIMPLE);
        SimpleConditionDto statusCondition = new SimpleConditionDto()
                .jsonPath("$.status")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(status));
        statusCondition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto condition = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(projectCondition, statusCondition));
        condition.setType(QueryConditionTypeDto.GROUP);
        return entityService.search(MODEL_SPEC, condition, DefectDTO.class)
                .data().stream().map(this::withId).toList();
    }

    /**
     * Returns all defects raised during a specific test run.
     * Relies on the {@code testRunId} field added to {@link com.java_template.application.dto.DefectDTO}.
     *
     * <p>CYODA's search engine throws a {@link java.util.concurrent.CompletionException} when the
     * queried JSON path does not exist in <em>any</em> stored document (i.e. all existing defects
     * pre-date this field). The try/catch converts that into an empty page so the Run Execution
     * view doesn't crash on runs that have no defects yet.</p>
     */
    public PageResult<DefectDTO> getDefectsByTestRunId(UUID testRunId, int page, int size) {
        try {
            SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                    .pageNumber(page).pageSize(size).build();
            GroupConditionDto condition = conditionByField("testRunId", testRunId.toString());
            PageResult<EntityWithMetadata<DefectDTO>> result =
                    entityService.search(MODEL_SPEC, condition, DefectDTO.class, params);
            return toPage(result);
        } catch (Exception e) {
            // No defects with testRunId yet — return an empty page rather than propagating a 500.
            return PageResult.of(null, List.of(), page, size, 0);
        }
    }

    public DefectDTO updateDefect(UUID id, DefectDTO defect) {
        defect.setUpdatedAt(LocalDateTime.now());
        return withId(entityService.update(id, defect, null));
    }

    /**
     * Returns ALL defects for a project without pagination.
     * Used internally for cascade-delete operations.
     */
    public List<DefectDTO> getAllDefectsByProjectId(UUID projectId) {
        GroupConditionDto condition = conditionByField("projectId", projectId.toString());
        return entityService.search(MODEL_SPEC, condition, DefectDTO.class).data()
                .stream()
                .map(this::withId)
                .toList();
    }

    public boolean deleteDefect(UUID id) {
        entityService.deleteById(id);
        return true;
    }

    public boolean defectExists(UUID id) {
        return getDefectById(id).isPresent();
    }
}

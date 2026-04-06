package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.common.condition.GroupCondition;
import org.cyoda.cloud.api.event.common.condition.Operation;
import org.cyoda.cloud.api.event.common.condition.SimpleCondition;
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

    private GroupCondition conditionByField(String jsonPath, String value) {
        SimpleCondition condition = new SimpleCondition()
                .withJsonPath("$." + jsonPath)
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(value));
        return new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(condition));
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
        GroupCondition condition = conditionByField("projectId", projectId.toString());
        PageResult<EntityWithMetadata<DefectDTO>> result =
                entityService.search(MODEL_SPEC, condition, DefectDTO.class, params);
        return toPage(result);
    }

    public List<DefectDTO> getDefectsByStatus(UUID projectId, String status) {
        SimpleCondition projectCondition = new SimpleCondition()
                .withJsonPath("$.projectId")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(projectId.toString()));
        SimpleCondition statusCondition = new SimpleCondition()
                .withJsonPath("$.status")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(status));
        GroupCondition condition = new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(projectCondition, statusCondition));
        return entityService.search(MODEL_SPEC, condition, DefectDTO.class)
                .data().stream().map(this::withId).toList();
    }

    public DefectDTO updateDefect(UUID id, DefectDTO defect) {
        defect.setUpdatedAt(LocalDateTime.now());
        return withId(entityService.update(id, defect, null));
    }

    public boolean deleteDefect(UUID id) {
        entityService.deleteById(id);
        return true;
    }

    public boolean defectExists(UUID id) {
        return getDefectById(id).isPresent();
    }
}

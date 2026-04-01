package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
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

    private GroupCondition conditionByField(String fieldName, Object value) {
        SimpleCondition condition = new SimpleCondition()
                .withJsonPath("$." + fieldName)
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(value));
        return new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(condition));
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
        // Filter in-memory: the Cyoda entity model for Defect was registered before
        // projectId was added, so $.projectId is not a valid search path in the schema.
        List<DefectDTO> all = entityService.findAll(MODEL_SPEC, DefectDTO.class)
                .data().stream()
                .map(this::withId)
                .filter(d -> projectId.equals(d.getProjectId()))
                .toList();
        int from = page * size;
        int to = Math.min(from + size, all.size());
        List<DefectDTO> pageData = from < all.size() ? all.subList(from, to) : List.of();
        return PageResult.of(null, pageData, page, size, (long) all.size());
    }

    public List<DefectDTO> getDefectsByStatus(UUID projectId, String status) {
        // Search by projectId first, then filter by status in memory
        // (Cyoda search supports one simple condition at a time via this helper)
        return getDefectsByProjectId(projectId, 0, Integer.MAX_VALUE)
                .data().stream()
                .filter(d -> status.equals(d.getStatus()))
                .toList();
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

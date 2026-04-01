package com.java_template.application.service;

import com.java_template.application.dto.DefectDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
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

    public DefectService(EntityService entityService) {
        this.entityService = entityService;
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
        PageResult<EntityWithMetadata<DefectDTO>> allDefects =
                entityService.findAll(MODEL_SPEC, DefectDTO.class, params);

        var filteredDefects = allDefects.data().stream()
                .filter(d -> d.entity().getProjectId() != null && d.entity().getProjectId().equals(projectId))
                .toList();

        return PageResult.of(allDefects.searchId(),
                filteredDefects.stream().map(this::withId).toList(),
                page, size, filteredDefects.size());
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

package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ProjectDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.common.condition.GroupCondition;
import org.cyoda.cloud.api.event.common.condition.Operation;
import org.cyoda.cloud.api.event.common.condition.SimpleCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Project operations
 */
@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(ProjectDTO.ENTITY_NAME).withVersion(ProjectDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final ObjectMapper objectMapper;

    public ProjectService(EntityService entityService, ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
    }

    private ProjectDTO withId(EntityWithMetadata<ProjectDTO> result) {
        ProjectDTO entity = result.entity();
        entity.setId(result.getId());

        // Timestamps are already set as strings in the entity
        // If somehow they're still null, set them to current time
        if (entity.getCreatedAt() == null) {
            String now = java.time.LocalDateTime.now().toString() + "Z";
            entity.setCreatedAt(now);
            logger.warn("[Project] Setting createdAt to current time: {}", now);
        }

        if (entity.getUpdatedAt() == null) {
            String now = java.time.LocalDateTime.now().toString() + "Z";
            entity.setUpdatedAt(now);
            logger.warn("[Project] Setting updatedAt to current time: {}", now);
        }

        return entity;
    }

    private PageResult<ProjectDTO> toPage(PageResult<EntityWithMetadata<ProjectDTO>> result) {
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

    public ProjectDTO createProject(ProjectDTO project) {
        project.setStatus("ACTIVE");

        // Set timestamps as ISO-8601 strings BEFORE saving to Cyoda
        String now = java.time.LocalDateTime.now().toString() + "Z";
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        logger.info("[Project] Creating project: {} with timestamp: {}", project.getName(), now);

        try {
            EntityWithMetadata<ProjectDTO> result = entityService.create(project);
            ProjectDTO entity = result.entity();
            entity.setId(result.getId());

            // Timestamps should already be set from project object above
            logger.info("[Project] Created project: id={}, createdAt={}, updatedAt={}",
                entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt());

            return entity;
        } catch (Exception e) {
            logger.error("[Project] Failed to create project: {}", project.getName(), e);
            throw e;
        }
    }

    public Optional<ProjectDTO> getProjectById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, ProjectDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public PageResult<ProjectDTO> getAllProjects(int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        return toPage(entityService.findAll(MODEL_SPEC, ProjectDTO.class, params));
    }

    public ProjectDTO updateProject(UUID id, ProjectDTO project) {
        return withId(entityService.update(id, project, null));
    }

    public boolean deleteProject(UUID id) {
        entityService.deleteById(id);
        return true;
    }

    public List<ProjectDTO> searchProjects(String query) {
        GroupCondition condition = conditionByField("name", query);
        return entityService.search(MODEL_SPEC, condition, ProjectDTO.class).data()
                .stream().map(this::withId).toList();
    }

    public boolean projectExists(UUID id) {
        return getProjectById(id).isPresent();
    }
}


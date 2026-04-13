package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ProjectDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    private final SuiteService suiteService;
    private final TestCaseService testCaseService;
    private final TestStepService testStepService;
    private final TestRunService testRunService;
    private final TestRunCaseService testRunCaseService;
    private final TestRunStepService testRunStepService;
    private final DefectService defectService;
    private final AttachmentService attachmentService;
    private final ProjectCounterService projectCounterService;

    public ProjectService(EntityService entityService,
                          ObjectMapper objectMapper,
                          @Lazy SuiteService suiteService,
                          @Lazy TestCaseService testCaseService,
                          @Lazy TestStepService testStepService,
                          @Lazy TestRunService testRunService,
                          @Lazy TestRunCaseService testRunCaseService,
                          @Lazy TestRunStepService testRunStepService,
                          @Lazy DefectService defectService,
                          @Lazy AttachmentService attachmentService,
                          @Lazy ProjectCounterService projectCounterService) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
        this.suiteService = suiteService;
        this.testCaseService = testCaseService;
        this.testStepService = testStepService;
        this.testRunService = testRunService;
        this.testRunCaseService = testRunCaseService;
        this.testRunStepService = testRunStepService;
        this.defectService = defectService;
        this.attachmentService = attachmentService;
        this.projectCounterService = projectCounterService;
    }

    private ProjectDTO withId(EntityWithMetadata<ProjectDTO> result) {
        ProjectDTO entity = result.entity();
        entity.setId(result.getId());

        // Use the actual Cyoda metadata creation date as a fallback when
        // the entity's own createdAt field is missing. Never default to
        // the current time, which would mask missing data with today's date.
        if (entity.getCreatedAt() == null) {
            java.util.Date metaCreated = result.getCreationDate();
            if (metaCreated != null) {
                entity.setCreatedAt(metaCreated.toInstant().toString());
                logger.warn("[Project] createdAt missing on entity {}; using metadata creation date: {}",
                        result.getId(), entity.getCreatedAt());
            } else {
                logger.warn("[Project] createdAt missing on entity {} and no metadata date available",
                        result.getId());
            }
        }

        if (entity.getUpdatedAt() == null) {
            java.util.Date metaCreated = result.getCreationDate();
            if (metaCreated != null) {
                entity.setUpdatedAt(metaCreated.toInstant().toString());
                logger.warn("[Project] updatedAt missing on entity {}; using metadata creation date: {}",
                        result.getId(), entity.getUpdatedAt());
            } else {
                logger.warn("[Project] updatedAt missing on entity {} and no metadata date available",
                        result.getId());
            }
        }

        return entity;
    }

    private PageResult<ProjectDTO> toPage(PageResult<EntityWithMetadata<ProjectDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }

    private GroupConditionDto conditionByField(String fieldName, Object value) {
        SimpleConditionDto condition = new SimpleConditionDto()
                .jsonPath("$." + fieldName)
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(value));
        condition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(condition));
        group.setType(QueryConditionTypeDto.GROUP);
        return group;
    }

    public ProjectDTO createProject(ProjectDTO project) {
        // Set timestamps as ISO-8601 UTC strings BEFORE saving to Cyoda
        String now = Instant.now().toString();
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
        logger.info("[Project] Fetching project by ID: {}", id);

        // Retry logic: Cyoda might not have indexed the entity yet
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                EntityWithMetadata<ProjectDTO> result = entityService.getById(id, MODEL_SPEC, ProjectDTO.class);
                logger.info("[Project] Found project on attempt {}: {}", attempt, id);
                return Optional.of(withId(result));
            } catch (Exception e) {
                logger.warn("[Project] Failed to fetch project on attempt {}: {} - {}", attempt, id, e.getMessage());
                if (attempt < 3) {
                    try {
                        Thread.sleep(500); // Wait 500ms before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("[Project] Project not found after 3 attempts: {}", id);
        return Optional.empty();
    }

    public PageResult<ProjectDTO> getAllProjects(int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        return toPage(entityService.findAll(MODEL_SPEC, ProjectDTO.class, params));
    }

    public ProjectDTO updateProject(UUID id, ProjectDTO project) {
        // Always send a non-null updatedAt — Cyoda's internal ObjectMapper does NOT respect
        // @JsonInclude(NON_NULL), so null is still serialised and sent. If the schema was
        // registered with updatedAt present (which createProject now guarantees), sending
        // null causes INVALID_ARGUMENT: No DataType defined for path $.updatedAt.
        project.setId(null);
        project.setUpdatedAt(Instant.now().toString());
        return withId(entityService.update(id, project, null));
    }

    /**
     * Cascade-deletes a project and ALL of its child entities in strict bottom-up order.
     *
     * <p>Deletion order (leaves first, root last):</p>
     * <ol>
     *   <li>TestRunStep  — fetched via TestRunCase IDs (parallel per-case queries)</li>
     *   <li>TestRunCase  — fetched directly by projectId (single query)</li>
     *   <li>TestRun      — fetched by projectId</li>
     *   <li>TestStep     — fetched via TestCase IDs (parallel per-case queries)</li>
     *   <li>TestCase     — fetched by projectId (including soft-deleted)</li>
     *   <li>Suite        — fetched by projectId</li>
     *   <li>Defect       — fetched by projectId</li>
     *   <li>Attachment   — fetched by projectId</li>
     *   <li>ProjectCounter</li>
     *   <li>Project</li>
     * </ol>
     *
     * <p>CompletableFuture is used to parallelise the per-entity-instance child
     * lookups (TestRunStep per TestRunCase, TestStep per TestCase) and reduce total
     * round-trip time when a project contains many test cases or run cases.</p>
     */
    public boolean deleteProject(UUID id) {
        if (!projectExists(id)) {
            logger.warn("[Project] Delete requested for non-existent project: {}", id);
            return false;
        }
        logger.info("[Project] Starting cascade delete for project: {}", id);

        // ── Phase 1: Execution data (TestRunSteps → TestRunCases → TestRuns) ─────────
        var allRunCases = testRunCaseService.getAllTestRunCasesByProjectId(id);
        logger.info("[Project] Cascade: {} TestRunCase(s) to delete", allRunCases.size());

        if (!allRunCases.isEmpty()) {
            var stepFutures = allRunCases.stream()
                    .map(rc -> CompletableFuture.runAsync(() ->
                            testRunStepService.getAllTestRunStepsByTestRunCaseId(rc.getId())
                                    .forEach(s -> testRunStepService.deleteTestRunStep(s.getId()))))
                    .toList();
            CompletableFuture.allOf(stepFutures.toArray(new CompletableFuture[0])).join();
            allRunCases.forEach(rc -> testRunCaseService.deleteTestRunCase(rc.getId()));
        }

        testRunService.getAllTestRunsByProjectId(id)
                .forEach(run -> testRunService.deleteTestRun(run.getId()));

        // ── Phase 2: Master data (TestSteps → TestCases → Suites) ────────────────────
        var allCases = testCaseService.getAllCasesByProjectIdIncludingDeleted(id);
        logger.info("[Project] Cascade: {} TestCase(s) to delete (including soft-deleted)", allCases.size());

        if (!allCases.isEmpty()) {
            var stepFutures = allCases.stream()
                    .map(tc -> CompletableFuture.runAsync(() ->
                            testStepService.getAllTestStepsByTestCaseIdIncludingDeleted(tc.getId())
                                    .forEach(s -> testStepService.hardDeleteTestStep(s.getId()))))
                    .toList();
            CompletableFuture.allOf(stepFutures.toArray(new CompletableFuture[0])).join();
            allCases.forEach(tc -> testCaseService.hardDeleteTestCase(tc.getId()));
        }

        suiteService.getAllSuitesByProjectId(id)
                .forEach(suite -> entityService.deleteById(suite.getId()));

        // ── Phase 3: Defects, attachments, counter ────────────────────────────────────
        defectService.getAllDefectsByProjectId(id)
                .forEach(d -> defectService.deleteDefect(d.getId()));

        attachmentService.getAllAttachmentsByProjectId(id)
                .forEach(a -> attachmentService.deleteAttachment(a.getId()));

        projectCounterService.deleteCounterForProject(id);

        // ── Phase 4: The project itself ────────────────────────────────────────────────
        entityService.deleteById(id);
        logger.info("[Project] Cascade delete complete for project: {}", id);
        return true;
    }

    public List<ProjectDTO> searchProjects(String query) {
        GroupConditionDto condition = conditionByField("name", query);
        return entityService.search(MODEL_SPEC, condition, ProjectDTO.class).data()
                .stream().map(this::withId).toList();
    }

    public boolean projectExists(UUID id) {
        return getProjectById(id).isPresent();
    }
}


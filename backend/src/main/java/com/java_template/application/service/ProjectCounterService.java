package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ProjectCounterDTO;
import com.java_template.application.dto.TestCaseDTO;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides globally unique, strictly-increasing, never-reusable display IDs
 * for test cases within a project.
 *
 * Format: "TC-{n}"  (e.g. TC-1, TC-42, TC-100)
 *
 * One {@link ProjectCounterDTO} record is stored per project.  On first use the
 * counter is bootstrapped by scanning ALL existing test-case displayIds
 * (including soft-deleted ones) so deleted IDs are never recycled.
 *
 * A per-project in-process lock prevents duplicate assignments when multiple
 * cases are created in quick succession within the same JVM instance.
 */
@Service
public class ProjectCounterService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCounterService.class);

    private static final ModelSpec COUNTER_SPEC = new ModelSpec()
            .withName(ProjectCounterDTO.ENTITY_NAME)
            .withVersion(ProjectCounterDTO.ENTITY_VERSION);

    private static final ModelSpec CASE_SPEC = new ModelSpec()
            .withName(TestCaseDTO.ENTITY_NAME)
            .withVersion(TestCaseDTO.ENTITY_VERSION);

    /** Matches "TC-{digits}" display IDs produced by this service. */
    private static final Pattern TC_PATTERN = Pattern.compile("^TC-(\\d+)$");

    private final EntityService entityService;
    private final ObjectMapper objectMapper;

    /**
     * One monitor object per projectId so concurrent creates for different
     * projects don't block each other unnecessarily.
     */
    private final ConcurrentHashMap<UUID, Object> projectLocks = new ConcurrentHashMap<>();

    public ProjectCounterService(EntityService entityService, ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.objectMapper = objectMapper;
    }

    private GroupCondition conditionByProjectId(UUID projectId) {
        SimpleCondition condition = new SimpleCondition()
                .withJsonPath("$.projectId")
                .withOperation(Operation.EQUALS)
                .withValue(objectMapper.valueToTree(projectId.toString()));
        return new GroupCondition()
                .withOperator(GroupCondition.Operator.AND)
                .withConditions(List.of(condition));
    }

    /**
     * Returns the next unique display ID for a new test case in {@code projectId}.
     * The returned value is immediately reserved; subsequent calls will never
     * return the same value for the same project.
     */
    public String nextDisplayId(UUID projectId) {
        Object lock = projectLocks.computeIfAbsent(projectId, k -> new Object());
        synchronized (lock) {
            Optional<ProjectCounterDTO> existing = findCounterForProject(projectId);

            if (existing.isPresent()) {
                ProjectCounterDTO counter = existing.get();
                long assigned = counter.getNextId();
                counter.setNextId(assigned + 1);
                entityService.update(counter.getId(), counter, null);
                logger.debug("Assigned TC-{} for project {}", assigned, projectId);
                return "TC-" + assigned;
            } else {
                // First time for this project: scan ALL cases (including deleted) to
                // find the highest number ever used, then start above it.
                long maxUsed = scanMaxUsedId(projectId);
                long assigned = maxUsed + 1;

                ProjectCounterDTO counter = new ProjectCounterDTO();
                counter.setProjectId(projectId);
                counter.setNextId(assigned + 1);   // store what comes AFTER the one we're returning
                entityService.create(counter);

                logger.info("Initialized TC counter for project {} — first assigned: TC-{} (prev max: {})",
                        projectId, assigned, maxUsed);
                return "TC-" + assigned;
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Optional<ProjectCounterDTO> findCounterForProject(UUID projectId) {
        try {
            // Server-side filter: only fetch counters for this project (at most one record).
            GroupCondition condition = conditionByProjectId(projectId);
            return entityService.search(COUNTER_SPEC, condition, ProjectCounterDTO.class)
                    .data().stream()
                    .map(e -> {
                        ProjectCounterDTO dto = e.entity();
                        dto.setId(e.getId());
                        return dto;
                    })
                    .findFirst();
        } catch (Exception ex) {
            logger.warn("Could not query ProjectCounter for project {}: {}", projectId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Scans all test cases for this project (including soft-deleted) and returns
     * the highest numeric suffix found in "TC-{n}" display IDs, or 0 if none.
     * Uses a server-side projectId filter so only this project's cases are loaded.
     */
    private long scanMaxUsedId(UUID projectId) {
        try {
            GroupCondition condition = conditionByProjectId(projectId);
            SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                    .pageNumber(0).pageSize(10_000).build();
            return entityService.search(CASE_SPEC, condition, TestCaseDTO.class, params)
                    .data().stream()
                    .map(e -> e.entity().getDisplayId())
                    .filter(id -> id != null)
                    .mapToLong(id -> {
                        Matcher m = TC_PATTERN.matcher(id);
                        return m.matches() ? Long.parseLong(m.group(1)) : 0L;
                    })
                    .max()
                    .orElse(0L);
        } catch (Exception ex) {
            logger.warn("Could not scan existing case IDs for project {}: {}", projectId, ex.getMessage());
            return 0L;
        }
    }
}

package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
import com.java_template.application.dto.ProjectCounterDTO;
import com.java_template.application.dto.ReportDTO;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunDTO;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ObjLongConsumer;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides globally unique, strictly-increasing, never-reusable display IDs
 * for test cases (TC-N), test runs (TR-N), and defects (DEF-N) within a project.
 *
 * One {@link ProjectCounterDTO} record is stored per project, containing
 * separate counters for each entity type.  On first use each counter is
 * bootstrapped by scanning existing displayIds (including soft-deleted records)
 * so that deleted IDs are never recycled.
 *
 * A per-project in-process lock prevents duplicate assignments when multiple
 * entities are created concurrently within the same JVM instance.
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

    private static final ModelSpec RUN_SPEC = new ModelSpec()
            .withName(TestRunDTO.ENTITY_NAME)
            .withVersion(TestRunDTO.ENTITY_VERSION);

    private static final ModelSpec DEFECT_SPEC = new ModelSpec()
            .withName(DefectDTO.ENTITY_NAME)
            .withVersion(DefectDTO.ENTITY_VERSION);

    private static final ModelSpec REPORT_SPEC = new ModelSpec()
            .withName(ReportDTO.ENTITY_NAME)
            .withVersion(ReportDTO.ENTITY_VERSION);

    private static final Pattern TC_PATTERN  = Pattern.compile("^TC-(\\d+)$");
    private static final Pattern TR_PATTERN  = Pattern.compile("^TR-(\\d+)$");
    private static final Pattern DEF_PATTERN = Pattern.compile("^DEF-(\\d+)$");
    private static final Pattern REP_PATTERN = Pattern.compile("^REP-(\\d+)$");

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

    // ── Public API ────────────────────────────────────────────────────────────

    /** Next unique TC-N display ID for a new test case. */
    public String nextDisplayId(UUID projectId) {
        return nextDisplayIdBatch(projectId, 1).get(0);
    }

    /** Batch-reserve {@code count} TC-N IDs (one counter round-trip). */
    public List<String> nextDisplayIdBatch(UUID projectId, int count) {
        return nextBatch(projectId, count, "TC",
                ProjectCounterDTO::getNextId,
                ProjectCounterDTO::setNextId,
                () -> scanMaxForSpec(projectId, CASE_SPEC, TestCaseDTO.class,
                        TestCaseDTO::getDisplayId, TC_PATTERN));
    }

    /** Next unique TR-N display ID for a new test run. */
    public String nextRunDisplayId(UUID projectId) {
        return nextBatch(projectId, 1, "TR",
                ProjectCounterDTO::getNextRunId,
                ProjectCounterDTO::setNextRunId,
                () -> scanMaxForSpec(projectId, RUN_SPEC, TestRunDTO.class,
                        TestRunDTO::getDisplayId, TR_PATTERN)).get(0);
    }

    /** Next unique DEF-N display ID for a new defect. */
    public String nextDefectDisplayId(UUID projectId) {
        return nextBatch(projectId, 1, "DEF",
                ProjectCounterDTO::getNextDefectId,
                ProjectCounterDTO::setNextDefectId,
                () -> scanMaxForSpec(projectId, DEFECT_SPEC, DefectDTO.class,
                        DefectDTO::getDisplayId, DEF_PATTERN)).get(0);
    }

    /** Next unique REP-N display ID for a new report. */
    public String nextReportDisplayId(UUID projectId) {
        return nextBatch(projectId, 1, "REP",
                ProjectCounterDTO::getNextReportId,
                ProjectCounterDTO::setNextReportId,
                () -> scanMaxForSpec(projectId, REPORT_SPEC, ReportDTO.class,
                        ReportDTO::getDisplayId, REP_PATTERN)).get(0);
    }

    /**
     * Deletes the ProjectCounter record for the given project, if it exists.
     * Called during cascade deletion of a Project entity.
     */
    public void deleteCounterForProject(UUID projectId) {
        findCounterForProject(projectId).ifPresent(counter -> {
            entityService.deleteById(counter.getId());
            logger.info("Deleted ProjectCounter for project {}", projectId);
        });
    }

    // ── Core generic implementation ───────────────────────────────────────────

    /**
     * Reserves {@code count} IDs from the specified counter field and returns
     * formatted strings like ["TR-1","TR-2"].
     *
     * @param getter    reads the relevant counter field from the DTO
     * @param setter    writes the incremented value back
     * @param bootstrap called once to scan the max already-used ID when no
     *                  counter record exists yet (never-reuse guarantee)
     */
    private List<String> nextBatch(UUID projectId, int count, String prefix,
                                   ToLongFunction<ProjectCounterDTO> getter,
                                   ObjLongConsumer<ProjectCounterDTO> setter,
                                   java.util.function.LongSupplier bootstrap) {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        logger.warn("===== NEXTBATCH CALLED ===== prefix={}, projectId={}", prefix, projectId);
        Object lock = projectLocks.computeIfAbsent(projectId, k -> new Object());
        synchronized (lock) {
            logger.warn("===== FINDCOUNTERFORPROJECT =====");
            Optional<ProjectCounterDTO> existing = findCounterForProject(projectId);
            logger.warn("===== FINDCOUNTERFORPROJECT DONE, exists={} =====", existing.isPresent());

            long firstAssigned;
            if (existing.isPresent()) {
                ProjectCounterDTO counter = existing.get();
                firstAssigned = getter.applyAsLong(counter);
                if (firstAssigned == 0) {
                    // Field not yet initialised on an older counter record — bootstrap
                    firstAssigned = bootstrap.getAsLong() + 1;
                }
                setter.accept(counter, firstAssigned + count);
                entityService.update(counter.getId(), counter, null);
                logger.debug("Assigned {}-{}..{}-{} for project {} (batch={})",
                        prefix, firstAssigned, prefix, firstAssigned + count - 1, projectId, count);
            } else {
                long maxUsed = bootstrap.getAsLong();
                firstAssigned = maxUsed + 1;

                ProjectCounterDTO counter = new ProjectCounterDTO();
                counter.setId(UUID.randomUUID());  // ← SET ID BEFORE CREATING!
                counter.setProjectId(projectId);
                // Initialise all counters so the record is complete from the start
                counter.setNextId(prefix.equals("TC") ? firstAssigned + count : 1);
                counter.setNextRunId(prefix.equals("TR") ? firstAssigned + count : 1);
                counter.setNextDefectId(prefix.equals("DEF") ? firstAssigned + count : 1);
                counter.setNextReportId(prefix.equals("REP") ? firstAssigned + count : 1);

                // Log the JSON
                try {
                    String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(counter);
                    logger.warn("===== ABOUT TO CREATE PROJECTCOUNTER WITH JSON =====\n{}\n=====", json);
                } catch (Exception e) {
                    logger.error("Failed to serialize counter", e);
                }

                entityService.create(counter);
                logger.warn("===== PROJECTCOUNTER CREATED SUCCESSFULLY =====");

                logger.info("Initialized {} counter for project {} — first batch: {}-{}..{}-{} (prev max: {})",
                        prefix, projectId, prefix, firstAssigned, prefix, firstAssigned + count - 1, maxUsed);
            }

            List<String> ids = new java.util.ArrayList<>(count);
            for (long i = 0; i < count; i++) {
                ids.add(prefix + "-" + (firstAssigned + i));
            }
            return ids;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GroupConditionDto conditionByProjectId(UUID projectId) {
        SimpleConditionDto condition = new SimpleConditionDto()
                .jsonPath("$.projectId")
                .operation(OperatorTypeDto.EQUALS)
                .value(objectMapper.valueToTree(projectId.toString()));
        condition.setType(QueryConditionTypeDto.SIMPLE);
        GroupConditionDto group = new GroupConditionDto()
                .operator(GroupOperatorDto.AND)
                .conditions(List.of(condition));
        group.setType(QueryConditionTypeDto.GROUP);
        return group;
    }

    private Optional<ProjectCounterDTO> findCounterForProject(UUID projectId) {
        try {
            GroupConditionDto condition = conditionByProjectId(projectId);
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
     * Scans all entities of the given spec/class for this project and returns the
     * highest numeric suffix found in displayIds matching {@code pattern}, or 0.
     * Used once per entity type to bootstrap the counter from pre-existing data.
     */
    private <T extends com.java_template.common.workflow.CyodaEntity> long scanMaxForSpec(
            UUID projectId, ModelSpec spec, Class<T> clazz,
            java.util.function.Function<T, String> idExtractor, Pattern pattern) {
        try {
            logger.warn("===== SCANMAXFORSPEC STARTING ===== spec={}, projectId={}", spec.getName(), projectId);
            GroupConditionDto condition = conditionByProjectId(projectId);
            SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                    .pageNumber(0).pageSize(10_000).build();
            logger.warn("===== CALLING entityService.search() =====");
            var result = entityService.search(spec, condition, clazz, params);
            logger.warn("===== entityService.search() RETURNED, scanning data =====");
            return result.data().stream()
                    .map(e -> idExtractor.apply(e.entity()))
                    .filter(id -> id != null)
                    .mapToLong(id -> {
                        Matcher m = pattern.matcher(id);
                        return m.matches() ? Long.parseLong(m.group(1)) : 0L;
                    })
                    .max()
                    .orElse(0L);
        } catch (Exception ex) {
            logger.warn("Could not scan existing {} IDs for project {}: {}",
                    pattern.pattern(), projectId, ex.getMessage());
            return 0L;
        }
    }

    /**
     * Initialize ProjectCounter for a new project with all counters starting at 1.
     * This avoids expensive scanning on first TestCase/TestRun/Defect creation.
     *
     * If the counter already exists, this is a no-op (idempotent).
     */
    public void initializeCounterForProject(UUID projectId) {
        try {
            // Check if counter already exists
            Optional<ProjectCounterDTO> existing = findCounterForProject(projectId);
            if (existing.isPresent()) {
                logger.info("[Counter] ProjectCounter already exists for project: {}", projectId);
                return;  // Already initialized, nothing to do
            }

            logger.info("[Counter] Initializing ProjectCounter for project: {}", projectId);

            // Create new counter with all counters starting at 1
            ProjectCounterDTO counter = new ProjectCounterDTO();
            counter.setId(UUID.randomUUID());  // Generate unique ID
            counter.setProjectId(projectId);
            counter.setNextId(1L);           // TC-N counter
            counter.setNextRunId(1L);        // TR-N counter
            counter.setNextDefectId(1L);     // DEF-N counter
            counter.setNextReportId(1L);     // REP-N counter

            // Initialize counter in Cyoda (no timestamps on ProjectCounter)
            entityService.create(counter);
            logger.info("[Counter] ProjectCounter initialized successfully for project: {}", projectId);
        } catch (Exception e) {
            logger.warn("[Counter] Failed to initialize ProjectCounter for project {}: {}", projectId, e.getMessage());
            // Don't throw — let next operation handle counter creation if needed
        }
    }
}

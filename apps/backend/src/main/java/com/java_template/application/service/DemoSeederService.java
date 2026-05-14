package com.java_template.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Seeds the "E-commerce Platform" demo project into the TMS.
 *
 * Reads demo-data.json from the classpath and creates all entities in the
 * correct dependency order using existing business services.
 * Idempotent: if a project named "E-commerce Platform" already exists the
 * seed is skipped and the existing project ID is returned.
 */
@Service
public class DemoSeederService {

    private static final Logger log = LoggerFactory.getLogger(DemoSeederService.class);
    private static final String DEMO_PROJECT_NAME = "E-commerce Platform";
    private static final String DEMO_DATA_PATH    = "demo/demo-data.json";

    /** Pause between major phases — lets Cyoda finish indexing before next batch of calls. */
    private static final long PHASE_DELAY_MS = 3_000;
    /** Short pause between create + update on the same entity. */
    private static final long STEP_DELAY_MS  =   800;
    /**
     * Thread pool size for parallel entity creation.
     * Kept intentionally low — Cyoda enforces a concurrent gRPC request limit
     * (typically 300). With 3 threads each making sequential calls we stay well
     * within that budget while still getting ~3× speedup over fully sequential.
     */
    private static final int  SEED_PARALLELISM = 3;

    // ── Injected services ────────────────────────────────────────────────────
    private final ProjectService      projectService;
    private final SuiteService        suiteService;
    private final TestCaseService     testCaseService;
    private final TestRunService      testRunService;
    private final TestRunCaseService  testRunCaseService;
    private final TestRunStepService  testRunStepService;
    private final DefectService       defectService;
    private final AttachmentService   attachmentService;
    private final ReportService       reportService;
    private final ObjectMapper        objectMapper;

    public DemoSeederService(ProjectService projectService,
                             SuiteService suiteService,
                             TestCaseService testCaseService,
                             TestRunService testRunService,
                             TestRunCaseService testRunCaseService,
                             TestRunStepService testRunStepService,
                             DefectService defectService,
                             AttachmentService attachmentService,
                             ReportService reportService,
                             ObjectMapper objectMapper) {
        this.projectService     = projectService;
        this.suiteService       = suiteService;
        this.testCaseService    = testCaseService;
        this.testRunService     = testRunService;
        this.testRunCaseService = testRunCaseService;
        this.testRunStepService = testRunStepService;
        this.defectService      = defectService;
        this.attachmentService  = attachmentService;
        this.reportService      = reportService;
        this.objectMapper       = objectMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Map<String, Object> seed() {
        // 1. Idempotency guard
        List<ProjectDTO> existing = projectService.searchProjects(DEMO_PROJECT_NAME);
        if (existing.stream().anyMatch(p -> DEMO_PROJECT_NAME.equals(p.getName()))) {
            UUID existingId = existing.stream()
                    .filter(p -> DEMO_PROJECT_NAME.equals(p.getName()))
                    .findFirst().map(ProjectDTO::getId).orElse(null);
            log.info("[DemoSeeder] Demo project already exists: {}", existingId);
            return Map.of("status", "skipped",
                          "message", "Demo project '" + DEMO_PROJECT_NAME + "' already exists.",
                          "projectId", String.valueOf(existingId));
        }

        try {
            DemoData data = loadDemoData();
            return runSeed(data);
        } catch (Exception e) {
            log.error("[DemoSeeder] Seed failed: {}", e.getMessage(), e);
            throw new RuntimeException("Demo seed failed: " + e.getMessage(), e);
        }
    }

    // ── Core seed logic ───────────────────────────────────────────────────────

    private Map<String, Object> runSeed(DemoData data) {
        ExecutorService exec = Executors.newFixedThreadPool(SEED_PARALLELISM, r -> {
            Thread t = new Thread(r, "demo-seed");
            t.setDaemon(true);
            return t;
        });
        try {
            return doSeed(data, exec);
        } finally {
            exec.shutdown();
        }
    }

    private Map<String, Object> doSeed(DemoData data, ExecutorService exec) {
        // STEP 1 – Project (sequential — everything depends on projectId)
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setName(data.project.name);
        projectDTO.setDescription(data.project.description);
        ProjectDTO project = projectService.createProject(projectDTO);
        UUID projectId = project.getId();
        log.info("[DemoSeeder] Created project: {}", projectId);

        // STEP 2-3 – Suites + TestCases (parallel per suite)
        ConcurrentHashMap<String, UUID>     caseRefToId   = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, DemoCase> caseRefToData = new ConcurrentHashMap<>();
        List<String> allCaseIds = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> suiteFutures = data.suites.stream()
                .map(suiteData -> CompletableFuture.runAsync(() -> {
                    SuiteDTO suiteDTO = new SuiteDTO();
                    suiteDTO.setProjectId(projectId);
                    suiteDTO.setName(suiteData.name);
                    suiteDTO.setDescription(suiteData.description);
                    SuiteDTO suite = suiteService.createSuite(suiteDTO);
                    UUID suiteId = suite.getId();
                    log.info("[DemoSeeder] Created suite '{}': {}", suiteData.name, suiteId);

                    for (DemoCase caseData : suiteData.cases) {
                        TestCaseDTO tc = new TestCaseDTO();
                        tc.setProjectId(projectId);
                        tc.setSuiteId(suiteId);
                        tc.setTitle(caseData.title);
                        tc.setDescription(caseData.description);
                        tc.setPreconditions(caseData.preconditions);
                        tc.setPriority(Priority.valueOf(caseData.priority));
                        tc.setSteps(toStepDTOs(caseData.steps));
                        TestCaseDTO created = testCaseService.createTestCase(tc);
                        caseRefToId.put(caseData.ref, created.getId());
                        caseRefToData.put(caseData.ref, caseData);
                        allCaseIds.add(created.getId().toString());
                        log.info("[DemoSeeder] Created case '{}' [{}]: {}", caseData.title, caseData.ref, created.getId());
                    }
                }, exec))
                .toList();
        joinAll(suiteFutures, "suites+cases");
        sleep(PHASE_DELAY_MS);

        // STEP 4 – Case attachments (parallel — independent of each other)
        List<CompletableFuture<Void>> attFutures = data.caseAttachments.stream()
                .map(att -> CompletableFuture.runAsync(() -> {
                    UUID caseId = caseRefToId.get(att.caseRef);
                    if (caseId == null) { log.warn("[DemoSeeder] Unknown caseRef for attachment: {}", att.caseRef); return; }
                    AttachmentDTO a = buildAttachment(projectId, caseId, null, att.fileName, att.fileType, att.content, "CASE", null, null);
                    attachmentService.uploadAttachment(a);
                    log.info("[DemoSeeder] Attached '{}' to case {}", att.fileName, caseId);
                }, exec))
                .toList();
        joinAll(attFutures, "case-attachments");

        // STEP 5 – TestRun (sequential — needs complete caseIds list)
        TestRunDTO runDTO = new TestRunDTO();
        runDTO.setProjectId(projectId);
        runDTO.setName(data.testRun.name);
        runDTO.setEnvironment(data.testRun.environment);
        runDTO.setBuildVersion(data.testRun.buildVersion);
        runDTO.setDescription(data.testRun.description);
        runDTO.setCaseIds(new ArrayList<>(allCaseIds));
        TestRunDTO run = testRunService.createTestRun(runDTO);
        UUID runId = run.getId();
        log.info("[DemoSeeder] Created TestRun: {}", runId);
        sleep(PHASE_DELAY_MS);

        // STEP 6-8 – TestRunCases + TestRunSteps (parallel per case)
        ConcurrentHashMap<String, UUID>   caseRefToRunCaseId = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> stepStatusMap      = new ConcurrentHashMap<>();
        AtomicInteger totalPassed  = new AtomicInteger(0);
        AtomicInteger totalFailed  = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);

        List<CompletableFuture<Void>> trcFutures = new ArrayList<>(caseRefToId.entrySet()).stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    String   ref    = entry.getKey();
                    UUID     caseId = entry.getValue();
                    DemoCase cd     = caseRefToData.get(ref);

                    TestRunCaseDTO trc = buildTestRunCase(runId, caseId, projectId, cd);
                    TestRunCaseDTO createdTrc = testRunCaseService.createTestRunCase(trc);
                    UUID trcId = createdTrc.getId();
                    caseRefToRunCaseId.put(ref, trcId);

                    for (int i = 0; i < cd.steps.size(); i++) {
                        DemoStep stepData = cd.steps.get(i);
                        String   outcome  = (i < cd.stepOutcomes.size()) ? cd.stepOutcomes.get(i) : "UNTESTED";

                        TestRunStepDTO trs = new TestRunStepDTO();
                        trs.setTestRunCaseId(trcId);
                        trs.setStepNumber(stepData.stepNumber);
                        trs.setAction(stepData.action);
                        trs.setExpectedResult(stepData.expectedResult);
                        TestRunStepDTO createdTrs = testRunStepService.createTestRunStep(trs);

                        sleep(STEP_DELAY_MS);
                        withRetry("updateTestRunStep " + createdTrs.getId(), 3, 1_500,
                                () -> testRunStepService.updateTestRunStep(createdTrs.getId(), outcome, null));

                        stepStatusMap.put(caseId + "::" + stepData.stepNumber, outcome.toUpperCase());
                    }

                    testRunCaseService.updateTestRunCaseStatus(trcId, cd.runOutcome);
                    switch (cd.runOutcome) {
                        case "PASSED"  -> totalPassed.incrementAndGet();
                        case "FAILED"  -> totalFailed.incrementAndGet();
                        case "SKIPPED" -> totalSkipped.incrementAndGet();
                    }
                }, exec))
                .toList();
        joinAll(trcFutures, "testRunCases+steps");
        sleep(PHASE_DELAY_MS);

        // STEP 9 – Update TestRun (sequential — FSM transition)
        final TestRunDTO[] runHolder = {run};
        withRetry("getTestRunById " + runId, 5, 2_000, () -> {
            runHolder[0] = testRunService.getTestRunById(runId)
                    .orElseThrow(() -> new RuntimeException("TestRun not found: " + runId));
            return runHolder[0];
        });
        TestRunDTO latestRun = runHolder[0];
        latestRun.setStepStatusesFromMap(new HashMap<>(stepStatusMap));
        latestRun.setPassed(totalPassed.get());
        latestRun.setFailed(totalFailed.get());
        latestRun.setSkipped(totalSkipped.get());
        latestRun.setUntested(0);
        latestRun.setStatus("active");
        withRetry("updateTestRun " + runId, 3, 2_000,
                () -> testRunService.updateTestRun(runId, latestRun));
        log.info("[DemoSeeder] TestRun updated — passed={} failed={} skipped={}", totalPassed, totalFailed, totalSkipped);
        sleep(PHASE_DELAY_MS);

        // STEP 10 – Defects (parallel — independent of each other)
        ConcurrentHashMap<String, DefectDTO> defectRefToId = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> defectFutures = data.defects.stream()
                .map(dd -> CompletableFuture.runAsync(() -> {
                    UUID caseId    = caseRefToId.get(dd.caseRef);
                    UUID runCaseId = caseRefToRunCaseId.get(dd.caseRef);
                    if (caseId == null) { log.warn("[DemoSeeder] Unknown caseRef for defect: {}", dd.caseRef); return; }
                    DefectDTO defect = new DefectDTO();
                    defect.setProjectId(projectId);
                    defect.setTestRunId(runId);
                    defect.setTestRunCaseId(runCaseId);
                    defect.setTestCaseId(caseId);
                    defect.setTitle(dd.title);
                    defect.setDescription(dd.description);
                    defect.setSeverity(dd.severity);
                    defect.setSource("Step " + dd.stepNumber);
                    DefectDTO created = defectService.createDefect(defect);
                    defectRefToId.put(dd.caseRef + "::" + dd.stepNumber, created);
                    log.info("[DemoSeeder] Created defect '{}': {}", dd.title, created.getId());
                }, exec))
                .toList();
        joinAll(defectFutures, "defects");

        // STEP 11 – Evidence attachments (parallel — independent of each other)
        List<CompletableFuture<Void>> evFutures = data.evidenceAttachments.stream()
                .map(ev -> CompletableFuture.runAsync(() -> {
                    UUID caseId = caseRefToId.get(ev.caseRef);
                    if (caseId == null) { log.warn("[DemoSeeder] Unknown caseRef for evidence: {}", ev.caseRef); return; }
                    AttachmentDTO a = buildAttachment(projectId, caseId, null,
                            ev.fileName, ev.fileType, ev.content, "EVIDENCE", runId, String.valueOf(ev.stepNumber));
                    attachmentService.uploadAttachment(a);
                    log.info("[DemoSeeder] Attached evidence '{}' to case {} step {}", ev.fileName, caseId, ev.stepNumber);
                }, exec))
                .toList();
        joinAll(evFutures, "evidence-attachments");

        // STEP 12 – Report (sequential — needs all data)
        String snapshotData = buildSnapshotData(data, totalPassed.get(), totalFailed.get(), totalSkipped.get(),
                run.getEnvironment(), run.getBuildVersion(), run.getName(), new HashMap<>(defectRefToId), data.defects);
        ReportDTO reportDTO = new ReportDTO();
        reportDTO.setProjectId(projectId);
        reportDTO.setName(data.report.name);
        reportDTO.setType(data.report.type);
        reportDTO.setDescription(data.report.description);
        reportDTO.setCreatedBy(data.report.createdBy);
        reportDTO.setSelectedRuns(List.of(runId.toString()));
        reportDTO.setSnapshotData(snapshotData);
        ReportDTO report = reportService.createReport(reportDTO);
        log.info("[DemoSeeder] Created report '{}': {}", data.report.name, report.getId());

        log.info("[DemoSeeder] Seed complete. projectId={}", projectId);
        return Map.of(
                "status",    "created",
                "projectId", projectId.toString(),
                "runId",     runId.toString(),
                "reportId",  report.getId().toString(),
                "summary",   Map.of("passed", totalPassed.get(), "failed", totalFailed.get(), "skipped", totalSkipped.get())
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<TestCaseDTO.StepDTO> toStepDTOs(List<DemoStep> steps) {
        return steps.stream()
                .map(s -> new TestCaseDTO.StepDTO(s.stepNumber, s.action, s.expectedResult))
                .toList();
    }

    private TestRunCaseDTO buildTestRunCase(UUID runId, UUID caseId, UUID projectId, DemoCase cd) {
        TestRunCaseDTO trc = new TestRunCaseDTO();
        trc.setTestRunId(runId);
        trc.setTestCaseId(caseId);
        trc.setProjectId(projectId);
        trc.setTitle(cd.title);
        trc.setDescription(cd.description);
        trc.setPreconditions(cd.preconditions);
        trc.setPriority(Priority.valueOf(cd.priority));
        trc.setSteps(toStepDTOs(cd.steps));
        return trc;
    }

    private AttachmentDTO buildAttachment(UUID projectId, UUID caseId, UUID defectId,
                                          String fileName, String fileType, String content,
                                          String type, UUID runId, String stepKey) {
        AttachmentDTO a = new AttachmentDTO();
        a.setProjectId(projectId);
        a.setCaseId(caseId);
        a.setDefectId(defectId);
        a.setFileName(fileName);
        a.setFileType(fileType);
        // Use a nominal demo file size; content is NOT stored in the Cyoda entity
        // (Cyoda entity schema defines content as nullable-only; actual file data
        // would be stored via EdgeMessage in production uploads).
        a.setFileSize(content != null ? content.length() : 1024L);
        a.setUploadedAt(Instant.now().toString());
        a.setAttachmentType(type);
        a.setRunId(runId);
        a.setStepKey(stepKey);
        // Do NOT set content — Cyoda entity model only accepts null for this field.
        // content=null is the default; actual file bytes live in EdgeMessage.
        return a;
    }

    private String buildSnapshotData(DemoData data, int passed, int failed, int skipped,
                                     String env, String build, String runName,
                                     Map<String, DefectDTO> defectRefToId, List<DemoDefect> defects) {
        try {
            int total = passed + failed + skipped;
            double passRate = total > 0 ? Math.round((passed * 1000.0 / total)) / 10.0 : 0.0;

            List<Map<String, Object>> suiteBreakdown = new ArrayList<>();
            for (DemoSuite s : data.suites) {
                int sp = 0, sf = 0, ss = 0;
                for (DemoCase c : s.cases) {
                    switch (c.runOutcome) {
                        case "PASSED"  -> sp++;
                        case "FAILED"  -> sf++;
                        case "SKIPPED" -> ss++;
                    }
                }
                suiteBreakdown.add(Map.of("suite", s.name, "passed", sp, "failed", sf, "skipped", ss, "untested", 0));
            }

            List<Map<String, Object>> defectList = new ArrayList<>();
            for (DemoDefect dd : defects) {
                String defectRef = dd.caseRef + "::" + dd.stepNumber;
                DefectDTO createdDefect = defectRefToId.get(defectRef);

                Map<String, Object> defectSnapshot = new LinkedHashMap<>();
                if (createdDefect != null && createdDefect.getId() != null) {
                    defectSnapshot.put("id", createdDefect.getId().toString());
                }
                if (createdDefect != null && createdDefect.getDisplayId() != null) {
                    defectSnapshot.put("displayId", createdDefect.getDisplayId());
                }
                defectSnapshot.put("title", dd.title);
                defectSnapshot.put("severity", dd.severity);
                defectSnapshot.put("status", createdDefect != null && createdDefect.getStatus() != null
                        ? createdDefect.getStatus()
                        : "Open");
                defectSnapshot.put("source", createdDefect != null && createdDefect.getSource() != null
                        ? createdDefect.getSource()
                        : "Step " + dd.stepNumber);
                if (createdDefect != null && createdDefect.getLink() != null) {
                    defectSnapshot.put("link", createdDefect.getLink());
                }
                if (createdDefect != null && createdDefect.getCreatedAt() != null) {
                    defectSnapshot.put("createdAt", createdDefect.getCreatedAt());
                }
                defectList.add(defectSnapshot);
            }

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("totalPassed",    passed);
            snapshot.put("totalFailed",    failed);
            snapshot.put("totalSkipped",   skipped);
            snapshot.put("totalUntested",  0);
            snapshot.put("passRate",       passRate);
            snapshot.put("suiteData",      suiteBreakdown);
            snapshot.put("defects",        defectList);
            snapshot.put("environment",    env);
            snapshot.put("buildVersion",   build);
            snapshot.put("runName",        runName);

            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("[DemoSeeder] Could not build snapshotData: {}", e.getMessage());
            return "{}";
        }
    }

    /** Waits for all futures; rethrows the first exception with a clear label. */
    private void joinAll(List<CompletableFuture<Void>> futures, String label) {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Parallel " + label + " failed: " + cause.getMessage(), cause);
        }
    }

    /** Sleeps the current thread, swallowing InterruptedException gracefully. */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[DemoSeeder] Sleep interrupted");
        }
    }

    /**
     * Retries a supplier up to {@code maxAttempts} times with {@code delayMs} between attempts.
     * Throws RuntimeException if all attempts fail.
     */
    private <T> T withRetry(String label, int maxAttempts, long delayMs,
                             java.util.function.Supplier<T> action) {
        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                last = e;
                log.warn("[DemoSeeder] {} — attempt {}/{} failed: {}", label, attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) sleep(delayMs);
            }
        }
        throw new RuntimeException(label + " failed after " + maxAttempts + " attempts: " + last.getMessage(), last);
    }

    private DemoData loadDemoData() {
        try {
            ClassPathResource resource = new ClassPathResource(DEMO_DATA_PATH);
            return objectMapper.readValue(resource.getInputStream(), DemoData.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load demo data from " + DEMO_DATA_PATH + ": " + e.getMessage(), e);
        }
    }

    // ── JSON model (inner classes) ────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoData {
        public DemoProject            project;
        public DemoTestRun            testRun;
        public List<DemoSuite>        suites;
        public List<DemoCaseAttachment> caseAttachments;
        public List<DemoDefect>       defects;
        public List<DemoEvidence>     evidenceAttachments;
        public DemoReport             report;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoProject {
        public String name;
        public String description;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoTestRun {
        public String name;
        public String environment;
        public String buildVersion;
        public String description;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoSuite {
        public String         ref;
        public String         name;
        public String         description;
        public List<DemoCase> cases;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoCase {
        public String              ref;
        public String              title;
        public String              description;
        public String              preconditions;
        public String              priority;
        public List<DemoStep>      steps;
        public String              runOutcome;
        public List<String>        stepOutcomes;
        public Map<String, String> failedStepActualResults;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoStep {
        public int    stepNumber;
        public String action;
        public String expectedResult;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoCaseAttachment {
        public String caseRef;
        public String fileName;
        public String fileType;
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoDefect {
        public String caseRef;
        public int    stepNumber;
        public String title;
        public String description;
        public String severity;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoEvidence {
        public String caseRef;
        public int    stepNumber;
        public String fileName;
        public String fileType;
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DemoReport {
        public String name;
        public String type;
        public String description;
        public String createdBy;
    }
}

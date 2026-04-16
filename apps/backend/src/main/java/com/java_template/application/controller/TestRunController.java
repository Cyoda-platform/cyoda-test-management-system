package com.java_template.application.controller;

import com.java_template.application.dto.TestRunDTO;
import com.java_template.application.dto.TestRunDetailDTO;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.application.service.TestRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.java_template.common.dto.PageResult;
import java.util.UUID;

/**
 * REST controller for Test Run operations
 */
@Slf4j
@RestController
@RequestMapping("/projects/{projectId}/runs")
@Tag(name = "Test Runs", description = "Test run management endpoints")
public class TestRunController {
    private final TestRunService testRunService;
    private final TestRunCaseService testRunCaseService;

    public TestRunController(TestRunService testRunService, TestRunCaseService testRunCaseService) {
        this.testRunService = testRunService;
        this.testRunCaseService = testRunCaseService;
    }

    @PostMapping
    @Operation(summary = "Create a new test run (snapshot)")
    public ResponseEntity<TestRunDTO> createTestRun(@PathVariable UUID projectId, @Valid @RequestBody TestRunDTO testRun) {
        testRun.setProjectId(projectId);
        TestRunDTO created = testRunService.createTestRun(testRun);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Get all test runs for a project")
    public ResponseEntity<PageResult<TestRunDTO>> getTestRunsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(testRunService.getTestRunsByProjectId(projectId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test run by ID")
    public ResponseEntity<TestRunDTO> getTestRun(@PathVariable UUID projectId, @PathVariable UUID id) {
        return testRunService.getTestRunById(id)
                .filter(tr -> tr.getProjectId().equals(projectId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Batch endpoint that returns the test run together with all of its DB-backed
     * {@code TestRunCase} records in a single HTTP round-trip.
     *
     * <p>This eliminates the two-request waterfall ({@code GET /runs/{id}} followed
     * by {@code GET /runs/{id}/cases}) that was responsible for the 10+ pending
     * network requests and &gt;10 s latency observed in the Run Execution view.</p>
     *
     * <p>The frontend {@code RunExecution} component should call this endpoint on
     * mount instead of separate {@code useTestRun} and {@code useTestRunCases} hooks.</p>
     */
    @GetMapping("/{id}/details")
    @Operation(summary = "Get full run structure (run + all run-cases) in one round-trip")
    public ResponseEntity<TestRunDetailDTO> getTestRunDetails(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        return testRunService.getTestRunById(id)
                .filter(tr -> tr.getProjectId().equals(projectId))
                .map(run -> {
                    // Fetch up to 500 run-case records — well above any realistic run size.
                    // Using a large page prevents silent truncation to the default page size of 20.
                    var runCases = testRunCaseService
                            .getTestRunCasesByTestRunId(run.getId(), 0, 500)
                            .data();
                    return ResponseEntity.ok(new TestRunDetailDTO(run, runCases));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a test run")
    public ResponseEntity<TestRunDTO> updateTestRun(@PathVariable UUID projectId, @PathVariable UUID id, @Valid @RequestBody TestRunDTO testRun) {
        if (!testRunService.testRunExists(id)) {
            return ResponseEntity.notFound().build();
        }
        testRun.setProjectId(projectId);
        TestRunDTO updated = testRunService.updateTestRun(id, testRun);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a test run (Tester, Admin)")
    public ResponseEntity<TestRunDTO> completeTestRun(@PathVariable UUID projectId, @PathVariable UUID id) {
        return testRunService.getTestRunById(id)
                .filter(tr -> tr.getProjectId().equals(projectId))
                .flatMap(tr -> testRunService.completeTestRun(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock a completed test run (Tester or Admin)")
    public ResponseEntity<TestRunDTO> unlockTestRun(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        log.warn("===== UNLOCK_RUN ===== role={}", role);
        // Allow both Tester and Admin to unlock runs (case-insensitive)
        if (role == null || (!role.equalsIgnoreCase("Tester") && !role.equalsIgnoreCase("Admin"))) {
            log.warn("===== UNLOCK_RUN FORBIDDEN ===== role={} is not Tester or Admin", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return testRunService.getTestRunById(id)
                .filter(tr -> tr.getProjectId().equals(projectId))
                .flatMap(tr -> testRunService.unlockTestRun(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a test run")
    public ResponseEntity<Void> deleteTestRun(@PathVariable UUID projectId, @PathVariable UUID id) {
        if (testRunService.deleteTestRun(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}


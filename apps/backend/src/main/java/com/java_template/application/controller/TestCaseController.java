package com.java_template.application.controller;

import com.java_template.application.dto.BatchImportCaseDTO;
import com.java_template.application.dto.MoveTestCaseDTO;
import com.java_template.application.dto.ReorderItemDTO;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.java_template.common.dto.PageResult;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Test Case operations
 */
@RestController
@RequestMapping("/projects/{projectId}/suites/{suiteId}/cases")
@Tag(name = "Test Cases", description = "Test case management endpoints")
public class TestCaseController {
    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping
    @Operation(summary = "Create a new test case")
    public ResponseEntity<TestCaseDTO> createTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @Valid @RequestBody TestCaseDTO testCase) {
        testCase.setProjectId(projectId);
        testCase.setSuiteId(suiteId);
        TestCaseDTO created = testCaseService.createTestCase(testCase);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Get all test cases for a suite")
    public ResponseEntity<PageResult<TestCaseDTO>> getTestCasesBySuite(
            @PathVariable UUID projectId,
            @PathVariable UUID suiteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(testCaseService.getTestCasesBySuiteId(suiteId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test case by ID")
    public ResponseEntity<TestCaseDTO> getTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @PathVariable UUID id) {
        return testCaseService.getTestCaseById(id)
                .filter(tc -> tc.getSuiteId().equals(suiteId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a test case")
    public ResponseEntity<TestCaseDTO> updateTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @PathVariable UUID id, @Valid @RequestBody TestCaseDTO testCase) {
        if (!testCaseService.testCaseExists(id)) {
            return ResponseEntity.notFound().build();
        }
        testCase.setProjectId(projectId);
        testCase.setSuiteId(suiteId);
        TestCaseDTO updated = testCaseService.updateTestCase(id, testCase);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a test case")
    public ResponseEntity<Void> deleteTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @PathVariable UUID id) {
        if (testCaseService.softDeleteTestCase(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Batch-create multiple test cases (with optional steps) in a single HTTP round-trip.
     *
     * All display IDs are reserved via a single counter update, so this endpoint is
     * significantly faster than calling {@code POST /cases} N times during a bulk import.
     * Steps embedded in each request item are created in the same call.
     *
     * @param projectId project UUID from the path
     * @param suiteId   suite UUID from the path
     * @param items     ordered list of cases to create (each may include steps)
     * @return HTTP 201 with the list of created test case DTOs
     */
    @PostMapping("/batch")
    @Operation(summary = "Batch-create multiple test cases with optional steps")
    public ResponseEntity<List<TestCaseDTO>> batchCreateTestCases(
            @PathVariable UUID projectId,
            @PathVariable UUID suiteId,
            @RequestBody List<BatchImportCaseDTO> items) {
        List<TestCaseDTO> created = testCaseService.batchCreateTestCases(projectId, suiteId, items);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Bulk-reorder test cases within a suite.
     * The request body is an ordered array of {@code {id, sortOrder}} pairs.
     * The server updates each case's {@code sortOrder} field; subsequent GETs
     * will return cases in ascending {@code sortOrder} order.
     */
    @PatchMapping("/reorder")
    @Operation(summary = "Reorder test cases within a suite")
    public ResponseEntity<Void> reorderTestCases(
            @PathVariable UUID projectId,
            @PathVariable UUID suiteId,
            @RequestBody List<ReorderItemDTO> items) {
        testCaseService.reorderTestCases(items);
        return ResponseEntity.ok().build();
    }

    /**
     * Move a test case to a different suite and/or position.
     * Updates both {@code suiteId} (foreign key) and {@code sortOrder} atomically.
     * After a successful move the caller should re-index the remaining cases in
     * both the source and destination suites via {@code PATCH /reorder}.
     */
    @PatchMapping("/{id}/move")
    @Operation(summary = "Move a test case to a different suite")
    public ResponseEntity<TestCaseDTO> moveTestCase(
            @PathVariable UUID projectId,
            @PathVariable UUID suiteId,
            @PathVariable UUID id,
            @RequestBody MoveTestCaseDTO moveDTO) {
        try {
            TestCaseDTO moved = testCaseService.moveTestCase(id, moveDTO.targetSuiteId(), moveDTO.sortOrder());
            return ResponseEntity.ok(moved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

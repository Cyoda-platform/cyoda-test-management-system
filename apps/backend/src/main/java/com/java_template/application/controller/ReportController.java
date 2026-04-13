package com.java_template.application.controller;

import com.java_template.application.dto.ReportDTO;
import com.java_template.application.service.ReportService;
import com.java_template.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Report operations
 */
@RestController
@RequestMapping("/projects/{projectId}/reports")
@Tag(name = "Reports", description = "Project report management endpoints")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Create a new report for a project")
    public ResponseEntity<ReportDTO> createReport(
            @PathVariable UUID projectId,
            @Valid @RequestBody ReportDTO report) {
        report.setProjectId(projectId);
        ReportDTO created = reportService.createReport(report);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Get all reports for a project")
    public ResponseEntity<PageResult<ReportDTO>> getReportsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReportsByProjectId(projectId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a report by ID")
    public ResponseEntity<ReportDTO> getReport(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        return reportService.getReportById(id)
                .filter(r -> r.getProjectId().equals(projectId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a report")
    public ResponseEntity<ReportDTO> updateReport(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody ReportDTO report) {
        if (!reportService.reportExists(id)) {
            return ResponseEntity.notFound().build();
        }
        report.setProjectId(projectId);
        ReportDTO updated = reportService.updateReport(id, report);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a report")
    public ResponseEntity<Void> deleteReport(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        if (!reportService.reportExists(id)) {
            return ResponseEntity.notFound().build();
        }
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}

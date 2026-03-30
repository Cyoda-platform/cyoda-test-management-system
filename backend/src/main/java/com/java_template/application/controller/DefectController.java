package com.java_template.application.controller;

import com.java_template.application.dto.DefectDTO;
import com.java_template.application.service.DefectService;
import com.java_template.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Defect operations
 */
@RestController
@RequestMapping("/projects/{projectId}/defects")
@Tag(name = "Defects", description = "Project defect management endpoints")
public class DefectController {

    private final DefectService defectService;

    public DefectController(DefectService defectService) {
        this.defectService = defectService;
    }

    @PostMapping
    @Operation(summary = "Create a new defect for a project")
    public ResponseEntity<DefectDTO> createDefect(
            @PathVariable UUID projectId,
            @Valid @RequestBody DefectDTO defect) {
        defect.setProjectId(projectId);
        DefectDTO created = defectService.createDefect(defect);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Get all defects for a project")
    public ResponseEntity<PageResult<DefectDTO>> getDefectsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(defectService.getDefectsByProjectId(projectId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a defect by ID")
    public ResponseEntity<DefectDTO> getDefect(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        return defectService.getDefectById(id)
                .filter(d -> d.getProjectId().equals(projectId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a defect")
    public ResponseEntity<DefectDTO> updateDefect(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody DefectDTO defect) {
        if (!defectService.defectExists(id)) {
            return ResponseEntity.notFound().build();
        }
        defect.setProjectId(projectId);
        DefectDTO updated = defectService.updateDefect(id, defect);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a defect")
    public ResponseEntity<Void> deleteDefect(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        if (!defectService.defectExists(id)) {
            return ResponseEntity.notFound().build();
        }
        defectService.deleteDefect(id);
        return ResponseEntity.noContent().build();
    }
}

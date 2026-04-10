package com.java_template.application.controller;

import com.java_template.application.dto.RepositoryDTO;
import com.java_template.application.service.RepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Aggregate endpoint for the Repository view.
 * Returns suites + cases in a single request, eliminating the 1+N browser waterfall.
 */
@RestController
@RequestMapping("/projects/{projectId}/repository")
@Tag(name = "Repository", description = "Aggregate endpoint for the test repository view")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @GetMapping
    @Operation(summary = "Get all suites and test cases for a project in one call")
    public ResponseEntity<RepositoryDTO> getRepository(@PathVariable UUID projectId) {
        return ResponseEntity.ok(repositoryService.getRepository(projectId));
    }
}

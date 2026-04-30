package com.java_template.application.controller;

import com.java_template.application.dto.ProjectDTO;
import com.java_template.application.service.ProjectService;
import com.java_template.application.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Global search controller for cross-project searches.
 * Used by header search without requiring a specific projectId.
 */
@Slf4j
@RestController
@RequestMapping("/v1/search")
@Tag(name = "Global Search", description = "Global search across all projects")
public class GlobalSearchController {

    private final SearchService searchService;
    private final ProjectService projectService;

    public GlobalSearchController(SearchService searchService, ProjectService projectService) {
        this.searchService = searchService;
        this.projectService = projectService;
    }

    /**
     * Global header search endpoint
     * Searches across all projects and entity types
     */
    @GetMapping
    @Operation(summary = "Global header search",
            description = "Search across all projects and entity types")
    public ResponseEntity<Map<String, Object>> globalSearch(
            @RequestParam(defaultValue = "")
            @Parameter(description = "Search query", required = false)
            String query,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number", required = false)
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Parameter(description = "Page size (max 50)", required = false)
            int pageSize) {

        log.info("[GlobalSearch] Request: query='{}', page={}, size={}", query, pageNumber, pageSize);

        // Validate pagination
        if (pageNumber < 0) pageNumber = 0;
        if (pageSize < 1 || pageSize > 50) pageSize = 10;

        try {
            // Get all projects
            var allProjects = projectService.getAllProjects(0, 1000).data();

            if (allProjects.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "query", query,
                        "results", List.of(),
                        "totalResults", 0,
                        "executionTimeMs", 0
                ));
            }

            long startTime = System.currentTimeMillis();
            List<Object> allResults = new ArrayList<>();

            // Search each project
            for (ProjectDTO project : allProjects) {
                try {
                    var projectResults = searchService.search(query, project.getId(), 0, 100);
                    if (projectResults != null && projectResults.getResults() != null) {
                        allResults.addAll(projectResults.getResults());
                    }
                } catch (Exception e) {
                    log.warn("[GlobalSearch] Search failed for project {}: {}", project.getId(), e.getMessage());
                }
            }

            // Sort by score and apply pagination
            allResults.sort((a, b) -> {
                if (a instanceof Map && b instanceof Map) {
                    Object scoreA = ((Map<?, ?>) a).get("score");
                    Object scoreB = ((Map<?, ?>) b).get("score");
                    if (scoreA instanceof Number && scoreB instanceof Number) {
                        return Double.compare(((Number) scoreB).doubleValue(), ((Number) scoreA).doubleValue());
                    }
                }
                return 0;
            });

            int startIdx = pageNumber * pageSize;
            int endIdx = Math.min(startIdx + pageSize, allResults.size());
            List<Object> pagedResults = allResults.subList(
                    Math.min(startIdx, allResults.size()),
                    endIdx
            );

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("[GlobalSearch] Completed: {} total results in {}ms", allResults.size(), executionTime);

            return ResponseEntity.ok(Map.of(
                    "query", query,
                    "results", pagedResults,
                    "totalResults", allResults.size(),
                    "pageNumber", pageNumber,
                    "pageSize", pageSize,
                    "executionTimeMs", executionTime
            ));

        } catch (Exception e) {
            log.error("[GlobalSearch] Failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Search failed: " + e.getMessage()
            ));
        }
    }
}

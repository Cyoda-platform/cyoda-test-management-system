package com.java_template.application.controller;

import com.java_template.application.dto.SearchRequestDTO;
import com.java_template.application.dto.SearchResponseDTO;
import com.java_template.application.service.ProjectService;
import com.java_template.application.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * REST controller for unified search operations across all TMS entities
 *
 * Supports searching across:
 * - Projects (name, description)
 * - Suites (name, description)
 * - Test Cases (name, description, steps)
 * - Test Runs (name, description, status)
 * - Test Run Cases (results, remarks)
 * - Test Run Steps (actions, results)
 * - Defects (title, description, severity, status, link)
 * - Reports (name, summary)
 *
 * All searches are scoped to a specific project for tenant isolation.
 */
@Slf4j
@RestController
@RequestMapping("/v1/projects/{projectId}/search")
@Tag(name = "Search", description = "Unified search across all TMS entities")
public class SearchController {

    private final SearchService searchService;
    private final ProjectService projectService;

    public SearchController(SearchService searchService, ProjectService projectService) {
        this.searchService = searchService;
        this.projectService = projectService;
    }

    /**
     * Execute unified search across all entity types within a project
     *
     * @param projectId Project ID for scoping (tenant isolation)
     * @param searchRequest Search criteria including query, filters, pagination
     * @return Aggregated search results grouped by entity type
     *
     * Example:
     * POST /api/v1/projects/{projectId}/search
     * {
     *   "query": "login button",
     *   "pageNumber": 0,
     *   "pageSize": 20
     * }
     */
    @PostMapping
    @Operation(summary = "Search across all entities in a project",
            description = "Performs parallel search across projects, suites, test cases, test runs, " +
                    "test run cases, test run steps, defects, and reports. Returns results sorted by relevance.")
    public ResponseEntity<SearchResponseDTO> search(
            @PathVariable
            @Parameter(description = "Project ID for scoping search", required = true)
            UUID projectId,

            @Valid
            @RequestBody
            SearchRequestDTO searchRequest) {

        log.info("[SearchController] Received search request: query='{}', page={}, size={}",
                searchRequest.getQuery(),
                searchRequest.getPageNumber() != null ? searchRequest.getPageNumber() : 0,
                searchRequest.getPageSize() != null ? searchRequest.getPageSize() : 20);

        int pageNumber = searchRequest.getPageNumber() != null ? searchRequest.getPageNumber() : 0;
        int pageSize = searchRequest.getPageSize() != null ? searchRequest.getPageSize() : 20;

        // Validate pagination parameters
        if (pageNumber < 0) pageNumber = 0;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;

        try {
            SearchResponseDTO response = searchService.search(
                    searchRequest.getQuery(),
                    projectId,
                    pageNumber,
                    pageSize
            );

            log.info("[SearchController] Search completed: {} total results", response.getTotalResults());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[SearchController] Search failed for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Search operation failed", e);
        }
    }

    /**
     * Quick search endpoint for header autocomplete
     *
     * Returns only top results (first 5) for quick preview
     */
    @GetMapping("/quick")
    @Operation(summary = "Quick search for autocomplete",
            description = "Returns top 5 results for quick preview in header search box")
    public ResponseEntity<SearchResponseDTO> quickSearch(
            @PathVariable
            @Parameter(description = "Project ID for scoping search", required = true)
            UUID projectId,

            @RequestParam
            @Parameter(description = "Search query", required = true)
            String query) {

        log.info("[SearchController] Quick search request: query='{}', project={}", query, projectId);

        try {
            SearchResponseDTO response = searchService.search(query, projectId, 0, 5);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[SearchController] Quick search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Quick search failed", e);
        }
    }

}



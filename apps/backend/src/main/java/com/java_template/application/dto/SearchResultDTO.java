package com.java_template.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for a single search result across all entity types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultDTO {

    /** Entity type: project, suite, testcase, testrun, testruncase, testrunchstep, defect, report, attachment */
    private String type;

    /** Unique identifier of the entity */
    private UUID id;

    /** Display ID for some entities (e.g., DEF-01, TEST-123) */
    private String displayId;

    /** Primary title/name for display */
    private String title;

    /** Short description or summary */
    private String description;

    /** Severity (for defects) or Status (for other entities) */
    private String metadata;

    /** Parent project ID for filtering/navigation */
    private UUID parentProjectId;

    /** List of fields that matched the search query */
    private List<String> matchedFields;

    /** Relevance score (0.0 - 1.0) */
    @Builder.Default
    private Double score = 1.0;

    /** Link to external tracker if available */
    private String externalLink;

    /** Creation timestamp */
    private String createdAt;
}

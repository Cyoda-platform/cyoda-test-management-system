package com.java_template.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for the complete search response
 * Groups results by entity type and includes metadata about the search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponseDTO {

    /** Original search query */
    private String query;

    /** Search ID for pagination (if applicable) */
    private UUID searchId;

    /** Total number of results across all entity types */
    private long totalResults;

    /** Number of results returned in this page */
    private int resultCount;

    /** Current page number (0-based) */
    private int pageNumber;

    /** Page size */
    private int pageSize;

    /** All results grouped by type */
    private List<SearchResultDTO> results;

    /** Time taken to complete search (milliseconds) */
    private long executionTimeMs;

    /** Number of entity types that were searched */
    @Builder.Default
    private int typesSearched = 8;
}

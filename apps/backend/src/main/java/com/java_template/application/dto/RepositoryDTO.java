package com.java_template.application.dto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate response for the Repository view.
 * Returns all suites and their test cases for a project in a single call,
 * eliminating the serial suites → cases waterfall that used to require 1+N round-trips.
 */
public record RepositoryDTO(List<SuiteView> suites) {

    public record SuiteView(
            UUID id,
            UUID projectId,
            String name,
            String description,
            Integer sortOrder,
            List<CaseView> cases
    ) {}

    public record CaseView(
            UUID id,
            String displayId,
            UUID suiteId,
            UUID projectId,
            String title,
            String description,
            String preconditions,
            Priority priority,
            Integer sortOrder,
            boolean deleted,
            List<TestCaseDTO.StepDTO> steps
    ) {}
}

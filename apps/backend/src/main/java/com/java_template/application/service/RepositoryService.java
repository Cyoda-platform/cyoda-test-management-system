package com.java_template.application.service;

import com.java_template.application.dto.RepositoryDTO;
import com.java_template.application.dto.SuiteDTO;
import com.java_template.application.dto.TestCaseDTO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregates suites + cases for the Repository view in two Cyoda calls instead of 1+N.
 *
 * Before: browser fetches suites (1 request), waits, then fetches cases per suite (N requests).
 * After:  browser makes one request here; we fire both Cyoda queries server-side and assemble.
 */
@Service
public class RepositoryService {

    private final SuiteService suiteService;
    private final TestCaseService testCaseService;

    public RepositoryService(SuiteService suiteService, TestCaseService testCaseService) {
        this.suiteService = suiteService;
        this.testCaseService = testCaseService;
    }

    /**
     * Returns all suites and their test cases for a project.
     * Two Cyoda calls total: one for suites, one for all non-deleted cases in the project.
     */
    public RepositoryDTO getRepository(UUID projectId) {
        // Call 1: suites ordered by sortOrder (uses server-side search from Fix #3)
        List<SuiteDTO> suites = suiteService.getSuitesByProjectId(projectId, 0, 500).data();

        // Call 2: all non-deleted cases for the project in one query (added in Fix #4)
        List<TestCaseDTO> allCases = testCaseService.getCasesByProjectId(projectId);

        // Group cases by suiteId for O(1) lookup when building the tree
        Map<UUID, List<TestCaseDTO>> casesBySuite = allCases.stream()
                .collect(Collectors.groupingBy(TestCaseDTO::getSuiteId));

        // Assemble the nested response
        List<RepositoryDTO.SuiteView> suiteViews = suites.stream()
                .map(suite -> {
                    List<RepositoryDTO.CaseView> caseViews = casesBySuite
                            .getOrDefault(suite.getId(), List.of())
                            .stream()
                            .sorted(Comparator.comparing(TestCaseDTO::getSortOrder,
                                            Comparator.nullsLast(Comparator.naturalOrder()))
                                    .thenComparing(c -> c.getId() != null ? c.getId().toString() : ""))
                            .map(c -> new RepositoryDTO.CaseView(
                                    c.getId(),
                                    c.getDisplayId(),
                                    c.getSuiteId(),
                                    c.getProjectId(),
                                    c.getTitle(),
                                    c.getDescription(),
                                    c.getPreconditions(),
                                    c.getPriority(),
                                    c.getSortOrder(),
                                    c.isDeleted()))
                            .toList();

                    return new RepositoryDTO.SuiteView(
                            suite.getId(),
                            suite.getProjectId(),
                            suite.getName(),
                            suite.getDescription(),
                            suite.getSortOrder(),
                            caseViews);
                })
                .toList();

        return new RepositoryDTO(suiteViews);
    }
}

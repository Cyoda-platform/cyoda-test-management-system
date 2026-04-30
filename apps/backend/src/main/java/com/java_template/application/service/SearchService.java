package com.java_template.application.service;

import com.java_template.application.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Unified search service across all TMS entities
 *
 * Provides cross-entity search with:
 * - Parallel searches across 8 entity types
 * - Query normalization (case-insensitive, trimmed)
 * - Result aggregation with relevance scoring
 * - Tenant isolation (projectId filtering)
 * - Pagination support
 *
 * Search coverage:
 * ├── Project (name, description)
 * ├── Suite (name, description)
 * ├── TestCase (title, description, steps)
 * ├── TestRun (name, description, status)
 * ├── TestRunCase (status, title, description)
 * ├── TestRunStep (action, expectedResult, actualResult, status)
 * ├── Defect (title, description, severity, status, link)
 * └── Report (name, description)
 */
@Slf4j
@Service
public class SearchService {

    private final ProjectService projectService;
    private final SuiteService suiteService;
    private final TestCaseService testCaseService;
    private final TestRunService testRunService;
    private final TestRunCaseService testRunCaseService;
    private final TestRunStepService testRunStepService;
    private final DefectService defectService;
    private final ReportService reportService;
    private final ExecutorService executorService;

    public SearchService(ProjectService projectService,
                        SuiteService suiteService,
                        TestCaseService testCaseService,
                        TestRunService testRunService,
                        TestRunCaseService testRunCaseService,
                        TestRunStepService testRunStepService,
                        DefectService defectService,
                        ReportService reportService) {
        this.projectService = projectService;
        this.suiteService = suiteService;
        this.testCaseService = testCaseService;
        this.testRunService = testRunService;
        this.testRunCaseService = testRunCaseService;
        this.testRunStepService = testRunStepService;
        this.defectService = defectService;
        this.reportService = reportService;

        // Thread pool for parallel searches
        this.executorService = Executors.newFixedThreadPool(8,
                r -> {
                    Thread t = new Thread(r, "SearchService-" + UUID.randomUUID());
                    t.setDaemon(true);
                    return t;
                });
    }

    /**
     * Execute unified search across all entity types
     *
     * @param query Search query (case-insensitive)
     * @param projectId Project ID for scoping (tenant isolation)
     * @param pageNumber Page number (0-based)
     * @param pageSize Results per page
     * @return Aggregated search results
     */
    public SearchResponseDTO search(String query, UUID projectId, int pageNumber, int pageSize) {
        long startTime = System.currentTimeMillis();

        String normalizedQuery = normalizeQuery(query);
        log.info("[Search] Starting unified search: query='{}', project={}, page={}, size={}",
                normalizedQuery, projectId, pageNumber, pageSize);

        try {
            // Parallel search across all entity types
            List<CompletableFuture<List<SearchResultDTO>>> futures = new ArrayList<>();

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchProjects(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] Project search failed", e);
                return Collections.emptyList();
            }));

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchSuites(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] Suite search failed", e);
                return Collections.emptyList();
            }));

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchTestCases(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] TestCase search failed", e);
                return Collections.emptyList();
            }));

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchTestRuns(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] TestRun search failed", e);
                return Collections.emptyList();
            }));

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchTestRunCases(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] TestRunCase search failed", e);
                return Collections.emptyList();
            }));

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchTestRunSteps(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] TestRunStep search failed", e);
                return Collections.emptyList();
            }));

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchDefects(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] Defect search failed", e);
                return Collections.emptyList();
            }));

            futures.add(CompletableFuture.supplyAsync(
                    () -> searchReports(normalizedQuery, projectId),
                    executorService
            ).exceptionally(e -> {
                log.error("[Search] Report search failed", e);
                return Collections.emptyList();
            }));

            // Wait for all searches to complete (30 second timeout)
            CompletableFuture<Void> allSearches = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allSearches.get(30, TimeUnit.SECONDS);

            // Aggregate results
            List<SearchResultDTO> allResults = new ArrayList<>();
            for (CompletableFuture<List<SearchResultDTO>> future : futures) {
                allResults.addAll(future.join());
            }

            // Sort by relevance score (descending)
            allResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            // Apply pagination
            int startIdx = pageNumber * pageSize;
            int endIdx = Math.min(startIdx + pageSize, allResults.size());
            List<SearchResultDTO> pagedResults = allResults.subList(
                    Math.min(startIdx, allResults.size()),
                    endIdx
            );

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[Search] Completed: {} total results in {}ms", allResults.size(), executionTime);

            return SearchResponseDTO.builder()
                    .query(query)
                    .totalResults(allResults.size())
                    .resultCount(pagedResults.size())
                    .pageNumber(pageNumber)
                    .pageSize(pageSize)
                    .results(pagedResults)
                    .executionTimeMs(executionTime)
                    .typesSearched(8)
                    .build();

        } catch (TimeoutException e) {
            log.error("[Search] Search timed out after 30 seconds");
            throw new RuntimeException("Search operation timed out", e);
        } catch (Exception e) {
            log.error("[Search] Search failed", e);
            throw new RuntimeException("Search operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Search projects by name or description
     */
    private List<SearchResultDTO> searchProjects(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allProjects = projectService.getAllProjects(0, 1000).data();

            for (ProjectDTO project : allProjects) {
                double score = calculateMatchScore(query, project.getName(), project.getDescription());
                if (score > 0) {
                    results.add(SearchResultDTO.builder()
                            .type("project")
                            .id(project.getId())
                            .title(project.getName())
                            .description(project.getDescription())
                            .parentProjectId(project.getId())
                            .matchedFields(getMatchedFields(query, project.getName(), project.getDescription()))
                            .score(score)
                            .createdAt(project.getCreatedAt())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search projects", e);
        }
        return results;
    }

    /**
     * Search suites by name or description
     */
    private List<SearchResultDTO> searchSuites(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allSuites = suiteService.getAllSuitesByProjectId(projectId);

            for (SuiteDTO suite : allSuites) {
                double score = calculateMatchScore(query, suite.getName(), suite.getDescription());
                if (score > 0) {
                    results.add(SearchResultDTO.builder()
                            .type("suite")
                            .id(suite.getId())
                            .title(suite.getName())
                            .description(suite.getDescription())
                            .parentProjectId(projectId)
                            .matchedFields(getMatchedFields(query, suite.getName(), suite.getDescription()))
                            .score(score)
                            .createdAt(suite.getCreatedAt())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search suites for project {}", projectId, e);
        }
        return results;
    }

    /**
     * Search test cases by title, description, or steps
     */
    private List<SearchResultDTO> searchTestCases(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allCases = testCaseService.getCasesByProjectId(projectId);

            for (TestCaseDTO testCase : allCases) {
                if (testCase.isDeleted()) continue; // Skip deleted test cases

                double score = calculateMatchScore(
                        query,
                        testCase.getTitle(),
                        testCase.getDescription()
                );

                // Also check steps if available
                if (testCase.getSteps() != null && !testCase.getSteps().isEmpty()) {
                    String stepsText = testCase.getSteps().stream()
                            .map(step -> (step.getAction() != null ? step.getAction() : "") + " " +
                                         (step.getExpectedResult() != null ? step.getExpectedResult() : ""))
                            .collect(Collectors.joining(" "));
                    score = Math.max(score, calculateMatchScore(query, stepsText));
                }

                if (score > 0) {
                    results.add(SearchResultDTO.builder()
                            .type("testcase")
                            .id(testCase.getId())
                            .displayId(testCase.getDisplayId())
                            .title(testCase.getTitle())
                            .description(testCase.getDescription())
                            .parentProjectId(projectId)
                            .matchedFields(getMatchedFields(query, testCase.getTitle(), testCase.getDescription()))
                            .score(score)
                            .createdAt(testCase.getCreatedAt())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search test cases for project {}", projectId, e);
        }
        return results;
    }

    /**
     * Search test runs by name, description, or status
     */
    private List<SearchResultDTO> searchTestRuns(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allRuns = testRunService.getAllTestRunsByProjectId(projectId);

            for (TestRunDTO testRun : allRuns) {
                double score = calculateMatchScore(
                        query,
                        testRun.getName(),
                        testRun.getDescription(),
                        testRun.getStatus()
                );

                if (score > 0) {
                    results.add(SearchResultDTO.builder()
                            .type("testrun")
                            .id(testRun.getId())
                            .displayId(testRun.getDisplayId())
                            .title(testRun.getName())
                            .description(testRun.getDescription())
                            .metadata(testRun.getStatus())
                            .parentProjectId(projectId)
                            .matchedFields(getMatchedFields(query, testRun.getName(), testRun.getDescription(), testRun.getStatus()))
                            .score(score)
                            .createdAt(testRun.getCreatedAt())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search test runs for project {}", projectId, e);
        }
        return results;
    }

    /**
     * Search test run cases by status, title, or description
     */
    private List<SearchResultDTO> searchTestRunCases(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allRunCases = testRunCaseService.getAllTestRunCasesByProjectId(projectId);

            for (TestRunCaseDTO runCase : allRunCases) {
                double score = calculateMatchScore(
                        query,
                        runCase.getStatus(),
                        runCase.getTitle(),
                        runCase.getDescription()
                );

                if (score > 0) {
                    results.add(SearchResultDTO.builder()
                            .type("testruncases")
                            .id(runCase.getId())
                            .title("Test Case: " + (runCase.getTitle() != null ? runCase.getTitle() : "Unknown"))
                            .description(runCase.getDescription())
                            .metadata(runCase.getStatus())
                            .parentProjectId(projectId)
                            .matchedFields(getMatchedFields(query, runCase.getStatus(), runCase.getTitle(), runCase.getDescription()))
                            .score(score)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search test run cases for project {}", projectId, e);
        }
        return results;
    }

    /**
     * Search test run steps by action, expected/actual results
     */
    private List<SearchResultDTO> searchTestRunSteps(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allRunCases = testRunCaseService.getAllTestRunCasesByProjectId(projectId);

            for (TestRunCaseDTO runCase : allRunCases) {
                var steps = testRunStepService.getAllTestRunStepsByTestRunCaseId(runCase.getId());

                for (TestRunStepDTO step : steps) {
                    double score = calculateMatchScore(
                            query,
                            step.getAction(),
                            step.getExpectedResult(),
                            step.getActualResult(),
                            step.getStatus()
                    );

                    if (score > 0) {
                        results.add(SearchResultDTO.builder()
                                .type("testrunchstep")
                                .id(step.getId())
                                .title("Step: " + (step.getAction() != null ? step.getAction() : "Unknown"))
                                .description(step.getActualResult())
                                .metadata(step.getStatus())
                                .parentProjectId(projectId)
                                .matchedFields(getMatchedFields(query, step.getAction(), step.getExpectedResult(),
                                        step.getActualResult(), step.getStatus()))
                                .score(score)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search test run steps for project {}", projectId, e);
        }
        return results;
    }

    /**
     * Search defects by title, description, severity, status, or link
     */
    private List<SearchResultDTO> searchDefects(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allDefects = defectService.getAllDefectsByProjectId(projectId);

            for (DefectDTO defect : allDefects) {
                double score = calculateMatchScore(
                        query,
                        defect.getTitle(),
                        defect.getDescription(),
                        defect.getSeverity(),
                        defect.getStatus(),
                        defect.getLink()
                );

                if (score > 0) {
                    results.add(SearchResultDTO.builder()
                            .type("defect")
                            .id(defect.getId())
                            .displayId(defect.getDisplayId())
                            .title(defect.getTitle())
                            .description(defect.getDescription())
                            .metadata(defect.getSeverity() + " - " + defect.getStatus())
                            .parentProjectId(projectId)
                            .matchedFields(getMatchedFields(query, defect.getTitle(), defect.getDescription(),
                                    defect.getSeverity(), defect.getStatus(), defect.getLink()))
                            .score(score)
                            .externalLink(defect.getLink())
                            .createdAt(defect.getCreatedAt())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search defects for project {}", projectId, e);
        }
        return results;
    }

    /**
     * Search reports by name or description
     */
    private List<SearchResultDTO> searchReports(String query, UUID projectId) {
        List<SearchResultDTO> results = new ArrayList<>();
        try {
            var allReports = reportService.getAllReportsByProjectId(projectId);

            for (ReportDTO report : allReports) {
                double score = calculateMatchScore(
                        query,
                        report.getName(),
                        report.getDescription()
                );

                if (score > 0) {
                    results.add(SearchResultDTO.builder()
                            .type("report")
                            .id(report.getId())
                            .displayId(report.getDisplayId())
                            .title(report.getName())
                            .description(report.getDescription())
                            .parentProjectId(projectId)
                            .matchedFields(getMatchedFields(query, report.getName(), report.getDescription()))
                            .score(score)
                            .createdAt(report.getCreatedAt())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Search] Failed to search reports for project {}", projectId, e);
        }
        return results;
    }

    /**
     * Normalize search query: trim, lowercase, remove extra spaces
     */
    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }

    /**
     * Calculate relevance score based on substring matching
     * Exact match = 1.0, word match = 0.8, partial match = 0.5
     */
    private double calculateMatchScore(String query, String... fields) {
        if (query == null || query.isEmpty()) return 0;

        String normalizedQuery = normalizeQuery(query);
        double maxScore = 0;

        for (String field : fields) {
            if (field == null || field.isEmpty()) continue;

            String normalizedField = field.toLowerCase();

            // Exact match
            if (normalizedField.equals(normalizedQuery)) {
                maxScore = Math.max(maxScore, 1.0);
            }
            // Word match (whole word)
            else if (normalizedField.contains(" " + normalizedQuery + " ") ||
                     normalizedField.startsWith(normalizedQuery + " ") ||
                     normalizedField.endsWith(" " + normalizedQuery)) {
                maxScore = Math.max(maxScore, 0.8);
            }
            // Contains match
            else if (normalizedField.contains(normalizedQuery)) {
                maxScore = Math.max(maxScore, 0.5);
            }
        }

        return maxScore;
    }

    /**
     * Determine which fields matched the query
     */
    private List<String> getMatchedFields(String query, String... fields) {
        String normalizedQuery = normalizeQuery(query);
        List<String> matched = new ArrayList<>();
        String[] fieldNames = {"name", "description", "status", "severity", "link", "title", "action", "result"};

        for (int i = 0; i < fields.length && i < fieldNames.length; i++) {
            String field = fields[i];
            if (field != null && field.toLowerCase().contains(normalizedQuery)) {
                matched.add(fieldNames[i]);
            }
        }

        return matched.isEmpty() ? List.of("general") : matched;
    }
}

package com.java_template.application.service;

import com.java_template.application.dto.RepositoryDTO;
import com.java_template.application.dto.SuiteDTO;
import com.java_template.application.dto.TestCaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Aggregates suites + cases for the Repository view in two Cyoda calls instead of 1+N.
 *
 * Before: browser fetches suites (1 request), waits, then fetches cases per suite (N requests).
 * After:  browser makes one request here; we fire both Cyoda queries server-side and assemble.
 */
@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    /** Matches valid server-assigned display IDs (e.g. "TC-1", "TC-42"). */
    private static final Pattern TC_PATTERN = Pattern.compile("^TC-\\d+$");

    private final SuiteService suiteService;
    private final TestCaseService testCaseService;
    private final ProjectCounterService projectCounterService;

    public RepositoryService(SuiteService suiteService,
                             TestCaseService testCaseService,
                             ProjectCounterService projectCounterService) {
        this.suiteService = suiteService;
        this.testCaseService = testCaseService;
        this.projectCounterService = projectCounterService;
    }

    /**
     * Returns all suites and their test cases for a project.
     * Two Cyoda calls total: one for suites, one for all non-deleted cases in the project.
     *
     * Lazy migration: any case that still has a null or non-TC-X displayId is assigned
     * a proper TC-N ID on the fly and persisted immediately, so the UI never shows
     * stale suite-scoped labels like "S1-1".
     */
    public RepositoryDTO getRepository(UUID projectId) {
        // Call 1: suites ordered by sortOrder (uses server-side search from Fix #3)
        List<SuiteDTO> suites = suiteService.getSuitesByProjectId(projectId, 0, 500).data();

        // Call 2: all non-deleted cases for the project in one query (added in Fix #4)
        List<TestCaseDTO> allCases = testCaseService.getCasesByProjectId(projectId);

        // Lazy migration: assign TC-X to any case that is missing a valid displayId
        backfillDisplayIds(projectId, allCases);

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

    /**
     * Assigns TC-N display IDs to any cases that are missing one (null or non-TC-X format).
     * IDs are reserved in a single batch call to the counter, then persisted via update.
     * This is a one-time lazy migration; once a case has a valid displayId it is skipped.
     */
    private void backfillDisplayIds(UUID projectId, List<TestCaseDTO> cases) {
        List<TestCaseDTO> needsId = cases.stream()
                .filter(c -> c.getDisplayId() == null || !TC_PATTERN.matcher(c.getDisplayId()).matches())
                .toList();

        if (needsId.isEmpty()) return;

        log.info("Backfilling displayId for {} case(s) in project {}", needsId.size(), projectId);
        List<String> ids = projectCounterService.nextDisplayIdBatch(projectId, needsId.size());

        for (int i = 0; i < needsId.size(); i++) {
            TestCaseDTO tc = needsId.get(i);
            tc.setDisplayId(ids.get(i));
            try {
                testCaseService.updateTestCase(tc.getId(), tc);
            } catch (Exception e) {
                log.warn("Failed to backfill displayId for case {}: {}", tc.getId(), e.getMessage());
            }
        }
    }
}

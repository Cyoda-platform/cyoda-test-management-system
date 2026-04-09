package com.java_template.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Composite response for {@code GET /projects/{projectId}/runs/{runId}/details}.
 *
 * <p>Bundles the run entity together with every DB-backed {@link TestRunCaseDTO}
 * record in a single HTTP response, eliminating the two-round-trip waterfall
 * ({@code GET /runs/{id}} then {@code GET /runs/{id}/cases}) that was causing
 * 10+ pending network requests and &gt;10 s latency in the Run Execution view.</p>
 *
 * <p>The frontend should replace the separate {@code useTestRun} + {@code useTestRunCases}
 * hooks with a single {@code useTestRunDetails} hook that calls this endpoint.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunDetailDTO {

    /**
     * The test run entity including {@code caseIds} and {@code stepStatuses}.
     */
    private TestRunDTO run;

    /**
     * All DB-backed {@link TestRunCaseDTO} records for this run.
     *
     * <p>Each record carries the {@code TestRunCase.id} that the frontend uses
     * as the canonical execution-snapshot ID (e.g. to scope defects and step
     * mutations). Fetching all of them up-front avoids lazy creation via
     * {@code ensureRunCaseId()} and eliminates per-case network requests.</p>
     */
    private List<TestRunCaseDTO> runCases;
}

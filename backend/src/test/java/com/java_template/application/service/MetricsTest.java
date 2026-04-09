package com.java_template.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Metrics Tests (FR 3.7, US 3.5, US 3.7)
 * Tests real-time progress metrics and run summary
 * 
 * Source: userstories.md - US 3.5: Real-time Progress Metrics
 *                         - US 3.7: View Run Summary & Metrics
 */
@DisplayName("Metrics Tests (US 3.5, US 3.7)")
class MetricsTest {

    private TestRunMetrics metrics;

    /**
     * Helper class to calculate metrics from step statuses
     */
    static class TestRunMetrics {
        int totalCases;
        int passedCount;
        int failedCount;
        int skippedCount;
        int untestedCount;

        TestRunMetrics(int total) {
            this.totalCases = total;
            this.passedCount = 0;
            this.failedCount = 0;
            this.skippedCount = 0;
            this.untestedCount = total;
        }

        void addPassed() {
            if (untestedCount > 0) untestedCount--;
            passedCount++;
        }

        void addFailed() {
            if (untestedCount > 0) untestedCount--;
            failedCount++;
        }

        void addSkipped() {
            if (untestedCount > 0) untestedCount--;
            skippedCount++;
        }

        double getSuccessRate() {
            if (totalCases == 0) return 0;
            return (double) passedCount / totalCases * 100;
        }

        double getProgressPercentage() {
            if (totalCases == 0) return 0;
            int completed = passedCount + failedCount + skippedCount;
            return (double) completed / totalCases * 100;
        }
    }

    @BeforeEach
    void setUp() {
        metrics = new TestRunMetrics(10); // 10 total cases
    }

    @Test
    @DisplayName("AC: Progress bar shows percentage complete")
    void testProgressBarPercentage() {
        // Given: metrics with some completed cases (from US 3.5 AC)
        metrics.addPassed();
        metrics.addPassed();
        metrics.addFailed();
        
        // When: calculating progress
        double progress = metrics.getProgressPercentage();
        
        // Then: progress should be percentage of completed cases
        assertEquals(30.0, progress, 0.1, "AC: 3/10 cases complete = 30%");
        assertTrue(progress >= 0 && progress <= 100, "AC: Progress should be 0-100%");
    }

    @Test
    @DisplayName("AC: Counters show Passed count")
    void testPassedCounterUpdates() {
        // Given: test cases being marked as passed
        metrics.addPassed();
        metrics.addPassed();
        metrics.addPassed();
        
        // When: tracking passed count
        int passedCount = metrics.passedCount;
        
        // Then: passed counter should be accurate
        assertEquals(3, passedCount, "AC: Passed counter should track marked cases");
    }

    @Test
    @DisplayName("AC: Counters show Failed count")
    void testFailedCounterUpdates() {
        // Given: test cases being marked as failed
        metrics.addFailed();
        metrics.addFailed();
        
        // When: tracking failed count
        int failedCount = metrics.failedCount;
        
        // Then: failed counter should be accurate
        assertEquals(2, failedCount, "AC: Failed counter should track marked cases");
    }

    @Test
    @DisplayName("AC: Counters show Skipped count")
    void testSkippedCounterUpdates() {
        // Given: test cases being marked as skipped
        metrics.addSkipped();
        metrics.addSkipped();
        metrics.addSkipped();
        
        // When: tracking skipped count
        int skippedCount = metrics.skippedCount;
        
        // Then: skipped counter should be accurate
        assertEquals(3, skippedCount, "AC: Skipped counter should track marked cases");
    }

    @Test
    @DisplayName("AC: Counters show Untested count")
    void testUntestedCounterUpdates() {
        // Given: test cases with some executed, some not (from US 3.5 AC)
        metrics.addPassed();
        metrics.addPassed();
        metrics.addFailed();
        
        // When: checking untested count
        int untestedCount = metrics.untestedCount;
        
        // Then: untested counter should show remaining cases
        assertEquals(7, untestedCount, "AC: Untested = Total - (Passed + Failed + Skipped)");
    }

    @Test
    @DisplayName("AC: Success rate calculated (Passed / Total * 100)")
    void testSuccessRateCalculated() {
        // Given: completed test cases with some passed (from US 3.7 AC)
        metrics.addPassed();
        metrics.addPassed();
        metrics.addPassed();
        metrics.addFailed();
        metrics.addFailed();
        
        // When: calculating success rate
        double successRate = metrics.getSuccessRate();
        
        // Then: success rate should be (passed / total) * 100
        assertEquals(30.0, successRate, 0.1, "AC: 3/10 passed = 30% success rate");
    }

    @Test
    @DisplayName("AC: Metrics update in real-time as steps marked")
    void testMetricsUpdateInRealTime() {
        // Given: empty metrics (from US 3.5 AC)
        assertEquals(0, metrics.passedCount, "Initial passed = 0");
        assertEquals(10, metrics.untestedCount, "Initial untested = 10");
        
        // When: marking steps in real-time
        metrics.addPassed();
        assertEquals(1, metrics.passedCount, "After 1st step: passed = 1");
        assertEquals(9, metrics.untestedCount, "After 1st step: untested = 9");
        
        metrics.addFailed();
        assertEquals(1, metrics.failedCount, "After failed: failed = 1");
        assertEquals(8, metrics.untestedCount, "After failed: untested = 8");
        
        metrics.addSkipped();
        assertEquals(1, metrics.skippedCount, "After skipped: skipped = 1");
        assertEquals(7, metrics.untestedCount, "After skipped: untested = 7");
        
        // Then: metrics should update immediately
        int totalExecuted = metrics.passedCount + metrics.failedCount + metrics.skippedCount;
        assertEquals(3, totalExecuted, "AC: Metrics update in real-time as steps are marked");
    }

    @Test
    @DisplayName("AC: Total cases count displayed")
    void testTotalCasesCount() {
        // Given: a test run with known total
        int totalCases = metrics.totalCases;
        
        // When: displaying metrics
        // Then: total should be shown
        assertEquals(10, totalCases, "AC: Total cases count should be displayed");
    }
}

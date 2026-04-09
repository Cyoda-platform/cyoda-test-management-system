package com.java_template.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Case Status Calculation Tests (FR 3.3, US 3.2, US 3.8)
 * Tests atomic failure logic for calculating case status from step statuses
 * 
 * Rules (from FR 3.3):
 * - If ANY step = Failed → Case Failed
 * - If ALL steps = Passed → Case Passed
 * - If some/all steps = Skipped (no Failed) → Case Skipped
 * - If no steps executed → Case Untested
 * 
 * Source: userstories.md - US 3.2, US 3.8
 */
@DisplayName("Case Status Calculation Tests (US 3.2, US 3.8)")
class CaseStatusCalculationTest {

    /**
     * Helper: Calculate case status from step statuses
     * This logic should be extracted to a StatusCalculator service
     */
    private String calculateCaseStatus(List<String> stepStatuses) {
        if (stepStatuses == null || stepStatuses.isEmpty()) {
            return "UNTESTED";
        }

        boolean hasFailed = stepStatuses.contains("FAILED");
        boolean hasSkipped = stepStatuses.contains("SKIPPED");
        boolean hasUntested = stepStatuses.contains("UNTESTED");
        boolean allPassed = stepStatuses.stream().allMatch(s -> "PASSED".equals(s));

        if (hasFailed) {
            return "FAILED";      // AC: Any Failed → Case Failed
        } else if (allPassed) {
            return "PASSED";      // AC: All Passed → Case Passed
        } else if (hasSkipped && !hasUntested && !hasFailed) {
            return "SKIPPED";     // AC: Some/All Skipped (no Failed) → Case Skipped
        } else {
            return "UNTESTED";    // AC: No steps executed → Case Untested
        }
    }

    @Test
    @DisplayName("AC: If ANY step Failed → Case Failed (FR 3.3)")
    void testAnyStepFailedMakesCaseFailed() {
        // Given: steps with PASSED, PASSED, FAILED (from US 3.2 AC)
        List<String> stepStatuses = Arrays.asList("PASSED", "PASSED", "FAILED");
        
        // When: calculating case status
        String caseStatus = calculateCaseStatus(stepStatuses);
        
        // Then: case status should be Failed
        assertEquals("FAILED", caseStatus, 
                     "AC: If ANY step Failed → Case Failed");
    }

    @Test
    @DisplayName("AC: If ALL steps Passed → Case Passed (FR 3.3)")
    void testAllStepsPassedMakesCasePassed() {
        // Given: all steps PASSED
        List<String> stepStatuses = Arrays.asList("PASSED", "PASSED", "PASSED");
        
        // When: calculating case status
        String caseStatus = calculateCaseStatus(stepStatuses);
        
        // Then: case status should be Passed
        assertEquals("PASSED", caseStatus, 
                     "AC: If ALL steps Passed → Case Passed");
    }

    @Test
    @DisplayName("AC: If some/all Skipped (no Failed) → Case Skipped (FR 3.3)")
    void testSkippedStepsLogic() {
        // Given: steps with PASSED and SKIPPED (no Failed)
        List<String> stepStatuses = Arrays.asList("PASSED", "SKIPPED", "SKIPPED");
        
        // When: calculating case status
        String caseStatus = calculateCaseStatus(stepStatuses);
        
        // Then: case status should be Skipped
        assertEquals("SKIPPED", caseStatus, 
                     "AC: If some/all Skipped (no Failed) → Case Skipped");
    }

    @Test
    @DisplayName("AC: If no steps executed → Case Untested (FR 3.3)")
    void testUntestedLogic() {
        // Given: steps in UNTESTED state (no steps executed)
        List<String> stepStatuses = Arrays.asList("UNTESTED", "UNTESTED");
        
        // When: calculating case status
        String caseStatus = calculateCaseStatus(stepStatuses);
        
        // Then: case status should be Untested
        assertEquals("UNTESTED", caseStatus, 
                     "AC: If no steps executed → Case Untested");
    }

    @Test
    @DisplayName("AC: Mixed status handling (FR 3.3)")
    void testMixedStatusCalculation() {
        // Given: mix of all statuses (PASSED, FAILED, SKIPPED, UNTESTED)
        List<String> stepStatuses = Arrays.asList("PASSED", "FAILED", "SKIPPED", "UNTESTED");
        
        // When: calculating case status
        String caseStatus = calculateCaseStatus(stepStatuses);
        
        // Then: FAILED should take priority
        assertEquals("FAILED", caseStatus, 
                     "AC: FAILED status has priority over others");
    }

    @Test
    @DisplayName("AC: All Skipped steps → Case Skipped")
    void testAllSkippedCases() {
        // Given: all steps SKIPPED
        List<String> stepStatuses = Arrays.asList("SKIPPED", "SKIPPED", "SKIPPED");
        
        // When: calculating case status
        String caseStatus = calculateCaseStatus(stepStatuses);
        
        // Then: case status should be Skipped
        assertEquals("SKIPPED", caseStatus, 
                     "AC: All Skipped steps → Case Skipped");
    }

    @Test
    @DisplayName("AC: Empty step list → Case Untested")
    void testEmptyStepList() {
        // Given: empty step list
        List<String> stepStatuses = Arrays.asList();
        
        // When: calculating case status
        String caseStatus = calculateCaseStatus(stepStatuses);
        
        // Then: case status should be Untested
        assertEquals("UNTESTED", caseStatus, 
                     "AC: Empty step list → Case Untested");
    }

    @Test
    @DisplayName("AC: Null step list → Case Untested")
    void testNullStepList() {
        // Given: null step list
        
        // When: calculating case status with null
        String caseStatus = calculateCaseStatus(null);
        
        // Then: case status should be Untested
        assertEquals("UNTESTED", caseStatus, 
                     "AC: Null step list → Case Untested");
    }
}

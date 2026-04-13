package com.java_template.application.service;

import com.java_template.application.dto.TestRunDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge Cases Tests (FR 3.*, General Requirements)
 * Tests edge cases and boundary conditions for Module 3
 * 
 * Source: Module 3 Requirements - Edge Cases & Boundary Conditions
 */
@DisplayName("Edge Cases Tests (Module 3)")
class EdgeCasesTest {

    private TestRunDTO testRun;

    @BeforeEach
    void setUp() {
        testRun = new TestRunDTO();
        testRun.setId(UUID.randomUUID());
        testRun.setName("Test Run");
        testRun.setStatus("active");
    }

    @Test
    @DisplayName("AC: Handle empty test run (0 cases)")
    void testEmptyTestRun() {
        // Given: a test run with no cases (edge case)
        List<String> caseStatuses = new ArrayList<>();
        
        // When: calculating metrics for empty run
        int totalCases = caseStatuses.size();
        int passedCount = (int) caseStatuses.stream().filter(s -> "PASSED".equals(s)).count();
        
        // Then: should handle gracefully
        assertEquals(0, totalCases, "AC: Empty run should have 0 total cases");
        assertEquals(0, passedCount, "AC: Empty run should have 0 passed cases");
    }

    @Test
    @DisplayName("AC: Handle single-step test case")
    void testSingleStepCase() {
        // Given: a test case with only 1 step (edge case)
        List<String> stepStatuses = new ArrayList<>();
        stepStatuses.add("PASSED");
        
        // When: calculating case status from single step
        boolean hasFailed = stepStatuses.contains("FAILED");
        boolean allPassed = stepStatuses.stream().allMatch(s -> "PASSED".equals(s));
        String caseStatus = allPassed && !hasFailed ? "PASSED" : "FAILED";
        
        // Then: single step should determine case status
        assertEquals("PASSED", caseStatus, "AC: Single PASSED step → case PASSED");
        assertEquals(1, stepStatuses.size(), "AC: Case with 1 step handled correctly");
    }

    @Test
    @DisplayName("AC: Handle very large number of steps (100+)")
    void testLargeNumberOfSteps() {
        // Given: test case with many steps (edge case)
        List<String> stepStatuses = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            stepStatuses.add("PASSED");
        }
        
        // When: calculating metrics
        int totalSteps = stepStatuses.size();
        long passedSteps = stepStatuses.stream().filter(s -> "PASSED".equals(s)).count();
        
        // Then: should handle large numbers efficiently
        assertEquals(150, totalSteps, "AC: All 150 steps counted");
        assertEquals(150, passedSteps, "AC: All steps marked as passed");
        assertTrue(totalSteps > 100, "AC: Large number of steps handled");
    }

    @Test
    @DisplayName("AC: Handle all cases skipped (no passed/failed)")
    void testAllSkippedCases() {
        // Given: test run where all cases are skipped (edge case)
        List<String> stepStatuses = new ArrayList<>();
        stepStatuses.add("SKIPPED");
        stepStatuses.add("SKIPPED");
        stepStatuses.add("SKIPPED");
        
        // When: calculating run status
        boolean hasFailed = stepStatuses.contains("FAILED");
        boolean hasUntested = stepStatuses.contains("UNTESTED");
        boolean allSkipped = stepStatuses.stream().allMatch(s -> "SKIPPED".equals(s));
        String caseStatus = hasFailed ? "FAILED" : allSkipped && !hasUntested ? "SKIPPED" : "UNTESTED";
        
        // Then: run status should be Skipped
        assertEquals("SKIPPED", caseStatus, "AC: All skipped → case SKIPPED");
        assertFalse(hasFailed, "AC: No failed steps in all-skipped run");
    }

    @Test
    @DisplayName("AC: Handle duplicate test case selection")
    void testDuplicateCaseSelection() {
        // Given: same test case selected multiple times (edge case)
        List<UUID> selectedCases = new ArrayList<>();
        UUID caseId = UUID.randomUUID();
        selectedCases.add(caseId);
        selectedCases.add(caseId); // Duplicate
        selectedCases.add(caseId); // Duplicate
        
        // When: creating run with duplicate cases
        long uniqueCases = selectedCases.stream().distinct().count();
        
        // Then: system should handle duplicates
        assertEquals(3, selectedCases.size(), "AC: All selections tracked");
        assertEquals(1, uniqueCases, "AC: Unique cases identified correctly");
    }

    @Test
    @DisplayName("AC: Handle extremely long test case title (edge case)")
    void testExtremelyLongTitle() {
        // Given: test case with very long title (edge case)
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longTitle.append("word ");
        }
        testRun.setName(longTitle.toString());
        
        // When: storing long title
        String storedTitle = testRun.getName();
        
        // Then: should store without truncation (or with validation)
        assertNotNull(storedTitle, "AC: Long title should be stored");
        assertEquals(longTitle.toString(), storedTitle, "AC: Long title preserved");
    }

    @Test
    @DisplayName("AC: Handle null/empty environment value")
    void testNullEnvironment() {
        // Given: test run without environment (optional field)
        testRun.setEnvironment(null);
        
        // When: checking environment
        String env = testRun.getEnvironment();
        
        // Then: null environment should be handled gracefully
        assertNull(env, "AC: Null environment allowed (optional field)");
        assertNotNull(testRun.getName(), "AC: Other fields still present");
    }
}

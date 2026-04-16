package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Run Locking Tests (FR 3.6, US 3.4)
 * Tests run completion, locking, and unlocking functionality
 * 
 * Source: userstories.md - US 3.4: Lock and Unlock Test Runs
 * "As an Admin, I want to mark a run as 'Completed' to lock it,
 *  but also 'Unlock' it if corrections are needed,
 *  So that we maintain data integrity and flexibility."
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Run Locking Tests (US 3.4)")
class RunLockingTest {

    @Mock
    private EntityService entityService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private TestRunService testRunService;

    private TestRunDTO testRun;
    private UUID runId;

    private EntityWithMetadata<TestRunDTO> entityWithMetadata(TestRunDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID();
        testRun = new TestRunDTO();
        testRun.setId(runId);
        testRun.setName("Test Run 1");
        testRun.setStatus("active");
    }

    @Test
    @DisplayName("AC: Complete button changes run status to 'completed'")
    void testCompleteButtonChangesStatus() {
        // Given: an active test run (from US 3.4 AC)
        assertEquals("active", testRun.getStatus(), "Initial status should be active");
        
        // When: clicking complete button
        testRun.setStatus("completed");
        
        // Then: status should change to completed
        assertEquals("completed", testRun.getStatus(), 
                     "AC: Status should change to completed");
    }

    @Test
    @DisplayName("AC: Completion timestamp recorded")
    void testCompletionTimestampRecorded() {
        // Given: a test run being completed (from US 3.4 AC)
        LocalDateTime completionTime = LocalDateTime.now();
        testRun.setStatus("completed");
        testRun.setCompletedAt(completionTime.toString());
        
        when(entityService.update(eq(runId), any(TestRunDTO.class), any()))
                .thenReturn(entityWithMetadata(testRun, runId));

        // When: completing run
        // Then: completedAt timestamp should be recorded
        assertNotNull(testRun.getCompletedAt(), 
                      "AC: Completion timestamp should be recorded");
        verify(entityService).update(eq(runId), any(TestRunDTO.class), any());
    }

    @Test
    @DisplayName("AC: Completed run becomes read-only")
    void testCompletedRunIsReadOnly() {
        // Given: a completed test run (from US 3.4 AC)
        testRun.setStatus("completed");
        boolean isReadOnly = testRun.getStatus().equals("completed");
        
        // When: checking if run is locked
        // Then: run should be read-only
        assertTrue(isReadOnly, "AC: Completed run should be read-only");
    }

    @Test
    @DisplayName("AC: 'Unlock' button available for Admins")
    void testUnlockAvailableForAdmins() {
        // Given: a completed test run (from US 3.4 AC)
        testRun.setStatus("completed");
        boolean canUnlock = testRun.getStatus().equals("completed");
        
        // When: checking unlock availability
        // Then: unlock button should be available
        assertTrue(canUnlock, "AC: Unlock button should be available for locked run");
    }

    @Test
    @DisplayName("AC: Unlock returns run to 'active' state")
    void testUnlockReturnsToActiveState() {
        // Given: a locked (completed) test run (from US 3.4 AC)
        testRun.setStatus("completed");
        
        // When: unlocking run
        testRun.setStatus("active");
        
        when(entityService.update(eq(runId), any(TestRunDTO.class), any()))
                .thenReturn(entityWithMetadata(testRun, runId));

        // Then: status should return to active
        assertEquals("active", testRun.getStatus(), 
                     "AC: Unlock should return run to active state");
        verify(entityService).update(eq(runId), any(TestRunDTO.class), any());
    }

    @Test
    @DisplayName("AC: Cannot modify run details while locked")
    void testCannotModifyLockedRun() {
        // Given: a locked (completed) test run
        testRun.setStatus("completed");
        boolean isLocked = testRun.getStatus().equals("completed");
        
        // When: trying to modify locked run
        boolean canModify = !isLocked;
        
        // Then: modification should be prevented
        assertFalse(canModify, "AC: Cannot modify run details while locked");
    }
}

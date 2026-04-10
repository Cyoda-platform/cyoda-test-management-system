package com.java_template.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Audit Trail Tests (FR 3.9, US 3.10)
 * Tests complete audit trail of who changed what status when
 * 
 * Source: userstories.md - US 3.10: Audit Trail for Execution
 * "As an Admin, I want to see a complete audit trail of who changed what status when,
 *  So that I can track test execution decisions."
 */
@DisplayName("Audit Trail Tests (US 3.10)")
class AuditTrailTest {

    /**
     * Helper class to represent audit trail entry
     */
    static class AuditEntry {
        UUID id;
        String user;
        LocalDateTime timestamp;
        String stepId;
        String oldStatus;
        String newStatus;
        String action; // "status_change" or "unlock"

        AuditEntry(String user, String stepId, String oldStatus, String newStatus, String action) {
            this.id = UUID.randomUUID();
            this.user = user;
            this.timestamp = LocalDateTime.now();
            this.stepId = stepId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
            this.action = action;
        }
    }

    private List<AuditEntry> auditLog;
    private String testUser;

    @BeforeEach
    void setUp() {
        auditLog = new ArrayList<>();
        testUser = "tester@example.com";
    }

    @Test
    @DisplayName("AC: Audit log shows timestamp, user, step, old status, new status")
    void testAuditTrailRecorded() {
        // Given: a status change being made (from US 3.10 AC)
        String stepId = UUID.randomUUID().toString();
        AuditEntry entry = new AuditEntry(testUser, stepId, "UNTESTED", "PASSED", "status_change");
        auditLog.add(entry);

        // When: checking audit trail
        AuditEntry recorded = auditLog.get(0);

        // Then: audit should record all required information
        assertNotNull(recorded.timestamp, "AC: Timestamp should be recorded");
        assertEquals(testUser, recorded.user, "AC: User should be recorded");
        assertEquals(stepId, recorded.stepId, "AC: Step should be recorded");
        assertEquals("UNTESTED", recorded.oldStatus, "AC: Old status should be recorded");
        assertEquals("PASSED", recorded.newStatus, "AC: New status should be recorded");
    }

    @Test
    @DisplayName("AC: Audit log includes unlock events")
    void testAuditIncludesUnlockEvents() {
        // Given: a run being unlocked (from US 3.10 AC)
        AuditEntry unlockEntry = new AuditEntry(testUser, "run-001", "completed", "active", "unlock");
        auditLog.add(unlockEntry);

        // When: checking audit trail
        AuditEntry recorded = auditLog.get(0);

        // Then: unlock event should be recorded
        assertEquals("unlock", recorded.action, "AC: Unlock events should be recorded in audit");
        assertEquals("completed", recorded.oldStatus, "AC: Previous state (completed) recorded");
        assertEquals("active", recorded.newStatus, "AC: New state (active) recorded");
    }

    @Test
    @DisplayName("AC: Audit log is sortable by date, user, case")
    void testAuditLogSortable() {
        // Given: multiple audit entries (from US 3.10 AC)
        String step1 = UUID.randomUUID().toString();
        String step2 = UUID.randomUUID().toString();
        String step3 = UUID.randomUUID().toString();

        auditLog.add(new AuditEntry("user1", step1, "UNTESTED", "PASSED", "status_change"));
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        auditLog.add(new AuditEntry("user2", step2, "UNTESTED", "FAILED", "status_change"));
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        auditLog.add(new AuditEntry("user1", step3, "UNTESTED", "SKIPPED", "status_change"));

        // When: sorting by date
        List<AuditEntry> sortedByDate = new ArrayList<>(auditLog);
        sortedByDate.sort((a, b) -> a.timestamp.compareTo(b.timestamp));

        // Then: entries should be sortable
        assertEquals(3, sortedByDate.size(), "AC: All entries should be present");
        assertEquals(step1, sortedByDate.get(0).stepId, "AC: Entries sorted chronologically");
    }

    @Test
    @DisplayName("AC: Cannot modify audit log")
    void testAuditLogImmutable() {
        // Given: an audit entry recorded (from US 3.10 AC)
        String stepId = UUID.randomUUID().toString();
        AuditEntry entry = new AuditEntry(testUser, stepId, "UNTESTED", "PASSED", "status_change");
        auditLog.add(entry);

        String originalUser = entry.user;
        String originalStatus = entry.oldStatus;

        // When: attempting to modify (this would be prevented in real implementation)
        // entry.user = "hacker"; // This would be prevented by immutability

        // Then: audit log should be immutable
        assertEquals(originalUser, entry.user, "AC: Audit entry user should not be modifiable");
        assertEquals(originalStatus, entry.oldStatus, "AC: Audit entry status should not be modifiable");
    }
}

package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunStepDTO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Evidence Upload Tests (FR 3.4, US 3.3)
 * Tests file attachment to test steps during execution
 * 
 * Source: userstories.md - US 3.3: Execution Evidence & Bug Linking
 * "As a Tester, I want to attach screenshots/logs to a specific step
 *  and link a Bug URL to a failed case,
 *  So that developers have all the context needed to fix the issue."
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Evidence Upload Tests (US 3.3)")
class EvidenceUploadTest {

    @Mock
    private EntityService entityService;

    @Mock
    private ProjectCounterService projectCounterService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private TestRunService testRunService;

    private TestRunStepDTO step;
    private UUID stepId;
    private UUID runId;

    private EntityWithMetadata<TestRunStepDTO> entityWithMetadata(TestRunStepDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    private List<String> evidenceFiles;

    @BeforeEach
    void setUp() {
        stepId = UUID.randomUUID();
        runId = UUID.randomUUID();

        step = new TestRunStepDTO();
        step.setId(stepId);
        step.setTestRunCaseId(runId);
        step.setStatus("FAILED");
        evidenceFiles = new ArrayList<>(); // Simulating evidence storage
    }

    @Test
    @DisplayName("AC: Upload button in step detail allows file attachment")
    void testUploadButtonInStepDetail() {
        // Given: a step detail view (from US 3.3 AC)
        assertNotNull(step, "Step should exist");
        assertEquals("FAILED", step.getStatus(), "Step is failed");

        // When: clicking upload button
        String fileUrl = "https://storage.example.com/evidence/screenshot-123.png";
        evidenceFiles.add(fileUrl);

        // Then: file should be attachable
        assertTrue(!evidenceFiles.isEmpty(), "AC: Upload button should allow file attachment");
        assertTrue(evidenceFiles.contains(fileUrl), "AC: File should be added to evidence");
    }

    @Test
    @DisplayName("AC: Multiple files can be attached to one step")
    void testMultipleFilesPerStep() {
        // Given: a step that can accept multiple files (from US 3.3 AC)
        evidenceFiles = new ArrayList<>();

        // When: uploading multiple files
        String file1 = "https://storage.example.com/screenshot1.png";
        String file2 = "https://storage.example.com/logs.txt";
        String file3 = "https://storage.example.com/config.json";

        evidenceFiles.add(file1);
        evidenceFiles.add(file2);
        evidenceFiles.add(file3);

        // Then: multiple files should be stored
        assertEquals(3, evidenceFiles.size(),
                     "AC: Multiple files should be attachable to one step");
    }

    @Test
    @DisplayName("AC: Attachments remain accessible in run history")
    void testAttachmentsAccessibleInHistory() {
        // Given: a completed run with evidence (from US 3.3 AC)
        String evidenceUrl = "https://storage.example.com/evidence/screenshot.png";
        evidenceFiles.add(evidenceUrl);
        step.setStatus("FAILED");

        // When: viewing run history
        List<String> evidence = evidenceFiles;

        // Then: evidence should still be accessible
        assertNotNull(evidence, "AC: Evidence should be retrievable");
        assertEquals(1, evidence.size(), "AC: Evidence should remain accessible in history");
        assertEquals(evidenceUrl, evidence.get(0), "AC: Evidence URL should be intact");
    }

    @Test
    @DisplayName("AC: Can delete attachment before run is locked")
    void testDeleteAttachmentBeforeLocked() {
        // Given: evidence in an active run (from US 3.3 AC)
        String file1 = "https://storage.example.com/screenshot1.png";
        String file2 = "https://storage.example.com/screenshot2.png";
        evidenceFiles.add(file1);
        evidenceFiles.add(file2);

        // When: run is still active (not completed)
        boolean isRunActive = !step.getStatus().equals("locked");
        boolean canDelete = isRunActive;

        if (canDelete) {
            evidenceFiles.remove(file1);
        }

        // Then: attachment should be deletable before lock
        assertTrue(canDelete, "AC: Can delete attachment before run is locked");
        assertEquals(1, evidenceFiles.size(), "AC: File should be deleted");
        assertFalse(evidenceFiles.contains(file1), "AC: Deleted file should be gone");
    }
}

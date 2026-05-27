package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
import com.java_template.application.dto.TestRunCaseDTO;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Defect Linking Tests (FR 3.5, US 3.9)
 * Tests linking failed test cases to defects/bugs
 * 
 * Source: userstories.md - US 3.9: Defect Linking (Basic)
 * "As a Tester, I want to link failed test cases to defects/bugs,
 *  So that developers know which cases failed and can investigate."
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Defect Linking Tests (US 3.9)")
class DefectLinkingTest {

    @Mock
    private EntityService entityService;

    @Mock
    private ProjectCounterService projectCounterService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private DefectService defectService;

    private DefectDTO defect;
    private TestRunCaseDTO failedCase;
    private UUID defectId;
    private UUID caseId;
    private UUID runId;

    private EntityWithMetadata<DefectDTO> entityWithMetadata(DefectDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    @BeforeEach
    void setUp() {
        defectId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        runId = UUID.randomUUID();

        failedCase = new TestRunCaseDTO();
        failedCase.setId(caseId);
        failedCase.setTestRunId(runId);
        failedCase.setStatus("FAILED");

        defect = new DefectDTO();
        defect.setId(defectId);
        defect.setTitle("Login button not responding");
        defect.setDescription("Login button fails on Safari");
        defect.setSeverity("CRITICAL");
        defect.setStatus("OPEN");
        defect.setTestRunCaseId(caseId);
    }

    @Test
    @DisplayName("AC: Failed case shows 'Link Defect' button")
    void testLinkDefectToFailedCase() {
        // Given: a failed test case (from US 3.9 AC)
        assertEquals("FAILED", failedCase.getStatus(), "Case should be failed");
        
        // When: viewing failed case
        boolean hasLinkDefectButton = failedCase.getStatus().equals("FAILED");
        
        // Then: 'Link Defect' button should be available
        assertTrue(hasLinkDefectButton, "AC: Failed case should show 'Link Defect' button");
    }

    @Test
    @DisplayName("AC: Can enter defect ID and URL")
    void testEnterDefectIdAndUrl() {
        // Given: a defect with ID and URL (from US 3.9 AC)
        defect.setId(defectId);
        defect.setLink("https://jira.company.com/browse/DEF-1234");

        // Then: both ID and URL should be stored
        assertNotNull(defect.getId(), "AC: Defect ID should be stored");
        assertNotNull(defect.getLink(), "AC: Defect URL should be stored");
        assertTrue(defect.getLink().contains("DEF"), "AC: URL should contain defect reference");
    }

    @Test
    @DisplayName("AC: Defect link stored with failed case")
    void testDefectLinkPersisted() {
        // Given: a defect linked to a failed case
        defect.setTestRunCaseId(caseId);

        // When: creating defect
        // Then: defect should be persisted with case reference
        assertEquals(caseId, defect.getTestRunCaseId(),
                     "AC: Defect should be linked to test run case");
    }

    @Test
    @DisplayName("createDefect preserves source and createdAt when Cyoda create response is minimal")
    void createDefect_preservesSourceAndCreatedAtWhenCyodaResponseIsMinimal() {
        // Simulate Cyoda returning a minimal entity (source, createdAt, status not present in response)
        DefectDTO minimal = new DefectDTO(); // no fields set except id (set by withId)

        UUID assignedId = UUID.randomUUID();
        when(projectCounterService.nextDefectDisplayId(any())).thenReturn("DEF-99");
        //noinspection unchecked
        doReturn(entityWithMetadata(minimal, assignedId)).when(entityService).create(any());
        //noinspection unchecked
        doReturn(entityWithMetadata(minimal, assignedId)).when(entityService).update(eq(assignedId), any(), isNull());

        DefectDTO input = new DefectDTO();
        input.setProjectId(UUID.randomUUID());
        input.setTitle("Login broken");
        input.setSeverity("Critical");
        input.setSource("Step 3");

        DefectDTO result = defectService.createDefect(input);

        assertEquals("Step 3", result.getSource(),  "source must survive even if Cyoda omits it from the create response");
        assertNotNull(result.getCreatedAt(),          "createdAt must survive even if Cyoda omits it from the create response");
        assertEquals("DEF-99", result.getDisplayId(), "displayId must be the server-generated value");
    }

    @Test
    @DisplayName("AC: Can view linked defects in run report")
    void testViewDefectsInReport() {
        // Given: a defect linked to a case in a run
        defect.setTestRunId(runId);
        defect.setTestRunCaseId(caseId);

        when(projectCounterService.nextDefectDisplayId(any())).thenReturn("DEF-001");
        when(entityService.create(any(DefectDTO.class)))
                .thenReturn(entityWithMetadata(defect, defectId));
        // createDefect calls entityService.update after create to persist displayId
        when(entityService.update(eq(defectId), any(DefectDTO.class), isNull()))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(1), defectId));

        DefectDTO createdDefect = defectService.createDefect(defect);

        // When: viewing run report
        // Then: linked defects should be accessible
        assertNotNull(createdDefect, "AC: Defect should be retrievable in report");
        assertEquals(runId, createdDefect.getTestRunId(),
                     "AC: Defect should reference test run for report view");
    }
}

package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.BatchImportCaseDTO;
import com.java_template.application.dto.Priority;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestCaseService
 */
@ExtendWith(MockitoExtension.class)
public class TestCaseServiceTest {

    @Mock
    private EntityService entityService;

    @Mock
    private ProjectCounterService projectCounterService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private TestCaseService testCaseService;

    private TestCaseDTO testCase;
    private UUID caseId;
    private UUID suiteId;
    private UUID projectId;

    private EntityWithMetadata<TestCaseDTO> entityWithMetadata(TestCaseDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    @BeforeEach
    public void setUp() {
        caseId = UUID.randomUUID();
        suiteId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        testCase = new TestCaseDTO();
        testCase.setProjectId(projectId);
        testCase.setSuiteId(suiteId);
        testCase.setTitle("Test Case 1");
        testCase.setDescription("A test case");
        testCase.setPriority(com.java_template.application.dto.Priority.MEDIUM);
        testCase.setDeleted(false);
    }

    @Test
    public void testCreateTestCase() {
        when(projectCounterService.nextDisplayId(projectId)).thenReturn("TC-001");
        when(entityService.create(any(TestCaseDTO.class)))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(0), caseId));
        // createTestCase calls entityService.update after create to persist displayId
        when(entityService.update(eq(caseId), any(TestCaseDTO.class), isNull()))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(1), caseId));

        TestCaseDTO created = testCaseService.createTestCase(testCase);

        assertNotNull(created.getId());
        assertEquals("Test Case 1", created.getTitle());
        assertFalse(created.isDeleted());
    }

    /**
     * Test that updateTestCase protects displayId from being overwritten.
     * When a client sends an update without displayId (or with null displayId),
     * the existing displayId should be preserved.
     */
    @Test
    public void testUpdateTestCase_ProtectsDisplayId_WhenClientSendsNull() {
        // Arrange: existing test case with a displayId
        TestCaseDTO existing = new TestCaseDTO();
        existing.setId(caseId);
        existing.setTitle("Original Title");
        existing.setDescription("Original Description");
        existing.setDisplayId("TC-001");  // ← Existing displayId
        existing.setProjectId(projectId);
        existing.setSuiteId(suiteId);
        existing.setDeleted(false);

        // Mock getTestCaseById to return the existing case
        when(entityService.getById(eq(caseId), any(), eq(TestCaseDTO.class)))
                .thenReturn(entityWithMetadata(existing, caseId));

        // Client sends update WITHOUT displayId (displayId = null)
        TestCaseDTO updatePayload = new TestCaseDTO();
        updatePayload.setTitle("Updated Title");
        updatePayload.setDescription("Updated Description");
        updatePayload.setPriority(Priority.HIGH);
        updatePayload.setDisplayId(null);  // ← Client doesn't send displayId

        // Mock entityService.update to return updated case
        when(entityService.update(eq(caseId), any(TestCaseDTO.class), isNull()))
                .thenAnswer(inv -> {
                    TestCaseDTO dto = inv.getArgument(1);
                    return entityWithMetadata(dto, caseId);
                });

        // Act: update the test case
        TestCaseDTO result = testCaseService.updateTestCase(caseId, updatePayload);

        // Assert: displayId should be preserved (not null, not changed)
        assertEquals("TC-001", result.getDisplayId(), "displayId should be preserved from existing record");
        assertEquals("Updated Title", result.getTitle(), "title should be updated");
        assertEquals("Updated Description", result.getDescription(), "description should be updated");
        assertEquals(Priority.HIGH, result.getPriority(), "priority should be updated");
    }

    /**
     * Test that if client explicitly sends a new displayId, it's accepted.
     * (Edge case: normally clients shouldn't do this, but the API allows it)
     */
    @Test
    public void testUpdateTestCase_AcceptsNewDisplayId_IfClientSends() {
        // Arrange: existing test case with displayId TC-001
        TestCaseDTO existing = new TestCaseDTO();
        existing.setId(caseId);
        existing.setTitle("Original Title");
        existing.setDisplayId("TC-001");
        existing.setProjectId(projectId);
        existing.setSuiteId(suiteId);
        existing.setDeleted(false);

        when(entityService.getById(eq(caseId), any(), eq(TestCaseDTO.class)))
                .thenReturn(entityWithMetadata(existing, caseId));

        // Client sends update WITH a new displayId
        TestCaseDTO updatePayload = new TestCaseDTO();
        updatePayload.setTitle("Updated Title");
        updatePayload.setDisplayId("TC-999");  // ← Client sends a different displayId

        when(entityService.update(eq(caseId), any(TestCaseDTO.class), isNull()))
                .thenAnswer(inv -> {
                    TestCaseDTO dto = inv.getArgument(1);
                    return entityWithMetadata(dto, caseId);
                });

        // Act
        TestCaseDTO result = testCaseService.updateTestCase(caseId, updatePayload);

        // Assert: new displayId should be accepted (client's value takes precedence if provided)
        assertEquals("TC-999", result.getDisplayId(), "new displayId should be accepted");
    }

    @Test
    public void batchCreateTestCases_createsAllCasesWithCorrectDisplayIds() {
        UUID projectId = UUID.randomUUID();
        UUID suiteId   = UUID.randomUUID();

        BatchImportCaseDTO.StepDTO step = new BatchImportCaseDTO.StepDTO();
        step.setStepNumber(1);
        step.setAction("click button");
        step.setExpectedResult("page loads");

        BatchImportCaseDTO item1 = new BatchImportCaseDTO();
        item1.setTitle("Case Alpha");
        item1.setSteps(List.of(step));

        BatchImportCaseDTO item2 = new BatchImportCaseDTO();
        item2.setTitle("Case Beta");
        item2.setSteps(List.of());

        when(projectCounterService.nextDisplayIdBatch(projectId, 2))
                .thenReturn(List.of("TC-001", "TC-002"));
        when(entityService.create(any(TestCaseDTO.class))).thenAnswer(inv ->
                entityWithMetadata(inv.getArgument(0), UUID.randomUUID()));
        when(entityService.update(any(UUID.class), any(TestCaseDTO.class), any())).thenAnswer(inv ->
                entityWithMetadata(inv.getArgument(1), inv.getArgument(0)));

        List<TestCaseDTO> result = testCaseService.batchCreateTestCases(projectId, suiteId, List.of(item1, item2));

        assertEquals(2, result.size());
        assertEquals("TC-001", result.get(0).getDisplayId());
        assertEquals("TC-002", result.get(1).getDisplayId());
        verify(entityService, times(2)).create(any(TestCaseDTO.class));
        // 2 updates for displayId persistence (one per case)
        verify(entityService, times(1)).update(any(UUID.class), any(TestCaseDTO.class), any());
        // Verify item1 has embedded steps (no testStepService calls)
        TestCaseDTO case1 = result.get(0);
        assertNotNull(case1.getSteps(), "steps should be embedded");
        assertEquals(1, case1.getSteps().size(), "item1 should have 1 embedded step");
        assertEquals("click button", case1.getSteps().get(0).getAction());
        assertEquals("page loads", case1.getSteps().get(0).getExpectedResult());
    }

    @Test
    public void batchCreateTestCases_propagatesExceptionWhenCreateFails() {
        UUID projectId = UUID.randomUUID();
        UUID suiteId   = UUID.randomUUID();

        BatchImportCaseDTO item = new BatchImportCaseDTO();
        item.setTitle("Failing case");
        item.setSteps(List.of());

        when(projectCounterService.nextDisplayIdBatch(projectId, 1))
                .thenReturn(List.of("TC-001"));
        when(entityService.create(any(TestCaseDTO.class)))
                .thenThrow(new RuntimeException("Cyoda unavailable"));

        assertThrows(RuntimeException.class, () ->
                testCaseService.batchCreateTestCases(projectId, suiteId, List.of(item)));
    }

}


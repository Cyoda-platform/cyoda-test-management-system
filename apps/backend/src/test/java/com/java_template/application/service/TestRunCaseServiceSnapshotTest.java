package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.Priority;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.service.EntityService;
import com.java_template.common.workflow.CyodaEntity;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestRunCaseService — createTestRunCaseWithStepSnapshots")
class TestRunCaseServiceSnapshotTest {

    @Mock private EntityService entityService;
    @Spy  private ObjectMapper objectMapper;
    @Mock private TestRunStepService testRunStepService;

    @InjectMocks
    private TestRunCaseService service;

    private UUID testRunId;
    private UUID testCaseId;
    private UUID projectId;
    private UUID runCaseId;

    private <T extends CyodaEntity> EntityWithMetadata<T> wrap(T dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @BeforeEach
    void setUp() {
        testRunId  = UUID.randomUUID();
        testCaseId = UUID.randomUUID();
        projectId  = UUID.randomUUID();
        runCaseId  = UUID.randomUUID();
    }

    @Test
    @DisplayName("Creates TestRunCase with snapshot fields copied from TestCase")
    void createsRunCaseWithSnapshotFields() {
        TestRunCaseDTO input = new TestRunCaseDTO();
        input.setTestRunId(testRunId);
        input.setTestCaseId(testCaseId);
        input.setProjectId(projectId);
        input.setTitle("Login test");
        input.setDescription("Verifies login flow");
        input.setPriority(Priority.HIGH);
        input.setDisplayId("TC-001");
        input.setSuiteId(UUID.randomUUID());

        when(entityService.create(any(TestRunCaseDTO.class))).thenReturn(wrap(input, runCaseId));

        TestRunCaseDTO result = service.createTestRunCaseWithStepSnapshots(input, List.of());

        assertEquals(runCaseId, result.getId());
        verify(entityService).create(any(TestRunCaseDTO.class));
        verifyNoInteractions(testRunStepService);
    }

    @Test
    @DisplayName("Creates TestRunStep for each TestStep with snapshot fields")
    void createsRunStepsWithSnapshotFields() {
        TestRunCaseDTO caseInput = new TestRunCaseDTO();
        caseInput.setTestRunId(testRunId);
        caseInput.setTestCaseId(testCaseId);
        caseInput.setProjectId(projectId);

        UUID stepId1 = UUID.randomUUID();
        UUID stepId2 = UUID.randomUUID();
        UUID runStepId1 = UUID.randomUUID();

        TestStepDTO step1 = new TestStepDTO();
        step1.setId(stepId1);
        step1.setStepNumber(1);
        step1.setAction("Open browser");
        step1.setExpectedResult("Browser opens");

        TestStepDTO step2 = new TestStepDTO();
        step2.setId(stepId2);
        step2.setStepNumber(2);
        step2.setAction("Enter URL");
        step2.setExpectedResult("Page loads");

        when(entityService.create(any(TestRunCaseDTO.class))).thenReturn(wrap(caseInput, runCaseId));

        TestRunStepDTO runStep1 = new TestRunStepDTO();
        runStep1.setId(runStepId1);

        when(testRunStepService.createTestRunStep(any())).thenReturn(runStep1).thenReturn(new TestRunStepDTO());

        service.createTestRunCaseWithStepSnapshots(caseInput, List.of(step1, step2));

        verify(testRunStepService, times(2)).createTestRunStep(argThat(s ->
            s.getTestRunCaseId().equals(runCaseId) && s.getAction() != null
        ));
    }

    @Test
    @DisplayName("Rolls back case and steps when step creation fails")
    void rollsBackOnStepFailure() {
        TestRunCaseDTO caseInput = new TestRunCaseDTO();
        caseInput.setTestRunId(testRunId);
        caseInput.setTestCaseId(testCaseId);
        caseInput.setProjectId(projectId);

        UUID runStepId1 = UUID.randomUUID();
        TestStepDTO step1 = new TestStepDTO();
        step1.setId(UUID.randomUUID());
        step1.setStepNumber(1);
        step1.setAction("Step 1");
        step1.setExpectedResult("Result 1");

        TestStepDTO step2 = new TestStepDTO();
        step2.setId(UUID.randomUUID());
        step2.setStepNumber(2);
        step2.setAction("Step 2");
        step2.setExpectedResult("Result 2");

        when(entityService.create(any(TestRunCaseDTO.class))).thenReturn(wrap(caseInput, runCaseId));

        TestRunStepDTO createdStep1 = new TestRunStepDTO();
        createdStep1.setId(runStepId1);
        when(testRunStepService.createTestRunStep(any()))
            .thenReturn(createdStep1)
            .thenThrow(new RuntimeException("Cyoda error"));

        assertThrows(RuntimeException.class, () ->
            service.createTestRunCaseWithStepSnapshots(caseInput, List.of(step1, step2))
        );

        verify(entityService).deleteById(runStepId1);
        verify(entityService).deleteById(runCaseId);
    }
}

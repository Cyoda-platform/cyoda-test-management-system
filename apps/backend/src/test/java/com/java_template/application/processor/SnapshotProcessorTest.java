package com.java_template.application.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java_template.application.dto.Priority;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.application.service.TestCaseService;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.application.service.TestStepService;
import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.DataPayload;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnapshotProcessor Tests")
class SnapshotProcessorTest {

    @Mock private TestCaseService testCaseService;
    @Mock private TestStepService testStepService;
    @Mock private TestRunCaseService testRunCaseService;

    private SnapshotProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new SnapshotProcessor(testCaseService, testStepService, testRunCaseService, objectMapper);
    }

    // ---- supports() --------------------------------------------------------

    @Test
    @DisplayName("supports SnapshotProcessor operation")
    void supportsCorrectOperation() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "SnapshotProcessor", "initial", "initialize_run", "TestRunWorkflow");
        assertTrue(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support other operations")
    void doesNotSupportOtherOperation() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "OtherProcessor", "initial", "initialize_run", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    // ---- process() — happy path --------------------------------------------

    @Test
    @DisplayName("creates TestRunCase with snapshot fields for each caseId")
    void createsSnapshotForEachCase() throws Exception {
        UUID testRunId  = UUID.randomUUID();
        UUID projectId  = UUID.randomUUID();
        UUID caseId     = UUID.randomUUID();
        UUID suiteId    = UUID.randomUUID();

        // Build TestRun payload
        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", projectId.toString());
        runJson.putArray("caseIds").add(caseId.toString());

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        // TestCase returned by service
        TestCaseDTO tc = new TestCaseDTO();
        tc.setId(caseId);
        tc.setTitle("Login flow");
        tc.setDescription("Tests login");
        tc.setPreconditions("User exists");
        tc.setPriority(Priority.HIGH);
        tc.setDisplayId("TC-001");
        tc.setSuiteId(suiteId);

        when(testCaseService.getTestCaseById(caseId)).thenReturn(Optional.of(tc));
        when(testStepService.getTestStepsByTestCaseId(caseId)).thenReturn(List.of());

        TestRunCaseDTO createdCase = new TestRunCaseDTO();
        createdCase.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCaseWithStepSnapshots(any(), any()))
                .thenReturn(createdCase);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        assertEquals(request.getId(), response.getId());

        // Verify the snapshot was built correctly
        ArgumentCaptor<TestRunCaseDTO> caseCaptor = ArgumentCaptor.forClass(TestRunCaseDTO.class);
        verify(testRunCaseService).createTestRunCaseWithStepSnapshots(caseCaptor.capture(), eq(List.of()));

        TestRunCaseDTO captured = caseCaptor.getValue();
        assertEquals(testRunId, captured.getTestRunId());
        assertEquals(caseId,    captured.getTestCaseId());
        assertEquals(projectId, captured.getProjectId());
        assertEquals("Login flow",  captured.getTitle());
        assertEquals("Tests login", captured.getDescription());
        assertEquals("User exists", captured.getPreconditions());
        assertEquals(Priority.HIGH, captured.getPriority());
        assertEquals("TC-001",      captured.getDisplayId());
        assertEquals(suiteId,       captured.getSuiteId());
    }

    @Test
    @DisplayName("passes TestStep snapshot data to createTestRunCaseWithStepSnapshots")
    void passesStepSnapshotData() throws Exception {
        UUID testRunId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID caseId    = UUID.randomUUID();
        UUID stepId    = UUID.randomUUID();

        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", projectId.toString());
        runJson.putArray("caseIds").add(caseId.toString());

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        TestCaseDTO tc = new TestCaseDTO();
        tc.setId(caseId);
        tc.setTitle("Checkout flow");

        TestStepDTO step = new TestStepDTO();
        step.setId(stepId);
        step.setStepNumber(1);
        step.setAction("Click checkout");
        step.setExpectedResult("Order confirmed");

        when(testCaseService.getTestCaseById(caseId)).thenReturn(Optional.of(tc));
        when(testStepService.getTestStepsByTestCaseId(caseId)).thenReturn(List.of(step));

        TestRunCaseDTO createdCase = new TestRunCaseDTO();
        createdCase.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCaseWithStepSnapshots(any(), any()))
                .thenReturn(createdCase);

        processor.process(context(request));

        ArgumentCaptor<List<TestStepDTO>> stepsCaptor = ArgumentCaptor.forClass(List.class);
        verify(testRunCaseService).createTestRunCaseWithStepSnapshots(any(), stepsCaptor.capture());

        List<TestStepDTO> captured = stepsCaptor.getValue();
        assertEquals(1, captured.size());
        assertEquals(stepId, captured.get(0).getId());
        assertEquals(1,                captured.get(0).getStepNumber());
        assertEquals("Click checkout", captured.get(0).getAction());
        assertEquals("Order confirmed",captured.get(0).getExpectedResult());
    }

    // ---- process() — edge cases --------------------------------------------

    @Test
    @DisplayName("returns success immediately when caseIds is null")
    void successWhenNoCaseIds() throws Exception {
        UUID testRunId = UUID.randomUUID();
        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", UUID.randomUUID().toString());
        // no caseIds field

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        verifyNoInteractions(testCaseService, testStepService, testRunCaseService);
    }

    @Test
    @DisplayName("returns success immediately when caseIds is empty")
    void successWhenEmptyCaseIds() throws Exception {
        UUID testRunId = UUID.randomUUID();
        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", UUID.randomUUID().toString());
        runJson.putArray("caseIds"); // empty array

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        verifyNoInteractions(testCaseService, testStepService, testRunCaseService);
    }

    @Test
    @DisplayName("skips missing TestCase and continues with remaining cases")
    void skipsMissingCase() throws Exception {
        UUID testRunId  = UUID.randomUUID();
        UUID projectId  = UUID.randomUUID();
        UUID missingId  = UUID.randomUUID();
        UUID presentId  = UUID.randomUUID();

        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", projectId.toString());
        runJson.putArray("caseIds")
               .add(missingId.toString())
               .add(presentId.toString());

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        when(testCaseService.getTestCaseById(missingId)).thenReturn(Optional.empty());

        TestCaseDTO present = new TestCaseDTO();
        present.setId(presentId);
        present.setTitle("Present case");
        when(testCaseService.getTestCaseById(presentId)).thenReturn(Optional.of(present));
        when(testStepService.getTestStepsByTestCaseId(presentId)).thenReturn(List.of());

        TestRunCaseDTO created = new TestRunCaseDTO();
        created.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCaseWithStepSnapshots(any(), any()))
                .thenReturn(created);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        // Only the present case is snapshotted
        verify(testRunCaseService, times(1))
                .createTestRunCaseWithStepSnapshots(any(), any());
    }

    // ---- helpers -----------------------------------------------------------

    private EntityProcessorCalculationRequest buildRequest(UUID entityId, ObjectNode data) {
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("id", entityId.toString());
        meta.put("state", "initial");

        DataPayload payload = new DataPayload();
        payload.setData(data);
        payload.setMeta(meta);

        EntityProcessorCalculationRequest request = new EntityProcessorCalculationRequest();
        request.setId("req-" + UUID.randomUUID());
        request.setEntityId(entityId);
        request.setPayload(payload);
        return request;
    }

    private CyodaEventContext<EntityProcessorCalculationRequest> context(
            EntityProcessorCalculationRequest request) {
        return new CyodaEventContext<>() {
            @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
            @Override public EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}

package com.java_template.application.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java_template.application.dto.Priority;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.service.TestCaseService;
import com.java_template.application.service.TestRunCaseService;
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
    @Mock private TestRunCaseService testRunCaseService;

    private SnapshotProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new SnapshotProcessor(testCaseService, testRunCaseService, objectMapper);
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

        // TestCase returned by service — no steps
        TestCaseDTO tc = new TestCaseDTO();
        tc.setId(caseId);
        tc.setTitle("Login flow");
        tc.setDescription("Tests login");
        tc.setPreconditions("User exists");
        tc.setPriority(Priority.HIGH);
        tc.setDisplayId("TC-001");
        tc.setSuiteId(suiteId);
        // steps is empty by default

        when(testCaseService.getTestCaseById(caseId)).thenReturn(Optional.of(tc));

        TestRunCaseDTO createdCase = new TestRunCaseDTO();
        createdCase.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCase(any()))
                .thenReturn(createdCase);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        assertEquals(request.getId(), response.getId());

        // Verify the snapshot was built correctly
        ArgumentCaptor<TestRunCaseDTO> caseCaptor = ArgumentCaptor.forClass(TestRunCaseDTO.class);
        verify(testRunCaseService).createTestRunCase(caseCaptor.capture());

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
    @DisplayName("does NOT embed steps in TestRunCase snapshot (prevents Cyoda schema rejection)")
    void doesNotEmbedStepsInSnapshot() throws Exception {
        UUID testRunId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID caseId    = UUID.randomUUID();

        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", projectId.toString());
        runJson.putArray("caseIds").add(caseId.toString());

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        TestCaseDTO.StepDTO embeddedStep = new TestCaseDTO.StepDTO(1, "Click checkout", "Order confirmed");

        TestCaseDTO tc = new TestCaseDTO();
        tc.setId(caseId);
        tc.setTitle("Checkout flow");
        tc.setSteps(List.of(embeddedStep));

        when(testCaseService.getTestCaseById(caseId)).thenReturn(Optional.of(tc));

        TestRunCaseDTO createdCase = new TestRunCaseDTO();
        createdCase.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCase(any()))
                .thenReturn(createdCase);

        processor.process(context(request));

        ArgumentCaptor<TestRunCaseDTO> caseCaptor = ArgumentCaptor.forClass(TestRunCaseDTO.class);
        verify(testRunCaseService).createTestRunCase(caseCaptor.capture());

        // Steps must NOT be embedded — Cyoda schema for TestRunCase doesn't include the steps
        // field yet, and including it causes entity rejection which breaks Suite Analysis.
        assertNull(caseCaptor.getValue().getSteps());
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
        verifyNoInteractions(testCaseService, testRunCaseService);
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
        verifyNoInteractions(testCaseService, testRunCaseService);
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
        // steps is empty by default
        when(testCaseService.getTestCaseById(presentId)).thenReturn(Optional.of(present));

        TestRunCaseDTO created = new TestRunCaseDTO();
        created.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCase(any()))
                .thenReturn(created);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        // Only the present case is snapshotted
        verify(testRunCaseService, times(1))
                .createTestRunCase(any());
    }

    @Test
    @DisplayName("continues snapshotting remaining cases when one case create throws")
    void continuesAfterCreateFailure() throws Exception {
        UUID testRunId  = UUID.randomUUID();
        UUID projectId  = UUID.randomUUID();
        UUID failingId  = UUID.randomUUID();
        UUID successId  = UUID.randomUUID();

        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", projectId.toString());
        runJson.putArray("caseIds")
               .add(failingId.toString())
               .add(successId.toString());

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        TestCaseDTO failing = new TestCaseDTO();
        failing.setId(failingId);
        failing.setTitle("Failing case");
        when(testCaseService.getTestCaseById(failingId)).thenReturn(Optional.of(failing));

        TestCaseDTO success = new TestCaseDTO();
        success.setId(successId);
        success.setTitle("Success case");
        when(testCaseService.getTestCaseById(successId)).thenReturn(Optional.of(success));

        TestRunCaseDTO created = new TestRunCaseDTO();
        created.setId(UUID.randomUUID());

        // First create throws (simulates Cyoda schema rejection for steps field),
        // second create succeeds.
        when(testRunCaseService.createTestRunCase(any()))
                .thenThrow(new RuntimeException("Cyoda schema rejected steps field"))
                .thenReturn(created);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        // Both cases were attempted — second one succeeded
        verify(testRunCaseService, times(2)).createTestRunCase(any());
    }

    // ---- caseIds string format (new Cyoda storage format) -----------------

    @Test
    @DisplayName("creates snapshot when caseIds stored as JSON string (new format)")
    void createsSnapshotFromStringFormatCaseIds() throws Exception {
        UUID testRunId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID caseId    = UUID.randomUUID();

        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", projectId.toString());
        // New format: stored as a quoted JSON string, not an array node
        runJson.put("caseIds", "[\"" + caseId + "\"]");

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        TestCaseDTO tc = new TestCaseDTO();
        tc.setId(caseId);
        tc.setTitle("String-format case");
        when(testCaseService.getTestCaseById(caseId)).thenReturn(Optional.of(tc));

        TestRunCaseDTO created = new TestRunCaseDTO();
        created.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCase(any())).thenReturn(created);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        verify(testRunCaseService, times(1)).createTestRunCase(any());
    }

    @Test
    @DisplayName("creates snapshots for multiple caseIds in JSON string format")
    void createsSnapshotsForMultipleCasesInStringFormat() throws Exception {
        UUID testRunId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID caseId1   = UUID.randomUUID();
        UUID caseId2   = UUID.randomUUID();

        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", projectId.toString());
        runJson.put("caseIds", "[\"" + caseId1 + "\",\"" + caseId2 + "\"]");

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        TestCaseDTO tc1 = new TestCaseDTO();
        tc1.setId(caseId1);
        tc1.setTitle("Case One");
        TestCaseDTO tc2 = new TestCaseDTO();
        tc2.setId(caseId2);
        tc2.setTitle("Case Two");
        when(testCaseService.getTestCaseById(caseId1)).thenReturn(Optional.of(tc1));
        when(testCaseService.getTestCaseById(caseId2)).thenReturn(Optional.of(tc2));

        TestRunCaseDTO created = new TestRunCaseDTO();
        created.setId(UUID.randomUUID());
        when(testRunCaseService.createTestRunCase(any())).thenReturn(created);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        verify(testRunCaseService, times(2)).createTestRunCase(any());
    }

    @Test
    @DisplayName("returns success immediately when caseIds is empty JSON string array")
    void successWhenCaseIdsIsEmptyJsonString() throws Exception {
        UUID testRunId = UUID.randomUUID();
        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", UUID.randomUUID().toString());
        runJson.put("caseIds", "[]");

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        verifyNoInteractions(testCaseService, testRunCaseService);
    }

    @Test
    @DisplayName("returns success (no crash) when caseIds string is malformed JSON")
    void successWhenCaseIdsStringIsMalformed() throws Exception {
        UUID testRunId = UUID.randomUUID();
        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", UUID.randomUUID().toString());
        runJson.put("caseIds", "not-valid-json[broken");

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        verifyNoInteractions(testCaseService, testRunCaseService);
    }

    @Test
    @DisplayName("returns success immediately when caseIds is blank string")
    void successWhenCaseIdsIsBlankString() throws Exception {
        UUID testRunId = UUID.randomUUID();
        ObjectNode runJson = objectMapper.createObjectNode();
        runJson.put("projectId", UUID.randomUUID().toString());
        runJson.put("caseIds", "   ");

        EntityProcessorCalculationRequest request = buildRequest(testRunId, runJson);

        EntityProcessorCalculationResponse response = processor.process(context(request));

        assertTrue(response.getSuccess());
        verifyNoInteractions(testCaseService, testRunCaseService);
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

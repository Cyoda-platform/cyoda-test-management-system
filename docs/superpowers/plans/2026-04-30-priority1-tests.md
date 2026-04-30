# Priority 1 Test Coverage — Processors, Controllers, E2E

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 74 tests across 10 new files: contract tests for 5 FSM processors, MockMvc unit tests for 3 TestRun-domain controllers, and 15 Gherkin E2E scenarios for TestRun lifecycle.

**Architecture:** Processor tests use plain `new Processor()` (no Spring context, no mocks). Controller tests use `MockMvcBuilders.standaloneSetup()` with Mockito-mocked services. E2E step definitions reuse `TmsHttpClient` and existing shared steps (`I am logged in as admin`, `the response HTTP status is {int}`).

**Tech Stack:** Java 21, JUnit 5, Mockito, Spring MockMvc, Cucumber 7, Bean Validation (Jakarta), `org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest/Response`, `com.java_template.common.workflow.OperationSpecification`

---

## File Map

| File to create | Responsibility |
|---|---|
| `src/test/java/com/java_template/application/processor/AtomicFailureProcessorTest.java` | Contract tests for AtomicFailureProcessor |
| `src/test/java/com/java_template/application/processor/BugLinkProcessorTest.java` | Contract tests for BugLinkProcessor |
| `src/test/java/com/java_template/application/processor/EdgeMessageProcessorTest.java` | Contract tests for EdgeMessageProcessor |
| `src/test/java/com/java_template/application/processor/MetricsAggregatorProcessorTest.java` | Contract tests for MetricsAggregatorProcessor |
| `src/test/java/com/java_template/application/processor/TestRunCompleteProcessorTest.java` | Contract tests for TestRunCompleteProcessor |
| `src/test/java/com/java_template/application/controller/TestRunControllerTest.java` | MockMvc tests for TestRunController (17 tests) |
| `src/test/java/com/java_template/application/controller/TestRunCaseControllerTest.java` | MockMvc tests for TestRunCaseController (9 tests) |
| `src/test/java/com/java_template/application/controller/TestRunStepControllerTest.java` | MockMvc tests for TestRunStepController (10 tests) |
| `src/test/resources/features/test-run-crud.feature` | 15 Gherkin scenarios for TestRun lifecycle |
| `src/test/java/e2e/steps/TestRunApiSteps.java` | Cucumber step definitions for test-run-crud.feature |

All paths are relative to `apps/backend/`.

---

## Task 1: Processor Contract Tests (5 files)

**Files to create:**
- `apps/backend/src/test/java/com/java_template/application/processor/AtomicFailureProcessorTest.java`
- `apps/backend/src/test/java/com/java_template/application/processor/BugLinkProcessorTest.java`
- `apps/backend/src/test/java/com/java_template/application/processor/EdgeMessageProcessorTest.java`
- `apps/backend/src/test/java/com/java_template/application/processor/MetricsAggregatorProcessorTest.java`
- `apps/backend/src/test/java/com/java_template/application/processor/TestRunCompleteProcessorTest.java`

**Background:** All 5 processors are stub implementations that return `success=true` with no dependencies. The only logic worth testing is `supports()` (name matching) and `process()` (response structure). Pattern is from `SnapshotProcessorTest.java` and `ExampleEntityProcessorTest.java`.

- [ ] **Step 1: Create AtomicFailureProcessorTest.java**

```java
package com.java_template.application.processor;

import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("AtomicFailureProcessor Tests")
class AtomicFailureProcessorTest {

    private AtomicFailureProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AtomicFailureProcessor();
    }

    @Test
    @DisplayName("supports correct operation name")
    void supports_correctName_returnsTrue() {
        ModelSpec spec = new ModelSpec().withName("TestRunStep").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "AtomicFailureProcessor", "ACTIVE", "fail", "TestRunWorkflow");
        assertTrue(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support a different operation name")
    void supports_wrongName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRunStep").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "OtherProcessor", "ACTIVE", "fail", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support empty operation name")
    void supports_emptyName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRunStep").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "", "ACTIVE", "fail", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("process returns success=true")
    void process_validRequest_returnsSuccess() {
        EntityProcessorCalculationRequest request = createRequest();
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertNotNull(response);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("process echoes the request id back in the response")
    void process_validRequest_preservesRequestId() {
        EntityProcessorCalculationRequest request = createRequest();
        request.setId("atomic-test-id");
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertEquals("atomic-test-id", response.getId());
    }

    private EntityProcessorCalculationRequest createRequest() {
        EntityProcessorCalculationRequest request = new EntityProcessorCalculationRequest();
        request.setId("req-001");
        request.setRequestId("r-456");
        request.setEntityId(UUID.randomUUID());
        return request;
    }

    private CyodaEventContext<EntityProcessorCalculationRequest> createContext(
            EntityProcessorCalculationRequest request) {
        return new CyodaEventContext<>() {
            @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
            @Override public @NotNull EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}
```

- [ ] **Step 2: Create BugLinkProcessorTest.java**

```java
package com.java_template.application.processor;

import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("BugLinkProcessor Tests")
class BugLinkProcessorTest {

    private BugLinkProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BugLinkProcessor();
    }

    @Test
    @DisplayName("supports correct operation name")
    void supports_correctName_returnsTrue() {
        ModelSpec spec = new ModelSpec().withName("TestRunCase").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "BugLinkProcessor", "ACTIVE", "link_bug", "TestRunWorkflow");
        assertTrue(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support a different operation name")
    void supports_wrongName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRunCase").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "SnapshotProcessor", "ACTIVE", "link_bug", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support empty operation name")
    void supports_emptyName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRunCase").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "", "ACTIVE", "link_bug", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("process returns success=true")
    void process_validRequest_returnsSuccess() {
        EntityProcessorCalculationRequest request = createRequest();
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertNotNull(response);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("process echoes the request id back in the response")
    void process_validRequest_preservesRequestId() {
        EntityProcessorCalculationRequest request = createRequest();
        request.setId("buglink-test-id");
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertEquals("buglink-test-id", response.getId());
    }

    private EntityProcessorCalculationRequest createRequest() {
        EntityProcessorCalculationRequest request = new EntityProcessorCalculationRequest();
        request.setId("req-001");
        request.setRequestId("r-456");
        request.setEntityId(UUID.randomUUID());
        return request;
    }

    private CyodaEventContext<EntityProcessorCalculationRequest> createContext(
            EntityProcessorCalculationRequest request) {
        return new CyodaEventContext<>() {
            @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
            @Override public @NotNull EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}
```

- [ ] **Step 3: Create EdgeMessageProcessorTest.java**

```java
package com.java_template.application.processor;

import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("EdgeMessageProcessor Tests")
class EdgeMessageProcessorTest {

    private EdgeMessageProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EdgeMessageProcessor();
    }

    @Test
    @DisplayName("supports correct operation name")
    void supports_correctName_returnsTrue() {
        ModelSpec spec = new ModelSpec().withName("Attachment").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "EdgeMessageProcessor", "ACTIVE", "upload", "AttachmentWorkflow");
        assertTrue(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support a different operation name")
    void supports_wrongName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("Attachment").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "MetricsAggregatorProcessor", "ACTIVE", "upload", "AttachmentWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support empty operation name")
    void supports_emptyName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("Attachment").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "", "ACTIVE", "upload", "AttachmentWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("process returns success=true")
    void process_validRequest_returnsSuccess() {
        EntityProcessorCalculationRequest request = createRequest();
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertNotNull(response);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("process echoes the request id back in the response")
    void process_validRequest_preservesRequestId() {
        EntityProcessorCalculationRequest request = createRequest();
        request.setId("edge-test-id");
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertEquals("edge-test-id", response.getId());
    }

    private EntityProcessorCalculationRequest createRequest() {
        EntityProcessorCalculationRequest request = new EntityProcessorCalculationRequest();
        request.setId("req-001");
        request.setRequestId("r-456");
        request.setEntityId(UUID.randomUUID());
        return request;
    }

    private CyodaEventContext<EntityProcessorCalculationRequest> createContext(
            EntityProcessorCalculationRequest request) {
        return new CyodaEventContext<>() {
            @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
            @Override public @NotNull EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}
```

- [ ] **Step 4: Create MetricsAggregatorProcessorTest.java**

```java
package com.java_template.application.processor;

import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("MetricsAggregatorProcessor Tests")
class MetricsAggregatorProcessorTest {

    private MetricsAggregatorProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MetricsAggregatorProcessor();
    }

    @Test
    @DisplayName("supports correct operation name")
    void supports_correctName_returnsTrue() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "MetricsAggregatorProcessor", "COMPLETED", "aggregate", "TestRunWorkflow");
        assertTrue(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support a different operation name")
    void supports_wrongName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "TestRunCompleteProcessor", "COMPLETED", "aggregate", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support empty operation name")
    void supports_emptyName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "", "COMPLETED", "aggregate", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("process returns success=true")
    void process_validRequest_returnsSuccess() {
        EntityProcessorCalculationRequest request = createRequest();
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertNotNull(response);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("process echoes the request id back in the response")
    void process_validRequest_preservesRequestId() {
        EntityProcessorCalculationRequest request = createRequest();
        request.setId("metrics-test-id");
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertEquals("metrics-test-id", response.getId());
    }

    private EntityProcessorCalculationRequest createRequest() {
        EntityProcessorCalculationRequest request = new EntityProcessorCalculationRequest();
        request.setId("req-001");
        request.setRequestId("r-456");
        request.setEntityId(UUID.randomUUID());
        return request;
    }

    private CyodaEventContext<EntityProcessorCalculationRequest> createContext(
            EntityProcessorCalculationRequest request) {
        return new CyodaEventContext<>() {
            @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
            @Override public @NotNull EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}
```

- [ ] **Step 5: Create TestRunCompleteProcessorTest.java**

```java
package com.java_template.application.processor;

import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("TestRunCompleteProcessor Tests")
class TestRunCompleteProcessorTest {

    private TestRunCompleteProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TestRunCompleteProcessor();
    }

    @Test
    @DisplayName("supports correct operation name")
    void supports_correctName_returnsTrue() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "TestRunCompleteProcessor", "ACTIVE", "complete", "TestRunWorkflow");
        assertTrue(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support a different operation name")
    void supports_wrongName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "SnapshotProcessor", "ACTIVE", "complete", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support empty operation name")
    void supports_emptyName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRun").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "", "ACTIVE", "complete", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("process returns success=true")
    void process_validRequest_returnsSuccess() {
        EntityProcessorCalculationRequest request = createRequest();
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertNotNull(response);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("process echoes the request id back in the response")
    void process_validRequest_preservesRequestId() {
        EntityProcessorCalculationRequest request = createRequest();
        request.setId("complete-test-id");
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertEquals("complete-test-id", response.getId());
    }

    private EntityProcessorCalculationRequest createRequest() {
        EntityProcessorCalculationRequest request = new EntityProcessorCalculationRequest();
        request.setId("req-001");
        request.setRequestId("r-456");
        request.setEntityId(UUID.randomUUID());
        return request;
    }

    private CyodaEventContext<EntityProcessorCalculationRequest> createContext(
            EntityProcessorCalculationRequest request) {
        return new CyodaEventContext<>() {
            @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
            @Override public @NotNull EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}
```

- [ ] **Step 6: Run processor tests**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
./gradlew :apps:backend:test --tests "com.java_template.application.processor.*ProcessorTest"
```

Expected: 25 tests PASS (5 per class × 5 classes).

- [ ] **Step 7: Commit processor tests**

```bash
git add apps/backend/src/test/java/com/java_template/application/processor/
git commit -m "test: add contract tests for 5 FSM processors"
```

---

## Task 2: TestRunControllerTest

**File:** `apps/backend/src/test/java/com/java_template/application/controller/TestRunControllerTest.java`

**Services mocked:** `TestRunService`, `TestRunCaseService`

**Key detail:** `unlockTestRun` reads `role` from `HttpServletRequest.getAttribute("role")` — set via `.requestAttr("role", "Admin")` in MockMvc perform chain.

- [ ] **Step 1: Create TestRunControllerTest.java**

```java
package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.application.dto.TestRunDetailDTO;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.application.service.TestRunService;
import com.java_template.common.dto.PageResult;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TestRunController Tests")
class TestRunControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AutoCloseable autoCloseable;

    @Mock private TestRunService testRunService;
    @Mock private TestRunCaseService testRunCaseService;

    private UUID projectId;
    private UUID runId;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        projectId = UUID.randomUUID();
        runId = UUID.randomUUID();
        TestRunController controller = new TestRunController(testRunService, testRunCaseService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(new SpringValidatorAdapter(
                        Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    // ── CREATE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST valid run returns 201")
    void createTestRun_validInput_returns201() throws Exception {
        TestRunDTO dto = buildRun("My Run");
        when(testRunService.createTestRun(any(TestRunDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/projects/{pid}/runs", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Run"));
    }

    @Test
    @DisplayName("POST run with blank name returns 400")
    void createTestRun_blankName_returns400() throws Exception {
        TestRunDTO dto = buildRun("");   // @NotBlank violation

        mockMvc.perform(post("/projects/{pid}/runs", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(testRunService, never()).createTestRun(any());
    }

    // ── GET LIST ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET runs returns 200 with page")
    void getTestRunsByProject_returns200WithPage() throws Exception {
        PageResult<TestRunDTO> page = PageResult.of(null, List.of(buildRun("R1")), 0, 20, 1);
        when(testRunService.getTestRunsByProjectId(eq(projectId), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/projects/{pid}/runs", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── GET BY ID ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} — found, same project — returns 200")
    void getTestRun_found_sameProject_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("R1"));
    }

    @Test
    @DisplayName("GET /{id} — project ID mismatch — returns 404")
    void getTestRun_projectMismatch_returns404() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(UUID.randomUUID()); // different project
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{id} — not found — returns 404")
    void getTestRun_notFound_returns404() throws Exception {
        when(testRunService.getTestRunById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNotFound());
    }

    // ── GET DETAILS ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id}/details — found — returns 200 with run and cases")
    void getTestRunDetails_found_returns200WithCases() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        PageResult<TestRunCaseDTO> cases = PageResult.of(null, List.of(new TestRunCaseDTO()), 0, 500, 1);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunCaseService.getTestRunCasesByTestRunId(eq(runId), eq(0), eq(500))).thenReturn(cases);

        mockMvc.perform(get("/projects/{pid}/runs/{id}/details", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.name").value("R1"))
                .andExpect(jsonPath("$.runCases").isArray());
    }

    @Test
    @DisplayName("GET /{id}/details — not found — returns 404")
    void getTestRunDetails_notFound_returns404() throws Exception {
        when(testRunService.getTestRunById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{pid}/runs/{id}/details", projectId, runId))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /{id} — run exists — returns 200")
    void updateTestRun_exists_returns200() throws Exception {
        TestRunDTO dto = buildRun("Updated Run");
        when(testRunService.testRunExists(eq(runId))).thenReturn(true);
        when(testRunService.updateTestRun(eq(runId), any(TestRunDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/projects/{pid}/runs/{id}", projectId, runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Run"));
    }

    @Test
    @DisplayName("PUT /{id} — run not found — returns 404")
    void updateTestRun_notFound_returns404() throws Exception {
        when(testRunService.testRunExists(eq(runId))).thenReturn(false);

        mockMvc.perform(put("/projects/{pid}/runs/{id}", projectId, runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRun("X"))))
                .andExpect(status().isNotFound());
    }

    // ── COMPLETE ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /{id}/complete — found — returns 200")
    void completeTestRun_found_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        dto.setStatus("COMPLETED");
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunService.completeTestRun(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{id}/complete", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /{id}/complete — not found — returns 404")
    void completeTestRun_notFound_returns404() throws Exception {
        when(testRunService.getTestRunById(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/projects/{pid}/runs/{id}/complete", projectId, runId))
                .andExpect(status().isNotFound());
    }

    // ── UNLOCK ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /{id}/unlock — Admin role — returns 200")
    void unlockTestRun_adminRole_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunService.unlockTestRun(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{id}/unlock", projectId, runId)
                        .requestAttr("role", "Admin"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /{id}/unlock — Tester role — returns 200")
    void unlockTestRun_testerRole_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunService.unlockTestRun(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{id}/unlock", projectId, runId)
                        .requestAttr("role", "Tester"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /{id}/unlock — no role — returns 403")
    void unlockTestRun_noRole_returns403() throws Exception {
        mockMvc.perform(post("/projects/{pid}/runs/{id}/unlock", projectId, runId))
                .andExpect(status().isForbidden());

        verify(testRunService, never()).unlockTestRun(any());
    }

    // ── DELETE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /{id} — exists — returns 204")
    void deleteTestRun_exists_returns204() throws Exception {
        when(testRunService.deleteTestRun(eq(runId))).thenReturn(true);

        mockMvc.perform(delete("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /{id} — not found — returns 404")
    void deleteTestRun_notFound_returns404() throws Exception {
        when(testRunService.deleteTestRun(eq(runId))).thenReturn(false);

        mockMvc.perform(delete("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private TestRunDTO buildRun(String name) {
        TestRunDTO dto = new TestRunDTO();
        dto.setId(runId);
        dto.setProjectId(projectId);
        dto.setName(name);
        dto.setStatus("ACTIVE");
        return dto;
    }
}
```

- [ ] **Step 2: Run TestRunControllerTest**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestRunControllerTest"
```

Expected: 17 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add apps/backend/src/test/java/com/java_template/application/controller/TestRunControllerTest.java
git commit -m "test: add MockMvc tests for TestRunController"
```

---

## Task 3: TestRunCaseControllerTest

**File:** `apps/backend/src/test/java/com/java_template/application/controller/TestRunCaseControllerTest.java`

**Service mocked:** `TestRunCaseService`

- [ ] **Step 1: Create TestRunCaseControllerTest.java**

```java
package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.common.dto.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TestRunCaseController Tests")
class TestRunCaseControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AutoCloseable autoCloseable;

    @Mock private TestRunCaseService testRunCaseService;

    private UUID projectId;
    private UUID runId;
    private UUID caseId;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        projectId = UUID.randomUUID();
        runId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TestRunCaseController(testRunCaseService)).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    // ── CREATE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST creates case and returns 201")
    void createTestRunCase_returns201() throws Exception {
        TestRunCaseDTO dto = buildCase();
        when(testRunCaseService.createTestRunCase(any(TestRunCaseDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/projects/{pid}/runs/{rid}/cases", projectId, runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // ── GET LIST ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET cases returns 200 with page")
    void getTestRunCasesByRun_returns200() throws Exception {
        PageResult<TestRunCaseDTO> page = PageResult.of(null, List.of(buildCase()), 0, 20, 1);
        when(testRunCaseService.getTestRunCasesByTestRunId(eq(runId), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── GET BY ID ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} — found, same run — returns 200")
    void getTestRunCase_found_sameRun_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setTestRunId(runId);
        when(testRunCaseService.getTestRunCaseById(eq(caseId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} — runId mismatch — returns 404")
    void getTestRunCase_runMismatch_returns404() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setTestRunId(UUID.randomUUID()); // different run
        when(testRunCaseService.getTestRunCaseById(eq(caseId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE (body) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /{id} — found — updates status from body and returns 200")
    void updateTestRunCase_found_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setStatus("PASSED");
        when(testRunCaseService.updateTestRunCaseStatus(eq(caseId), eq("PASSED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id} — not found — returns 404")
    void updateTestRunCase_notFound_returns404() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setStatus("PASSED");
        when(testRunCaseService.updateTestRunCaseStatus(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE STATUS (param) ────────────────────────────────────────────

    @Test
    @DisplayName("PUT /{id}/status — found — returns 200")
    void updateTestRunCaseStatus_found_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setStatus("FAILED");
        when(testRunCaseService.updateTestRunCaseStatus(eq(caseId), eq("FAILED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}/status",
                        projectId, runId, caseId)
                        .param("status", "FAILED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id}/status — not found — returns 404")
    void updateTestRunCaseStatus_notFound_returns404() throws Exception {
        when(testRunCaseService.updateTestRunCaseStatus(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}/status",
                        projectId, runId, caseId)
                        .param("status", "FAILED"))
                .andExpect(status().isNotFound());
    }

    // ── LINK BUG ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /{id}/link-bug — found — returns 200")
    void linkBug_found_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setBugUrl("https://jira.example.com/BUG-123");
        when(testRunCaseService.linkBug(eq(caseId), eq("https://jira.example.com/BUG-123")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{rid}/cases/{id}/link-bug",
                        projectId, runId, caseId)
                        .param("bugUrl", "https://jira.example.com/BUG-123"))
                .andExpect(status().isOk());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private TestRunCaseDTO buildCase() {
        TestRunCaseDTO dto = new TestRunCaseDTO();
        dto.setId(caseId);
        dto.setTestRunId(runId);
        dto.setProjectId(projectId);
        dto.setStatus("UNTESTED");
        dto.setTitle("Sample Case");
        return dto;
    }
}
```

- [ ] **Step 2: Run TestRunCaseControllerTest**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestRunCaseControllerTest"
```

Expected: 9 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add apps/backend/src/test/java/com/java_template/application/controller/TestRunCaseControllerTest.java
git commit -m "test: add MockMvc tests for TestRunCaseController"
```

---

## Task 4: TestRunStepControllerTest

**File:** `apps/backend/src/test/java/com/java_template/application/controller/TestRunStepControllerTest.java`

**Services mocked:** `TestRunStepService`, `AttachmentService`

**Key detail:** `uploadEvidence` is a `multipart/form-data` endpoint — use `MockMultipartFile` and `mockMvc.perform(multipart(...))`.

- [ ] **Step 1: Create TestRunStepControllerTest.java**

```java
package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.AttachmentDTO;
import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.application.service.AttachmentService;
import com.java_template.application.service.TestRunStepService;
import com.java_template.common.dto.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TestRunStepController Tests")
class TestRunStepControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AutoCloseable autoCloseable;

    @Mock private TestRunStepService testRunStepService;
    @Mock private AttachmentService attachmentService;

    private UUID projectId;
    private UUID runId;
    private UUID caseId;
    private UUID stepId;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        projectId = UUID.randomUUID();
        runId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        stepId = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TestRunStepController(testRunStepService, attachmentService)).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    // ── CREATE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST creates step and returns 201")
    void createTestRunStep_returns201() throws Exception {
        TestRunStepDTO dto = buildStep();
        when(testRunStepService.createTestRunStep(any(TestRunStepDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/projects/{pid}/runs/{rid}/cases/{cid}/steps",
                        projectId, runId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // ── GET LIST ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET steps returns 200 with page")
    void getTestRunStepsByCase_returns200() throws Exception {
        PageResult<TestRunStepDTO> page = PageResult.of(null, List.of(buildStep()), 0, 20, 1);
        when(testRunStepService.getTestRunStepsByTestRunCaseId(eq(caseId), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{cid}/steps",
                        projectId, runId, caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── GET BY ID ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} — found, same case — returns 200")
    void getTestRunStep_found_sameCase_returns200() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setTestRunCaseId(caseId);
        when(testRunStepService.getTestRunStepById(eq(stepId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} — caseId mismatch — returns 404")
    void getTestRunStep_caseMismatch_returns404() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setTestRunCaseId(UUID.randomUUID()); // different case
        when(testRunStepService.getTestRunStepById(eq(stepId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE (body) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /{id} — found — updates from body and returns 200")
    void updateTestRunStep_found_returns200() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setStatus("PASSED");
        when(testRunStepService.updateTestRunStepStatus(eq(stepId), eq("PASSED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id} — not found — returns 404")
    void updateTestRunStep_notFound_returns404() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setStatus("PASSED");
        when(testRunStepService.updateTestRunStepStatus(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE STATUS (param) ────────────────────────────────────────────

    @Test
    @DisplayName("PUT /{id}/status — found — returns 200")
    void updateTestRunStepStatus_found_returns200() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setStatus("FAILED");
        when(testRunStepService.updateTestRunStepStatus(eq(stepId), eq("FAILED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}/status",
                        projectId, runId, caseId, stepId)
                        .param("status", "FAILED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id}/status — not found — returns 404")
    void updateTestRunStepStatus_notFound_returns404() throws Exception {
        when(testRunStepService.updateTestRunStepStatus(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}/status",
                        projectId, runId, caseId, stepId)
                        .param("status", "FAILED"))
                .andExpect(status().isNotFound());
    }

    // ── UPLOAD EVIDENCE (multipart) ──────────────────────────────────────

    @Test
    @DisplayName("POST /{id}/evidence — valid file — returns 201")
    void uploadEvidence_validFile_returns201() throws Exception {
        AttachmentDTO attachment = new AttachmentDTO();
        attachment.setId(UUID.randomUUID());
        attachment.setFileName("screenshot.png");
        when(attachmentService.uploadAttachment(
                eq(projectId), eq(caseId), isNull(), any(), eq("EVIDENCE"), eq(runId), isNull()))
                .thenReturn(attachment);

        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}/evidence",
                        projectId, runId, caseId, stepId)
                        .file(file))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /{id}/evidence — service throws — returns 500")
    void uploadEvidence_serviceThrows_returns500() throws Exception {
        when(attachmentService.uploadAttachment(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("storage error"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "log.txt", "text/plain", "log content".getBytes());

        mockMvc.perform(multipart("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}/evidence",
                        projectId, runId, caseId, stepId)
                        .file(file))
                .andExpect(status().isInternalServerError());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private TestRunStepDTO buildStep() {
        TestRunStepDTO dto = new TestRunStepDTO();
        dto.setId(stepId);
        dto.setTestRunCaseId(caseId);
        dto.setStepNumber(1);
        dto.setAction("Click button");
        dto.setExpectedResult("Button is clicked");
        dto.setStatus("UNTESTED");
        return dto;
    }
}
```

- [ ] **Step 2: Run TestRunStepControllerTest**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestRunStepControllerTest"
```

Expected: 10 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add apps/backend/src/test/java/com/java_template/application/controller/TestRunStepControllerTest.java
git commit -m "test: add MockMvc tests for TestRunStepController"
```

---

## Task 5: test-run-crud.feature

**File:** `apps/backend/src/test/resources/features/test-run-crud.feature`

**Reused steps (already defined, do NOT redefine):**
- `Given I am not authenticated` — `ProjectApiSteps`
- `Given I am logged in as admin` — `ProjectApiSteps`
- `When I POST to {string} with body:` — `ProjectApiSteps`
- `When I GET {string}` — `ProjectApiSteps`
- `Then the response HTTP status is {int}` — `HealthCheckSteps`
- `Then the paged response body contains the {string} array field` — `ProjectApiSteps`
- `Then the paged response body contains the {string} field` — `ProjectApiSteps`

**New steps** defined in `TestRunApiSteps` (Task 6):
- `When I create a test run named {string} in project {string}`
- `When I GET the last created test run by ID`
- `When I GET the last created test run details`
- `When I update the last created test run name to {string}`
- `When I complete the last created test run`
- `When I unlock the last created test run as {string}`
- `When I delete the last created test run`
- `Then the run response HTTP status is {int}`
- `Then the run response body contains field {string}`
- `Then the run response body contains field {string} with value {string}`

- [ ] **Step 1: Create test-run-crud.feature**

```gherkin
@e2e @tms
Feature: Test Run REST API — CRUD lifecycle
  As a Test Manager I need to create, execute and complete test runs
  so that I can track the execution of test cases within a project.

  # -----------------------------------------------------------------------
  # Test Run API (TestRunController):
  #   POST   /api/projects/{projectId}/runs          → 201 Created
  #   GET    /api/projects/{projectId}/runs          → 200 { data:[], totalElements, ... }
  #   GET    /api/projects/{projectId}/runs/{id}     → 200 | 404
  #   GET    /api/projects/{projectId}/runs/{id}/details → 200 { run, runCases }
  #   PUT    /api/projects/{projectId}/runs/{id}     → 200 | 404
  #   POST   /api/projects/{projectId}/runs/{id}/complete → 200 | 404
  #   POST   /api/projects/{projectId}/runs/{id}/unlock   → 200 | 403 | 404
  #   DELETE /api/projects/{projectId}/runs/{id}     → 204 | 404
  #
  # Validation rules on TestRunDTO:
  #   name  @NotBlank, @Size(max=255)
  #
  # RBAC on unlock: role attribute must be "Tester" or "Admin" (case-insensitive).
  # Role is set by AuthorizationFilter on authenticated requests.
  #
  # @smoke          — does NOT call EntityService; no Cyoda instance required.
  # @requires-cyoda — calls EntityService; needs a live Cyoda instance.
  # -----------------------------------------------------------------------

  # ── Auth / Validation (smoke) ─────────────────────────────────────────

  @smoke
  Scenario: Creating a test run without auth returns 401
    Given I am not authenticated
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/runs" with body:
      """
      {"name":"Smoke Run"}
      """
    Then the response HTTP status is 401

  @smoke
  Scenario: Creating a test run with a blank name returns 400
    Given I am logged in as admin
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/runs" with body:
      """
      {"name":""}
      """
    Then the response HTTP status is 400

  @smoke
  Scenario: Creating a test run with name longer than 255 chars returns 400
    Given I am logged in as admin
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/runs" with body:
      """
      {"name":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
      """
    Then the response HTTP status is 400

  @smoke
  Scenario: Completing a test run without auth returns 401
    Given I am not authenticated
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/runs/00000000-0000-0000-0000-000000000002/complete" with body:
      """
      {}
      """
    Then the response HTTP status is 401

  @smoke
  Scenario: Unlocking a test run without a role returns 403
    Given I am logged in as admin
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/runs/00000000-0000-0000-0000-000000000002/unlock" with body:
      """
      {}
      """
    Then the response HTTP status is 403

  # ── Full lifecycle (requires live Cyoda instance) ──────────────────────

  @requires-cyoda
  Scenario: Admin creates a test run — 201 with required fields
    Given I am logged in as admin
    And I have a test project
    When I create a test run named "E2E Run 1" in that project
    Then the run response HTTP status is 201
    And the run response body contains field "id"
    And the run response body contains field "name" with value "E2E Run 1"
    And the run response body contains field "status"

  @requires-cyoda
  Scenario: Can retrieve the created test run by ID
    Given I am logged in as admin
    And I have a test project
    And I have created a test run named "Fetch Me Run"
    When I GET the last created test run by ID
    Then the run response HTTP status is 200
    And the run response body contains field "name" with value "Fetch Me Run"

  @requires-cyoda
  Scenario: GET all test runs returns a paged response
    Given I am logged in as admin
    And I have a test project
    And I have created a test run named "Paged Run"
    When I GET all test runs for that project
    Then the response HTTP status is 200
    And the paged response body contains the "data" array field
    And the paged response body contains the "totalElements" field

  @requires-cyoda
  Scenario: GET /details returns run and runCases in one response
    Given I am logged in as admin
    And I have a test project
    And I have created a test run named "Details Run"
    When I GET the last created test run details
    Then the run response HTTP status is 200
    And the run response body contains field "run"
    And the run response body contains field "runCases"

  @requires-cyoda
  Scenario: Admin updates test run name
    Given I am logged in as admin
    And I have a test project
    And I have created a test run named "Original Run"
    When I update the last created test run name to "Renamed Run"
    Then the run response HTTP status is 200
    And the run response body contains field "name" with value "Renamed Run"

  @requires-cyoda
  Scenario: Admin completes a test run — status becomes COMPLETED
    Given I am logged in as admin
    And I have a test project
    And I have created a test run named "Run To Complete"
    When I complete the last created test run
    Then the run response HTTP status is 200
    And the run response body contains field "status" with value "COMPLETED"

  @requires-cyoda
  Scenario: Admin unlocks a completed test run
    Given I am logged in as admin
    And I have a test project
    And I have created and completed a test run named "Run To Unlock"
    When I unlock the last created test run as "Admin"
    Then the run response HTTP status is 200

  @requires-cyoda
  Scenario: Tester can also unlock a completed test run
    Given I am logged in as tester
    And I have a test project created by admin
    And I have a completed test run in that project
    When I unlock the last created test run as "Tester"
    Then the run response HTTP status is 200

  @requires-cyoda
  Scenario: Admin deletes a test run — 204
    Given I am logged in as admin
    And I have a test project
    And I have created a test run named "Run To Delete"
    When I delete the last created test run
    Then the response HTTP status is 204

  @requires-cyoda
  Scenario: GET deleted test run returns 404
    Given I am logged in as admin
    And I have a test project
    And I have created and deleted a test run named "Deleted Run"
    When I GET the last created test run by ID
    Then the response HTTP status is 404
```

- [ ] **Step 2: Verify feature file is valid Gherkin (no syntax errors)**

```bash
# Visual check: open the file and confirm all scenarios are properly formatted.
# The Cucumber runner will report parse errors at startup if Gherkin is invalid.
cat apps/backend/src/test/resources/features/test-run-crud.feature | grep -c "Scenario"
```

Expected output: `15` (15 scenarios total).

- [ ] **Step 3: Commit feature file**

```bash
git add apps/backend/src/test/resources/features/test-run-crud.feature
git commit -m "test: add E2E Gherkin feature for TestRun CRUD lifecycle"
```

---

## Task 6: TestRunApiSteps

**File:** `apps/backend/src/test/java/e2e/steps/TestRunApiSteps.java`

**Dependencies injected:** `TmsHttpClient http`, `GherkinE2eTest runner`

**Shared state within a scenario:** `lastRunResponse` (captures the run endpoint response), `lastProjectId` (UUID of the project created for that scenario), `lastRunId` (UUID extracted from the create response).

**Key behaviours:**
- `@Before` calls `http.setServerPort(runner.serverPort)` and resets state.
- `@After` deletes any project and run created during the scenario (best-effort cleanup).
- "I have a test project" creates a project via `/api/projects` and stores its ID.
- "I have created a test run named X" POSTs to `/api/projects/{projectId}/runs` and stores run ID.

- [ ] **Step 1: Create TestRunApiSteps.java**

```java
package e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.GherkinE2eTest;
import e2e.support.TmsHttpClient;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for {@code test-run-crud.feature}.
 *
 * <p>Reuses shared steps from {@link ProjectApiSteps} and {@code HealthCheckSteps}:
 * {@code Given I am logged in as admin}, {@code Then the response HTTP status is {int}}, etc.
 *
 * <p>Cleanup: project and run IDs are tracked and deleted in the {@code @After} hook.
 */
public class TestRunApiSteps {

    private static final Logger log = LoggerFactory.getLogger(TestRunApiSteps.class);
    private static final String API = "/api";

    @Autowired private TmsHttpClient http;
    @Autowired private GherkinE2eTest runner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> lastRunResponse;
    private String lastProjectId;
    private String lastRunId;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Before
    public void configureHttpClient() {
        http.setServerPort(runner.serverPort);
        http.reset();
        lastRunResponse = null;
        lastProjectId = null;
        lastRunId = null;
    }

    @After
    public void cleanupCreatedResources() {
        if (lastRunId != null && lastProjectId != null) {
            try {
                http.delete(API + "/projects/" + lastProjectId + "/runs/" + lastRunId);
            } catch (Exception e) {
                log.warn("Cleanup: failed to delete run {}: {}", lastRunId, e.getMessage());
            }
        }
        if (lastProjectId != null) {
            try {
                http.delete(API + "/projects/" + lastProjectId);
            } catch (Exception e) {
                log.warn("Cleanup: failed to delete project {}: {}", lastProjectId, e.getMessage());
            }
        }
    }

    // ── Given ─────────────────────────────────────────────────────────────

    @Given("I have a test project")
    public void i_have_a_test_project() throws Exception {
        ResponseEntity<String> resp = http.post(API + "/projects",
                "{\"name\":\"E2E TestRun Project\",\"description\":\"Created by TestRunApiSteps\"}");
        assertEquals(201, resp.getStatusCode().value(),
                "Expected 201 creating project, got: " + resp.getBody());
        JsonNode body = objectMapper.readTree(resp.getBody());
        lastProjectId = body.get("id").asText();
    }

    @Given("I have a test project created by admin")
    public void i_have_a_test_project_created_by_admin() throws Exception {
        // Login as admin to create project, then continue as current user
        String currentToken = null; // save token state if needed
        ResponseEntity<String> loginResp = http.post(API + "/auth/login",
                "{\"username\":\"admin\",\"password\":\"admin123\"}");
        JsonNode loginBody = objectMapper.readTree(loginResp.getBody());
        String adminToken = loginBody.get("token").asText();
        http.setBearerToken(adminToken);

        ResponseEntity<String> resp = http.post(API + "/projects",
                "{\"name\":\"E2E Tester Project\",\"description\":\"Created by admin for tester\"}");
        assertEquals(201, resp.getStatusCode().value());
        lastProjectId = objectMapper.readTree(resp.getBody()).get("id").asText();
    }

    @Given("I have created a test run named {string}")
    public void i_have_created_a_test_run_named(String name) throws Exception {
        assertNotNull(lastProjectId, "No project created — call 'I have a test project' first");
        String body = "{\"name\":\"" + name + "\"}";
        lastRunResponse = http.post(API + "/projects/" + lastProjectId + "/runs", body);
        assertEquals(201, lastRunResponse.getStatusCode().value(),
                "Expected 201 creating run, got: " + lastRunResponse.getBody());
        lastRunId = objectMapper.readTree(lastRunResponse.getBody()).get("id").asText();
    }

    @Given("I have created and completed a test run named {string}")
    public void i_have_created_and_completed_a_test_run_named(String name) throws Exception {
        i_have_created_a_test_run_named(name);
        http.post(API + "/projects/" + lastProjectId + "/runs/" + lastRunId + "/complete", "{}");
    }

    @Given("I have a completed test run in that project")
    public void i_have_a_completed_test_run_in_that_project() throws Exception {
        i_have_created_and_completed_a_test_run_named("E2E Completed Run");
    }

    @Given("I have created and deleted a test run named {string}")
    public void i_have_created_and_deleted_a_test_run_named(String name) throws Exception {
        i_have_created_a_test_run_named(name);
        http.delete(API + "/projects/" + lastProjectId + "/runs/" + lastRunId);
        // keep lastRunId so the next GET can use it
    }

    // ── When ──────────────────────────────────────────────────────────────

    @When("I create a test run named {string} in that project")
    public void i_create_a_test_run_named_in_that_project(String name) throws Exception {
        assertNotNull(lastProjectId, "No project created — call 'I have a test project' first");
        String body = "{\"name\":\"" + name + "\"}";
        lastRunResponse = http.post(API + "/projects/" + lastProjectId + "/runs", body);
        if (lastRunResponse.getStatusCode().value() == 201) {
            lastRunId = objectMapper.readTree(lastRunResponse.getBody()).get("id").asText();
        }
    }

    @When("I GET the last created test run by ID")
    public void i_get_the_last_created_test_run_by_id() {
        assertNotNull(lastRunId, "No run created yet in this scenario");
        lastRunResponse = http.get(API + "/projects/" + lastProjectId + "/runs/" + lastRunId);
    }

    @When("I GET all test runs for that project")
    public void i_get_all_test_runs_for_that_project() {
        assertNotNull(lastProjectId, "No project created yet in this scenario");
        http.get(API + "/projects/" + lastProjectId + "/runs");
    }

    @When("I GET the last created test run details")
    public void i_get_the_last_created_test_run_details() {
        assertNotNull(lastRunId, "No run created yet in this scenario");
        lastRunResponse = http.get(
                API + "/projects/" + lastProjectId + "/runs/" + lastRunId + "/details");
    }

    @When("I update the last created test run name to {string}")
    public void i_update_the_last_created_test_run_name_to(String newName) throws Exception {
        assertNotNull(lastRunId, "No run created yet in this scenario");
        String currentBody = lastRunResponse != null ? lastRunResponse.getBody() : "{}";
        JsonNode current = objectMapper.readTree(currentBody);
        String updated = objectMapper.createObjectNode()
                .put("name", newName)
                .put("status", current.has("status") ? current.get("status").asText() : "ACTIVE")
                .toString();
        lastRunResponse = http.put(
                API + "/projects/" + lastProjectId + "/runs/" + lastRunId, updated);
    }

    @When("I complete the last created test run")
    public void i_complete_the_last_created_test_run() {
        assertNotNull(lastRunId, "No run created yet in this scenario");
        lastRunResponse = http.post(
                API + "/projects/" + lastProjectId + "/runs/" + lastRunId + "/complete", "{}");
    }

    @When("I unlock the last created test run as {string}")
    public void i_unlock_the_last_created_test_run_as(String role) {
        assertNotNull(lastRunId, "No run created yet in this scenario");
        lastRunResponse = http.post(
                API + "/projects/" + lastProjectId + "/runs/" + lastRunId + "/unlock", "{}");
    }

    @When("I delete the last created test run")
    public void i_delete_the_last_created_test_run() {
        assertNotNull(lastRunId, "No run created yet in this scenario");
        http.delete(API + "/projects/" + lastProjectId + "/runs/" + lastRunId);
        lastRunId = null; // prevent double-delete in @After
    }

    // ── Then ──────────────────────────────────────────────────────────────

    @Then("the run response HTTP status is {int}")
    public void the_run_response_http_status_is(int expected) {
        assertNotNull(lastRunResponse, "No run API call has been made");
        assertEquals(expected, lastRunResponse.getStatusCode().value(),
                "Expected HTTP " + expected + " but got "
                        + lastRunResponse.getStatusCode().value()
                        + ". Body: " + lastRunResponse.getBody());
    }

    @Then("the run response body contains field {string}")
    public void the_run_response_body_contains_field(String field) throws Exception {
        assertNotNull(lastRunResponse, "No run API call has been made");
        JsonNode root = objectMapper.readTree(lastRunResponse.getBody());
        assertTrue(root.has(field),
                "Expected field '" + field + "' in run response: " + lastRunResponse.getBody());
    }

    @Then("the run response body contains field {string} with value {string}")
    public void the_run_response_body_contains_field_with_value(String field, String expected)
            throws Exception {
        assertNotNull(lastRunResponse, "No run API call has been made");
        JsonNode root = objectMapper.readTree(lastRunResponse.getBody());
        assertTrue(root.has(field),
                "Expected field '" + field + "' in run response: " + lastRunResponse.getBody());
        assertEquals(expected, root.get(field).asText(),
                "Field '" + field + "' value mismatch. Body: " + lastRunResponse.getBody());
    }
}
```

- [ ] **Step 2: Verify step compilation**

```bash
./gradlew :apps:backend:compileTestJava
```

Expected: BUILD SUCCESSFUL — no compilation errors.

- [ ] **Step 3: Run @smoke E2E scenarios (no Cyoda required)**

```bash
./gradlew :apps:backend:cucumberTest -Dcucumber.filter.tags="@smoke and @tms"
```

Expected: All 5 @smoke scenarios in `test-run-crud.feature` PASS. If any fails check auth filter configuration — the `unlockTestRun` 403 scenario relies on the filter rejecting requests without a valid role attribute.

- [ ] **Step 4: Commit**

```bash
git add apps/backend/src/test/java/e2e/steps/TestRunApiSteps.java
git commit -m "test: add E2E step definitions for TestRun CRUD lifecycle"
```

---

## Task 7: Full verification

- [ ] **Step 1: Run all unit tests**

```bash
./gradlew :apps:backend:test
```

Expected: BUILD SUCCESSFUL. All existing tests pass + 59 new tests pass (25 processor + 17 TestRunController + 9 TestRunCaseController + 10 TestRunStepController = 61 new, minus 2 already existed = net 59 new).

If compilation fails:
- `cannot find symbol: SpringValidatorAdapter` → add import `org.springframework.validation.beanvalidation.SpringValidatorAdapter`
- `cannot find symbol: Validation` → add import `jakarta.validation.Validation`
- `cannot find symbol: MockMultipartFile` → add import `org.springframework.mock.web.MockMultipartFile`

- [ ] **Step 2: Run all @smoke E2E scenarios**

```bash
./gradlew :apps:backend:cucumberTest -Dcucumber.filter.tags="@smoke"
```

Expected: All @smoke scenarios pass across all 3 feature files (project-crud, defect-crud, health-check, test-run-crud).

- [ ] **Step 3: Final commit updating analysis doc**

Update `docs/TEST_COVERAGE_ANALYSIS.md` — change processor coverage from 17% → 100%, controller coverage from 24% → 53% (now 9/17).

```bash
git add docs/TEST_COVERAGE_ANALYSIS.md
git commit -m "docs: update test coverage analysis after Priority 1 implementation"
```

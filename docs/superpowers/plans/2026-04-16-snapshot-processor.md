# SnapshotProcessor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement SnapshotProcessor to create immutable TestRunCase/TestRunStep snapshot entities when a test run transitions `initial → active`, and fix the double-fetch race condition in TestRunService.

**Architecture:** SnapshotProcessor is a Spring `@Component` injected with `TestCaseService`, `TestStepService`, `TestRunCaseService`, and `ObjectMapper`. On `initialize_run` it parses the TestRun payload, fetches each referenced TestCase and its TestSteps, and creates TestRunCase/TestRunStep entities with copied snapshot fields. `TestRunService.updateTestRun` is fixed to fetch the existing run once and reuse the result for both transition detection and merge logic.

**Tech Stack:** Java 21, Spring Boot 3.5, JUnit 5 + Mockito, Jackson ObjectMapper, Cyoda EntityService (gRPC)

---

## File Map

| File | Action |
|---|---|
| `apps/backend/src/main/java/com/java_template/application/dto/TestRunCaseDTO.java` | Modify — add snapshot fields |
| `apps/backend/src/main/java/com/java_template/application/dto/TestRunStepDTO.java` | Modify — add snapshot fields |
| `apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java` | Modify — add `createTestRunCaseWithStepSnapshots` |
| `apps/backend/src/main/java/com/java_template/application/processor/SnapshotProcessor.java` | Modify — full implementation |
| `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java` | Modify — fix double-fetch |
| `apps/backend/src/main/resources/entity/testruncase/version_1/testruncase.json` | Modify — add snapshot fields |
| `apps/backend/src/main/resources/entity/testrunstep/version_1/testrunstep.json` | Modify — add snapshot fields |
| `apps/backend/src/test/java/com/java_template/application/service/TestRunCaseServiceSnapshotTest.java` | Create — tests for new method |
| `apps/backend/src/test/java/com/java_template/application/processor/SnapshotProcessorTest.java` | Create — processor unit tests |
| `apps/backend/src/test/java/com/java_template/application/service/TestRunServiceRaceConditionTest.java` | Create — single-fetch regression test |

---

### Task 1: Add snapshot fields to TestRunCaseDTO and TestRunStepDTO

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/dto/TestRunCaseDTO.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/dto/TestRunStepDTO.java`

- [ ] **Step 1: Add snapshot fields to TestRunCaseDTO**

Open `apps/backend/src/main/java/com/java_template/application/dto/TestRunCaseDTO.java`.

After the line `private String bugUrl;`, add:

```java
    // Snapshot fields — copied from TestCase at initialize_run time.
    // These are immutable once set and allow historical reporting even after
    // the original TestCase or TestStep is soft-deleted.
    private String title;
    private String description;
    private String preconditions;
    private Priority priority;
    private String displayId;
    private UUID suiteId;
```

Also add the import at the top of the file (it is already imported via the existing `Priority` usage elsewhere — verify `import com.java_template.application.dto.Priority;` is present).

- [ ] **Step 2: Add snapshot fields to TestRunStepDTO**

Open `apps/backend/src/main/java/com/java_template/application/dto/TestRunStepDTO.java`.

After the line `private UUID testStepId;`, add:

```java
    // Snapshot fields — copied from TestStep at initialize_run time.
    private Integer stepNumber;
    private String action;
    private String expectedResult;
```

- [ ] **Step 3: Compile to verify no errors**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:compileJava compileKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/backend/src/main/java/com/java_template/application/dto/TestRunCaseDTO.java \
        apps/backend/src/main/java/com/java_template/application/dto/TestRunStepDTO.java
git commit -m "feat: add snapshot fields to TestRunCaseDTO and TestRunStepDTO

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 2: Add createTestRunCaseWithStepSnapshots to TestRunCaseService (TDD)

**Files:**
- Create: `apps/backend/src/test/java/com/java_template/application/service/TestRunCaseServiceSnapshotTest.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java`

- [ ] **Step 1: Write the failing test**

Create `apps/backend/src/test/java/com/java_template/application/service/TestRunCaseServiceSnapshotTest.java`:

```java
package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.Priority;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.application.dto.TestStepDTO;
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

    private <T> EntityWithMetadata<T> wrap(T dto, UUID id) {
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
        UUID runStepId2 = UUID.randomUUID();

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
        runStep1.setTestRunCaseId(runCaseId);
        runStep1.setTestStepId(stepId1);
        runStep1.setStepNumber(1);
        runStep1.setAction("Open browser");
        runStep1.setExpectedResult("Browser opens");

        TestRunStepDTO runStep2 = new TestRunStepDTO();
        runStep2.setTestRunCaseId(runCaseId);
        runStep2.setTestStepId(stepId2);
        runStep2.setStepNumber(2);
        runStep2.setAction("Enter URL");
        runStep2.setExpectedResult("Page loads");

        when(testRunStepService.createTestRunStep(any())).thenReturn(runStep1).thenReturn(runStep2);

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

        // Compensating: step1 and the case are deleted
        verify(entityService).deleteById(runStepId1);
        verify(entityService).deleteById(runCaseId);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:test \
  --tests "com.java_template.application.service.TestRunCaseServiceSnapshotTest" 2>&1 | tail -20
```

Expected: compilation error — `createTestRunCaseWithStepSnapshots` does not exist yet.

- [ ] **Step 3: Add createTestRunCaseWithStepSnapshots to TestRunCaseService**

In `apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java`, add the following method after the existing `createTestRunCaseWithSteps` method:

```java
    /**
     * Creates a {@link TestRunCaseDTO} together with all of its {@link TestRunStepDTO}
     * records, copying snapshot content from the supplied {@link TestStepDTO} objects.
     *
     * <p>This is the snapshot variant of {@link #createTestRunCaseWithSteps}: instead of
     * taking only step IDs, it takes full {@link TestStepDTO} objects so that
     * {@code action}, {@code expectedResult}, and {@code stepNumber} are persisted on
     * the {@code TestRunStep} entity — preserving historical data even if the original
     * {@code TestStep} is later soft-deleted.</p>
     *
     * <p>Uses the same compensating-transaction rollback as the ID-only variant.</p>
     *
     * @param testRunCase the case to create (snapshot fields must already be set by caller)
     * @param steps       ordered list of original {@link TestStepDTO} objects to snapshot
     * @return the persisted {@link TestRunCaseDTO} with its assigned {@code id}
     */
    public TestRunCaseDTO createTestRunCaseWithStepSnapshots(TestRunCaseDTO testRunCase,
                                                              List<TestStepDTO> steps) {
        TestRunCaseDTO created = createTestRunCase(testRunCase);
        UUID runCaseId = created.getId();

        List<UUID> createdStepIds = new ArrayList<>();
        try {
            for (TestStepDTO step : steps) {
                TestRunStepDTO runStep = new TestRunStepDTO();
                runStep.setTestRunCaseId(runCaseId);
                runStep.setTestStepId(step.getId());
                runStep.setStepNumber(step.getStepNumber());
                runStep.setAction(step.getAction());
                runStep.setExpectedResult(step.getExpectedResult());
                TestRunStepDTO createdStep = testRunStepService.createTestRunStep(runStep);
                createdStepIds.add(createdStep.getId());
            }
        } catch (Exception stepException) {
            log.error("Step snapshot creation failed for TestRunCase {}. Rolling back {} step(s) and the case.",
                    runCaseId, createdStepIds.size(), stepException);
            createdStepIds.forEach(stepId -> {
                try {
                    entityService.deleteById(stepId);
                } catch (Exception ignored) {
                    log.warn("Cleanup failed for TestRunStep {}", stepId);
                }
            });
            try {
                entityService.deleteById(runCaseId);
            } catch (Exception ignored) {
                log.warn("Cleanup failed for TestRunCase {}", runCaseId);
            }
            throw new RuntimeException(
                    "Failed to initialise TestRunCase " + runCaseId +
                    ": step snapshot creation failed. The partial write has been rolled back.", stepException);
        }

        return created;
    }
```

Also add the missing import at the top of `TestRunCaseService.java`:

```java
import com.java_template.application.dto.TestStepDTO;
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:test \
  --tests "com.java_template.application.service.TestRunCaseServiceSnapshotTest" 2>&1 | tail -20
```

Expected: `3 tests completed, 0 failed` — `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java \
        apps/backend/src/test/java/com/java_template/application/service/TestRunCaseServiceSnapshotTest.java
git commit -m "feat: add createTestRunCaseWithStepSnapshots to TestRunCaseService

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 3: Implement SnapshotProcessor (TDD)

**Files:**
- Create: `apps/backend/src/test/java/com/java_template/application/processor/SnapshotProcessorTest.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/processor/SnapshotProcessor.java`

- [ ] **Step 1: Write the failing tests**

Create `apps/backend/src/test/java/com/java_template/application/processor/SnapshotProcessorTest.java`:

```java
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
import org.jetbrains.annotations.NotNull;
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
            @Override public @NotNull EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:test \
  --tests "com.java_template.application.processor.SnapshotProcessorTest" 2>&1 | tail -20
```

Expected: compilation error — constructor with 4 args does not exist, `process` method is stub.

- [ ] **Step 3: Implement SnapshotProcessor**

Replace the entire content of `apps/backend/src/main/java/com/java_template/application/processor/SnapshotProcessor.java`:

```java
package com.java_template.application.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.application.service.TestCaseService;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.application.service.TestStepService;
import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.CyodaProcessor;
import com.java_template.common.workflow.OperationSpecification;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Processor for capturing an immutable snapshot of all TestCase and TestStep data
 * at the moment a TestRun transitions from {@code initial} to {@code active}.
 *
 * <p>For each case ID listed in the TestRun payload, it fetches the current
 * {@link TestCaseDTO} and its {@link TestStepDTO} records, then creates
 * {@link TestRunCaseDTO} and {@link TestRunStepDTO} entities that embed the
 * full content. This means the run remains self-contained: even if the original
 * TestCase or TestStep is soft-deleted later, the historical run data is preserved.</p>
 *
 * <p>Constraint: this processor must NOT call {@code EntityService} to modify the
 * TestRun entity being processed (Cyoda would deadlock). Creating new TestRunCase /
 * TestRunStep entities is safe because they are separate entity types.</p>
 */
@Component("SnapshotProcessor")
public class SnapshotProcessor implements CyodaProcessor {

    private static final Logger log = LoggerFactory.getLogger(SnapshotProcessor.class);
    private static final String PROCESSOR_NAME = "SnapshotProcessor";

    private final TestCaseService testCaseService;
    private final TestStepService testStepService;
    private final TestRunCaseService testRunCaseService;
    private final ObjectMapper objectMapper;

    public SnapshotProcessor(TestCaseService testCaseService,
                              TestStepService testStepService,
                              TestRunCaseService testRunCaseService,
                              ObjectMapper objectMapper) {
        this.testCaseService    = testCaseService;
        this.testStepService    = testStepService;
        this.testRunCaseService = testRunCaseService;
        this.objectMapper       = objectMapper;
    }

    @Override
    public EntityProcessorCalculationResponse process(
            CyodaEventContext<EntityProcessorCalculationRequest> context) {

        EntityProcessorCalculationRequest request = context.getEvent();
        UUID testRunId = request.getEntityId();
        log.info("SnapshotProcessor starting for testRunId={}", testRunId);

        try {
            JsonNode data = request.getPayload().getData();

            UUID projectId = parseUuid(data, "projectId");
            List<String> caseIds = parseCaseIds(data);

            if (caseIds.isEmpty()) {
                log.info("SnapshotProcessor: no caseIds in TestRun {}, nothing to snapshot", testRunId);
                return successResponse(request);
            }

            for (String caseIdStr : caseIds) {
                snapshotCase(testRunId, projectId, caseIdStr);
            }

            log.info("SnapshotProcessor completed for testRunId={}, snapshotted {} case(s)",
                    testRunId, caseIds.size());

        } catch (Exception e) {
            log.error("SnapshotProcessor failed for testRunId={}: {}", testRunId, e.getMessage(), e);
            // Return success anyway — the run must not be blocked by a snapshot failure.
            // The snapshot can be recreated later if needed.
        }

        return successResponse(request);
    }

    private void snapshotCase(UUID testRunId, UUID projectId, String caseIdStr) {
        UUID caseId;
        try {
            caseId = UUID.fromString(caseIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("SnapshotProcessor: invalid caseId '{}', skipping", caseIdStr);
            return;
        }

        Optional<TestCaseDTO> tcOpt = testCaseService.getTestCaseById(caseId);
        if (tcOpt.isEmpty()) {
            log.warn("SnapshotProcessor: TestCase {} not found (already deleted?), skipping", caseId);
            return;
        }
        TestCaseDTO tc = tcOpt.get();

        TestRunCaseDTO caseSnapshot = new TestRunCaseDTO();
        caseSnapshot.setTestRunId(testRunId);
        caseSnapshot.setTestCaseId(caseId);
        caseSnapshot.setProjectId(projectId);
        // snapshot content
        caseSnapshot.setTitle(tc.getTitle());
        caseSnapshot.setDescription(tc.getDescription());
        caseSnapshot.setPreconditions(tc.getPreconditions());
        caseSnapshot.setPriority(tc.getPriority());
        caseSnapshot.setDisplayId(tc.getDisplayId());
        caseSnapshot.setSuiteId(tc.getSuiteId());

        List<TestStepDTO> steps = testStepService.getTestStepsByTestCaseId(caseId);

        testRunCaseService.createTestRunCaseWithStepSnapshots(caseSnapshot, steps);
        log.debug("SnapshotProcessor: snapshotted case {} with {} step(s)", caseId, steps.size());
    }

    private UUID parseUuid(JsonNode data, String field) {
        JsonNode node = data.get(field);
        if (node == null || node.isNull()) return null;
        try {
            return UUID.fromString(node.asText());
        } catch (IllegalArgumentException e) {
            log.warn("SnapshotProcessor: cannot parse UUID field '{}': {}", field, node.asText());
            return null;
        }
    }

    private List<String> parseCaseIds(JsonNode data) {
        JsonNode node = data.get("caseIds");
        if (node == null || !node.isArray() || node.isEmpty()) return List.of();
        return objectMapper.convertValue(node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private EntityProcessorCalculationResponse successResponse(EntityProcessorCalculationRequest request) {
        EntityProcessorCalculationResponse response = new EntityProcessorCalculationResponse();
        response.setId(request.getId());
        response.setSuccess(true);
        return response;
    }

    @Override
    public boolean supports(OperationSpecification opSpec) {
        return PROCESSOR_NAME.equals(opSpec.operationName());
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:test \
  --tests "com.java_template.application.processor.SnapshotProcessorTest" 2>&1 | tail -20
```

Expected: `6 tests completed, 0 failed` — `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/backend/src/main/java/com/java_template/application/processor/SnapshotProcessor.java \
        apps/backend/src/test/java/com/java_template/application/processor/SnapshotProcessorTest.java
git commit -m "feat: implement SnapshotProcessor to create TestRunCase/TestRunStep snapshots

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 4: Fix double-fetch race condition in TestRunService (TDD)

**Files:**
- Create: `apps/backend/src/test/java/com/java_template/application/service/TestRunServiceRaceConditionTest.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java`

- [ ] **Step 1: Write the failing test**

Create `apps/backend/src/test/java/com/java_template/application/service/TestRunServiceRaceConditionTest.java`:

```java
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestRunService — single-fetch on updateTestRun")
class TestRunServiceRaceConditionTest {

    @Mock private EntityService entityService;
    @Mock private ProjectCounterService projectCounterService;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private TestRunService testRunService;

    private UUID runId;
    private UUID projectId;

    private EntityWithMetadata<TestRunDTO> wrap(TestRunDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @BeforeEach
    void setUp() {
        runId     = UUID.randomUUID();
        projectId = UUID.randomUUID();
    }

    @Test
    @DisplayName("fetches existing run exactly once even when caseIds and stepStatuses are missing")
    void fetchesExistingRunOnce_whenMergeNeeded() {
        // Existing run in 'active' state (not initial — no transition needed)
        TestRunDTO existing = new TestRunDTO();
        existing.setId(runId);
        existing.setProjectId(projectId);
        existing.setStatus("active");
        existing.setCaseIds(List.of("case-1"));
        existing.setStepStatuses("{\"k\":\"PASSED\"}");

        // getTestRunById calls entityService.getById
        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenReturn(wrap(existing, runId));

        // Incoming update with no caseIds and no stepStatuses — triggers merge
        TestRunDTO incoming = new TestRunDTO();
        incoming.setProjectId(projectId);
        incoming.setStatus("active");
        incoming.setName("Updated Name");
        incoming.setCaseIds(null);
        incoming.setStepStatuses(null);

        when(entityService.update(eq(runId), any(), isNull()))
                .thenReturn(wrap(incoming, runId));

        testRunService.updateTestRun(runId, incoming);

        // entityService.getById must be called exactly once (not twice)
        verify(entityService, times(1)).getById(eq(runId), any(), eq(TestRunDTO.class));
    }

    @Test
    @DisplayName("fetches existing run exactly once when in initial state (transition check + merge)")
    void fetchesExistingRunOnce_whenInitialState() {
        TestRunDTO existing = new TestRunDTO();
        existing.setId(runId);
        existing.setProjectId(projectId);
        existing.setStatus("initial");
        existing.setCaseIds(List.of("case-1"));
        existing.setStepStatuses("{\"k\":\"UNTESTED\"}");

        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenReturn(wrap(existing, runId));

        TestRunDTO incoming = new TestRunDTO();
        incoming.setProjectId(projectId);
        incoming.setStatus("active");
        incoming.setName("Starting run");
        incoming.setCaseIds(null);      // triggers merge
        incoming.setStepStatuses(null); // triggers merge

        when(entityService.update(eq(runId), any(), eq("initialize_run")))
                .thenReturn(wrap(incoming, runId));

        testRunService.updateTestRun(runId, incoming);

        verify(entityService, times(1)).getById(eq(runId), any(), eq(TestRunDTO.class));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:test \
  --tests "com.java_template.application.service.TestRunServiceRaceConditionTest" 2>&1 | tail -20
```

Expected: tests fail — `findAll` is called twice, not once.

- [ ] **Step 3: Fix updateTestRun to use a single fetch**

In `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java`, replace the `updateTestRun` method body with:

```java
    public TestRunDTO updateTestRun(UUID id, TestRunDTO testRun) {
        boolean needsCaseIdsMerge = testRun.getCaseIds() == null || testRun.getCaseIds().isEmpty();
        boolean needsStatusMerge  = testRun.getStepStatuses() == null
                || testRun.getStepStatuses().isEmpty()
                || testRun.getStepStatuses().equals("{}");

        log.warn("===== UPDATETESTRUN ENTRY ===== id={}, incomingStatus={}, incomingStepStatusesEmpty={}",
                id, testRun.getStatus(), needsStatusMerge);

        final String[] operationName = new String[]{null};

        // Single fetch — reused for BOTH transition detection AND merge logic.
        // Previously two separate getTestRunById() calls caused concurrent threads to
        // each see status=initial and each send initialize_run, flooding Cyoda with
        // conflicting transactions that timed out after 60 s.
        Optional<TestRunDTO> existingOpt = getTestRunById(id);

        existingOpt.ifPresent(existing -> {
            if ("initial".equals(existing.getStatus())) {
                operationName[0] = "initialize_run";
                log.warn("===== UPDATETESTRUN DETECTED INITIAL STATE - transitioning to active =====");
            }
        });

        if (needsCaseIdsMerge || needsStatusMerge) {
            existingOpt.ifPresent(existing -> {
                log.warn("===== UPDATETESTRUN MERGE ===== existingStatus={}, existingStepStatusesEmpty={}",
                        existing.getStatus(),
                        (existing.getStepStatuses() == null
                                || existing.getStepStatuses().isEmpty()
                                || existing.getStepStatuses().equals("{}")));

                if (needsCaseIdsMerge
                        && existing.getCaseIds() != null
                        && !existing.getCaseIds().isEmpty()) {
                    testRun.setCaseIds(existing.getCaseIds());
                }
                if (needsStatusMerge
                        && existing.getStepStatuses() != null
                        && !existing.getStepStatuses().equals("{}")) {
                    testRun.setStepStatuses(existing.getStepStatuses());
                } else if (!needsStatusMerge
                        && existing.getStepStatuses() != null
                        && !existing.getStepStatuses().equals("{}")) {
                    java.util.Map<String, String> existingMap = existing.getStepStatusesAsMap();
                    java.util.Map<String, String> incomingMap = testRun.getStepStatusesAsMap();
                    existingMap.putAll(incomingMap);
                    testRun.setStepStatusesFromMap(existingMap);
                    log.warn("===== UPDATETESTRUN MERGED ===== finalStepStatusesCount={}", existingMap.size());
                }
            });
        }

        log.warn("===== UPDATETESTRUN CALLING entityService.update() with status={}, stepStatusesEmpty={}, operationName={}",
                testRun.getStatus(),
                (testRun.getStepStatuses() == null
                        || testRun.getStepStatuses().isEmpty()
                        || testRun.getStepStatuses().equals("{}")),
                operationName[0]);

        return withId(entityService.update(id, testRun, operationName[0]));
    }
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:test \
  --tests "com.java_template.application.service.TestRunServiceRaceConditionTest" 2>&1 | tail -20
```

Expected: `2 tests completed, 0 failed` — `BUILD SUCCESSFUL`

- [ ] **Step 5: Run the full test suite to check for regressions**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && ./gradlew :apps:backend:test 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`, all tests green.

- [ ] **Step 6: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/backend/src/main/java/com/java_template/application/service/TestRunService.java \
        apps/backend/src/test/java/com/java_template/application/service/TestRunServiceRaceConditionTest.java
git commit -m "fix: eliminate double-fetch race condition in TestRunService.updateTestRun

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 5: Update Cyoda sample JSON files

**Files:**
- Modify: `apps/backend/src/main/resources/entity/testruncase/version_1/testruncase.json`
- Modify: `apps/backend/src/main/resources/entity/testrunstep/version_1/testrunstep.json`

- [ ] **Step 1: Update testruncase.json**

Replace the content of `apps/backend/src/main/resources/entity/testruncase/version_1/testruncase.json`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440040",
  "testRunId": "550e8400-e29b-41d4-a716-446655440041",
  "testCaseId": "550e8400-e29b-41d4-a716-446655440042",
  "projectId": "550e8400-e29b-41d4-a716-446655440043",
  "status": "UNTESTED",
  "bugUrl": null,
  "startedAt": "2026-04-14T00:00:00Z",
  "completedAt": "2026-04-14T00:00:00Z",
  "title": "Sample test case title",
  "description": "Sample test case description",
  "preconditions": "User is logged in",
  "priority": "MEDIUM",
  "displayId": "TC-001",
  "suiteId": "550e8400-e29b-41d4-a716-446655440044"
}
```

- [ ] **Step 2: Update testrunstep.json**

Replace the content of `apps/backend/src/main/resources/entity/testrunstep/version_1/testrunstep.json`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440050",
  "testRunCaseId": "550e8400-e29b-41d4-a716-446655440040",
  "testStepId": "550e8400-e29b-41d4-a716-446655440051",
  "status": "UNTESTED",
  "actualResult": null,
  "startedAt": "2026-04-14T00:00:00Z",
  "completedAt": "2026-04-14T00:00:00Z",
  "stepNumber": 1,
  "action": "Open the login page and enter valid credentials",
  "expectedResult": "User is redirected to the dashboard"
}
```

- [ ] **Step 3: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/backend/src/main/resources/entity/testruncase/version_1/testruncase.json \
        apps/backend/src/main/resources/entity/testrunstep/version_1/testrunstep.json
git commit -m "feat: add snapshot fields to testruncase and testrunstep Cyoda sample JSON

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 6: Re-import schemas into Cyoda

**Files:** none (infrastructure operation)

> ⚠️ This task deletes ALL Cyoda tenant data. Ensure you are on a dev/staging instance.

- [ ] **Step 1: Get a fresh token and delete all tenant entities (bottom-up)**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
set -a; source .env; set +a

TOKEN=$(curl -s \
  -u "$CYODA_CLIENT_ID:$CYODA_CLIENT_SECRET" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&scope=ROLE_M2M' \
  "https://$CYODA_HOST/api/oauth/token" \
  | python3 -c 'import sys, json; print(json.load(sys.stdin).get("access_token",""))')

echo "Token obtained: ${TOKEN:0:20}..."

ENTITY_ORDER=("TestRunStep" "TestRunCase" "TestRun" "Attachment" "Defect" "TestStep" "TestCase" "Suite" "Report" "ProjectCounter" "Project")

for model in "${ENTITY_ORDER[@]}"; do
  echo "Deleting entities: $model"
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
    "https://$CYODA_HOST/api/entity/$model/1" > /dev/null 2>&1
done
echo "Entity deletion complete."
```

- [ ] **Step 2: Unlock and delete all models**

```bash
MODELS=("Project" "Suite" "TestCase" "TestStep" "Defect" "TestRun" "TestRunCase" "TestRunStep" "Attachment" "Report" "ProjectCounter")

for model in "${MODELS[@]}"; do
  echo "Unlocking $model"
  curl -s -X PUT -H "Authorization: Bearer $TOKEN" \
    "https://$CYODA_HOST/api/model/$model/1/unlock" > /dev/null 2>&1
done

for model in "${MODELS[@]}"; do
  echo "Deleting model $model"
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
    "https://$CYODA_HOST/api/model/$model/1" > /dev/null 2>&1
done
echo "Model deletion complete."
```

- [ ] **Step 3: Run the import script**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
./import-schemas.sh
```

Expected: script runs without errors, all 11 entity schemas and 11 workflows imported, all models locked.

- [ ] **Step 4: Verify in Cyoda UI that TestRunCase and TestRunStep schemas include snapshot fields**

Check that `TestRunCase` has: `title`, `description`, `preconditions`, `priority`, `displayId`, `suiteId`.
Check that `TestRunStep` has: `stepNumber`, `action`, `expectedResult`.

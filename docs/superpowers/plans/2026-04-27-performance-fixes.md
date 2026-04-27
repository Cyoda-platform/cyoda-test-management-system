# Performance Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all performance bottlenecks from the performance report: parallelise batch import, add replaceSteps endpoint, cache TestRun queries, parallelise cascade delete, add React.memo and code splitting.

**Architecture:** Eight independent tasks — backend (Tasks 1–5) then frontend (Tasks 6–8). Each task touches different files. Backend tasks require no live Cyoda instance (unit tests only). Frontend tasks require `npm run test` passing.

**Tech Stack:** Java 21, CompletableFuture, Spring Cache + Caffeine, React 18 lazy/Suspense, Vite manualChunks.

---

## File Map

| File | Task | Change |
|---|---|---|
| `apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java` | 1 | Parallelise batchCreateTestCases |
| `apps/backend/src/test/java/com/java_template/application/service/TestCaseServiceTest.java` | 1 | Add regression + error tests |
| `apps/backend/src/main/java/com/java_template/application/dto/StepReplaceDTO.java` | 2 | New DTO |
| `apps/backend/src/main/java/com/java_template/application/service/TestStepService.java` | 2 | New replaceSteps method |
| `apps/backend/src/test/java/com/java_template/application/service/TestStepServiceTest.java` | 2 | New test file |
| `apps/backend/src/main/java/com/java_template/application/controller/TestStepController.java` | 3 | New replaceSteps endpoint |
| `apps/backend/src/test/java/com/java_template/application/controller/TestStepControllerTest.java` | 3 | New test file |
| `apps/backend/src/main/java/com/java_template/application/config/CacheConfig.java` | 4 | New Caffeine cache manager |
| `apps/backend/src/main/java/com/java_template/Application.java` | 4 | Add @EnableCaching |
| `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java` | 4 | @Cacheable / @CacheEvict |
| `apps/backend/build.gradle` | 4 | Add spring-boot-starter-cache |
| `apps/backend/src/test/java/com/java_template/application/service/TestRunServiceCacheTest.java` | 4 | New test file |
| `apps/backend/src/main/java/com/java_template/application/service/ProjectService.java` | 5 | Parallelise remaining delete loops |
| `apps/backend/src/main/resources/application.yml` | 5 | resolve-lazily=true, keep-alive=30s |
| `apps/frontend/src/pages/RunExecution.tsx` | 6 | React.memo on DefectAttachmentsList |
| `apps/frontend/src/pages/Defects.tsx` | 6 | React.memo on DefectAttachmentsList |
| `apps/frontend/src/lib/api.ts` | 7 | Add testStepsApi.replace() |
| `apps/frontend/src/pages/Repository.tsx` | 7 | Use replaceSteps in overwrite flow |
| `apps/frontend/src/hooks/useApi.ts` | 7 | staleTime: 5000 on useDefectsByRun |
| `apps/frontend/src/App.tsx` | 8 | React.lazy for heavy pages |
| `apps/frontend/vite.config.ts` | 8 | manualChunks for recharts + radix |

---

## Task 1: Parallelise batchCreateTestCases

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java`
- Test: `apps/backend/src/test/java/com/java_template/application/service/TestCaseServiceTest.java`

- [ ] **Step 1: Write regression tests in TestCaseServiceTest.java**

Add these two tests inside the existing `TestCaseServiceTest` class:

```java
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
    when(testStepService.createTestStep(any())).thenReturn(new com.java_template.application.dto.TestStepDTO());

    List<TestCaseDTO> result = testCaseService.batchCreateTestCases(projectId, suiteId, List.of(item1, item2));

    assertEquals(2, result.size());
    assertEquals("TC-001", result.get(0).getDisplayId());
    assertEquals("TC-002", result.get(1).getDisplayId());
    verify(entityService, times(2)).create(any(TestCaseDTO.class));
    verify(entityService, times(2)).update(any(UUID.class), any(TestCaseDTO.class), any());
    verify(testStepService, times(1)).createTestStep(any()); // 1 step in item1, 0 in item2
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
```

Also add this import at the top of `TestCaseServiceTest.java`:
```java
import com.java_template.application.dto.BatchImportCaseDTO;
```

- [ ] **Step 2: Run tests to confirm they pass with the current sequential code**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
./gradlew :apps:backend:test --tests "com.java_template.application.service.TestCaseServiceTest" 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL` (both new tests pass — sequential code is correct, just slow).

- [ ] **Step 3: Replace the sequential loop with a parallel implementation**

In `TestCaseService.java`, replace the entire `batchCreateTestCases` method body (lines 303–347) with:

```java
public List<TestCaseDTO> batchCreateTestCases(UUID projectId, UUID suiteId,
                                              List<BatchImportCaseDTO> items) {
    if (items == null || items.isEmpty()) return Collections.emptyList();

    List<String> displayIds = projectCounterService.nextDisplayIdBatch(projectId, items.size());

    TestCaseDTO[] results = new TestCaseDTO[items.size()];
    List<CompletableFuture<Void>> futures = new ArrayList<>(items.size());

    for (int i = 0; i < items.size(); i++) {
        final int idx        = i;
        final BatchImportCaseDTO item = items.get(i);
        final String displayId        = displayIds.get(i);

        futures.add(CompletableFuture.runAsync(() -> {
            TestCaseDTO tc = new TestCaseDTO();
            tc.setProjectId(projectId);
            tc.setSuiteId(suiteId);
            tc.setTitle(item.getTitle());
            tc.setDescription(item.getDescription() != null ? item.getDescription() : "");
            tc.setPreconditions(item.getPreconditions() != null ? item.getPreconditions() : "");
            tc.setPriority(item.getPriority() != null ? item.getPriority() : com.java_template.application.dto.Priority.MEDIUM);
            tc.setDeleted(false);
            tc.setDisplayId(displayId);

            TestCaseDTO saved = withId(entityService.create(tc));
            saved.setDisplayId(displayId);
            entityService.update(saved.getId(), saved, null);

            if (item.getSteps() != null && !item.getSteps().isEmpty()) {
                int stepNum = 1;
                for (BatchImportCaseDTO.StepDTO s : item.getSteps()) {
                    TestStepDTO step = new TestStepDTO();
                    step.setTestCaseId(saved.getId());
                    step.setStepNumber(s.getStepNumber() != null ? s.getStepNumber() : stepNum);
                    step.setAction(s.getAction() != null ? s.getAction() : "");
                    step.setExpectedResult(s.getExpectedResult() != null ? s.getExpectedResult() : "");
                    testStepService.createTestStep(step);
                    stepNum++;
                }
            }
            results[idx] = saved;
        }));
    }

    try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    } catch (java.util.concurrent.CompletionException e) {
        Throwable cause = e.getCause();
        throw new RuntimeException("Batch import failed: " + cause.getMessage(), cause);
    }

    return Arrays.asList(results);
}
```

Add these imports at the top of `TestCaseService.java` if not already present:
```java
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
```

- [ ] **Step 4: Run tests to confirm still green**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.service.TestCaseServiceTest" 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java \
        apps/backend/src/test/java/com/java_template/application/service/TestCaseServiceTest.java
git commit -m "perf(import): parallelise batchCreateTestCases with CompletableFuture"
```

---

## Task 2: Add replaceSteps service method

**Files:**
- Create: `apps/backend/src/main/java/com/java_template/application/dto/StepReplaceDTO.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestStepService.java`
- Create: `apps/backend/src/test/java/com/java_template/application/service/TestStepServiceTest.java`

- [ ] **Step 1: Create StepReplaceDTO.java**

Create `apps/backend/src/main/java/com/java_template/application/dto/StepReplaceDTO.java`:

```java
package com.java_template.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StepReplaceDTO {
    private Integer stepNumber;

    @Size(max = 2000)
    private String action;

    @Size(max = 2000)
    private String expectedResult;
}
```

- [ ] **Step 2: Write the failing test for replaceSteps**

Create `apps/backend/src/test/java/com/java_template/application/service/TestStepServiceTest.java`:

```java
package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.StepReplaceDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestStepServiceTest {

    @Mock
    private EntityService entityService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private TestStepService testStepService;

    private EntityWithMetadata<TestStepDTO> wrap(TestStepDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void replaceSteps_softDeletesExistingAndCreatesNew() {
        UUID caseId   = UUID.randomUUID();
        UUID stepId1  = UUID.randomUUID();
        UUID stepId2  = UUID.randomUUID();

        // existing steps returned by search
        TestStepDTO existing1 = new TestStepDTO();
        existing1.setId(stepId1);
        existing1.setTestCaseId(caseId);
        existing1.setDeleted(false);
        existing1.setStepNumber(1);

        TestStepDTO existing2 = new TestStepDTO();
        existing2.setId(stepId2);
        existing2.setTestCaseId(caseId);
        existing2.setDeleted(false);
        existing2.setStepNumber(2);

        // getTestStepsByTestCaseId calls entityService.search — stub it
        var searchResult = new com.java_template.common.dto.PageResult<EntityWithMetadata<TestStepDTO>>(
                null, List.of(wrap(existing1, stepId1), wrap(existing2, stepId2)), 0, 100, 2L);
        when(entityService.search(any(), any(), eq(TestStepDTO.class))).thenReturn(searchResult);

        // stub update for soft-delete
        when(entityService.update(any(UUID.class), any(TestStepDTO.class), any())).thenAnswer(inv ->
                wrap(inv.getArgument(1), inv.getArgument(0)));

        // stub create for new steps
        when(entityService.create(any(TestStepDTO.class))).thenAnswer(inv ->
                wrap(inv.getArgument(0), UUID.randomUUID()));

        StepReplaceDTO newStep = new StepReplaceDTO();
        newStep.setStepNumber(1);
        newStep.setAction("new action");
        newStep.setExpectedResult("new expected");

        List<TestStepDTO> result = testStepService.replaceSteps(caseId, List.of(newStep));

        // existing 2 steps soft-deleted (update called twice with deleted=true)
        verify(entityService, times(2)).update(any(UUID.class), any(TestStepDTO.class), any());
        // 1 new step created
        verify(entityService, times(1)).create(any(TestStepDTO.class));
        assertEquals(1, result.size());
        assertEquals("new action", result.get(0).getAction());
    }

    @Test
    public void replaceSteps_withNoNewSteps_onlySoftDeletesExisting() {
        UUID caseId  = UUID.randomUUID();
        UUID stepId  = UUID.randomUUID();

        TestStepDTO existing = new TestStepDTO();
        existing.setId(stepId);
        existing.setTestCaseId(caseId);
        existing.setDeleted(false);
        existing.setStepNumber(1);

        var searchResult = new com.java_template.common.dto.PageResult<EntityWithMetadata<TestStepDTO>>(
                null, List.of(wrap(existing, stepId)), 0, 100, 1L);
        when(entityService.search(any(), any(), eq(TestStepDTO.class))).thenReturn(searchResult);
        when(entityService.update(any(UUID.class), any(TestStepDTO.class), any())).thenAnswer(inv ->
                wrap(inv.getArgument(1), inv.getArgument(0)));

        List<TestStepDTO> result = testStepService.replaceSteps(caseId, List.of());

        verify(entityService, times(1)).update(any(UUID.class), any(TestStepDTO.class), any());
        verify(entityService, never()).create(any());
        assertTrue(result.isEmpty());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.service.TestStepServiceTest" 2>&1 | tail -15
```

Expected: FAIL — `replaceSteps` method does not exist yet.

- [ ] **Step 4: Implement replaceSteps in TestStepService.java**

Add these imports at the top of `TestStepService.java` if not already present:
```java
import com.java_template.application.dto.StepReplaceDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
```

Add this method at the end of `TestStepService` class (before the closing `}`):

```java
/**
 * Atomically replaces all steps for a test case:
 * soft-deletes existing steps in parallel, then creates new ones in parallel.
 */
public List<TestStepDTO> replaceSteps(UUID caseId, List<StepReplaceDTO> newSteps) {
    // Soft-delete existing steps in parallel
    List<TestStepDTO> existing = getTestStepsByTestCaseId(caseId);
    if (!existing.isEmpty()) {
        List<CompletableFuture<Void>> deleteFutures = existing.stream()
                .map(s -> CompletableFuture.runAsync(() -> {
                    s.setDeleted(true);
                    entityService.update(s.getId(), s, null);
                }))
                .toList();
        CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();
    }

    if (newSteps == null || newSteps.isEmpty()) return List.of();

    // Create new steps in parallel (stepNumber is stored on each step, order is independent)
    TestStepDTO[] results = new TestStepDTO[newSteps.size()];
    List<CompletableFuture<Void>> createFutures = new ArrayList<>(newSteps.size());
    for (int i = 0; i < newSteps.size(); i++) {
        final int idx = i;
        final StepReplaceDTO dto = newSteps.get(i);
        createFutures.add(CompletableFuture.runAsync(() -> {
            TestStepDTO step = new TestStepDTO();
            step.setTestCaseId(caseId);
            step.setStepNumber(dto.getStepNumber() != null ? dto.getStepNumber() : idx + 1);
            step.setAction(dto.getAction() != null ? dto.getAction() : "");
            step.setExpectedResult(dto.getExpectedResult() != null ? dto.getExpectedResult() : "");
            step.setDeleted(false);
            results[idx] = withId(entityService.create(step));
        }));
    }
    CompletableFuture.allOf(createFutures.toArray(new CompletableFuture[0])).join();
    return Arrays.asList(results);
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.service.TestStepServiceTest" 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/dto/StepReplaceDTO.java \
        apps/backend/src/main/java/com/java_template/application/service/TestStepService.java \
        apps/backend/src/test/java/com/java_template/application/service/TestStepServiceTest.java
git commit -m "perf(import): add replaceSteps service method with parallel delete+create"
```

---

## Task 3: Add replaceSteps controller endpoint

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/controller/TestStepController.java`
- Create: `apps/backend/src/test/java/com/java_template/application/controller/TestStepControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

Create `apps/backend/src/test/java/com/java_template/application/controller/TestStepControllerTest.java`:

```java
package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.StepReplaceDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.application.service.TestStepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestStepController.class)
public class TestStepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestStepService testStepService;

    @Test
    public void replaceSteps_returns200WithCreatedSteps() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId   = UUID.randomUUID();
        UUID caseId    = UUID.randomUUID();

        StepReplaceDTO dto = new StepReplaceDTO();
        dto.setStepNumber(1);
        dto.setAction("click login");
        dto.setExpectedResult("user logged in");

        TestStepDTO created = new TestStepDTO();
        created.setId(UUID.randomUUID());
        created.setTestCaseId(caseId);
        created.setStepNumber(1);
        created.setAction("click login");
        created.setExpectedResult("user logged in");

        when(testStepService.replaceSteps(eq(caseId), anyList()))
                .thenReturn(List.of(created));

        mockMvc.perform(put("/projects/{p}/suites/{s}/cases/{c}/steps/replace",
                        projectId, suiteId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("click login"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestStepControllerTest" 2>&1 | tail -15
```

Expected: FAIL — endpoint does not exist.

- [ ] **Step 3: Add the replaceSteps endpoint to TestStepController.java**

Add this import at the top of `TestStepController.java`:
```java
import com.java_template.application.dto.StepReplaceDTO;
import jakarta.validation.Valid;
```

Add this method inside the `TestStepController` class (after the existing `deleteTestStep` method, before the closing `}`):

```java
@PutMapping("/replace")
@Operation(summary = "Replace all steps for a test case (delete existing, create new)")
public ResponseEntity<List<TestStepDTO>> replaceSteps(
        @PathVariable UUID projectId,
        @PathVariable UUID suiteId,
        @PathVariable UUID caseId,
        @Valid @RequestBody List<StepReplaceDTO> steps) {
    List<TestStepDTO> result = testStepService.replaceSteps(caseId, steps);
    return ResponseEntity.ok(result);
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestStepControllerTest" 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run all backend tests to check for regressions**

```bash
./gradlew :apps:backend:test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/controller/TestStepController.java \
        apps/backend/src/test/java/com/java_template/application/controller/TestStepControllerTest.java
git commit -m "feat(api): add PUT /steps/replace endpoint for atomic step replacement"
```

---

## Task 4: Spring Cache for TestRunService

**Files:**
- Modify: `apps/backend/build.gradle`
- Modify: `apps/backend/src/main/java/com/java_template/Application.java`
- Create: `apps/backend/src/main/java/com/java_template/application/config/CacheConfig.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java`
- Create: `apps/backend/src/test/java/com/java_template/application/service/TestRunServiceCacheTest.java`

- [ ] **Step 1: Add spring-boot-starter-cache dependency**

In `apps/backend/build.gradle`, add this line after the existing `spring-boot-starter-web` entry (around line 103):

```gradle
implementation 'org.springframework.boot:spring-boot-starter-cache'
```

- [ ] **Step 2: Write the failing cache test**

Create `apps/backend/src/test/java/com/java_template/application/service/TestRunServiceCacheTest.java`:

```java
package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.config.CacheConfig;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TestRunService.class, CacheConfig.class})
@EnableCaching
@Import(CacheConfig.class)
public class TestRunServiceCacheTest {

    @MockBean
    private EntityService entityService;

    @MockBean
    private ProjectCounterService projectCounterService;

    @Autowired
    private TestRunService testRunService;

    private EntityWithMetadata<TestRunDTO> wrap(TestRunDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void getTestRunsByProjectId_hitsCacheOnSecondCall() {
        UUID projectId = UUID.randomUUID();
        UUID runId     = UUID.randomUUID();

        TestRunDTO run = new TestRunDTO();
        run.setProjectId(projectId);
        run.setName("Run 1");

        PageResult<EntityWithMetadata<TestRunDTO>> page = new PageResult<>(
                null, List.of(wrap(run, runId)), 0, 20, 1L);
        when(entityService.findAll(any(), eq(TestRunDTO.class))).thenReturn(page);

        // Call twice with same projectId
        testRunService.getTestRunsByProjectId(projectId, 0, 20);
        testRunService.getTestRunsByProjectId(projectId, 0, 20);

        // entityService.findAll should only be called ONCE (second call is a cache hit)
        verify(entityService, times(1)).findAll(any(), eq(TestRunDTO.class));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.service.TestRunServiceCacheTest" 2>&1 | tail -15
```

Expected: FAIL — CacheConfig class does not exist, @EnableCaching not set.

- [ ] **Step 4: Create CacheConfig.java**

Create `apps/backend/src/main/java/com/java_template/application/config/CacheConfig.java`:

```java
package com.java_template.application.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "testRunsByProject",
                "allTestRunsByProject"
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(500));
        return manager;
    }
}
```

- [ ] **Step 5: Remove @EnableCaching from Application.java (CacheConfig handles it)**

`Application.java` should remain as-is — `CacheConfig` carries `@EnableCaching`.

- [ ] **Step 6: Annotate TestRunService methods with @Cacheable / @CacheEvict**

Add these imports at the top of `TestRunService.java`:
```java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
```

Add `@Cacheable` to `getTestRunsByProjectId` (line 118):
```java
@Cacheable(value = "testRunsByProject", key = "#projectId + ':' + #page + ':' + #size")
public PageResult<TestRunDTO> getTestRunsByProjectId(UUID projectId, int page, int size) {
```

Add `@Cacheable` to `getAllTestRunsByProjectId` (line 150):
```java
@Cacheable(value = "allTestRunsByProject", key = "#projectId")
public List<TestRunDTO> getAllTestRunsByProjectId(UUID projectId) {
```

Add `@CacheEvict` to `createTestRun` (line 69):
```java
@Caching(evict = {
    @CacheEvict(value = "testRunsByProject",    allEntries = true),
    @CacheEvict(value = "allTestRunsByProject", allEntries = true)
})
public TestRunDTO createTestRun(TestRunDTO testRun) {
```

Add `@CacheEvict` to `updateTestRun` (line 172):
```java
@Caching(evict = {
    @CacheEvict(value = "testRunsByProject",    allEntries = true),
    @CacheEvict(value = "allTestRunsByProject", allEntries = true)
})
public TestRunDTO updateTestRun(UUID id, TestRunDTO testRun) {
```

Add `@CacheEvict` to `deleteTestRun` (line 272):
```java
@Caching(evict = {
    @CacheEvict(value = "testRunsByProject",    allEntries = true),
    @CacheEvict(value = "allTestRunsByProject", allEntries = true)
})
public boolean deleteTestRun(UUID id) {
```

- [ ] **Step 7: Run cache test to verify it passes**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.service.TestRunServiceCacheTest" 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Run all backend tests to confirm no regressions**

```bash
./gradlew :apps:backend:test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add apps/backend/build.gradle \
        apps/backend/src/main/java/com/java_template/application/config/CacheConfig.java \
        apps/backend/src/main/java/com/java_template/application/service/TestRunService.java \
        apps/backend/src/test/java/com/java_template/application/service/TestRunServiceCacheTest.java
git commit -m "perf(cache): add Caffeine cache for TestRun project queries (TTL 30s)"
```

---

## Task 5: Parallelise cascade delete + config tweaks

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/service/ProjectService.java`
- Modify: `apps/backend/src/main/resources/application.yml`

- [ ] **Step 1: Parallelise the 5 remaining sequential delete loops in ProjectService.java**

In `deleteProject` (starting around line 220), replace the 5 sequential `forEach` delete calls with parallel versions. The current sequential code looks like:

```java
// BEFORE (sequential — lines 255-296):
testRunService.getAllTestRunsByProjectId(id)
        .forEach(run -> testRunService.deleteTestRun(run.getId()));

// ...
suiteService.getAllSuitesByProjectId(id)
        .forEach(suite -> entityService.deleteById(suite.getId()));

defectService.getAllDefectsByProjectId(id)
        .forEach(d -> defectService.deleteDefect(d.getId()));

attachmentService.getAllAttachmentsByProjectId(id)
        .forEach(a -> attachmentService.deleteAttachment(a.getId()));

reportService.getAllReportsByProjectId(id)
        .forEach(r -> reportService.deleteReport(r.getId()));
```

Replace all five with this block (insert after the existing TestRunCase parallel block, before Phase 2 comment):

```java
// ── Parallel TestRun deletion ─────────────────────────────────────────────────
var allRuns = testRunService.getAllTestRunsByProjectId(id);
logger.info("[Project] Cascade: {} TestRun(s) to delete", allRuns.size());
if (!allRuns.isEmpty()) {
    var runFutures = allRuns.stream()
            .map(run -> CompletableFuture.runAsync(() -> {
                try {
                    testRunService.deleteTestRun(run.getId());
                } catch (Exception e) {
                    logger.error("[Project] Failed to delete TestRun {}: {}", run.getId(), e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }))
            .toList();
    try {
        CompletableFuture.allOf(runFutures.toArray(new CompletableFuture[0])).join();
    } catch (Exception e) {
        throw new RuntimeException("Cascade delete failed at TestRun phase: " + e.getMessage(), e);
    }
}
```

And replace the three remaining sequential loops in Phase 3 with:

```java
// ── Phase 3: Defects, attachments, reports, counter ──────────────────────────
var allDefects = defectService.getAllDefectsByProjectId(id);
var allAttachments = attachmentService.getAllAttachmentsByProjectId(id);
var allReports = reportService.getAllReportsByProjectId(id);

List<CompletableFuture<Void>> phase3Futures = new java.util.ArrayList<>();

allDefects.forEach(d -> phase3Futures.add(CompletableFuture.runAsync(() -> {
    try { defectService.deleteDefect(d.getId()); }
    catch (Exception e) { throw new RuntimeException("Delete defect failed: " + d.getId(), e); }
})));
allAttachments.forEach(a -> phase3Futures.add(CompletableFuture.runAsync(() -> {
    try { attachmentService.deleteAttachment(a.getId()); }
    catch (Exception e) { throw new RuntimeException("Delete attachment failed: " + a.getId(), e); }
})));
allReports.forEach(r -> phase3Futures.add(CompletableFuture.runAsync(() -> {
    try { reportService.deleteReport(r.getId()); }
    catch (Exception e) { throw new RuntimeException("Delete report failed: " + r.getId(), e); }
})));

try {
    CompletableFuture.allOf(phase3Futures.toArray(new CompletableFuture[0])).join();
} catch (Exception e) {
    throw new RuntimeException("Cascade delete failed at Phase 3: " + e.getMessage(), e);
}
```

Also replace the sequential suites loop (between Phase 2 case deletion and Phase 3):
```java
// replace: suiteService.getAllSuitesByProjectId(id).forEach(suite -> entityService.deleteById(suite.getId()));
var allSuites = suiteService.getAllSuitesByProjectId(id);
if (!allSuites.isEmpty()) {
    var suiteFutures = allSuites.stream()
            .map(suite -> CompletableFuture.runAsync(() -> entityService.deleteById(suite.getId())))
            .toList();
    CompletableFuture.allOf(suiteFutures.toArray(new CompletableFuture[0])).join();
}
```

- [ ] **Step 2: Run all backend tests to confirm correctness**

```bash
./gradlew :apps:backend:test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Apply config tweaks in application.yml**

In `apps/backend/src/main/resources/application.yml`, make two changes:

Change line 22 from:
```yaml
      resolve-lazily: false
```
to:
```yaml
      resolve-lazily: true
```

Change line 123 from:
```yaml
    grpc-keep-alive-time-seconds: 15                # More frequent keep-alives (was 30s)
```
to:
```yaml
    grpc-keep-alive-time-seconds: 30
```

- [ ] **Step 4: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/service/ProjectService.java \
        apps/backend/src/main/resources/application.yml
git commit -m "perf(delete): parallelise cascade delete for runs/suites/defects/attachments/reports"
```

---

## Task 6: React.memo on DefectAttachmentsList

**Files:**
- Modify: `apps/frontend/src/pages/RunExecution.tsx`
- Modify: `apps/frontend/src/pages/Defects.tsx`

- [ ] **Step 1: Wrap DefectAttachmentsList in RunExecution.tsx with React.memo**

In `apps/frontend/src/pages/RunExecution.tsx`, ensure `React` is imported (add `import React from 'react';` if not present, or add `memo` to the existing React import).

Change line 139 from:
```tsx
const DefectAttachmentsList = ({ projectId, defectId }: { projectId: string; defectId: string }) => {
```
to:
```tsx
const DefectAttachmentsList = React.memo(({ projectId, defectId }: { projectId: string; defectId: string }) => {
```

And add the closing `)` after the component's closing `};` at line 168:
```tsx
});
```

The final shape should be:
```tsx
const DefectAttachmentsList = React.memo(({ projectId, defectId }: { projectId: string; defectId: string }) => {
  const [attachments, setAttachments] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setIsLoading(true);
    attachmentsApi.listByDefect(projectId, defectId)
      .then((atts) => setAttachments(atts || []))
      .catch(() => setAttachments([]))
      .finally(() => setIsLoading(false));
  }, [projectId, defectId]);

  if (isLoading) return <div className="text-sm text-muted-foreground">Loading...</div>;
  if (!attachments || attachments.length === 0) return <p className="text-sm text-muted-foreground mt-1.5">No attachments</p>;

  return (
    <div className="mt-2 space-y-1.5">
      {attachments.map((att) => {
        const IconComp = getFileIcon(att.fileType || att.type || '');
        return (
          <div key={att.id} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
            <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
            <span className="text-sm text-foreground truncate flex-1">{att.fileName || att.name}</span>
            <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(att.fileSize || att.size || 0)}</span>
          </div>
        );
      })}
    </div>
  );
});
```

- [ ] **Step 2: Wrap DefectAttachmentsList in Defects.tsx with React.memo**

In `apps/frontend/src/pages/Defects.tsx`, change line 51 from:
```tsx
const DefectAttachmentsList = ({ projectId, defectId }: DefectAttachmentsListProps) => {
```
to:
```tsx
const DefectAttachmentsList = React.memo(({ projectId, defectId }: DefectAttachmentsListProps) => {
```

And change the closing `};` at line 85 to `});`.

Ensure `React` is imported at the top: add `import React from 'react';` if the file only has named imports like `import { useState, useEffect } from 'react'`. Otherwise add `React` to the existing named import: `import React, { useState, useEffect } from 'react';`.

- [ ] **Step 3: Run frontend tests**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run test -- --run 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/frontend/src/pages/RunExecution.tsx \
        apps/frontend/src/pages/Defects.tsx
git commit -m "perf(fe): wrap DefectAttachmentsList with React.memo to prevent O(n) refetches"
```

---

## Task 7: Frontend API + overwrite flow + staleTime fix

**Files:**
- Modify: `apps/frontend/src/lib/api.ts`
- Modify: `apps/frontend/src/pages/Repository.tsx`
- Modify: `apps/frontend/src/hooks/useApi.ts`

- [ ] **Step 1: Add testStepsApi.replace() to api.ts**

In `apps/frontend/src/lib/api.ts`, find the `testStepsApi` object (around line 226). Add a `replace` method after the existing `delete` method:

```ts
export const testStepsApi = {
  list:   (projectId: string, suiteId: string, caseId: string) =>
    api.get<TestStep[]>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps`),
  create: (projectId: string, suiteId: string, caseId: string, body: Partial<TestStep>) =>
    api.post<TestStep>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps`, body),
  update: (projectId: string, suiteId: string, caseId: string, id: string, body: Partial<TestStep>) =>
    api.put<TestStep>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps/${id}`, body),
  delete: (projectId: string, suiteId: string, caseId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps/${id}`),
  replace: (projectId: string, suiteId: string, caseId: string, steps: Array<{ stepNumber: number; action: string; expectedResult: string }>) =>
    api.put<TestStep[]>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps/replace`, steps),
};
```

- [ ] **Step 2: Replace the overwrite step loop in Repository.tsx**

In `apps/frontend/src/pages/Repository.tsx`, find the overwrite section (around lines 836–866). Replace the existing step-replacement block:

```ts
// BEFORE (find this block):
if (ow.data.steps && ow.data.steps.length > 0) {
  const existingSteps = await testStepsApi.list(projectId, ow.existingSuiteId, ow.existingCaseId);
  await Promise.all(
    existingSteps.map(s =>
      testStepsApi.delete(projectId, ow.existingSuiteId, ow.existingCaseId, s.id),
    ),
  );
  for (const step of ow.data.steps) {
    await testStepsApi.create(projectId, ow.existingSuiteId, ow.existingCaseId, {
      stepNumber: step.order || 1,
      action: step.action,
      expectedResult: step.expectedResult,
    });
  }
}
```

Replace with:

```ts
// AFTER: single HTTP call replaces all steps atomically
if (ow.data.steps !== undefined) {
  const stepsPayload = (ow.data.steps || []).map(s => ({
    stepNumber:     s.order || 1,
    action:         s.action,
    expectedResult: s.expectedResult,
  }));
  await testStepsApi.replace(projectId, ow.existingSuiteId, ow.existingCaseId, stepsPayload);
}
```

- [ ] **Step 3: Fix staleTime in useApi.ts**

In `apps/frontend/src/hooks/useApi.ts`, find `useDefectsByRun` (around line 717). Change:

```ts
staleTime: 0,
refetchOnMount: true,
```
to:
```ts
staleTime: 5_000,
refetchOnMount: 'always',
```

- [ ] **Step 4: Run frontend tests**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run test -- --run 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/frontend/src/lib/api.ts \
        apps/frontend/src/pages/Repository.tsx \
        apps/frontend/src/hooks/useApi.ts
git commit -m "perf(fe): use replaceSteps API for import overwrites, fix staleTime on useDefectsByRun"
```

---

## Task 8: Code splitting — lazy load heavy pages

**Files:**
- Modify: `apps/frontend/src/App.tsx`
- Modify: `apps/frontend/vite.config.ts`

- [ ] **Step 1: Apply manualChunks to vite.config.ts**

Replace the entire content of `apps/frontend/vite.config.ts` with:

```ts
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { componentTagger } from "lovable-tagger";

export default defineConfig(({ mode }) => ({
  server: {
    host: "::",
    port: 5173,
    hmr: {
      overlay: false,
    },
  },
  plugins: [react(), mode === "development" && componentTagger()].filter(Boolean),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
    dedupe: ["react", "react-dom", "react/jsx-runtime", "react/jsx-dev-runtime"],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("recharts") || id.includes("d3-") || id.includes("victory-")) {
            return "vendor-recharts";
          }
          if (id.includes("@radix-ui")) {
            return "vendor-radix";
          }
          if (id.includes("node_modules")) {
            return "vendor";
          }
        },
      },
    },
  },
}));
```

- [ ] **Step 2: Add lazy imports and Suspense to App.tsx**

Replace the heavy page imports at the top of `apps/frontend/src/App.tsx`:

```tsx
// Replace these static imports:
import Repository from "./pages/Repository";
import RunExecution from "./pages/RunExecution";
import Defects from "./pages/Defects";
import Attachments from "./pages/Attachments";
import Reports from "./pages/Reports";
import CreateReport from "./pages/CreateReport";
import ReportDetail from "./pages/ReportDetail";
```

With:
```tsx
import { lazy, Suspense } from "react";

const Repository   = lazy(() => import("./pages/Repository"));
const RunExecution = lazy(() => import("./pages/RunExecution"));
const Defects      = lazy(() => import("./pages/Defects"));
const Attachments  = lazy(() => import("./pages/Attachments"));
const Reports      = lazy(() => import("./pages/Reports"));
const CreateReport = lazy(() => import("./pages/CreateReport"));
const ReportDetail = lazy(() => import("./pages/ReportDetail"));
```

Keep the non-lazy imports (Login, Projects, ProjectLayout, TestRuns, CreateTestRun, NotFound) as static imports — they are small and needed on first render.

Then wrap every `<Route>` that uses a lazy component with `<Suspense>`. Add a `PageLoader` constant before the `App` component:

```tsx
const PageLoader = () => (
  <div className="min-h-screen flex items-center justify-center">
    <span className="text-muted-foreground text-sm">Loading…</span>
  </div>
);
```

And wrap each lazy route:
```tsx
<Route path="repository" element={
  <Suspense fallback={<PageLoader />}><Repository /></Suspense>
} />
<Route path="runs/:runId" element={
  <Suspense fallback={<PageLoader />}><RunExecution /></Suspense>
} />
<Route path="defects" element={
  <Suspense fallback={<PageLoader />}><Defects /></Suspense>
} />
<Route path="attachments" element={
  <Suspense fallback={<PageLoader />}><Attachments /></Suspense>
} />
<Route path="reports" element={
  <Suspense fallback={<PageLoader />}><Reports /></Suspense>
} />
<Route path="reports/create" element={
  <Suspense fallback={<PageLoader />}><CreateReport /></Suspense>
} />
<Route path="reports/:reportId" element={
  <Suspense fallback={<PageLoader />}><ReportDetail /></Suspense>
} />
```

Keep non-lazy routes (`runs`, `runs/create`) without Suspense.

- [ ] **Step 3: Run frontend tests**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run test -- --run 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 4: Build frontend to confirm no bundle errors**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run build 2>&1 | tail -30
```

Expected: `✓ built in` with multiple chunk files visible (vendor-recharts, vendor-radix, vendor, index).

- [ ] **Step 5: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/frontend/src/App.tsx \
        apps/frontend/vite.config.ts
git commit -m "perf(fe): add code splitting with React.lazy and Vite manualChunks"
```

---

## Self-Review

**Spec coverage check:**

| Spec item | Task |
|---|---|
| Parallelise batchCreateTestCases | Task 1 ✓ |
| replaceSteps endpoint (backend) | Tasks 2–3 ✓ |
| Spring Cache for TestRun queries | Task 4 ✓ |
| Parallelise cascade delete loops | Task 5 ✓ |
| resolve-lazily=true | Task 5 ✓ |
| grpc-keep-alive=30s | Task 5 ✓ |
| React.memo on DefectAttachmentsList | Task 6 ✓ |
| replaceSteps in frontend | Task 7 ✓ |
| staleTime fix on useDefectsByRun | Task 7 ✓ |
| Code splitting + manualChunks | Task 8 ✓ |

**Placeholder scan:** No TBDs or incomplete steps found.

**Type consistency:**
- `StepReplaceDTO` defined in Task 2, used in Tasks 3 and 7 — consistent.
- `testStepsApi.replace()` defined in Task 7 Step 1, used in Task 7 Step 2 — consistent.
- Cache names `testRunsByProject` and `allTestRunsByProject` defined in `CacheConfig` (Task 4), used in `@Cacheable`/`@CacheEvict` annotations (Task 4) — consistent.

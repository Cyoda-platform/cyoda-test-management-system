# Performance Fixes Round 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Caffeine cache to hot services, replace ObjectMapper with TextNode in conditionByField, raise thread pool defaults, and stabilise useMemo deps in RunExecution.

**Architecture:** Four independent tasks in a single worktree branch. Backend tasks 1–3 touch only service/config files. Frontend task 4 touches one component. No shared state between tasks.

**Tech Stack:** Java 21, Spring Cache + Caffeine, `com.fasterxml.jackson.databind.node.TextNode`, React 18 useMemo.

---

## File Map

| File | Task | Change |
|---|---|---|
| `apps/backend/src/main/java/com/java_template/application/config/CacheConfig.java` | 1 | Add 4 new cache names, raise maxSize to 1000 |
| `apps/backend/src/main/java/com/java_template/application/service/SuiteService.java` | 1+2 | @Cacheable + TextNode in conditionByField |
| `apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java` | 1+2 | @Cacheable + TextNode in conditionByField only |
| `apps/backend/src/main/java/com/java_template/application/service/TestStepService.java` | 1+2 | @Cacheable + TextNode in conditionByField |
| `apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java` | 1+2 | @Cacheable + TextNode in conditionByField |
| `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java` | 2 | TextNode in conditionByField |
| `apps/backend/src/main/java/com/java_template/application/service/AttachmentService.java` | 2 | TextNode in conditionByField |
| `apps/backend/src/main/java/com/java_template/application/service/DefectService.java` | 2 | TextNode in conditionByField |
| `apps/backend/src/test/java/com/java_template/application/service/ServiceCacheTest.java` | 1 | New cache hit tests for all 4 services |
| `apps/backend/src/main/resources/application.yml` | 3 | processor/criteria thread pools 20→40 |
| `apps/frontend/src/pages/RunExecution.tsx` | 4 | Stable useMemo deps via caseIdsKey + runCases.length |

---

## Task 1: Add cache to SuiteService, TestCaseService, TestStepService, TestRunCaseService

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/config/CacheConfig.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/SuiteService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestStepService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java`
- Create: `apps/backend/src/test/java/com/java_template/application/service/ServiceCacheTest.java`

- [ ] **Step 1: Expand CacheConfig.java with 4 new cache names**

Replace the entire `cacheManager()` bean in `CacheConfig.java`:

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager(
            "testRunsByProject",
            "allTestRunsByProject",
            "suitesByProject",
            "casesBySuite",
            "stepsByCase",
            "runCasesByRun"
    );
    manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(1000));
    return manager;
}
```

- [ ] **Step 2: Annotate SuiteService**

Add these imports to `SuiteService.java`:
```java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
```

Add `@Cacheable` on `getAllSuitesByProjectId` (line ~137):
```java
@Cacheable(value = "suitesByProject", key = "#projectId")
public List<SuiteDTO> getAllSuitesByProjectId(UUID projectId) {
```

Add `@CacheEvict` on `createSuite`, `updateSuite`, `deleteSuite`:
```java
@CacheEvict(value = "suitesByProject", allEntries = true)
public SuiteDTO createSuite(SuiteDTO suite) {
```
```java
@CacheEvict(value = "suitesByProject", allEntries = true)
public SuiteDTO updateSuite(UUID id, SuiteDTO suite) {
```
```java
@CacheEvict(value = "suitesByProject", allEntries = true)
public boolean deleteSuite(UUID id) {
```

- [ ] **Step 3: Annotate TestCaseService**

Add imports to `TestCaseService.java`:
```java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
```

Add `@Cacheable` on `getTestCasesBySuiteId` (line ~116):
```java
@Cacheable(value = "casesBySuite", key = "#suiteId + ':' + #page + ':' + #size")
public PageResult<TestCaseDTO> getTestCasesBySuiteId(UUID suiteId, int page, int size) {
```

Add `@CacheEvict` on mutating methods. Use `allEntries = true` since keys include suiteId which may not be available on updates/deletes:
```java
@CacheEvict(value = "casesBySuite", allEntries = true)
public TestCaseDTO createTestCase(TestCaseDTO testCase) {
```
```java
@CacheEvict(value = "casesBySuite", allEntries = true)
public TestCaseDTO updateTestCase(UUID id, TestCaseDTO testCase) {
```
```java
@CacheEvict(value = "casesBySuite", allEntries = true)
public boolean softDeleteTestCase(UUID id) {
```
```java
@CacheEvict(value = "casesBySuite", allEntries = true)
public TestCaseDTO moveTestCase(UUID id, UUID targetSuiteId, Integer sortOrder) {
```
```java
@CacheEvict(value = "casesBySuite", allEntries = true)
public List<TestCaseDTO> batchCreateTestCases(UUID projectId, UUID suiteId,
                                              List<BatchImportCaseDTO> items) {
```

- [ ] **Step 4: Annotate TestStepService**

Add imports to `TestStepService.java`:
```java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
```

Add `@Cacheable` on `getTestStepsByTestCaseId` (line ~78):
```java
@Cacheable(value = "stepsByCase", key = "#testCaseId")
public List<TestStepDTO> getTestStepsByTestCaseId(UUID testCaseId) {
```

Add `@CacheEvict` on mutating methods:
```java
@CacheEvict(value = "stepsByCase", allEntries = true)
public TestStepDTO createTestStep(TestStepDTO testStep) {
```
```java
@CacheEvict(value = "stepsByCase", allEntries = true)
public TestStepDTO updateTestStep(UUID id, TestStepDTO testStep) {
```
```java
@CacheEvict(value = "stepsByCase", allEntries = true)
public boolean deleteTestStep(UUID id) {
```
```java
@CacheEvict(value = "stepsByCase", allEntries = true)
public List<TestStepDTO> replaceSteps(UUID caseId, List<StepReplaceDTO> newSteps) {
```

- [ ] **Step 5: Annotate TestRunCaseService**

Add imports to `TestRunCaseService.java`:
```java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
```

Add `@Cacheable` on `getAllTestRunCasesByTestRunId` (line ~284):
```java
@Cacheable(value = "runCasesByRun", key = "#testRunId")
public List<TestRunCaseDTO> getAllTestRunCasesByTestRunId(UUID testRunId) {
```

Add `@CacheEvict` on mutating methods:
```java
@CacheEvict(value = "runCasesByRun", allEntries = true)
public TestRunCaseDTO createTestRunCase(TestRunCaseDTO testRunCase) {
```
```java
@CacheEvict(value = "runCasesByRun", allEntries = true)
public TestRunCaseDTO createTestRunCaseWithSteps(TestRunCaseDTO testRunCase,
```
```java
@CacheEvict(value = "runCasesByRun", allEntries = true)
public TestRunCaseDTO createTestRunCaseWithStepSnapshots(TestRunCaseDTO testRunCase,
```
```java
@CacheEvict(value = "runCasesByRun", allEntries = true)
public void deleteTestRunCase(UUID id) {
```
```java
@CacheEvict(value = "runCasesByRun", allEntries = true)
public Optional<TestRunCaseDTO> updateTestRunCaseStatus(UUID id, String status) {
```

- [ ] **Step 6: Write cache hit tests**

Create `apps/backend/src/test/java/com/java_template/application/service/ServiceCacheTest.java`:

```java
package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.config.CacheConfig;
import com.java_template.application.dto.SuiteDTO;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        SuiteService.class,
        TestCaseService.class,
        TestStepService.class,
        TestRunCaseService.class,
        CacheConfig.class
})
public class ServiceCacheTest {

    @MockBean EntityService entityService;
    @MockBean ObjectMapper objectMapper;
    @MockBean ProjectCounterService projectCounterService;
    @MockBean TestStepService testStepServiceMock; // avoid circular; unused here

    @Autowired SuiteService suiteService;
    @Autowired TestCaseService testCaseService;
    @Autowired TestStepService testStepService;
    @Autowired TestRunCaseService testRunCaseService;

    private <T> EntityWithMetadata<T> wrap(T dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void getAllSuitesByProjectId_hitsCacheOnSecondCall() {
        UUID projectId = UUID.randomUUID();
        SuiteDTO suite = new SuiteDTO();
        suite.setProjectId(projectId);
        PageResult<EntityWithMetadata<SuiteDTO>> page =
                PageResult.of(null, List.of(wrap(suite, UUID.randomUUID())), 0, 1000, 1L);
        when(entityService.search(any(), any(), eq(SuiteDTO.class))).thenReturn(page);

        suiteService.getAllSuitesByProjectId(projectId);
        suiteService.getAllSuitesByProjectId(projectId);

        verify(entityService, times(1)).search(any(), any(), eq(SuiteDTO.class));
    }

    @Test
    public void getTestCasesBySuiteId_hitsCacheOnSecondCall() {
        UUID suiteId = UUID.randomUUID();
        TestCaseDTO tc = new TestCaseDTO();
        tc.setSuiteId(suiteId);
        PageResult<EntityWithMetadata<TestCaseDTO>> page =
                PageResult.of(null, List.of(wrap(tc, UUID.randomUUID())), 0, 20, 1L);
        when(entityService.search(any(), any(), eq(TestCaseDTO.class))).thenReturn(page);

        testCaseService.getTestCasesBySuiteId(suiteId, 0, 20);
        testCaseService.getTestCasesBySuiteId(suiteId, 0, 20);

        verify(entityService, times(1)).search(any(), any(), eq(TestCaseDTO.class));
    }

    @Test
    public void getTestStepsByTestCaseId_hitsCacheOnSecondCall() {
        UUID caseId = UUID.randomUUID();
        TestStepDTO step = new TestStepDTO();
        step.setTestCaseId(caseId);
        PageResult<EntityWithMetadata<TestStepDTO>> page =
                PageResult.of(null, List.of(wrap(step, UUID.randomUUID())), 0, 1000, 1L);
        when(entityService.search(any(), any(), eq(TestStepDTO.class))).thenReturn(page);

        testStepService.getTestStepsByTestCaseId(caseId);
        testStepService.getTestStepsByTestCaseId(caseId);

        verify(entityService, times(1)).search(any(), any(), eq(TestStepDTO.class));
    }

    @Test
    public void getAllTestRunCasesByTestRunId_hitsCacheOnSecondCall() {
        UUID runId = UUID.randomUUID();
        TestRunCaseDTO rc = new TestRunCaseDTO();
        PageResult<EntityWithMetadata<TestRunCaseDTO>> page =
                PageResult.of(null, List.of(wrap(rc, UUID.randomUUID())), 0, 1000, 1L);
        when(entityService.search(any(), any(), eq(TestRunCaseDTO.class))).thenReturn(page);

        testRunCaseService.getAllTestRunCasesByTestRunId(runId);
        testRunCaseService.getAllTestRunCasesByTestRunId(runId);

        verify(entityService, times(1)).search(any(), any(), eq(TestRunCaseDTO.class));
    }
}
```

**Note:** If `@SpringBootTest` complains about circular dependencies or missing beans, split into one test class per service (follow the exact pattern of `TestRunServiceCacheTest.java` — that file uses `@SpringBootTest(classes = {ServiceUnderTest.class, CacheConfig.class})`).

- [ ] **Step 7: Run tests to verify green**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
./gradlew :apps:backend:test 2>&1 | tail -8
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/config/CacheConfig.java \
        apps/backend/src/main/java/com/java_template/application/service/SuiteService.java \
        apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java \
        apps/backend/src/main/java/com/java_template/application/service/TestStepService.java \
        apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java \
        apps/backend/src/test/java/com/java_template/application/service/ServiceCacheTest.java
git commit -m "perf(cache): add Caffeine cache to SuiteService, TestCaseService, TestStepService, TestRunCaseService"
```

---

## Task 2: Replace ObjectMapper.valueToTree with TextNode in conditionByField

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/service/SuiteService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestStepService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/AttachmentService.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/DefectService.java`

**What to change:** In each service, find the `conditionByField` method. Replace:
```java
.value(objectMapper.valueToTree(value))
```
with:
```java
.value(com.fasterxml.jackson.databind.node.TextNode.valueOf(value.toString()))
```

**Important:** Change ONLY the `conditionByField` method. Do NOT change inline condition-building code that uses `objectMapper.valueToTree(false)` (boolean) — those must remain as-is.

- [ ] **Step 1: Apply TextNode in SuiteService.conditionByField**

In `SuiteService.java`, the `conditionByField` method (line ~49):
```java
private GroupConditionDto conditionByField(String fieldName, Object value) {
    SimpleConditionDto condition = new SimpleConditionDto()
            .jsonPath("$." + fieldName)
            .operation(OperatorTypeDto.EQUALS)
            .value(com.fasterxml.jackson.databind.node.TextNode.valueOf(value.toString()));
    condition.setType(QueryConditionTypeDto.SIMPLE);
    GroupConditionDto group = new GroupConditionDto()
            .operator(GroupOperatorDto.AND)
            .conditions(List.of(condition));
    group.setType(QueryConditionTypeDto.GROUP);
    return group;
}
```

- [ ] **Step 2: Apply TextNode in TestCaseService.conditionByField**

In `TestCaseService.java`, only the `conditionByField` method (around line 52). Leave the inline conditions that use `objectMapper.valueToTree(false)` (lines 127, 156, 239) unchanged:
```java
private GroupConditionDto conditionByField(String fieldName, Object value) {
    SimpleConditionDto condition = new SimpleConditionDto()
            .jsonPath("$." + fieldName)
            .operation(OperatorTypeDto.EQUALS)
            .value(com.fasterxml.jackson.databind.node.TextNode.valueOf(value.toString()));
    condition.setType(QueryConditionTypeDto.SIMPLE);
    GroupConditionDto group = new GroupConditionDto()
            .operator(GroupOperatorDto.AND)
            .conditions(List.of(condition));
    group.setType(QueryConditionTypeDto.GROUP);
    return group;
}
```

- [ ] **Step 3: Apply TextNode in TestStepService.conditionByField**

In `TestStepService.java`, the `conditionByField` method (line ~36). Note: `getTestStepsByTestCaseId` uses a hand-built two-field condition (not `conditionByField`) — leave that unchanged. Only change `conditionByField`:
```java
private GroupConditionDto conditionByField(String fieldName, Object value) {
    SimpleConditionDto condition = new SimpleConditionDto()
            .jsonPath("$." + fieldName)
            .operation(OperatorTypeDto.EQUALS)
            .value(com.fasterxml.jackson.databind.node.TextNode.valueOf(value.toString()));
    condition.setType(QueryConditionTypeDto.SIMPLE);
    GroupConditionDto group = new GroupConditionDto()
            .operator(GroupOperatorDto.AND)
            .conditions(List.of(condition));
    group.setType(QueryConditionTypeDto.GROUP);
    return group;
}
```

- [ ] **Step 4: Apply TextNode in TestRunCaseService.conditionByField**

Same change in `TestRunCaseService.java` `conditionByField` method (line ~61).

- [ ] **Step 5: Apply TextNode in TestRunService.conditionByField**

Same change in `TestRunService.java` `conditionByField` method (line ~44).

- [ ] **Step 6: Apply TextNode in AttachmentService and DefectService**

Read `AttachmentService.java` and `DefectService.java`, find their `conditionByField` methods, apply the same TextNode replacement. Do not touch any other objectMapper usages.

- [ ] **Step 7: Run all tests to confirm green**

```bash
./gradlew :apps:backend:test 2>&1 | tail -8
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/service/SuiteService.java \
        apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java \
        apps/backend/src/main/java/com/java_template/application/service/TestStepService.java \
        apps/backend/src/main/java/com/java_template/application/service/TestRunCaseService.java \
        apps/backend/src/main/java/com/java_template/application/service/TestRunService.java \
        apps/backend/src/main/java/com/java_template/application/service/AttachmentService.java \
        apps/backend/src/main/java/com/java_template/application/service/DefectService.java
git commit -m "perf(search): replace objectMapper.valueToTree with TextNode in conditionByField"
```

---

## Task 3: Raise thread pool defaults in application.yml

**File:**
- Modify: `apps/backend/src/main/resources/application.yml`

- [ ] **Step 1: Update thread pool values**

In `apps/backend/src/main/resources/application.yml`, find the thread pool block (lines ~128–130):

```yaml
# Thread Pool Configuration
processor-thread-pool: 20
criteria-thread-pool: 20
control-thread-pool: 3
```

Change to:

```yaml
# Thread Pool Configuration
# Override via PROCESSOR_THREAD_POOL / CRITERIA_THREAD_POOL env vars for larger instances.
processor-thread-pool: ${PROCESSOR_THREAD_POOL:40}
criteria-thread-pool:  ${CRITERIA_THREAD_POOL:40}
control-thread-pool:   ${CONTROL_THREAD_POOL:3}
```

- [ ] **Step 2: Run all backend tests to confirm nothing broke**

```bash
./gradlew :apps:backend:test 2>&1 | tail -8
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add apps/backend/src/main/resources/application.yml
git commit -m "perf(config): raise thread pool defaults to 40, make overridable via env vars"
```

---

## Task 4: Stable useMemo dependencies in RunExecution.tsx

**File:**
- Modify: `apps/frontend/src/pages/RunExecution.tsx`

- [ ] **Step 1: Locate the filteredSuitesWithCases useMemo (line ~225)**

Read `apps/frontend/src/pages/RunExecution.tsx` around lines 215–255. Find:
```tsx
const filteredSuitesWithCases = useMemo(() => {
  // ...
}, [getRunCaseTestCaseId, suitesWithCases, run?.caseIds, runCases]);
```

- [ ] **Step 2: Add caseIdsKey and replace unstable deps**

Add `caseIdsKey` immediately before `filteredSuitesWithCases`:
```tsx
const caseIdsKey = run?.caseIds?.join(',') ?? '';
```

Then change the `useMemo` dependency array from:
```tsx
}, [getRunCaseTestCaseId, suitesWithCases, run?.caseIds, runCases]);
```
to:
```tsx
}, [getRunCaseTestCaseId, suitesWithCases, caseIdsKey, runCases.length]);
```

The logic inside the memo body is unchanged. `runCases.length` is safe because the memo only needs to re-run when the number of run cases changes, not on every TanStack Query cache invalidation that creates a new array reference.

- [ ] **Step 3: Run frontend tests**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run test -- --run 2>&1 | tail -8
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/frontend/src/pages/RunExecution.tsx
git commit -m "perf(fe): stabilise filteredSuitesWithCases useMemo deps via caseIdsKey + runCases.length"
```

---

## Self-Review

**Spec coverage:**

| Spec requirement | Task |
|---|---|
| Cache on SuiteService (suitesByProject) | Task 1 ✓ |
| Cache on TestCaseService (casesBySuite) | Task 1 ✓ |
| Cache on TestStepService (stepsByCase) | Task 1 ✓ |
| Cache on TestRunCaseService (runCasesByRun) | Task 1 ✓ |
| CacheConfig updated with 4 new names, maxSize 1000 | Task 1 ✓ |
| TextNode in conditionByField across all services | Task 2 ✓ |
| Thread pools 20→40, env-var overridable | Task 3 ✓ |
| Stable useMemo deps in RunExecution | Task 4 ✓ |

**Placeholder scan:** No TBDs or vague steps found.

**Type consistency:**
- Cache names in CacheConfig (`suitesByProject`, `casesBySuite`, `stepsByCase`, `runCasesByRun`) match `@Cacheable` annotations in all tasks.
- `caseIdsKey` defined before use in Task 4.

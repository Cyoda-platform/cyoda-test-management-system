# Design: Priority 1 Test Coverage — Processors, Controllers, E2E

**Date:** 2026-04-30  
**Status:** Approved  
**Scope:** Unit tests for 5 FSM processors, unit tests for 3 TestRun-domain controllers, E2E Gherkin for TestRun lifecycle

---

## 1. Context

Analysis (`docs/TEST_COVERAGE_ANALYSIS.md`) identified three critical gaps:

- **Processors:** 5/6 untested. All are currently stub implementations returning `success = true`.
- **Controllers:** TestRun domain (TestRunController, TestRunCaseController, TestRunStepController) has zero unit tests despite being the core feature of the application.
- **E2E:** No Gherkin scenarios for TestRun workflow; only `health-check`, `project-crud`, and `defect-crud` are covered.

---

## 2. Design Decisions

### Processors: Contract tests only (Option A)

All 5 processors are stubs. Tests verify the **public contract**:
- `supports()` returns `true` for the correct operation name, `false` for anything else
- `process()` returns `success = true` with request metadata (id, requestId, entityId) echoed back

This creates "anchor tests" — when stubs are replaced with real logic, the contract tests stay green and the developer adds new tests for business behaviour.

No `EntityService` dependency in any processor (architectural constraint of Cyoda FSM — processors cannot call EntityService on the entity being processed). Tests instantiate processors with `new`, no Spring context.

### Controllers: MockMvc without Spring context

Follow `ExampleEntityControllerTest.java` pattern exactly:
- `MockitoAnnotations.openMocks(this)` + `MockMvcBuilders.standaloneSetup(controller)`
- Mock all service dependencies
- Cover happy paths, not-found, ID-mismatch (projectId/runId/caseId filter), RBAC (unlock endpoint), validation (400), and multipart upload

### E2E: Smoke + full @requires-cyoda lifecycle (Option B)

- `@smoke` scenarios run without a live Cyoda instance — test auth, validation, RBAC
- `@requires-cyoda` scenarios cover the full TestRun lifecycle against a live Cyoda instance
- Reuse `TmsHttpClient` and `E2eTestConfig` from existing step infrastructure

---

## 3. Deliverables

### 3.1 Processor Unit Tests (5 files)

**Location:** `apps/backend/src/test/java/com/java_template/application/processor/`

| File | Tests |
|------|-------|
| `AtomicFailureProcessorTest.java` | 5 |
| `BugLinkProcessorTest.java` | 5 |
| `EdgeMessageProcessorTest.java` | 5 |
| `MetricsAggregatorProcessorTest.java` | 5 |
| `TestRunCompleteProcessorTest.java` | 5 |

**Test methods per file (identical structure):**

```
supports_correctOperationName_returnsTrue
supports_incorrectOperationName_returnsFalse
supports_emptyOperationName_returnsFalse
process_validRequest_returnsSuccess
process_validRequest_preservesRequestMetadata
```

**setUp pattern:**
```java
processor = new XxxProcessor();  // no dependencies
```

**Context helper (anonymous class, same as ExampleEntityProcessorTest):**
```java
private CyodaEventContext<EntityProcessorCalculationRequest> createContext(
        EntityProcessorCalculationRequest request) {
    return new CyodaEventContext<>() {
        @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
        @Override public EntityProcessorCalculationRequest getEvent() { return request; }
    };
}
```

**Request builder:**
```java
private EntityProcessorCalculationRequest createRequest() {
    var request = new EntityProcessorCalculationRequest();
    request.setId("req-id");
    request.setRequestId("req-456");
    request.setEntityId(UUID.randomUUID());
    return request;
}
```

### 3.2 Controller Unit Tests (3 files)

**Location:** `apps/backend/src/test/java/com/java_template/application/controller/`

#### TestRunControllerTest.java (15 tests)

Mocks: `TestRunService`, `TestRunCaseService`  
Base URL: `/projects/{projectId}/runs`

| Test method | Endpoint | Expected |
|-------------|----------|----------|
| `createTestRun_validInput_returns201` | POST | 201 + body |
| `createTestRun_blankName_returns400` | POST | 400 |
| `getTestRunsByProject_returns200WithPage` | GET | 200 + PageResult |
| `getTestRun_found_sameProject_returns200` | GET `/{id}` | 200 |
| `getTestRun_projectMismatch_returns404` | GET `/{id}` | projectId differs → 404 |
| `getTestRun_notFound_returns404` | GET `/{id}` | 404 |
| `getTestRunDetails_found_returns200WithCases` | GET `/{id}/details` | 200 + run + cases |
| `getTestRunDetails_notFound_returns404` | GET `/{id}/details` | 404 |
| `updateTestRun_exists_returns200` | PUT `/{id}` | 200 |
| `updateTestRun_notFound_returns404` | PUT `/{id}` | 404 |
| `completeTestRun_found_returns200` | POST `/{id}/complete` | 200 |
| `completeTestRun_notFound_returns404` | POST `/{id}/complete` | 404 |
| `unlockTestRun_adminRole_returns200` | POST `/{id}/unlock` | role=Admin → 200 |
| `unlockTestRun_testerRole_returns200` | POST `/{id}/unlock` | role=Tester → 200 |
| `unlockTestRun_noRole_returns403` | POST `/{id}/unlock` | no role → 403 |
| `deleteTestRun_exists_returns204` | DELETE `/{id}` | 204 |
| `deleteTestRun_notFound_returns404` | DELETE `/{id}` | 404 |

**Note on unlock:** The controller reads `role` from `HttpServletRequest.getAttribute("role")`. In MockMvc, set it via `.request.setAttribute("role", "Admin")` in the perform chain.

#### TestRunCaseControllerTest.java (9 tests)

Mocks: `TestRunCaseService`  
Base URL: `/projects/{projectId}/runs/{runId}/cases`

| Test method | Endpoint | Expected |
|-------------|----------|----------|
| `createTestRunCase_returns201` | POST | 201 + body |
| `getTestRunCasesByRun_returns200` | GET | 200 + PageResult |
| `getTestRunCase_found_sameRun_returns200` | GET `/{id}` | 200 |
| `getTestRunCase_runMismatch_returns404` | GET `/{id}` | runId differs → 404 |
| `updateTestRunCase_found_returns200` | PUT `/{id}` | status from body → 200 |
| `updateTestRunCase_notFound_returns404` | PUT `/{id}` | 404 |
| `updateTestRunCaseStatus_found_returns200` | PUT `/{id}/status` | status param → 200 |
| `updateTestRunCaseStatus_notFound_returns404` | PUT `/{id}/status` | 404 |
| `linkBug_found_returns200` | POST `/{id}/link-bug` | bugUrl → 200 |

#### TestRunStepControllerTest.java (10 tests)

Mocks: `TestRunStepService`, `AttachmentService`  
Base URL: `/projects/{projectId}/runs/{runId}/cases/{caseId}/steps`

| Test method | Endpoint | Expected |
|-------------|----------|----------|
| `createTestRunStep_returns201` | POST | 201 + body |
| `getTestRunStepsByCase_returns200` | GET | 200 + PageResult |
| `getTestRunStep_found_sameCase_returns200` | GET `/{id}` | 200 |
| `getTestRunStep_caseMismatch_returns404` | GET `/{id}` | caseId differs → 404 |
| `updateTestRunStep_found_returns200` | PUT `/{id}` | from body → 200 |
| `updateTestRunStep_notFound_returns404` | PUT `/{id}` | 404 |
| `updateTestRunStepStatus_found_returns200` | PUT `/{id}/status` | from param → 200 |
| `updateTestRunStepStatus_notFound_returns404` | PUT `/{id}/status` | 404 |
| `uploadEvidence_validFile_returns201` | POST `/{id}/evidence` | MockMultipartFile → 201 |
| `uploadEvidence_serviceThrows_returns500` | POST `/{id}/evidence` | exception → 500 |

**Multipart pattern:**
```java
mockMvc.perform(multipart("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{sid}/evidence",
        projectId, runId, caseId, stepId)
    .file(new MockMultipartFile("file", "screenshot.png",
            "image/png", "fake-content".getBytes())))
    .andExpect(status().isCreated());
```

### 3.3 E2E Gherkin (2 files)

#### Feature file: `apps/backend/src/test/resources/features/test-run-crud.feature`

Tags follow existing convention: `@e2e @tms` on Feature, `@smoke` or `@requires-cyoda` on each Scenario.

**@smoke scenarios (5):**
1. Creating a test run without auth returns 401
2. Creating a test run with blank name returns 400
3. Creating a test run with name > 255 chars returns 400
4. Completing a run without auth returns 401
5. Unlocking a run without a role returns 403 (RBAC check fires before any service call)

**@requires-cyoda scenarios (10):**
1. Admin creates a test run — 201 + required fields (id, name, status, projectId)
2. Can retrieve the created test run by ID — 200 + correct name
3. GET all test runs returns paged response — data array + totalElements
4. GET /details returns run + runCases in one response
5. Admin updates test run name — 200 + updated name
6. Admin completes test run — status field equals "COMPLETED"
7. Admin (role=Admin) unlocks completed run — 200 + status reverts
8. Tester (role=Tester) can also unlock a completed run — 200
9. Admin deletes test run — 204
10. GET deleted test run returns 404

#### Step definitions: `apps/backend/src/test/java/e2e/steps/TestRunApiSteps.java`

- `@Autowired TmsHttpClient` — reuse existing HTTP client
- Shared state: `lastCreatedRunId` (UUID), `lastRunResponse` (response body as Map)
- Glue steps cover: `I create a test run named "X"`, `I GET the last created run by ID`, `I complete the last created run`, `I unlock the last created run`, `I delete the last created run`

---

## 4. What is NOT in scope

- Tests for `ReportController`, `ExportController`, `AttachmentController` — separate initiative
- Frontend component tests — separate initiative
- Processor business logic tests (stubs not yet implemented)
- Jacoco coverage thresholds — separate configuration task

---

## 5. Summary

| Category | New files | New tests |
|----------|-----------|-----------|
| Processor unit tests | 5 | 25 |
| Controller unit tests | 3 | ~34 |
| E2E feature + step defs | 2 | ~15 scenarios |
| **Total** | **10** | **~74** |

> **Note on unlock RBAC split:** The `POST /{id}/unlock` endpoint checks `role` from `HttpServletRequest.getAttribute("role")` before calling the service. The 403 (no role) case never reaches Cyoda → @smoke. The 200 (Tester/Admin role) case calls `testRunService.getTestRunById` → @requires-cyoda.

All unit tests run with `./gradlew :apps:backend:test` (no Cyoda required).  
E2E smoke scenarios run with `./gradlew :apps:backend:cucumberTest` without Cyoda.  
E2E `@requires-cyoda` scenarios need a live Cyoda instance.

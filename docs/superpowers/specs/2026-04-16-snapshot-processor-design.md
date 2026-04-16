# SnapshotProcessor Design

**Date:** 2026-04-16  
**Status:** Approved  

---

## 1. Goal

When a test run transitions from `initial` to `active` (operation `initialize_run`), capture an immutable snapshot of all test cases and their steps. This ensures that if the original `TestCase` or `TestStep` entities are deleted later, the test run retains complete historical data.

Defects (`DefectDTO`) are **not** snapshotted — they are already linked directly to `testRunId`/`testRunCaseId`/`testRunStepId` and live independently.

---

## 2. Trigger

- **Entity:** `TestRun`
- **Transition:** `initialize_run` (`initial → active`)
- **Processor:** `SnapshotProcessor`
- **Execution mode:** `SYNC`
- **Tag:** `cyoda_application`
- **Timeout:** 60 000 ms

---

## 3. Data Model Changes

### 3.1 `TestRunCaseDTO` — add snapshot fields

```java
// Snapshot fields — copied from TestCase at initialize_run time
private String title;
private String description;
private String preconditions;
private Priority priority;
private String displayId;   // e.g. "S1-3"
private UUID suiteId;
```

Existing fields (`testRunId`, `testCaseId`, `projectId`, `status`, `bugUrl`, `startedAt`, `completedAt`) are unchanged.

### 3.2 `TestRunStepDTO` — add snapshot fields

```java
// Snapshot fields — copied from TestStep at initialize_run time
private Integer stepNumber;
private String action;
private String expectedResult;
```

Existing fields (`testRunCaseId`, `testStepId`, `status`, `actualResult`, `startedAt`, `completedAt`) are unchanged.

`actualResult` is execution data — what the tester observed during this specific run. It is not a snapshot field.

---

## 4. SnapshotProcessor Logic

Cyoda constraint: a processor **cannot** call `EntityService` to update the entity currently being processed (`TestRun`). It **can** create new entities (`TestRunCase`, `TestRunStep`) via `EntityService`.

```
for each caseId in testRun.caseIds:
    1. fetch TestCase by caseId
       → if not found: log warning, skip
    2. create TestRunCase {
           testRunId    = testRun.id,
           testCaseId   = caseId,
           projectId    = testRun.projectId,
           status       = "UNTESTED",
           // snapshot:
           title, description, preconditions, priority, displayId, suiteId
       }
    3. fetch all TestSteps by testCaseId
    4. for each TestStep:
           create TestRunStep {
               testRunCaseId = newly created TestRunCase.id,
               testStepId    = testStep.id,
               status        = "UNTESTED",
               // snapshot:
               stepNumber, action, expectedResult
           }

return EntityProcessorCalculationResponse { success = true }
```

Creation is sequential (not parallel) for simplicity and safety.

---

## 5. Race Condition Fix

**Problem:** `TestRunService.updateTestRun` fetches the existing run twice — once for transition check, once for merge logic. Under rapid concurrent requests, multiple threads all see `status = initial` and each sends `initialize_run`, causing competing transactions and 60 s timeouts in Cyoda.

**Fix:** Fetch the existing run once and reuse the result for both the transition check and the merge logic. This eliminates the redundant gRPC call and reduces the window for concurrent duplicate transitions.

Cyoda will reject any duplicate `initialize_run` attempts once the entity is already in `active` state. Our goal is to avoid generating unnecessary concurrent calls in the first place.

---

## 6. Schema & Workflow Re-import

After changing the DTOs, update the sample JSON files that Cyoda uses to infer the entity schema:

- `src/main/resources/entity/testruncase/version_1/testruncase.json` — add snapshot fields with realistic example values
- `src/main/resources/entity/testrunstep/version_1/testrunstep.json` — add snapshot fields with realistic example values

The `TestRun` workflow JSON (`src/main/resources/workflow/testrun/version_1/TestRun.json`) does **not** need changes — `SnapshotProcessor` is already configured correctly.

**Full re-import cycle (per `SCHEMA_AND_WORKFLOW_IMPORT.md`):**
1. Delete all tenant entities bottom-up: `TestRunStep → TestRunCase → TestRun → Attachment → Defect → TestStep → TestCase → Suite → Report → ProjectCounter → Project`
2. Unlock and delete all models
3. Run `./import-schemas.sh`

> ⚠️ All data in Cyoda will be lost and must be recreated after re-import.

---

## 7. Files Changed

| File | Change |
|---|---|
| `application/dto/TestRunCaseDTO.java` | Add snapshot fields |
| `application/dto/TestRunStepDTO.java` | Add snapshot fields |
| `application/processor/SnapshotProcessor.java` | Implement snapshot logic |
| `application/service/TestRunService.java` | Fix double-fetch race condition |
| `src/main/resources/entity/testruncase/version_1/testruncase.json` | Add snapshot fields to sample |
| `src/main/resources/entity/testrunstep/version_1/testrunstep.json` | Add snapshot fields to sample |

---

## 8. Out of Scope

- Frontend changes (reading snapshot fields from `TestRunCase`/`TestRunStep`)
- Migrating existing test runs to have snapshots
- Async snapshot creation

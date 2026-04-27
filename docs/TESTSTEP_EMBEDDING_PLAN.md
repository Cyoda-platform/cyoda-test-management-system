# Architectural Plan: Embedding Test Steps into Test Case

**Date:** 2026-04-27  
**Author:** Victoria  
**Status:** For discussion  
**Estimate:** 1–2 weeks

---

## Problem

Test case steps (`TestStep`) are currently stored as separate Cyoda entities. Each step requires an individual gRPC call to create, read, or delete.

**Consequences:**
- Importing 300 cases × 5 steps = **~1,800 gRPC calls** → takes hours
- Opening the repository = 2 HTTP requests per test case (case + steps separately)
- No scalability: more cases → proportionally longer wait

**Root cause:** TestStep was designed as a Cyoda entity with its own FSM workflow. This is technically correct for Cyoda but architecturally wrong for a TMS — test case steps never exist independently of their parent case.

---

## Solution

Embed test case steps (TestStep) as a JSON array inside the TestCase entity.

### Concept

```
// Current
TestCase { id, title, priority, ... }
TestStep { id, testCaseId, stepNumber, action, expectedResult }  ← separate Cyoda entity

// After
TestCase {
  id, title, priority, ...,
  steps: [                           ← JSON array embedded in the case
    { stepNumber, action, expectedResult },
    { stepNumber, action, expectedResult }
  ]
}
```

### Key Distinction: TestStep vs TestRunStep

**TestStep (definition steps)** — embed into TestCase.  
**TestRunStep (execution steps)** — **keep as-is**. These have their own execution status (pass/fail/skip), are updated one at a time during a test run, and are correctly modelled as separate entities.

---

## What Changes

### Backend

#### Removed entirely
- `TestStepDTO` — Cyoda entity Java class
- `TestStepService` — service for step operations
- `TestStepController` — REST controller for `/cases/{id}/steps`
- `TestStepServiceCacheTest` — cache tests (stepsByCase)
- FSM workflow `teststep/version_1/TestStep.json`
- `stepsByCase` cache from `CacheConfig`
- `StepReplaceDTO` and the `PUT /steps/replace` endpoint (no longer needed)

#### Modified

**TestCaseDTO** — add embedded steps field:
```java
private List<StepDTO> steps = new ArrayList<>();

@Data
public static class StepDTO {
    private Integer stepNumber;
    private String action;
    private String expectedResult;
}
```

**TestCaseService** — all create/update methods now include steps inline.  
`createTestCase` accepts steps in the request body.  
`updateTestCase` updates the entire case including steps.  
`getTestCasesBySuiteId` returns cases with steps in one request.

**batchCreateTestCases** — significantly simplified:
```
// Current:  300 cases × (2 gRPC case + 5 gRPC steps) = 1,800 gRPC calls
// After:    300 cases × 1 gRPC                        =   300 gRPC calls
```
No more `testStepService.createTestStep()` — steps are included in the case create payload.

**Cyoda entity schema** for TestCase — add `steps` JSON field and re-import the schema.

#### Unchanged
- `TestRunStep` and everything related to it
- `TestRunCaseService`
- Logic for copying steps into a test run: `case.steps` → `TestRunStep`

### Frontend

#### Removed
- `testStepsApi` (list, create, update, delete, replace) from `api.ts`
- Step-related hooks in `useApi.ts`
- Separate step fetch calls in components

#### Modified
- `Repository.tsx` — steps are read from `case.steps`, no separate API call
- Case form / editor — step CRUD goes through case update as a whole
- `CreateTestRun.tsx` — steps sourced from `case.steps` when building a run
- Import flow — simplified: steps are part of the case payload, no `testStepsApi.replace`
- `TestCase` type in `api.ts` — add `steps: TestStep[]`

### Cyoda

- Re-import `TestCase` schema with the new `steps` field
- Remove (or deprecate) the `TestStep` workflow from Cyoda
- **Data migration:** move all existing `TestStep` entities into `steps[]` on their parent `TestCase`

---

## Data Migration Strategy

This is the highest-risk step. Existing steps are stored as separate Cyoda entities and must be moved into their parent cases.

### Option A: One-shot migration script (recommended)

1. Take a maintenance window (stop the application)
2. Run migration script:
   - For each project: load all test cases
   - For each case: load its TestStep entities
   - Write steps into `case.steps` via case update
   - Delete the TestStep entities
3. Re-import Cyoda schema
4. Deploy new application version

**Pros:** clean, single code version after migration  
**Cons:** requires maintenance window

### Option B: Dual-write (zero downtime)

1. Deploy version N+1: writes steps to both TestStep entities and `case.steps`
2. Run background migrator: copies old TestStep → `case.steps`
3. Once migration is complete: deploy version N+2 that reads only from `case.steps`
4. Remove old TestStep entities

**Pros:** no downtime  
**Cons:** more complex, temporary data duplication

**Recommendation:** Option A. This is not a 24/7 production system, so a 1–2 hour maintenance window is simpler and safer.

---

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Cyoda schema re-import requires deleting all cases | High | High | Run data migration before deletion; take a backup |
| Cyoda does not support nested JSON arrays in schema | Low | High | **Verify on test environment before starting any work** |
| TestRunStep references TestStep IDs | Medium | Medium | Confirm TestRunStep stores a snapshot (action/expectedResult), not a FK to TestStep |
| Old test runs lose connection to step definitions | Low | Medium | TestRunStep is independent — it stores its own copy of step data |

---

## What Does NOT Change

- `TestRunStep` — all execution-time steps remain separate Cyoda entities
- Test run creation logic (copying steps into TestRunStep)
- Test run API
- Defects, attachments, reports — untouched

---

## Task Breakdown

| Task | Estimate |
|---|---|
| Verify Cyoda schema supports JSON array field | 0.5 day |
| Update TestCase schema + re-import on test environment | 1 day |
| Backend: update TestCaseDTO, TestCaseService; remove TestStepService/Controller | 2 days |
| Backend: update batchCreateTestCases and import endpoint | 1 day |
| Backend: update TestRunCase step-copy logic (case.steps → TestRunStep) | 1 day |
| Backend: update tests | 1 day |
| Frontend: remove testStepsApi, update components | 2 days |
| Frontend: update import/export | 0.5 day |
| Data migration script | 1 day |
| QA + E2E tests | 1 day |
| **Total** | **~10 working days** |

---

## Expected Outcome

| Metric | Current | After |
|---|---|---|
| Import 300 cases × 5 steps | ~1,800 gRPC calls, hours | ~300 gRPC calls, minutes |
| Opening a case in the editor | 2 HTTP requests | 1 HTTP request |
| Loading the repository | N+1 step requests | 0 extra requests |
| Codebase complexity | Higher (separate controller/service) | Lower |

---

## Discussion Points for the Daily

1. **Cyoda schema:** does Cyoda support a nested JSON array field on an entity? This must be verified on a test environment **before any code work begins**.
2. **Downtime:** is a maintenance window acceptable for the data migration? How much data is already in the system?
3. **Priority:** this refactoring vs other backlog items — when do we schedule it?
4. **Test environment:** is there an isolated Cyoda environment to test the schema change safely?
5. **TestRunStep:** confirm that TestRunStep stores a full snapshot of step data (action/expectedResult) and has no foreign key dependency on TestStep ID.

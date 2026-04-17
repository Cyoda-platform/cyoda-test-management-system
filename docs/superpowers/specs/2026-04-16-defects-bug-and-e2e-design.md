# Design: Defects — Bug Fix + Cucumber E2E Tests

**Date:** 2026-04-16  
**Status:** Approved

---

## Context

The Defects feature already exists end-to-end:
- Backend: `DefectController` (`POST/GET/PUT/DELETE /api/projects/{projectId}/defects`), `DefectService`, `DefectDTO`
- Frontend: `Defects.tsx` page, `CreateDefectModal.tsx`, hooks in `useApi.ts`
- Defects can be linked to a `testRunId` and/or a `testRunCaseId`

Two issues need to be addressed:
1. A frontend bug that traps users on the RunExecution page
2. Missing Cucumber E2E test coverage for the Defects API

---

## Part 1: Bug Fix — RunExecution.tsx

### Problem

`RunExecution.tsx` lines 1018 and 1041 call `keys.testRunDetails(projectId!, runId!)`.
`testRunDetails` does not exist on the `keys` object in `useApi.ts`.
The correct key factory is `keys.runs.detail(projectId!, runId!)`.

When the user adds a defect to a test step and the `completeRun` or `unlockRun` mutation's
`onSuccess` fires, it throws `TypeError: keys.testRunDetails is not a function`.
This is an unhandled promise rejection that freezes navigation.

### Fix

Replace both occurrences:
```
keys.testRunDetails(projectId!, runId!)
→
keys.runs.detail(projectId!, runId!)
```

Files changed: `apps/frontend/src/pages/RunExecution.tsx` (2 lines).

---

## Part 2: Cucumber E2E Tests — Defects API

### Pattern

Follows exactly the same structure as the existing `project-crud.feature` + `ProjectApiSteps.java`:
- Feature file with `@smoke` (no Cyoda needed) and `@requires-cyoda` (live Cyoda) tags
- Step definitions class with `@Autowired TmsHttpClient`, `@Before` lifecycle, `@After` cleanup
- Auth steps (`I am logged in as admin`, etc.) reused from `ProjectApiSteps` — they are globally shared in Cucumber

### New Files

| File | Purpose |
|------|---------|
| `apps/backend/src/test/resources/features/defect-crud.feature` | Gherkin scenarios |
| `apps/backend/src/test/java/e2e/steps/DefectApiSteps.java` | Step definitions |

### Scenarios

#### `@smoke` — validation layer only, no Cyoda required

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Unauthenticated POST to defects | 401 |
| 2 | Create defect with blank `title` | 400 |
| 3 | Create defect with blank `severity` | 400 |

#### `@requires-cyoda` — full lifecycle

| # | Scenario | Expected |
|---|----------|----------|
| 4 | Admin creates a defect | 201, body contains `id`, `title`, `severity`, `displayId`, `createdAt` |
| 5 | Admin retrieves defect by ID | 200, body matches created title |
| 6 | GET all defects for a project | 200, paged response with `data` array and `totalElements` |
| 7 | Admin updates defect severity and status | 200, new values reflected in response |
| 8 | Admin deletes a defect | 204 |
| 9 | Create defect with `testRunId` | 201, `testRunId` field present in response |
| 10 | GET defects filtered by `?testRunId=...` | 200, only returns defects for that run (uses the same UUID from scenario 9 — no real TestRun entity required, filtering is by UUID equality) |

### DefectApiSteps.java Design

```
- @Autowired TmsHttpClient http
- @Autowired GherkinE2eTest runner
- createdDefectIds: List<String>   — tracked for @After cleanup
- createdProjectId: String         — created once per scenario needing Cyoda, cleaned up in @After
- lastDefectResponse: ResponseEntity<String>
- @Before: http.setServerPort + http.reset + clear tracked IDs
- @After: delete all tracked defects, then delete tracked project (best-effort, log failures)
```

Auth steps (`I am logged in as admin`, `I am not authenticated`) are NOT redefined here —
they are already defined in `ProjectApiSteps` and are globally available within the same
Cucumber test run.

### Cleanup Strategy

`@After` in `DefectApiSteps` deletes defects first, then the project — matching the
bottom-up deletion order documented in `apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md`.
Each delete is wrapped in try/catch to avoid masking scenario failures.

---

## CLAUDE.md Update

Added to the "Workflow Configurations" section:
- Reference to `apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md`
- Warning not to use `WorkflowImportTool` (superseded by `import-schemas.sh`)
- Requirement to run bottom-up entity deletion before re-importing

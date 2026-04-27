# Performance Fixes Design

**Date:** 2026-04-27  
**Status:** Approved  
**Scope:** Backend (Java 21 + Spring Boot 3.5) + Frontend (React 18 + TypeScript)

---

## Problem Statement

The application is slow on first page open, when creating entities, and especially during CSV import (50–500 test cases). Test run status clicks are already fast and not in scope. The refactoring done two weeks ago improved things but did not address the root causes.

---

## Root Causes (Priority Order)

1. **Import is very slow** — `batchCreateTestCases` creates each case + its steps sequentially: ~700 gRPC calls in a line for 100 cases × 5 steps.
2. **Overwrite during import is slow** — frontend does GET steps → DELETE each → POST each per case: 12+ HTTP calls per overwritten case.
3. **List pages fetch all data** — `getTestRunsByProjectId` fetches ALL test runs across all projects and filters in memory (O(n) full table scan).
4. **First app load is slow** — no code splitting; full ~1.2 MB bundle downloaded even for simple views.
5. **DefectAttachmentsList re-fetches on every step click** — no React.memo, fires attachment API call on every parent render.
6. **Cascade delete has remaining sequential loops** — TestRuns, Suites, Defects, Attachments, Reports deleted sequentially after earlier parallelization of steps.
7. **Minor config issues** — eager multipart resolution, aggressive gRPC keep-alive.

---

## Design

### 1. Import — Parallelize batchCreate (CRITICAL)

**File:** `TestCaseService.java` → `batchCreateTestCases`

Replace the sequential `for` loop with parallel execution using `CompletableFuture`. Each case's full creation block (create entity → update displayId → create steps) runs as an independent async task. Results collected into a pre-sized array to preserve order.

```
// Before: sequential
for each case:
  create(tc)         ← gRPC
  update(saved)      ← gRPC (persist displayId)
  for each step:
    createStep(s)    ← gRPC

// After: parallel across cases
CompletableFuture[] futures = cases.stream()
  .map(case -> CompletableFuture.supplyAsync(() -> {
      create(tc) + update(saved) + createSteps(steps)
  }))
  .toArray();
CompletableFuture.allOf(futures).join();
```

Steps within each case remain sequential (dependency: need case ID before creating steps). Cases run concurrently bounded by the existing `processor-thread-pool: 20`.

**Expected impact:** Import of 100 cases drops from ~70 s to ~5–7 s.

---

### 2. Import Overwrite — New replaceSteps Endpoint (HIGH)

**New endpoint:** `PUT /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps/replace`  
**Body:** `List<StepReplaceDTO>` (stepNumber, action, expectedResult)

Backend deletes all existing steps for the case (in parallel via CompletableFuture), then creates new ones (in parallel). Returns 200 with the new step list.

Frontend `handleImport` changes: replace the GET → delete loop → create loop with a single `testStepsApi.replace(...)` call per overwritten case. The 6-worker pooledRun already handles concurrency across cases.

**Files changed:**
- New: `TestStepController.java` — `replaceSteps` endpoint
- Changed: `TestStepService.java` — `replaceSteps(caseId, steps)` method
- Changed: `apps/frontend/src/lib/api.ts` — add `testStepsApi.replace()`
- Changed: `Repository.tsx` — use `testStepsApi.replace()` instead of the expand/delete/recreate loop

---

### 3. TestRun Full Table Scan — Spring Cache (MEDIUM)

**File:** `TestRunService.java`

Add `@Cacheable` on `getTestRunsByProjectId` and `getAllTestRunsByProjectId` with a 30-second TTL. Annotate create/update/delete methods with `@CacheEvict` to invalidate on mutation.

Cache name: `testRunsByProject`. Key: `projectId`.

Add `spring-boot-starter-cache` dependency (already on classpath via Spring Boot) and `@EnableCaching` on the main application class or a config class.

Schema fix (`$.projectId` as valid Cyoda search path) is deferred — it requires deleting all tenant entities and re-importing the schema, which needs separate coordination.

**Files changed:**
- `TestRunService.java` — `@Cacheable` / `@CacheEvict` annotations
- A new `CacheConfig.java` in `application/` — `CaffeineCacheManager` with TTL 30 s
- `build.gradle` — add `com.github.ben-manes.caffeine:caffeine` (Caffeine-backed cache)

---

### 4. Cascade Delete — Parallelize Remaining Loops (MEDIUM)

**File:** `ProjectService.java` → `deleteProject`

The following sections are still sequential forEach loops — wrap each in `CompletableFuture.allOf(...)`:

- `testRunService.getAllTestRunsByProjectId(id).forEach(run -> deleteTestRun(...))` 
- `suiteService.getAllSuitesByProjectId(id).forEach(suite -> deleteById(...))`
- `defectService.getAllDefectsByProjectId(id).forEach(d -> deleteDefect(...))`
- `attachmentService.getAllAttachmentsByProjectId(id).forEach(a -> deleteAttachment(...))`
- `reportService.getAllReportsByProjectId(id).forEach(r -> deleteReport(...))`

Pattern already established for TestRunSteps and TestSteps — use the same `CompletableFuture.runAsync` approach.

---

### 5. Frontend — React.memo on DefectAttachmentsList (MEDIUM)

**Files:** `RunExecution.tsx` (line 139), `Defects.tsx` (line 51)

Wrap `DefectAttachmentsList` with `React.memo()` in both files. The component receives only primitive props (`projectId: string`, `defectId: string`) so the default shallow comparison is correct — no custom comparator needed.

```tsx
const DefectAttachmentsList = React.memo(({ projectId, defectId }: ...) => {
  ...
});
```

**Impact:** Eliminates O(n) attachment API calls during run execution (one call per step click × number of visible defects).

---

### 6. Frontend — Code Splitting (MEDIUM)

**File:** `vite.config.ts`

Add `rollupOptions.output.manualChunks` to split heavy dependencies:

```ts
manualChunks: {
  'vendor-recharts': ['recharts'],
  'vendor-radix':    Object.keys(dependencies).filter(k => k.startsWith('@radix-ui')),
}
```

**File:** Router / App entry point

Wrap heavy page imports with `React.lazy` + `Suspense`:

```tsx
const ReportPage     = lazy(() => import('./pages/Report'));
const RepositoryPage = lazy(() => import('./pages/Repository'));
const DefectsPage    = lazy(() => import('./pages/Defects'));
```

`recharts` (~250 KB) only downloads when the user opens a report. Initial bundle shrinks by ~30%.

---

### 7. Config Tweaks (LOW)

**File:** `application.yml`

| Setting | Before | After | Reason |
|---|---|---|---|
| `resolve-lazily` | `false` | `true` | Stream large files; don't block request thread |
| `grpc-keep-alive-time-seconds` | `15` | `30` | Reduce ping overhead on stable connections |

**File:** `useApi.ts`

| Hook | `staleTime` before | After |
|---|---|---|
| `useDefectsByRun` | `0` | `5000` |

Prevents unnecessary refetch when navigating between runs and back.

---

## Files Changed Summary

| File | Change |
|---|---|
| `TestCaseService.java` | Parallelize batchCreateTestCases |
| `TestStepController.java` | New replaceSteps endpoint |
| `TestStepService.java` | New replaceSteps method |
| `TestRunService.java` | @Cacheable / @CacheEvict |
| `CacheConfig.java` (new) | Caffeine cache manager, TTL 30s |
| `ProjectService.java` | Parallelize remaining delete loops |
| `build.gradle` | Add caffeine dependency |
| `application.yml` | resolve-lazily=true, keep-alive=30s |
| `RunExecution.tsx` | React.memo on DefectAttachmentsList |
| `Defects.tsx` | React.memo on DefectAttachmentsList |
| `vite.config.ts` | manualChunks for recharts + radix |
| Router/App | React.lazy for Report, Repository, Defects pages |
| `api.ts` | Add testStepsApi.replace() |
| `Repository.tsx` | Use replaceSteps in overwrite flow |
| `useApi.ts` | staleTime: 5000 on useDefectsByRun |

---

## Out of Scope

- Schema re-registration for `$.projectId` search path (requires tenant data migration, separate coordination)
- Thread pool auto-sizing (minor, no user-facing impact)
- Condition object caching in services (micro-optimization, low ROI)
- Lucide icon tree-shaking (already handled by Vite with named imports)

---

## Testing

- Unit tests for `batchCreateTestCases` with parallelism (mock EntityService, verify all cases created)
- Unit test for `replaceSteps` (verify delete-then-create sequence)
- Unit test for cache eviction (verify cache miss after create/update/delete)
- Frontend: existing vitest suite must pass; no new tests for memo/lazy (structural changes)
- E2E: CSV import of 100+ cases must complete in under 30 s (manual verification against live Cyoda)

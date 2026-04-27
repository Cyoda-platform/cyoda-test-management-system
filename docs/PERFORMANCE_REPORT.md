# Performance Analysis Report

**Date:** 2026-04-27  
**Project:** Cyoda Test Management System  
**Scope:** Backend (Java 21 + Spring Boot 3.5) + Frontend (React 18 + TypeScript)

---

## Executive Summary

The application received performance improvements two weeks ago but remains slower than expected. This analysis identifies **12 bottlenecks** across backend services, frontend rendering, and configuration. The most critical issues involve O(n) full-table scans on list endpoints and cascade delete N+1 query patterns that scale poorly with data volume.

---

## Backend Issues

### CRITICAL — Full Table Scan with Client-Side Filtering

**File:** `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java` (lines 118–131)

`getTestRunsByProjectId()` calls `entityService.findAll()` fetching **all test runs across all projects**, then filters in memory:

```java
List<TestRunDTO> all = entityService.findAll(MODEL_SPEC, TestRunDTO.class)
    .data().stream()
    .map(this::withId)
    .filter(r -> projectId.equals(r.getProjectId()))  // client-side filter
    .toList();
```

The code comment notes that `$.projectId` is not a valid Cyoda search path due to a schema registration ordering issue. As data grows, this scales O(n) across **all** runs for every list request.

**Impact:** A workspace with 25 projects × 20 runs = 500 runs fetched and 480 discarded on every page load.

**Fix options:**
- Fix the Cyoda entity schema registration to support `$.projectId` as a search path.
- Add server-side project-scoped caching as a short-term mitigation.

---

### HIGH — N+1 Queries in Cascade Delete

**File:** `apps/backend/src/main/java/com/java_template/application/service/ProjectService.java` (lines 220–300)

Project deletion issues ~11 sequential fetch operations followed by individual delete calls per entity:

1. `getAllTestRunCasesByProjectId()` — 1 query
2. Loop: `getAllTestRunStepsByTestRunCaseId()` per run case — N queries
3. Loop: `deleteTestRunCase()` per run case — N queries
4. `getAllTestRunsByProjectId()` — 1 query
5. Loop: `deleteTestRun()` per run — M queries
6. `getAllCasesByProjectIdIncludingDeleted()` — 1 query
7. Loop: `getAllTestStepsByTestCaseIdIncludingDeleted()` per case — P queries
8. Further individual step/attachment deletions

**Example:** Deleting a project with 100 test cases and 50 test runs produces **300+ database round-trips**.

**Fix:** Add batch query methods (`getAllTestStepsByTestCaseIdBatch(List<UUID>)`, etc.) to fetch steps/cases/attachments for multiple parents in a single call.

---

### MEDIUM — Concurrency Race on State Transitions

**File:** `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java` (lines 172–246)

`updateTestRun()` reads the current run state, detects FSM transitions, then calls `entityService.update()`. Under concurrent updates (e.g., two browser tabs), both reads see the same state and both attempt to send the same FSM transition, causing a state machine error. Lines 238–242 catch and silently retry, adding latency on every concurrent write.

**Fix:** Add an optimistic lock / version field to detect and reject stale writes before sending the FSM event.

---

### MEDIUM — No Caching Layer on Read Methods

No `@Cacheable` annotations exist on any service read methods. Every controller call fetches fresh data from Cyoda.

**Affected patterns:**
- Clicking 5 step status buttons triggers 5 separate `updateTestRun()` calls, each re-reading the run.
- Opening a run execution view fetches the run + all run-cases with no short-term cache.

**Fix:** Apply Spring Cache (`@Cacheable` / `@CacheEvict`) with TTLs of 10–30 seconds on read methods. Evict on mutations.

---

### MEDIUM — Eager Multipart Resolution Blocks Upload Threads

**File:** `apps/backend/src/main/resources/application.yml` (line 22)

```yaml
resolve-lazily: false
```

With a 100 MB upload limit, this buffers the entire file in memory before the handler runs, blocking the request thread. Setting `resolve-lazily: true` enables streaming.

---

### LOW — Condition Objects Rebuilt on Every Request

**Files:** `TestRunService.java` (lines 44–55), `TestCaseService.java` (lines 52–63), and similar

Every search call reconstructs a `GroupConditionDto` and calls `objectMapper.valueToTree()` inline. For a page loading 20 suites, this runs 20+ times. The condition for a fixed field+value pair could be a static final constant or lightweight factory cache.

---

### LOW — Static Thread Pool Sizing

**File:** `apps/backend/src/main/resources/application.yml` (lines 128–130)

```yaml
processor-thread-pool: 20
criteria-thread-pool: 20
control-thread-pool: 3
```

Fixed at 20/20/3 regardless of available CPU cores. On a 4-core container this creates 5× oversubscription and excessive context-switch overhead. Thread counts should scale with `Runtime.getRuntime().availableProcessors()`.

---

### LOW — Overly Aggressive gRPC Keep-Alive

**File:** `apps/backend/src/main/resources/application.yml` (line 123)

```yaml
grpc-keep-alive-time-seconds: 15
```

Pings every 15 seconds on all gRPC connections. Recommended: 30–60 seconds for stable production networks.

---

## Frontend Issues

### MEDIUM — Missing React.memo() Causes O(n) Refetches

**Files:**
- `apps/frontend/src/pages/RunExecution.tsx` (lines 139–168)
- `apps/frontend/src/pages/Defects.tsx` (lines 51–85)

`DefectAttachmentsList` is not wrapped in `React.memo()`. When the parent re-renders (e.g., after a step-status update), the child re-renders unconditionally and its `useEffect` fires an attachment list API call:

```tsx
useEffect(() => {
  attachmentsApi.listByDefect(projectId, defectId)  // fires on every parent render
    .then(atts => setAttachments(atts || []))
}, [projectId, defectId]);
```

**Impact:** Each step click during run execution triggers an attachment refetch for every visible defect.

**Fix:** Wrap with `React.memo()`. Consider migrating to a TanStack Query hook with stable cache keys.

---

### MEDIUM — No Code Splitting

**File:** `apps/frontend/vite.config.ts` (lines 1–22)

No `manualChunks`, `rollupOptions`, or `React.lazy()` dynamic imports are configured. The entire app (reports, defects, runs, repository) is bundled and downloaded on initial load.

**Impact:** ~1.2 MB uncompressed (~350 KB compressed) bundle. Estimated 1–2 second parse delay on mobile/4G.

**Fix:** Add route-based code splitting:
```tsx
const ReportPage = lazy(() => import('./pages/Report'));
const DefectsPage = lazy(() => import('./pages/Defects'));
```

And configure `rollupOptions.output.manualChunks` in `vite.config.ts` to split `recharts` and heavy Radix UI packages into separate chunks.

---

### LOW — Inefficient Memoization Dependencies

**File:** `apps/frontend/src/pages/RunExecution.tsx` (lines 225–250)

`filteredSuitesWithCases` depends on `run?.caseIds` and `runCases` (array references). TanStack Query returns new array references on cache invalidations, causing the memo to recompute even when contents are unchanged.

**Fix:** Stabilize array dependencies with a deep-equality comparator or serialize to a primitive (e.g., join IDs to a string as the memo key).

---

### LOW — Defect-by-Run Hook Always Stale

**File:** `apps/frontend/src/hooks/useApi.ts` (lines 717–726)

```tsx
staleTime: 0,         // always refetch
refetchOnMount: true,
```

Navigating between two runs and back triggers two full refetches. A 5-second stale window would eliminate most redundant requests.

---

### LOW — Oversized Bundle Dependencies

**File:** `apps/frontend/package.json` (lines 16–65)

| Library | Issue |
|---|---|
| `recharts` | Full chart library (~250 KB) loaded for all routes, only used in Report view |
| `lucide-react` | ~1000 icons bundled; ~30 are used |
| `@radix-ui/*` | 40+ packages included; not all used in every route |

**Fix:** Lazy-import `recharts` in the Report route. For `lucide-react`, either use per-icon imports or configure tree-shaking in Vite.

---

## Priority Summary

| Priority | Issue | File | Est. Impact |
|---|---|---|---|
| **CRITICAL** | Full table scan in `getTestRunsByProjectId` | `TestRunService.java:118` | List pages: O(n) fetches |
| **HIGH** | Cascade delete N+1 (300+ DB calls) | `ProjectService.java:220` | Deletion: 10+ seconds |
| **MEDIUM** | Missing React.memo on DefectAttachmentsList | `RunExecution.tsx:139` | O(n) API calls per step click |
| **MEDIUM** | No caching layer on read methods | All services | Redundant DB round-trips |
| **MEDIUM** | No frontend code splitting | `vite.config.ts` | Initial load: +1–2 seconds |
| **MEDIUM** | Concurrency race on FSM state transitions | `TestRunService.java:172` | Silent retry latency |
| **MEDIUM** | `resolve-lazily: false` blocks upload threads | `application.yml:22` | File upload throughput |
| **LOW** | Condition objects rebuilt per request | Multiple services | Minor CPU overhead |
| **LOW** | Static thread pool sizing | `application.yml:128` | Context-switch overhead |
| **LOW** | Aggressive gRPC keep-alive (15s) | `application.yml:123` | Unnecessary ping overhead |
| **LOW** | Memoization uses unstable array refs | `RunExecution.tsx:225` | Minor re-computation |
| **LOW** | Oversized bundle (recharts, lucide, Radix) | `package.json` | ~100 KB waste |

---

## Recommended Fix Order

1. **Fix schema registration** so `$.projectId` is a valid Cyoda search path → eliminates the CRITICAL full table scan.
2. **Add batch query methods** in `ProjectService` for cascade deletes → resolves N+1 pattern.
3. **Wrap DefectAttachmentsList in React.memo()** → stops O(n) attachment refetches during run execution.
4. **Add Spring Cache** with short TTLs on service read methods → reduces redundant DB calls.
5. **Configure Vite code splitting** with `React.lazy()` per route → improves initial load time.
6. **Set `resolve-lazily: true`** in `application.yml` → unblocks upload threads.
7. **Tune thread pools and gRPC keep-alive** to match deployment target.
8. **Lazy-import recharts** in the Report route and configure Lucide tree-shaking.

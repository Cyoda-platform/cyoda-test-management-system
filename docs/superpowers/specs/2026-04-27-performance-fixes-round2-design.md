# Performance Fixes Round 2 Design

**Date:** 2026-04-27  
**Status:** Approved  
**Scope:** Backend (Java 21 + Spring Boot 3.5) + Frontend (React 18 + TypeScript)

---

## Problem Statement

After Round 1 performance fixes, four items from the original performance report remain:
1. No caching on SuiteService, TestCaseService, TestStepService, TestRunCaseService
2. `conditionByField` rebuilds ObjectMapper-backed condition objects on every search call
3. Thread pool sizes are hardcoded at 20/20/3, not scaling with available CPU cores
4. `filteredSuitesWithCases` useMemo in RunExecution.tsx uses unstable array references as dependencies

Concurrency race on FSM transitions was explicitly deferred — it requires a Cyoda schema migration and does not cause user-visible slowness in practice.

---

## Design

### 1. Cache on Hot Services

Add four new named caches to the existing `CacheConfig.java` (Caffeine, TTL 30 s, max 1 000 entries).

| Cache name | Service | Method | Key |
|---|---|---|---|
| `suitesByProject` | `SuiteService` | `getAllSuitesByProjectId(UUID projectId)` | `#projectId` |
| `casesBySuite` | `TestCaseService` | `getTestCasesBySuiteId(UUID suiteId, int page, int size)` | `#suiteId + ':' + #page + ':' + #size` |
| `stepsByCase` | `TestStepService` | `getTestStepsByTestCaseId(UUID caseId)` | `#caseId` |
| `runCasesByRun` | `TestRunCaseService` | `getAllTestRunCasesByRunId(UUID runId)` | `#runId` |

Cache eviction: every create / update / delete method in the owning service evicts its cache with `allEntries = true` (same pattern as `TestRunService`).

**Files changed:**
- `CacheConfig.java` — add 4 cache names, keep existing Caffeine spec
- `SuiteService.java` — `@Cacheable` on `getAllSuitesByProjectId`, `@CacheEvict` on create/update/delete
- `TestCaseService.java` — `@Cacheable` on `getTestCasesBySuiteId`, `@CacheEvict` on create/update/delete/move
- `TestStepService.java` — `@Cacheable` on `getTestStepsByTestCaseId`, `@CacheEvict` on create/update/delete
- `TestRunCaseService.java` — `@Cacheable` on `getAllTestRunCasesByRunId`, `@CacheEvict` on create/update/delete

---

### 2. Condition Objects — Replace ObjectMapper with TextNode

Every service has a `conditionByField(String fieldName, Object value)` helper that calls `objectMapper.valueToTree(value)`. For UUID string values this allocates a full Jackson tree unnecessarily.

**Fix:** Replace `objectMapper.valueToTree(value)` with `com.fasterxml.jackson.databind.node.TextNode.valueOf(value.toString())` in each service's `conditionByField` method. Remove the `ObjectMapper` constructor dependency from services that only use it for this purpose.

**Files changed:**
- `SuiteService.java`
- `TestCaseService.java`
- `TestStepService.java`
- `TestRunCaseService.java`
- `TestRunService.java`
- `AttachmentService.java`
- `DefectService.java`

**Note:** Services that use `ObjectMapper` for other purposes (e.g., serialisation) keep the dependency; only the `conditionByField` call site changes.

---

### 3. Dynamic Thread Pool Sizing

**File:** `apps/backend/src/main/resources/application.yml`

Change static values to env-var overrides that default to `0` (meaning "auto"):

```yaml
processor-thread-pool: ${PROCESSOR_THREAD_POOL:0}
criteria-thread-pool:  ${CRITERIA_THREAD_POOL:0}
control-thread-pool:   ${CONTROL_THREAD_POOL:3}
```

**File:** The framework config class that reads these values (in `common/` — but we cannot modify `common/`).

Since `common/` is read-only, we cannot change how the thread pool value is consumed. Instead, set a sensible default directly in `application.yml` that scales with typical deployment sizes:

```yaml
processor-thread-pool: ${PROCESSOR_THREAD_POOL:40}
criteria-thread-pool:  ${CRITERIA_THREAD_POOL:40}
control-thread-pool:   ${CONTROL_THREAD_POOL:3}
```

Rationale: doubling from 20 to 40 accommodates 8–16 core containers (the typical cloud VM) without oversubscription, while still being overridable per-environment via env vars. A runtime formula is not possible because `common/` reads the value at startup from config.

---

### 4. Stable useMemo Dependencies in RunExecution.tsx

**File:** `apps/frontend/src/pages/RunExecution.tsx`

Replace the unstable array references `run?.caseIds` and `runCases` in `filteredSuitesWithCases` with stable primitives:

```tsx
const caseIdsKey = run?.caseIds?.join(',') ?? '';

const filteredSuitesWithCases = useMemo(() => {
  // existing logic unchanged
}, [getRunCaseTestCaseId, suitesWithCases, caseIdsKey, runCases.length]);
```

- `caseIdsKey` — string, only changes when IDs actually change
- `runCases.length` — number, only changes when cases are added/removed

---

## Out of Scope

- Concurrency race on FSM transitions — deferred (requires Cyoda schema migration, not user-visible in practice)
- Caching on `DefectService`, `AttachmentService`, `ReportService` — low call frequency, not on hot path
- `getAllCasesByProjectId` on `TestCaseService` — used only for cascade delete, not for UI rendering

---

## Testing

- Unit tests for each `@Cacheable` method: verify second call hits cache (same pattern as `TestRunServiceCacheTest`)
- Verify `TextNode` replacement compiles and existing tests pass
- Frontend: existing vitest suite must pass after memoization change

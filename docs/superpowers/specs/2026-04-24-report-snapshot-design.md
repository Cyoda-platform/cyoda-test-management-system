# Report Snapshot Design

**Date:** 2026-04-24  
**Status:** Approved

## Problem

Reports currently fetch live data (`TestRunCase.status`, `run.stepStatuses`, defects) on every page open. If test statuses change after a report is created, the report reflects the new state — breaking the contract that a report captures progress at a specific point in time.

## Goal

Reports must be **point-in-time snapshots**: KPIs, Suite Analysis, and the Defect Table are frozen at creation time and never change after.

## Out of Scope (separate tickets)

- Public/shareable HTML link for colleagues without accounts
- PDF improvements beyond current `window.print()`

---

## Data Schema

### Backend — `ReportDTO.java`

Add one field:

```java
@JsonInclude(JsonInclude.Include.NON_EMPTY)
private String snapshotData; // serialized JSON, empty for legacy reports
```

### Frontend — `api.ts`, `Report` interface

```typescript
snapshotData?: string;
```

### Snapshot JSON structure

```json
{
  "totalPassed": 10,
  "totalFailed": 2,
  "totalSkipped": 1,
  "totalUntested": 5,
  "suiteData": [
    { "suite": "Login Suite", "passed": 5, "failed": 1, "skipped": 0, "untested": 2 }
  ],
  "defects": [
    {
      "id": "uuid",
      "displayId": "DEF-01",
      "title": "Button crashes on click",
      "severity": "Critical",
      "status": "Open",
      "link": "https://jira.company.com/...",
      "source": "TR-01 · Step 2",
      "createdAt": "2026-04-24"
    }
  ]
}
```

`suiteData` is pre-sorted by suite `sortOrder` at creation time — no re-sorting needed on view.

---

## CreateReport.tsx — Snapshot Computation

`handleCreate` becomes `async`. On "Create Report" click:

### 1. Determine effective run IDs
```
selectedRuns.size > 0 ? Array.from(selectedRuns) : runs.map(r => r.id)
```

### 2. Fetch data in parallel (Promise.all)
- `testRunCasesApi.list(projectId, runId)` for each run → provides `suiteId` + `status`
- `defectsApi.listByRun(projectId, runId)` for each run → provides defect list

### 3. Compute snapshot
- Build `runStepStatusMap` from `run.stepStatuses` (reliable source — updated on every step click)
- Override `TestRunCase.status` from `stepStatuses` where available (same fix applied to ReportDetail)
- `deduplicateRunCases` → keeps latest-run record per `testCaseId`
- `groupBySuite` → count statuses per suite, sorted by `sortOrder` from `suites`
- Count KPI totals from deduplicated cases
- Deduplicate defects by `id` across runs

### 4. Create report
Send `snapshotData: JSON.stringify({ totalPassed, totalFailed, totalSkipped, totalUntested, suiteData, defects })` in the create body.

### UX
- Button shows `"Формируем отчёт…"` while fetching
- If any fetch fails: create report without `snapshotData` (fallback, do not block user)

### Hooks to add to CreateReport
- `useSuites(projectId!)` — for suite `sortOrder` and name mapping

---

## ReportDetail.tsx — Snapshot-aware Rendering

### Snapshot detection
```typescript
const parsedSnapshot = useMemo(() => {
  if (!report?.snapshotData) return null;
  try { return JSON.parse(report.snapshotData); }
  catch { return null; }
}, [report?.snapshotData]);
```

### Data fetching — skip when snapshot present
```typescript
// resolvedRunIds = [] when snapshot exists → useTestRunCasesForRuns makes no requests
const resolvedRunIds = useMemo(() => {
  if (parsedSnapshot) return [];
  ...
}, [parsedSnapshot, report, allRuns]);
```

`useDefects` call is also bypassed — defect data comes from snapshot.

### KPIs (Executive Summary)
Use `parsedSnapshot.totalPassed` etc. when present; fall back to live `deduped` computation for legacy reports.

### Suite Analysis chart
Use `parsedSnapshot.suiteData` directly when present (already sorted); fall back to live `groupBySuite` computation.

### Defect Table
Use `parsedSnapshot.defects` when present; fall back to live `useDefects`.

**Table changes — read-only mode:**
- Remove severity/status `Select` dropdowns → plain styled text badges
- Remove Edit, Delete, View buttons and their modals
- Remove `ViewDefectModal` and `EditDefectModal` imports
- Keep `ExternalLink` button → opens `defect.link` in new tab
- Source column unchanged

### Hooks removed when snapshot present
| Hook | Removed |
|---|---|
| `useTestRunCasesForRuns` | ✅ (resolvedRunIds = []) |
| `useDefects` | ✅ (data from snapshot) |
| `useSuites` | ✅ (names already in snapshot) |
| `useProject`, `useTestRuns`, `useReport` | kept (breadcrumbs, Environment Info) |

---

## Backward Compatibility

Legacy reports without `snapshotData` continue to use current live-fetch behavior. No migration needed — legacy reports will be deleted during testing phase.

---

## Files Changed

| File | Change |
|---|---|
| `apps/backend/.../dto/ReportDTO.java` | Add `snapshotData` String field |
| `apps/frontend/src/lib/api.ts` | Add `snapshotData?: string` to `Report` |
| `apps/frontend/src/pages/CreateReport.tsx` | Async `handleCreate`, snapshot computation |
| `apps/frontend/src/pages/ReportDetail.tsx` | Snapshot-aware rendering, read-only defect table |

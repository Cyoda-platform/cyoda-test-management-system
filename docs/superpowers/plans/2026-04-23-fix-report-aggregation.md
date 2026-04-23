# Fix Report Aggregation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the Executive Summary KPI double-count bug and Suite Analysis blindness to `selectedRuns` by replacing global step-based aggregation with `TestRunCase`-based aggregation; remove the unused Date Range field from the report creation form.

**Architecture:** Extract pure aggregation functions into a testable utility (`reportAggregation.ts`). Add a `useTestRunCasesForRuns` hook that fetches `TestRunCase` records for multiple runs in parallel. Rewrite `ReportDetail` to use these instead of the global `testStepsApi` pipeline. Delete the dead `casesQueries` / `stepsQueries` code and the Date Range UI in `CreateReport`.

**Tech Stack:** React 18, TypeScript, TanStack Query v5 (`useQueries`), Vitest 3

---

## File Map

| Action | Path | Purpose |
|---|---|---|
| **Create** | `apps/frontend/src/lib/reportAggregation.ts` | Pure aggregation functions (deduplicate, group by suite) |
| **Create** | `apps/frontend/src/test/reportAggregation.test.ts` | Unit tests for aggregation logic |
| **Modify** | `apps/frontend/src/lib/api.ts:502-510` | Add `suiteId`, `title`, `startedAt?`, `completedAt?` to `TestRunCase` interface |
| **Modify** | `apps/frontend/src/hooks/useApi.ts` | Add `useTestRunCasesForRuns` hook |
| **Modify** | `apps/frontend/src/pages/ReportDetail.tsx` | Replace step-based pipeline with TestRunCase aggregation |
| **Modify** | `apps/frontend/src/pages/CreateReport.tsx` | Remove Date Range section |

---

## Task 1: Write failing tests for aggregation logic

**Files:**
- Create: `apps/frontend/src/test/reportAggregation.test.ts`

- [ ] **Step 1: Create the test file**

```typescript
// apps/frontend/src/test/reportAggregation.test.ts
import { describe, it, expect } from 'vitest';
import { deduplicateRunCases, groupBySuite } from '@/lib/reportAggregation';

describe('deduplicateRunCases', () => {
  it('returns a single case from a single run unchanged', () => {
    const input = [
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r1', status: 'PASSED', runCreatedAt: '2025-01-01T00:00:00Z' },
    ];
    const result = deduplicateRunCases(input);
    expect(result).toHaveLength(1);
    expect(result[0].status).toBe('PASSED');
  });

  it('keeps the latest status when the same case appears in two runs', () => {
    const input = [
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r1', status: 'FAILED', runCreatedAt: '2025-01-01T00:00:00Z' },
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r2', status: 'PASSED', runCreatedAt: '2025-01-02T00:00:00Z' },
    ];
    const result = deduplicateRunCases(input);
    expect(result).toHaveLength(1);
    expect(result[0].status).toBe('PASSED');
    expect(result[0].testRunId).toBe('r2');
  });

  it('keeps all cases when they have different testCaseIds', () => {
    const input = [
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r1', status: 'PASSED', runCreatedAt: '2025-01-01T00:00:00Z' },
      { testCaseId: 'c2', suiteId: 's1', testRunId: 'r1', status: 'FAILED', runCreatedAt: '2025-01-01T00:00:00Z' },
    ];
    const result = deduplicateRunCases(input);
    expect(result).toHaveLength(2);
  });

  it('returns empty array for empty input', () => {
    expect(deduplicateRunCases([])).toHaveLength(0);
  });

  it('handles three runs: picks the latest (third) status', () => {
    const input = [
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r1', status: 'FAILED',  runCreatedAt: '2025-01-01T00:00:00Z' },
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r2', status: 'SKIPPED', runCreatedAt: '2025-01-02T00:00:00Z' },
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r3', status: 'PASSED',  runCreatedAt: '2025-01-03T00:00:00Z' },
    ];
    const result = deduplicateRunCases(input);
    expect(result).toHaveLength(1);
    expect(result[0].status).toBe('PASSED');
  });
});

describe('groupBySuite', () => {
  it('groups cases by suiteId and counts statuses', () => {
    const cases = [
      { testCaseId: 'c1', suiteId: 's1', testRunId: 'r1', status: 'PASSED',   runCreatedAt: '' },
      { testCaseId: 'c2', suiteId: 's1', testRunId: 'r1', status: 'FAILED',   runCreatedAt: '' },
      { testCaseId: 'c3', suiteId: 's2', testRunId: 'r1', status: 'UNTESTED', runCreatedAt: '' },
    ];
    const suiteNameMap = new Map([['s1', 'Login Suite'], ['s2', 'Checkout Suite']]);
    const result = groupBySuite(cases, suiteNameMap);
    expect(result).toHaveLength(2);

    const login = result.find(s => s.suite === 'Login Suite')!;
    expect(login.passed).toBe(1);
    expect(login.failed).toBe(1);
    expect(login.skipped).toBe(0);
    expect(login.untested).toBe(0);

    const checkout = result.find(s => s.suite === 'Checkout Suite')!;
    expect(checkout.untested).toBe(1);
  });

  it('falls back to suiteId as name when suite not in map', () => {
    const cases = [
      { testCaseId: 'c1', suiteId: 's-unknown', testRunId: 'r1', status: 'PASSED', runCreatedAt: '' },
    ];
    const result = groupBySuite(cases, new Map());
    expect(result[0].suite).toBe('s-unknown');
  });

  it('skips cases with empty suiteId', () => {
    const cases = [
      { testCaseId: 'c1', suiteId: '', testRunId: 'r1', status: 'PASSED', runCreatedAt: '' },
    ];
    expect(groupBySuite(cases, new Map())).toHaveLength(0);
  });

  it('returns empty array for empty input', () => {
    expect(groupBySuite([], new Map())).toHaveLength(0);
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail (module not found)**

```bash
cd apps/frontend && npm run test -- reportAggregation
```

Expected: FAIL — `Cannot find module '@/lib/reportAggregation'`

---

## Task 2: Implement aggregation utilities

**Files:**
- Create: `apps/frontend/src/lib/reportAggregation.ts`

- [ ] **Step 1: Create the module with types and implementations**

```typescript
// apps/frontend/src/lib/reportAggregation.ts

export interface RunCaseWithMeta {
  testCaseId: string;
  suiteId: string;
  testRunId: string;
  status: string;
  /** ISO timestamp of the parent run's createdAt — used to resolve latest status */
  runCreatedAt: string;
}

export interface SuiteData {
  suite: string;
  passed: number;
  failed: number;
  skipped: number;
  untested: number;
}

/**
 * Deduplicates run case records by testCaseId.
 * When the same test case appears in multiple runs, the record from the
 * most recently created run (highest runCreatedAt) is kept.
 */
export function deduplicateRunCases(cases: RunCaseWithMeta[]): RunCaseWithMeta[] {
  const latest = new Map<string, RunCaseWithMeta>();
  for (const rc of cases) {
    const prev = latest.get(rc.testCaseId);
    if (!prev || rc.runCreatedAt > prev.runCreatedAt) {
      latest.set(rc.testCaseId, rc);
    }
  }
  return Array.from(latest.values());
}

/**
 * Groups deduplicated run case records by suiteId and counts statuses.
 * Records without a suiteId are skipped.
 */
export function groupBySuite(
  cases: RunCaseWithMeta[],
  suiteNameMap: Map<string, string>,
): SuiteData[] {
  const bySuite = new Map<string, SuiteData>();
  for (const rc of cases) {
    if (!rc.suiteId) continue;
    const name = suiteNameMap.get(rc.suiteId) ?? rc.suiteId;
    const entry = bySuite.get(rc.suiteId) ?? { suite: name, passed: 0, failed: 0, skipped: 0, untested: 0 };
    const st = rc.status.toLowerCase();
    if (st === 'passed')       entry.passed++;
    else if (st === 'failed')  entry.failed++;
    else if (st === 'skipped') entry.skipped++;
    else                       entry.untested++;
    bySuite.set(rc.suiteId, entry);
  }
  return Array.from(bySuite.values()).filter(
    s => s.passed + s.failed + s.skipped + s.untested > 0,
  );
}
```

- [ ] **Step 2: Run tests to confirm they pass**

```bash
cd apps/frontend && npm run test -- reportAggregation
```

Expected: all 9 tests PASS

- [ ] **Step 3: Commit**

```bash
git add apps/frontend/src/lib/reportAggregation.ts apps/frontend/src/test/reportAggregation.test.ts
git commit -m "feat(report): add reportAggregation utils with tests"
```

---

## Task 3: Extend TestRunCase interface and add hook

**Files:**
- Modify: `apps/frontend/src/lib/api.ts:502-510`
- Modify: `apps/frontend/src/hooks/useApi.ts`

- [ ] **Step 1: Extend the `TestRunCase` interface in `api.ts`**

Replace the existing interface (lines 502–510):

```typescript
export interface TestRunCase {
  id: string;
  testRunId: string;
  /** References the original TestCase.id */
  testCaseId: string;
  /** References the original Suite.id — snapshot set at run creation */
  suiteId: string;
  /** Test case title snapshot */
  title: string;
  /** Uppercase: 'UNTESTED' | 'PASSED' | 'FAILED' | 'SKIPPED' */
  status: string;
  bugUrl?: string;
  startedAt?: string;
  completedAt?: string;
}
```

- [ ] **Step 2: Add `useTestRunCasesForRuns` hook in `useApi.ts`**

Find the `useTestRunCases` function (around line 800) and add the new hook directly after it:

```typescript
/**
 * Fetches TestRunCase records for multiple runs in parallel.
 * Returns one query result per runId in the same order.
 */
export function useTestRunCasesForRuns(projectId: string, runIds: string[]) {
  return useQueries({
    queries: runIds.map(runId => ({
      queryKey: keys.runCases.all(projectId, runId),
      queryFn:  () => testRunCasesApi.list(projectId, runId),
      enabled:  !!projectId && runIds.length > 0,
      select:   (res: { data: TestRunCase[] }) => res.data,
    })),
  });
}
```

- [ ] **Step 3: Verify the hook import compiles — run the build check**

```bash
cd apps/frontend && npm run build 2>&1 | tail -20
```

Expected: build succeeds (no TypeScript errors)

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/lib/api.ts apps/frontend/src/hooks/useApi.ts
git commit -m "feat(report): extend TestRunCase interface + add useTestRunCasesForRuns hook"
```

---

## Task 4: Fix ReportDetail aggregation

**Files:**
- Modify: `apps/frontend/src/pages/ReportDetail.tsx`

- [ ] **Step 1: Add imports at the top of the file**

After the existing imports, add:

```typescript
import { deduplicateRunCases, groupBySuite } from '@/lib/reportAggregation';
import type { RunCaseWithMeta } from '@/lib/reportAggregation';
import { useTestRunCasesForRuns } from '@/hooks/useApi';
```

- [ ] **Step 2: Replace the dead data pipeline in the hooks section**

Find and remove this block (approximately lines 69–91):

```typescript
// ── Suite Analysis — real data pipeline ─────────────────────────────────────
const { data: suites = [] } = useSuites(projectId!);

const casesQueries = useQueries({
  queries: suites.map((suite) => ({
    queryKey: keys.cases.all(projectId!, suite.id),
    queryFn:  () => testCasesApi.list(projectId!, suite.id),
    enabled:  !!projectId && suites.length > 0,
    select:   (res: { data: Array<{ id: string; suiteId: string }> }) => res.data,
  })),
});

const casePairs = suites.flatMap((suite, i) =>
  (casesQueries[i]?.data ?? []).map((c) => ({ suiteId: suite.id, caseId: c.id }))
);

const stepsQueries = useQueries({
  queries: casePairs.map(({ suiteId, caseId }) => ({
    queryKey: keys.steps.all(projectId!, suiteId, caseId),
    queryFn:  () => testStepsApi.list(projectId!, suiteId, caseId),
    enabled:  !!projectId && casePairs.length > 0,
  })),
});
```

Replace it with:

```typescript
const { data: suites = [] } = useSuites(projectId!);

// Resolve run IDs before early returns so hook call count is stable.
// When report is not yet loaded, resolvedRunIds is [] → no queries fired.
const resolvedRunIds = useMemo(() => {
  if (!report) return [];
  const ids = report.selectedRuns ?? [];
  return ids.length > 0 ? ids : allRuns.map(r => r.id);
}, [report, allRuns]);

const runCasesQueries = useTestRunCasesForRuns(projectId!, resolvedRunIds);

// Flatten all TestRunCase records, attaching the parent run's createdAt so
// deduplicateRunCases can pick the latest status per test case.
const deduped = useMemo((): RunCaseWithMeta[] => {
  const runCreatedAtMap = new Map(allRuns.map(r => [r.id, r.createdAt ?? '']));
  const allRunCases: RunCaseWithMeta[] = runCasesQueries.flatMap(q =>
    (q.data ?? []).map(rc => ({
      testCaseId:   rc.testCaseId,
      suiteId:      rc.suiteId,
      testRunId:    rc.testRunId,
      status:       rc.status,
      runCreatedAt: runCreatedAtMap.get(rc.testRunId) ?? '',
    })),
  );
  return deduplicateRunCases(allRunCases);
}, [runCasesQueries, allRuns]);
```

- [ ] **Step 3: Replace the `suiteData` useMemo**

Find and replace the existing `suiteData` useMemo (approximately lines 130–150):

```typescript
// Remove:
const suiteData = useMemo(() => {
  if (!suites.length) return [];
  let pairIdx = 0;
  return suites
    .map((suite, i) => {
      ...stepsQueries logic...
    })
    .filter(...);
}, [suites, casesQueries, stepsQueries]);
```

Replace with:

```typescript
const suiteData = useMemo(() => {
  const suiteNameMap = new Map(suites.map(s => [s.id, s.name]));
  return groupBySuite(deduped, suiteNameMap);
}, [deduped, suites]);
```

- [ ] **Step 4: Replace the KPI calculations (after the early returns)**

Find this block (approximately lines 174–183):

```typescript
const totalPassed   = linkedRuns.reduce((s, r) => s + r.passed,   0);
const totalFailed   = linkedRuns.reduce((s, r) => s + r.failed,   0);
const totalSkipped  = linkedRuns.reduce((s, r) => s + r.skipped,  0);
const totalUntested = linkedRuns.reduce((s, r) => s + r.untested, 0);
```

Replace with:

```typescript
const totalPassed   = deduped.filter(rc => rc.status === 'PASSED').length;
const totalFailed   = deduped.filter(rc => rc.status === 'FAILED').length;
const totalSkipped  = deduped.filter(rc => rc.status === 'SKIPPED').length;
const totalUntested = deduped.filter(rc => rc.status === 'UNTESTED').length;
```

- [ ] **Step 5: Remove unused imports**

Remove from the import block at the top of `ReportDetail.tsx`:
- `testCasesApi` (no longer used)
- `testStepsApi` (no longer used)
- `useQueries` (no longer used — `useTestRunCasesForRuns` is used instead)
- `keys` (check if still used; remove if only referenced in the deleted queries)

Also remove the dead `computeCaseStatus` function definition (lines 51–58) — it is no longer called.

- [ ] **Step 6: Build check**

```bash
cd apps/frontend && npm run build 2>&1 | tail -20
```

Expected: no TypeScript errors

- [ ] **Step 7: Commit**

```bash
git add apps/frontend/src/pages/ReportDetail.tsx
git commit -m "fix(report): compute KPI and suite analysis from TestRunCase records"
```

---

## Task 5: Remove Date Range from CreateReport

**Files:**
- Modify: `apps/frontend/src/pages/CreateReport.tsx`

- [ ] **Step 1: Remove the two state variables**

Find and remove these two lines (approximately line 46–47):

```typescript
const [dateFrom, setDateFrom] = useState('');
const [dateTo, setDateTo]     = useState('');
```

- [ ] **Step 2: Remove the Date Range block from `handleCreate`**

Find and remove these two lines inside the `body` object passed to `createReport.mutate` (approximately lines 82–83):

```typescript
dateFrom: dateFrom || undefined,
dateTo:   dateTo   || undefined,
```

- [ ] **Step 3: Remove the Date Range JSX section**

Find and remove the entire section (approximately lines 149–157):

```tsx
{/* Date Range Filter */}
<div>
  <label className={labelCls}>Date Range</label>
  <div className="flex gap-3 mt-1.5">
    <Input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} className="flex-1 h-10 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
    <span className="self-center text-muted-foreground text-sm">to</span>
    <Input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} className="flex-1 h-10 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
  </div>
</div>
```

- [ ] **Step 4: Build check**

```bash
cd apps/frontend && npm run build 2>&1 | tail -20
```

Expected: no TypeScript errors

- [ ] **Step 5: Run all tests**

```bash
cd apps/frontend && npm run test
```

Expected: all tests PASS (including the 9 reportAggregation tests)

- [ ] **Step 6: Commit**

```bash
git add apps/frontend/src/pages/CreateReport.tsx
git commit -m "fix(report): remove unused date range fields from create form"
```

---

## Task 6: Push

- [ ] **Step 1: Push to origin**

```bash
git push origin main
```

# Report Snapshot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Freeze report KPIs, Suite Analysis, and Defect Table at creation time so they never update when test statuses change later.

**Architecture:** At report creation, the frontend fetches TestRunCase and defect data for all selected runs, computes a snapshot, and stores it as a JSON string in `ReportDTO.snapshotData`. When viewing, ReportDetail reads the snapshot instead of making live API calls — falling back to live data for legacy reports without a snapshot.

**Tech Stack:** Java 21 / Spring Boot (backend DTO), React 18 + TypeScript + Vite (frontend), vitest (unit tests), existing `deduplicateRunCases` / `groupBySuite` utilities from `reportAggregation.ts`.

---

## File Map

| File | Action | Purpose |
|---|---|---|
| `apps/backend/src/main/java/com/java_template/application/dto/ReportDTO.java` | Modify | Add `snapshotData` String field |
| `apps/frontend/src/lib/api.ts` | Modify | Add `snapshotData?: string` to `Report` interface |
| `apps/frontend/src/lib/reportSnapshot.ts` | **Create** | Pure `computeSnapshotData` function + types |
| `apps/frontend/src/test/reportSnapshot.test.ts` | **Create** | Unit tests for snapshot computation |
| `apps/frontend/src/pages/CreateReport.tsx` | Modify | Async `handleCreate` that computes + sends snapshot |
| `apps/frontend/src/pages/ReportDetail.tsx` | Modify | Parse snapshot, skip live fetches, read-only defect table |

---

## Task 1: Backend — Add `snapshotData` to ReportDTO

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/dto/ReportDTO.java`

- [ ] **Step 1: Add the field**

Open `ReportDTO.java`. After the `updatedAt` field (around line 66), add:

```java
/**
 * JSON-serialized point-in-time snapshot of KPIs, suite breakdown, and defects.
 * Computed by the frontend at report creation time. Empty for legacy reports.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
private String snapshotData;
```

The class already has `@JsonInclude(JsonInclude.Include.NON_NULL)` at the class level, but we want `NON_EMPTY` specifically so an empty string is also excluded.

- [ ] **Step 2: Compile to verify no errors**

```bash
./gradlew :apps:backend:compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/dto/ReportDTO.java
git commit -m "feat(report): add snapshotData field to ReportDTO"
```

---

## Task 2: Frontend type — Add `snapshotData` to Report interface

**Files:**
- Modify: `apps/frontend/src/lib/api.ts`

- [ ] **Step 1: Add the field to the Report interface**

In `apps/frontend/src/lib/api.ts`, find the `Report` interface (around line 362). Add after `updatedAt`:

```typescript
export interface Report {
  id: string;
  displayId?: string;
  projectId: string;
  name: string;
  type: 'Summary' | 'Regression' | 'Sprint' | 'Custom';
  description: string;
  createdBy: string;
  dateFrom?: string;
  dateTo?: string;
  selectedRuns: string[];
  sectionExecutiveSummary: boolean;
  sectionSuiteAnalytics:   boolean;
  sectionDefectTable:      boolean;
  sectionEnvironmentInfo:  boolean;
  createdAt: string;
  updatedAt?: string;
  /** JSON-serialized ReportSnapshotData. Present on all new reports; absent on legacy. */
  snapshotData?: string;
}
```

- [ ] **Step 2: Type-check**

```bash
cd apps/frontend && npx tsc --noEmit
```

Expected: no output (no errors).

- [ ] **Step 3: Commit**

```bash
git add apps/frontend/src/lib/api.ts
git commit -m "feat(report): add snapshotData to Report interface"
```

---

## Task 3: Create `reportSnapshot.ts` + unit tests (TDD)

**Files:**
- Create: `apps/frontend/src/lib/reportSnapshot.ts`
- Create: `apps/frontend/src/test/reportSnapshot.test.ts`

This task extracts the snapshot computation into a pure, testable function. It uses the existing `deduplicateRunCases` and `groupBySuite` utilities.

- [ ] **Step 1: Write the failing tests**

Create `apps/frontend/src/test/reportSnapshot.test.ts`:

```typescript
import { describe, it, expect } from 'vitest';
import { computeSnapshotData } from '@/lib/reportSnapshot';
import type { TestRunCase, TestRun, Suite, Defect } from '@/lib/api';

const baseRun = (overrides: Partial<TestRun> = {}): TestRun => ({
  id: 'r1', name: 'Run 1', projectId: 'p1', environment: 'Staging',
  buildVersion: '1.0', description: '', status: 'active',
  passed: 0, failed: 0, skipped: 0, untested: 0,
  createdAt: '2026-01-01T00:00:00Z',
  ...overrides,
});

const baseCase = (overrides: Partial<TestRunCase> = {}): TestRunCase => ({
  id: 'rc1', testRunId: 'r1', testCaseId: 'c1',
  status: 'UNTESTED', suiteId: 's1', title: 'Case 1',
  ...overrides,
});

const baseSuite = (overrides: Partial<Suite> = {}): Suite => ({
  id: 's1', projectId: 'p1', name: 'Suite 1', sortOrder: 1,
  ...overrides,
});

describe('computeSnapshotData — KPI totals', () => {
  it('counts PASSED, FAILED, SKIPPED, UNTESTED correctly', () => {
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testCaseId: 'c1', status: 'PASSED' }),
      baseCase({ id: 'rc2', testCaseId: 'c2', status: 'FAILED' }),
      baseCase({ id: 'rc3', testCaseId: 'c3', status: 'SKIPPED' }),
      baseCase({ id: 'rc4', testCaseId: 'c4', status: 'UNTESTED' }),
    ];
    const result = computeSnapshotData(cases, [baseRun()], [baseSuite()], []);
    expect(result.totalPassed).toBe(1);
    expect(result.totalFailed).toBe(1);
    expect(result.totalSkipped).toBe(1);
    expect(result.totalUntested).toBe(1);
  });

  it('returns all zeros for empty run cases', () => {
    const result = computeSnapshotData([], [], [], []);
    expect(result.totalPassed).toBe(0);
    expect(result.totalFailed).toBe(0);
    expect(result.totalSkipped).toBe(0);
    expect(result.totalUntested).toBe(0);
  });
});

describe('computeSnapshotData — stepStatuses override', () => {
  it('uses stepStatuses over TestRunCase.status when all steps PASSED', () => {
    const stepStatuses = JSON.stringify({ 'c1::1': 'PASSED', 'c1::2': 'PASSED' });
    const cases: TestRunCase[] = [baseCase({ status: 'UNTESTED' })];
    const runs: TestRun[] = [baseRun({ stepStatuses: stepStatuses as any })];
    const result = computeSnapshotData(cases, runs, [baseSuite()], []);
    expect(result.totalPassed).toBe(1);
    expect(result.totalUntested).toBe(0);
  });

  it('uses stepStatuses: marks FAILED if any step failed', () => {
    const stepStatuses = JSON.stringify({ 'c1::1': 'PASSED', 'c1::2': 'FAILED' });
    const cases: TestRunCase[] = [baseCase({ status: 'UNTESTED' })];
    const runs: TestRun[] = [baseRun({ stepStatuses: stepStatuses as any })];
    const result = computeSnapshotData(cases, runs, [baseSuite()], []);
    expect(result.totalFailed).toBe(1);
    expect(result.totalPassed).toBe(0);
  });

  it('falls back to TestRunCase.status when no stepStatuses for that case', () => {
    // stepStatuses is for a different case (c2), not c1
    const stepStatuses = JSON.stringify({ 'c2::1': 'PASSED' });
    const cases: TestRunCase[] = [baseCase({ testCaseId: 'c1', status: 'PASSED' })];
    const runs: TestRun[] = [baseRun({ stepStatuses: stepStatuses as any })];
    const result = computeSnapshotData(cases, runs, [baseSuite()], []);
    expect(result.totalPassed).toBe(1);
  });
});

describe('computeSnapshotData — deduplication', () => {
  it('keeps latest run status when same testCaseId appears in two runs', () => {
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testRunId: 'r1', testCaseId: 'c1', status: 'FAILED' }),
      baseCase({ id: 'rc2', testRunId: 'r2', testCaseId: 'c1', status: 'PASSED' }),
    ];
    const runs: TestRun[] = [
      baseRun({ id: 'r1', createdAt: '2026-01-01T00:00:00Z' }),
      baseRun({ id: 'r2', createdAt: '2026-01-02T00:00:00Z' }),
    ];
    const result = computeSnapshotData(cases, runs, [baseSuite()], []);
    expect(result.totalPassed).toBe(1);
    expect(result.totalFailed).toBe(0);
  });
});

describe('computeSnapshotData — suiteData', () => {
  it('groups cases by suite and counts statuses', () => {
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testCaseId: 'c1', suiteId: 's1', status: 'PASSED' }),
      baseCase({ id: 'rc2', testCaseId: 'c2', suiteId: 's1', status: 'FAILED' }),
      baseCase({ id: 'rc3', testCaseId: 'c3', suiteId: 's2', status: 'UNTESTED' }),
    ];
    const suites: Suite[] = [
      baseSuite({ id: 's1', name: 'Suite A', sortOrder: 1 }),
      baseSuite({ id: 's2', name: 'Suite B', sortOrder: 2 }),
    ];
    const result = computeSnapshotData(cases, [baseRun()], suites, []);
    expect(result.suiteData).toHaveLength(2);
    const a = result.suiteData.find(s => s.suite === 'Suite A')!;
    expect(a.passed).toBe(1);
    expect(a.failed).toBe(1);
    const b = result.suiteData.find(s => s.suite === 'Suite B')!;
    expect(b.untested).toBe(1);
  });

  it('sorts suiteData by sortOrder ascending', () => {
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testCaseId: 'c1', suiteId: 's2', status: 'PASSED' }),
      baseCase({ id: 'rc2', testCaseId: 'c2', suiteId: 's1', status: 'PASSED' }),
    ];
    const suites: Suite[] = [
      baseSuite({ id: 's1', name: 'First', sortOrder: 1 }),
      baseSuite({ id: 's2', name: 'Second', sortOrder: 2 }),
    ];
    const result = computeSnapshotData(cases, [baseRun()], suites, []);
    expect(result.suiteData[0].suite).toBe('First');
    expect(result.suiteData[1].suite).toBe('Second');
  });

  it('skips cases with empty suiteId', () => {
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testCaseId: 'c1', suiteId: '', status: 'PASSED' }),
    ];
    const result = computeSnapshotData(cases, [baseRun()], [], []);
    expect(result.suiteData).toHaveLength(0);
  });
});

describe('computeSnapshotData — defects', () => {
  const baseDefect = (overrides: Partial<Defect> = {}): Defect => ({
    id: 'd1', projectId: 'p1', title: 'Bug 1', description: '',
    severity: 'Major', status: 'Open', link: 'https://jira/1',
    source: 'Step 1', createdAt: '2026-01-01',
    ...overrides,
  });

  it('includes all defect fields in snapshot', () => {
    const result = computeSnapshotData([], [], [], [baseDefect()]);
    expect(result.defects).toHaveLength(1);
    expect(result.defects[0].id).toBe('d1');
    expect(result.defects[0].title).toBe('Bug 1');
    expect(result.defects[0].link).toBe('https://jira/1');
  });

  it('deduplicates defects by id', () => {
    const result = computeSnapshotData([], [], [], [
      baseDefect({ id: 'd1' }),
      baseDefect({ id: 'd1' }),
      baseDefect({ id: 'd2', title: 'Bug 2' }),
    ]);
    expect(result.defects).toHaveLength(2);
  });

  it('returns empty defects array for no defects', () => {
    const result = computeSnapshotData([], [], [], []);
    expect(result.defects).toHaveLength(0);
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd apps/frontend && npm run test -- reportSnapshot
```

Expected: `FAIL` — `Cannot find module '@/lib/reportSnapshot'`

- [ ] **Step 3: Implement `reportSnapshot.ts`**

Create `apps/frontend/src/lib/reportSnapshot.ts`:

```typescript
import { deduplicateRunCases, groupBySuite } from './reportAggregation';
import type { RunCaseWithMeta } from './reportAggregation';
import type { TestRunCase, TestRun, Suite, Defect } from './api';

export interface SnapshotDefect {
  id: string;
  displayId?: string;
  title: string;
  severity: string;
  status: string;
  link: string;
  source: string;
  createdAt: string;
}

export interface ReportSnapshotData {
  totalPassed:   number;
  totalFailed:   number;
  totalSkipped:  number;
  totalUntested: number;
  suiteData: Array<{ suite: string; passed: number; failed: number; skipped: number; untested: number }>;
  defects: SnapshotDefect[];
}

function computeCaseStatusFromSteps(steps: string[]): string {
  if (steps.length === 0)                                                    return 'UNTESTED';
  if (steps.some(s => s === 'failed'))                                       return 'FAILED';
  if (steps.every(s => s === 'passed'))                                      return 'PASSED';
  if (steps.some(s => s === 'skipped') && !steps.some(s => s === 'untested')) return 'SKIPPED';
  return 'UNTESTED';
}

/**
 * Compute a point-in-time snapshot from raw run data.
 *
 * Uses run.stepStatuses (the flat caseId::stepNumber → STATUS map saved on the
 * TestRun entity) as the authoritative status source — same logic as RunExecution
 * and ReportDetail. Falls back to TestRunCase.status when stepStatuses has no
 * entries for a given case.
 */
export function computeSnapshotData(
  rawRunCases: TestRunCase[],
  runs:        TestRun[],
  suites:      Suite[],
  rawDefects:  Defect[],
): ReportSnapshotData {
  // Parse each run's flat step-status map
  const runStepStatusMap = new Map<string, Record<string, string>>();
  runs.forEach(run => {
    if (!run.stepStatuses) return;
    try {
      const parsed: Record<string, string> =
        typeof run.stepStatuses === 'string'
          ? JSON.parse(run.stepStatuses as unknown as string)
          : (run.stepStatuses as unknown as Record<string, string>);
      if (Object.keys(parsed).length > 0) runStepStatusMap.set(run.id, parsed);
    } catch { /* malformed JSON — ignore */ }
  });

  const runCreatedAtMap = new Map(runs.map(r => [r.id, r.createdAt ?? '']));

  // Build RunCaseWithMeta with accurate status
  const all: RunCaseWithMeta[] = rawRunCases.map(rc => {
    const stepMap = runStepStatusMap.get(rc.testRunId);
    let status = rc.status;
    if (stepMap) {
      const prefix = `${rc.testCaseId}::`;
      const caseSteps = Object.entries(stepMap)
        .filter(([k]) => k.startsWith(prefix))
        .map(([, v]) => v.toLowerCase());
      if (caseSteps.length > 0) status = computeCaseStatusFromSteps(caseSteps);
    }
    return {
      testCaseId:   rc.testCaseId,
      suiteId:      rc.suiteId ?? '',
      testRunId:    rc.testRunId,
      status,
      runCreatedAt: runCreatedAtMap.get(rc.testRunId) ?? '',
    };
  });

  const deduped = deduplicateRunCases(all);

  // KPI totals
  const totalPassed   = deduped.filter(rc => rc.status === 'PASSED').length;
  const totalFailed   = deduped.filter(rc => rc.status === 'FAILED').length;
  const totalSkipped  = deduped.filter(rc => rc.status === 'SKIPPED').length;
  const totalUntested = deduped.filter(rc => rc.status === 'UNTESTED').length;

  // Suite data — sorted by suite sortOrder
  const suiteNameMap = new Map(suites.map(s => [s.id, s.name]));
  const rawSuiteData = groupBySuite(deduped, suiteNameMap);
  const suiteOrderMap = new Map(
    [...suites]
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
      .map((s, i) => [s.name, i]),
  );
  const suiteData = rawSuiteData.sort(
    (a, b) => (suiteOrderMap.get(a.suite) ?? Infinity) - (suiteOrderMap.get(b.suite) ?? Infinity),
  );

  // Defects — deduplicate by id, preserve field subset needed for read-only display
  const seen = new Set<string>();
  const defects: SnapshotDefect[] = [];
  for (const d of rawDefects) {
    if (seen.has(d.id)) continue;
    seen.add(d.id);
    defects.push({
      id:        d.id,
      displayId: d.displayId,
      title:     d.title,
      severity:  d.severity,
      status:    d.status,
      link:      d.link      ?? '',
      source:    d.source    ?? '',
      createdAt: d.createdAt ?? '',
    });
  }

  return { totalPassed, totalFailed, totalSkipped, totalUntested, suiteData, defects };
}
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
cd apps/frontend && npm run test -- reportSnapshot
```

Expected: all tests `PASS`.

- [ ] **Step 5: Type-check**

```bash
cd apps/frontend && npx tsc --noEmit
```

Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add apps/frontend/src/lib/reportSnapshot.ts apps/frontend/src/test/reportSnapshot.test.ts
git commit -m "feat(report): add computeSnapshotData utility with unit tests"
```

---

## Task 4: CreateReport — async `handleCreate` with snapshot

**Files:**
- Modify: `apps/frontend/src/pages/CreateReport.tsx`

- [ ] **Step 1: Add missing imports**

Replace the existing import block at the top of `CreateReport.tsx` with:

```typescript
import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { PieChart, BarChart3, Bug, Server } from 'lucide-react';
import { toast } from 'sonner';
import { useProject, useTestRuns, useReports, useCreateReport, useSuites } from '@/hooks/useApi';
import { useAuth } from '@/contexts/AuthContext';
import { listDisplayId } from '@/lib/utils';
import { testRunCasesApi, defectsApi } from '@/lib/api';
import { computeSnapshotData } from '@/lib/reportSnapshot';
```

- [ ] **Step 2: Add `useSuites` hook and `isBuilding` state inside the component**

After the existing `const createReport = useCreateReport();` line, add:

```typescript
const { data: suitesList = [] } = useSuites(projectId!);
const [isBuilding, setIsBuilding] = useState(false);
```

- [ ] **Step 3: Replace `handleCreate` with async version**

Replace the entire `handleCreate` function (currently starts at `const handleCreate = () => {`):

```typescript
const handleCreate = async () => {
  if (!reportName.trim()) {
    toast.error('Report name is required');
    return;
  }

  const effectiveRunIds = selectedRuns.size > 0
    ? Array.from(selectedRuns)
    : runs.map(r => r.id);

  setIsBuilding(true);
  let snapshotDataStr: string | undefined;
  try {
    const [runCaseResults, defectResults] = await Promise.all([
      Promise.all(
        effectiveRunIds.map(runId =>
          testRunCasesApi.list(projectId!, runId).catch(() => ({ data: [] })),
        ),
      ),
      Promise.all(
        effectiveRunIds.map(runId =>
          defectsApi.listByRun(projectId!, runId).catch(() => ({ data: [] })),
        ),
      ),
    ]);

    const allRunCases = runCaseResults.flatMap(r => r.data ?? []);
    const allDefects  = defectResults.flatMap(r => r.data ?? []);
    const effectiveRuns = runs.filter(r => effectiveRunIds.includes(r.id));

    const snapshot = computeSnapshotData(allRunCases, effectiveRuns, suitesList, allDefects);
    snapshotDataStr = JSON.stringify(snapshot);
  } catch {
    // snapshot failed — create report without it rather than blocking the user
  } finally {
    setIsBuilding(false);
  }

  createReport.mutate(
    {
      projectId: projectId!,
      body: {
        name:                    reportName.trim(),
        type:                    reportType as 'Summary' | 'Regression' | 'Sprint' | 'Custom',
        description,
        createdBy:               user?.username || 'unknown',
        selectedRuns:            Array.from(selectedRuns),
        sectionExecutiveSummary: sections.executiveSummary,
        sectionSuiteAnalytics:   sections.suiteAnalytics,
        sectionDefectTable:      sections.defectTable,
        sectionEnvironmentInfo:  sections.environmentInfo,
        ...(snapshotDataStr ? { snapshotData: snapshotDataStr } : {}),
      },
    },
    {
      onSuccess: () => {
        toast.success(`Report "${reportName}" created`);
        navigate(`/projects/${projectId}/reports`);
      },
      onError: (e) => toast.error(e.message),
    },
  );
};
```

- [ ] **Step 4: Update the Create button to reflect both loading states**

Find the Create button in the JSX (near the bottom). Replace `disabled={createReport.isPending}` and its label:

```tsx
<Button
  size="sm"
  className="bg-primary text-primary-foreground hover:bg-primary/90 border-0 px-6"
  onClick={handleCreate}
  disabled={isBuilding || createReport.isPending}
>
  {isBuilding ? 'Building report…' : createReport.isPending ? 'Creating…' : 'Create Report'}
</Button>
```

- [ ] **Step 5: Type-check**

```bash
cd apps/frontend && npx tsc --noEmit
```

Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add apps/frontend/src/pages/CreateReport.tsx
git commit -m "feat(report): compute and store snapshot at creation time"
```

---

## Task 5: ReportDetail — snapshot-aware rendering + read-only defect table

**Files:**
- Modify: `apps/frontend/src/pages/ReportDetail.tsx`

This task has the most changes. Work through it in the sub-steps below.

### 5a: Add snapshot import and parse it

- [ ] **Step 1: Add `ReportSnapshotData` import**

At the top of `ReportDetail.tsx`, add to the existing `@/lib/api` import line:

```typescript
import type { Defect } from '@/lib/api';
import type { ReportSnapshotData, SnapshotDefect } from '@/lib/reportSnapshot';
```

- [ ] **Step 2: Parse snapshot at the top of the component body**

After `const { data: report, isLoading: reportLoading } = useReport(projectId!, reportId!);` (around line 60), add:

```typescript
const parsedSnapshot = useMemo((): ReportSnapshotData | null => {
  if (!report?.snapshotData) return null;
  try { return JSON.parse(report.snapshotData) as ReportSnapshotData; }
  catch { return null; }
}, [report?.snapshotData]);
```

### 5b: Skip live run-case fetch when snapshot present

- [ ] **Step 3: Gate `resolvedRunIds` on snapshot**

Find the existing `resolvedRunIds` useMemo (around line 64). Replace it with:

```typescript
const resolvedRunIds = useMemo(() => {
  if (parsedSnapshot) return [];          // snapshot present — no live fetch needed
  if (!report) return [];
  const ids = report.selectedRuns ?? [];
  return ids.length > 0 ? ids : allRuns.map(r => r.id);
}, [parsedSnapshot, report, allRuns]);
```

### 5c: Use snapshot for KPIs and suite chart

- [ ] **Step 4: Replace KPI variables**

Find the block (around line 200):
```typescript
const totalPassed   = deduped.filter(rc => rc.status === 'PASSED').length;
const totalFailed   = deduped.filter(rc => rc.status === 'FAILED').length;
const totalSkipped  = deduped.filter(rc => rc.status === 'SKIPPED').length;
const totalUntested = deduped.filter(rc => rc.status === 'UNTESTED').length;
```

Replace with:
```typescript
const totalPassed   = parsedSnapshot?.totalPassed   ?? deduped.filter(rc => rc.status === 'PASSED').length;
const totalFailed   = parsedSnapshot?.totalFailed   ?? deduped.filter(rc => rc.status === 'FAILED').length;
const totalSkipped  = parsedSnapshot?.totalSkipped  ?? deduped.filter(rc => rc.status === 'SKIPPED').length;
const totalUntested = parsedSnapshot?.totalUntested ?? deduped.filter(rc => rc.status === 'UNTESTED').length;
```

- [ ] **Step 5: Replace `suiteData` useMemo**

Find the existing `suiteData` useMemo (around line 133). Replace it with:

```typescript
const suiteData = useMemo(() => {
  if (parsedSnapshot?.suiteData) return parsedSnapshot.suiteData;
  const suiteNameMap = new Map(suites.map(s => [s.id, s.name]));
  const data = groupBySuite(deduped, suiteNameMap);
  const suiteOrderMap = new Map(
    [...suites]
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
      .map((s, i) => [s.name, i]),
  );
  return data.sort(
    (a, b) => (suiteOrderMap.get(a.suite) ?? Infinity) - (suiteOrderMap.get(b.suite) ?? Infinity),
  );
}, [parsedSnapshot, deduped, suites]);
```

### 5d: Read-only defect table

The defect table currently has edit/delete/view actions and Select dropdowns. We remove all mutation UI and replace with plain badges + external link only. This applies to both snapshot and legacy mode (the spec says the defect table is always read-only going forward).

- [ ] **Step 6: Remove unused imports**

From the top-level imports, remove these lines entirely:

```typescript
// Remove:
import { useUpdateDefect, useDeleteDefect } from '@/hooks/useApi';   // part of the useApi import line
import ViewDefectModal from '@/components/ViewDefectModal';
import EditDefectModal from '@/components/EditDefectModal';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
```

Also remove from the lucide-react import: `Trash2`, `Eye`, `Pencil`, `Loader2` (keep `ExternalLink`, `ArrowLeft`, `Download`, `AlertTriangle`).

- [ ] **Step 7: Remove mutation hooks and related state from component body**

Remove these lines from the component body:

```typescript
// Remove all of these:
const updateDefect = useUpdateDefect();
const deleteDefect = useDeleteDefect();
const [deleteTarget, setDeleteTarget] = useState<{ id: string; displayId: string } | null>(null);
const [viewOpen, setViewOpen] = useState(false);
const [viewTarget, setViewTarget] = useState<Defect | null>(null);
const [viewAttachments, setViewAttachments] = useState<any[]>([]);
const [editOpen, setEditOpen] = useState(false);
const [editTarget, setEditTarget] = useState<Defect | null>(null);
const [editAttachments, setEditAttachments] = useState<any[]>([]);
const [updatingId, setUpdatingId] = useState<string | null>(null);
```

Also remove `buildDefectUpdateBody`, `handleSeverityChange`, `handleStatusChange`, `handleDeleteDefect`, `openView`, `openEdit`, `handleEditDefectSave` functions entirely.

- [ ] **Step 8: Replace the defect table rows with read-only version**

Find the `{sortedDefects.map((d) => (` block inside the Defect Table section. Determine whether to use snapshot or live defects, and render read-only rows.

Replace the entire defect table `<tbody>` contents with:

```tsx
<tbody>
  {(() => {
    // Use snapshot defects if available, otherwise live defects
    const rows: Array<Defect | SnapshotDefect> =
      parsedSnapshot?.defects ?? sortedDefects;

    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={6} className="text-center py-12 text-muted-foreground text-sm">
            No defects linked to this report
          </td>
        </tr>
      );
    }

    return rows.map((d) => (
      <tr
        key={d.id}
        className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card"
      >
        <td className="px-5 py-3.5 font-mono text-[10px] text-accent tracking-wider" title={d.id}>
          {d.displayId ?? '-'}
        </td>
        <td className="px-5 py-3.5 font-medium text-foreground">{d.title}</td>
        <td className="px-5 py-3.5">
          <span className={`text-[10px] font-mono uppercase tracking-widest ${defectSeverityStyles[d.severity] || ''}`}>
            {d.severity}
          </span>
        </td>
        <td className="px-5 py-3.5">
          <span className={`text-[10px] font-mono uppercase tracking-widest ${defectStatusStyles[d.status] || ''}`}>
            {d.status}
          </span>
        </td>
        <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider">
          {'source' in d ? (d.source || '—') : '—'}
        </td>
        <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">
          {formatDate(d.createdAt)}
        </td>
        <td className="px-5 py-3.5 w-px whitespace-nowrap">
          {d.link && (
            <a
              href={d.link}
              target="_blank"
              rel="noopener noreferrer"
              className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors inline-flex"
            >
              <ExternalLink className="h-3.5 w-3.5" strokeWidth={1.5} />
            </a>
          )}
        </td>
      </tr>
    ));
  })()}
</tbody>
```

- [ ] **Step 9: Update the defect table `<thead>` to remove the Actions column label**

Find the `<thead>` of the defect table. Replace the Actions `<th>`:

```tsx
<thead>
  <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">ID</th>
    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Title</th>
    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Severity</th>
    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Status</th>
    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Source</th>
    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Created</th>
    <th className="px-5 py-3 w-px"></th>
  </tr>
</thead>
```

- [ ] **Step 10: Remove modal JSX from the bottom of the return statement**

Remove all of the following JSX blocks from inside the returned JSX (they come after the main content div):

- `<ViewDefectModal ... />` block
- `<EditDefectModal ... />` block  
- The delete confirmation `<Dialog open={deleteTarget !== null} ...>` block

- [ ] **Step 11: Remove `attachmentsApi` import if unused**

Check if `attachmentsApi` is still used anywhere in the file. If not (it was only used in `openView`/`openEdit`), remove it from the import.

- [ ] **Step 12: Type-check**

```bash
cd apps/frontend && npx tsc --noEmit
```

Expected: no output. Fix any residual type errors (e.g., `Defect | SnapshotDefect` union — both have `id`, `displayId`, `title`, `severity`, `status`, `link`, `createdAt`; `source` is guarded with `'source' in d`).

- [ ] **Step 13: Run all frontend unit tests**

```bash
cd apps/frontend && npm run test
```

Expected: all tests pass including the new `reportSnapshot` tests.

- [ ] **Step 14: Commit**

```bash
git add apps/frontend/src/pages/ReportDetail.tsx
git commit -m "feat(report): use snapshot for KPIs/suites/defects, read-only defect table"
```

---

## Task 6: Final verification

- [ ] **Step 1: Full TypeScript check**

```bash
cd apps/frontend && npx tsc --noEmit
```

Expected: no output.

- [ ] **Step 2: Run all frontend tests**

```bash
cd apps/frontend && npm run test
```

Expected: all pass.

- [ ] **Step 3: Backend compile check**

```bash
./gradlew :apps:backend:compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Manual smoke test**

1. Open the app, navigate to a project with test runs that have statuses set.
2. Create a new report selecting those runs.
3. Verify the "Building report…" spinner appears briefly.
4. Open the report — KPIs and Suite Analysis should reflect statuses at creation time.
5. Go to the test run, change a case status to something different.
6. Re-open the report — KPIs and Suite Analysis must NOT have changed.
7. In the Defect Table: verify no edit/delete buttons; verify Jira link opens in new tab.

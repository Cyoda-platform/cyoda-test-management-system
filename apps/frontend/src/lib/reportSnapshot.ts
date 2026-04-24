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
  if (steps.length === 0)                                                       return 'UNTESTED';
  if (steps.some(s => s === 'failed'))                                          return 'FAILED';
  if (steps.every(s => s === 'passed'))                                         return 'PASSED';
  if (steps.some(s => s === 'skipped') && !steps.some(s => s === 'untested'))  return 'SKIPPED';
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
  // Parse each run's flat step-status map.
  // stepStatuses may arrive as a Record<string, string> (typed) or as a
  // JSON string (e.g. when read back from a serialised snapshot/test fixture).
  const runStepStatusMap = new Map<string, Record<string, string>>();
  runs.forEach(run => {
    if (!run.stepStatuses) return;
    try {
      const parsed: Record<string, string> =
        typeof run.stepStatuses === 'string'
          ? JSON.parse(run.stepStatuses as unknown as string)
          : (run.stepStatuses as Record<string, string>);
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
  const suiteData = [...rawSuiteData].sort(
    (a, b) => (suiteOrderMap.get(a.suite) ?? Infinity) - (suiteOrderMap.get(b.suite) ?? Infinity),
  );

  // Defects — deduplicate by id
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

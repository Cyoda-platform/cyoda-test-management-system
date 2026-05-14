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

function toSnapshotDefect(defect: Defect): SnapshotDefect {
  return {
    id:        defect.id,
    displayId: defect.displayId,
    title:     defect.title,
    severity:  defect.severity,
    status:    defect.status,
    link:      defect.link ?? '',
    source:    defect.source ?? '',
    createdAt: defect.createdAt ?? '',
  };
}

/**
 * Enriches snapshot defects with live defect data when legacy snapshots are missing
 * fields like id/displayId/createdAt.
 *
 * Matching order:
 * 1. By id when present in the snapshot
 * 2. By unique business signature (title + severity + status + source)
 *
 * Keeps snapshot ordering so the report remains a point-in-time view.
 */
export function resolveReportDefects(
  snapshotDefects: SnapshotDefect[] | undefined,
  liveDefects: Defect[],
): SnapshotDefect[] {
  const sortedLive = [...liveDefects].sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
  if (!snapshotDefects?.length) return sortedLive.map(toSnapshotDefect);

  const liveById = new Map(sortedLive.map(defect => [defect.id, defect]));
  const usedLiveIds = new Set<string>();

  return snapshotDefects.map((snapshotDefect, index) => {
    let liveMatch = snapshotDefect.id ? liveById.get(snapshotDefect.id) : undefined;

    if (!liveMatch) {
      const candidates = sortedLive.filter(defect =>
        !usedLiveIds.has(defect.id)
        && defect.title === snapshotDefect.title
        && defect.severity === snapshotDefect.severity
        && defect.status === snapshotDefect.status
        && (defect.source ?? '') === (snapshotDefect.source ?? ''),
      );
      if (candidates.length === 1) liveMatch = candidates[0];
    }

    if (liveMatch) usedLiveIds.add(liveMatch.id);

    return {
      id:        snapshotDefect.id || liveMatch?.id || `snapshot-defect-${index}`,
      displayId: snapshotDefect.displayId || liveMatch?.displayId,
      title:     snapshotDefect.title || liveMatch?.title || '',
      severity:  snapshotDefect.severity || liveMatch?.severity || '',
      status:    snapshotDefect.status || liveMatch?.status || '',
      link:      snapshotDefect.link || liveMatch?.link || '',
      source:    snapshotDefect.source || liveMatch?.source || '',
      createdAt: snapshotDefect.createdAt || liveMatch?.createdAt || '',
    };
  });
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
/**
 * Optional mapping of caseId → suiteId from the repository.
 * Used as fallback for suite analytics when TestRunCase records are unavailable
 * (e.g. Cyoda schema not yet updated to include the `steps` field on TestRunCase).
 */
export type CaseToSuiteMap = Map<string, string>;

export function computeSnapshotData(
  rawRunCases:     TestRunCase[],
  runs:            TestRun[],
  suites:          Suite[],
  rawDefects:      Defect[],
  caseToSuiteMap?: CaseToSuiteMap,
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

  // KPI totals — prefer per-case computation; fall back to run-level aggregates
  // when TestRunCase records are unavailable (e.g. Cyoda schema not yet re-imported).
  const totalPassed   = deduped.length > 0
    ? deduped.filter(rc => rc.status === 'PASSED').length
    : runs.reduce((s, r) => s + (r.passed  ?? 0), 0);
  const totalFailed   = deduped.length > 0
    ? deduped.filter(rc => rc.status === 'FAILED').length
    : runs.reduce((s, r) => s + (r.failed  ?? 0), 0);
  const totalSkipped  = deduped.length > 0
    ? deduped.filter(rc => rc.status === 'SKIPPED').length
    : runs.reduce((s, r) => s + (r.skipped ?? 0), 0);
  const totalUntested = deduped.length > 0
    ? deduped.filter(rc => rc.status === 'UNTESTED').length
    : runs.reduce((s, r) => s + (r.untested ?? 0), 0);

  // Suite data — sorted by suite sortOrder
  const suiteNameMap = new Map(suites.map(s => [s.id, s.name]));
  const suiteOrderMap = new Map(
    [...suites]
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
      .map((s, i) => [s.name, i]),
  );

  // Enrich deduped records: use caseToSuiteMap as fallback when suiteId is empty.
  const enrichedDeduped: RunCaseWithMeta[] = deduped.map(rc => ({
    ...rc,
    suiteId: rc.suiteId || (caseToSuiteMap?.get(rc.testCaseId) ?? ''),
  }));

  // Add UNTESTED entries for cases in run.caseIds that have no TestRunCase record.
  // Also compute their status from stepStatuses in case they were partially executed.
  const knownCaseIds = new Set(enrichedDeduped.map(rc => rc.testCaseId));
  const extraCases: RunCaseWithMeta[] = [];
  if (caseToSuiteMap && caseToSuiteMap.size > 0) {
    runs.forEach(run => {
      const stepMap = runStepStatusMap.get(run.id) ?? {};
      const caseIds: string[] = run.caseIds?.length
        ? run.caseIds
        : [...new Set(Object.keys(stepMap).map(k => k.split('::')[0]).filter(Boolean))];
      caseIds.forEach(caseId => {
        if (knownCaseIds.has(caseId)) return;
        const suiteId = caseToSuiteMap.get(caseId);
        if (!suiteId) return;
        const caseSteps = Object.entries(stepMap)
          .filter(([k]) => k.startsWith(`${caseId}::`))
          .map(([, v]) => v.toLowerCase());
        const status = computeCaseStatusFromSteps(caseSteps);
        extraCases.push({ testCaseId: caseId, suiteId, testRunId: run.id, status, runCreatedAt: runCreatedAtMap.get(run.id) ?? '' });
      });
    });
  }

  const allCasesForSuite = [...enrichedDeduped, ...deduplicateRunCases(extraCases)];

  let rawSuiteData;
  if (allCasesForSuite.length > 0) {
    rawSuiteData = groupBySuite(allCasesForSuite, suiteNameMap);
  } else if (caseToSuiteMap && caseToSuiteMap.size > 0) {
    // Final fallback when no TestRunCase records and no run.caseIds:
    // derive case IDs from stepStatuses keys only.
    const fallbackCases: RunCaseWithMeta[] = runs.flatMap(run => {
      const stepMap = runStepStatusMap.get(run.id) ?? {};
      const caseIds = [...new Set(Object.keys(stepMap).map(k => k.split('::')[0]).filter(Boolean))];
      return caseIds.flatMap(caseId => {
        const suiteId = caseToSuiteMap.get(caseId);
        if (!suiteId) return [];
        const caseSteps = Object.entries(stepMap)
          .filter(([k]) => k.startsWith(`${caseId}::`))
          .map(([, v]) => v.toLowerCase());
        const status = computeCaseStatusFromSteps(caseSteps);
        return [{ testCaseId: caseId, suiteId, testRunId: run.id, status, runCreatedAt: run.createdAt ?? '' }];
      });
    });
    rawSuiteData = groupBySuite(deduplicateRunCases(fallbackCases), suiteNameMap);
  } else {
    rawSuiteData = [];
  }

  const suiteData = [...rawSuiteData].sort(
    (a, b) => (suiteOrderMap.get(a.suite) ?? Infinity) - (suiteOrderMap.get(b.suite) ?? Infinity),
  );

  // Defects — deduplicate by id
  const seen = new Set<string>();
  const defects: SnapshotDefect[] = [];
  for (const d of rawDefects) {
    if (seen.has(d.id)) continue;
    seen.add(d.id);
    defects.push(toSnapshotDefect(d));
  }

  return { totalPassed, totalFailed, totalSkipped, totalUntested, suiteData, defects };
}

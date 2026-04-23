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
    const st = rc.status.toUpperCase();
    if (st === 'PASSED')       entry.passed++;
    else if (st === 'FAILED')  entry.failed++;
    else if (st === 'SKIPPED') entry.skipped++;
    else                       entry.untested++;
    bySuite.set(rc.suiteId, entry);
  }
  return Array.from(bySuite.values()).filter(
    s => s.passed + s.failed + s.skipped + s.untested > 0,
  );
}

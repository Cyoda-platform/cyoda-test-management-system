import { describe, it, expect } from 'vitest';
import { computeSnapshotData, resolveReportDefects, type SnapshotDefect } from '@/lib/reportSnapshot';
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
    const stepStatuses = JSON.stringify({ 'c2::1': 'PASSED' });
    const cases: TestRunCase[] = [baseCase({ testCaseId: 'c1', status: 'PASSED' })];
    const runs: TestRun[] = [baseRun({ stepStatuses: stepStatuses as any })];
    const result = computeSnapshotData(cases, runs, [baseSuite()], []);
    expect(result.totalPassed).toBe(1);
  });

  it('uses stepStatuses: marks SKIPPED if all steps skipped and none untested', () => {
    const stepStatuses = JSON.stringify({ 'c1::1': 'SKIPPED', 'c1::2': 'SKIPPED' });
    const cases: TestRunCase[] = [baseCase({ status: 'UNTESTED' })];
    const runs: TestRun[] = [baseRun({ stepStatuses: stepStatuses as any })];
    const result = computeSnapshotData(cases, runs, [baseSuite()], []);
    expect(result.totalSkipped).toBe(1);
    expect(result.totalUntested).toBe(0);
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

describe('computeSnapshotData — fallback to run aggregates when no TestRunCase records', () => {
  it('uses run.passed/failed/skipped/untested when rawRunCases is empty', () => {
    const runs: TestRun[] = [
      baseRun({ passed: 3, failed: 1, skipped: 2, untested: 4 }),
    ];
    const result = computeSnapshotData([], runs, [], []);
    expect(result.totalPassed).toBe(3);
    expect(result.totalFailed).toBe(1);
    expect(result.totalSkipped).toBe(2);
    expect(result.totalUntested).toBe(4);
  });

  it('sums run aggregates across multiple runs when rawRunCases is empty', () => {
    const runs: TestRun[] = [
      baseRun({ id: 'r1', passed: 2, failed: 1, skipped: 0, untested: 1 }),
      baseRun({ id: 'r2', passed: 1, failed: 2, skipped: 1, untested: 0 }),
    ];
    const result = computeSnapshotData([], runs, [], []);
    expect(result.totalPassed).toBe(3);
    expect(result.totalFailed).toBe(3);
    expect(result.totalSkipped).toBe(1);
    expect(result.totalUntested).toBe(1);
  });

  it('prefers TestRunCase records over run aggregates when both are present', () => {
    const cases: TestRunCase[] = [
      baseCase({ testCaseId: 'c1', status: 'PASSED' }),
      baseCase({ id: 'rc2', testCaseId: 'c2', status: 'FAILED' }),
    ];
    const runs: TestRun[] = [baseRun({ passed: 99, failed: 99, skipped: 99, untested: 99 })];
    const result = computeSnapshotData(cases, runs, [baseSuite()], []);
    expect(result.totalPassed).toBe(1);
    expect(result.totalFailed).toBe(1);
    expect(result.totalSkipped).toBe(0);
    expect(result.totalUntested).toBe(0);
  });
});

describe('computeSnapshotData — suiteData fallback via caseToSuiteMap when no TestRunCase records', () => {
  it('computes suiteData from run.caseIds + stepStatuses + caseToSuiteMap', () => {
    const runs: TestRun[] = [
      baseRun({
        caseIds: ['c1', 'c2'],
        stepStatuses: JSON.stringify({ 'c1::1': 'PASSED', 'c2::1': 'FAILED' }) as unknown as Record<string, string>,
      }),
    ];
    const suites = [baseSuite({ id: 's1', name: 'Suite A' })];
    const caseToSuiteMap = new Map([['c1', 's1'], ['c2', 's1']]);
    const result = computeSnapshotData([], runs, suites, [], caseToSuiteMap);
    expect(result.suiteData).toHaveLength(1);
    expect(result.suiteData[0].suite).toBe('Suite A');
    expect(result.suiteData[0].passed).toBe(1);
    expect(result.suiteData[0].failed).toBe(1);
  });

  it('maps untested when case has no stepStatuses entries', () => {
    const runs: TestRun[] = [
      baseRun({ caseIds: ['c1'], stepStatuses: {} }),
    ];
    const suites = [baseSuite({ id: 's1', name: 'Suite A' })];
    const caseToSuiteMap = new Map([['c1', 's1']]);
    const result = computeSnapshotData([], runs, suites, [], caseToSuiteMap);
    expect(result.suiteData[0].untested).toBe(1);
  });

  it('ignores cases not in caseToSuiteMap', () => {
    const runs: TestRun[] = [
      baseRun({
        caseIds: ['c1', 'c-unknown'],
        stepStatuses: JSON.stringify({ 'c1::1': 'PASSED' }) as unknown as Record<string, string>,
      }),
    ];
    const suites = [baseSuite({ id: 's1', name: 'Suite A' })];
    const caseToSuiteMap = new Map([['c1', 's1']]);
    const result = computeSnapshotData([], runs, suites, [], caseToSuiteMap);
    expect(result.suiteData[0].passed).toBe(1);
    expect(result.suiteData[0].passed + result.suiteData[0].failed +
      result.suiteData[0].skipped + result.suiteData[0].untested).toBe(1);
  });

  it('includes untested cases from run.caseIds not present in stepStatuses', () => {
    const runs: TestRun[] = [
      baseRun({
        caseIds: ['c1', 'c2', 'c3'],  // 3 cases in run
        stepStatuses: JSON.stringify({ 'c1::1': 'PASSED' }) as unknown as Record<string, string>,
        // c2 and c3 were never clicked → should appear as untested
      }),
    ];
    const suites = [baseSuite({ id: 's1', name: 'Suite A' })];
    const caseToSuiteMap = new Map([['c1', 's1'], ['c2', 's1'], ['c3', 's1']]);
    const result = computeSnapshotData([], runs, suites, [], caseToSuiteMap);
    expect(result.suiteData).toHaveLength(1);
    expect(result.suiteData[0].passed).toBe(1);
    expect(result.suiteData[0].untested).toBe(2);
  });

  it('derives case IDs from stepStatuses keys when run.caseIds is absent', () => {
    const runs: TestRun[] = [
      baseRun({
        // caseIds not set — simulates older runs where caseIds wasn't persisted
        stepStatuses: JSON.stringify({ 'c1::1': 'PASSED', 'c2::1': 'FAILED' }) as unknown as Record<string, string>,
      }),
    ];
    const suites = [baseSuite({ id: 's1', name: 'Suite A' })];
    const caseToSuiteMap = new Map([['c1', 's1'], ['c2', 's1']]);
    const result = computeSnapshotData([], runs, suites, [], caseToSuiteMap);
    expect(result.suiteData).toHaveLength(1);
    expect(result.suiteData[0].passed).toBe(1);
    expect(result.suiteData[0].failed).toBe(1);
  });

  it('does not use caseToSuiteMap when TestRunCase records are present', () => {
    const cases: TestRunCase[] = [
      baseCase({ testCaseId: 'c1', suiteId: 's1', status: 'PASSED' }),
    ];
    const wrongMap = new Map([['c1', 's-wrong']]);
    const suites = [baseSuite({ id: 's1', name: 'Suite A' })];
    const result = computeSnapshotData(cases, [baseRun()], suites, [], wrongMap);
    expect(result.suiteData[0].suite).toBe('Suite A');
  });
});

describe('computeSnapshotData — suite data with partial TestRunCase coverage', () => {
  it('shows all suites including those with only untested cases and no TestRunCase records', () => {
    // Exact user scenario: 1 run, 4 suites, only suite s1 has TestRunCase records (4 tested cases).
    // Suites s2/s3/s4 have cases in run.caseIds but zero TestRunCase records.
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testCaseId: 'c1', suiteId: 's1', status: 'PASSED' }),
      baseCase({ id: 'rc2', testCaseId: 'c2', suiteId: 's1', status: 'FAILED' }),
      baseCase({ id: 'rc3', testCaseId: 'c3', suiteId: 's1', status: 'PASSED' }),
      baseCase({ id: 'rc4', testCaseId: 'c4', suiteId: 's1', status: 'PASSED' }),
    ];
    const run = baseRun({
      caseIds: ['c1', 'c2', 'c3', 'c4', 'c5', 'c6', 'c7', 'c8', 'c9', 'c10'],
    });
    const suites: Suite[] = [
      baseSuite({ id: 's1', name: 'Suite 1', sortOrder: 1 }),
      baseSuite({ id: 's2', name: 'Suite 2', sortOrder: 2 }),
      baseSuite({ id: 's3', name: 'Suite 3', sortOrder: 3 }),
      baseSuite({ id: 's4', name: 'Suite 4', sortOrder: 4 }),
    ];
    const caseToSuiteMap = new Map([
      ['c1', 's1'], ['c2', 's1'], ['c3', 's1'], ['c4', 's1'],
      ['c5', 's2'], ['c6', 's2'],
      ['c7', 's3'], ['c8', 's3'],
      ['c9', 's4'], ['c10', 's4'],
    ]);
    const result = computeSnapshotData(cases, [run], suites, [], caseToSuiteMap);
    expect(result.suiteData).toHaveLength(4);
    const s1 = result.suiteData.find(s => s.suite === 'Suite 1')!;
    expect(s1.passed).toBe(3);
    expect(s1.failed).toBe(1);
    expect(s1.untested).toBe(0);
    const s2 = result.suiteData.find(s => s.suite === 'Suite 2')!;
    expect(s2.untested).toBe(2);
    const s3 = result.suiteData.find(s => s.suite === 'Suite 3')!;
    expect(s3.untested).toBe(2);
    const s4 = result.suiteData.find(s => s.suite === 'Suite 4')!;
    expect(s4.untested).toBe(2);
  });

  it('uses caseToSuiteMap as fallback suiteId when TestRunCase.suiteId is empty', () => {
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testCaseId: 'c1', suiteId: '', status: 'PASSED' }),
      baseCase({ id: 'rc2', testCaseId: 'c2', suiteId: '', status: 'UNTESTED' }),
    ];
    const suites: Suite[] = [baseSuite({ id: 's1', name: 'Suite A' })];
    const caseToSuiteMap = new Map([['c1', 's1'], ['c2', 's1']]);
    const result = computeSnapshotData(cases, [baseRun()], suites, [], caseToSuiteMap);
    expect(result.suiteData).toHaveLength(1);
    expect(result.suiteData[0].passed).toBe(1);
    expect(result.suiteData[0].untested).toBe(1);
  });

  it('does not double-count cases that have both a TestRunCase record and appear in run.caseIds', () => {
    const cases: TestRunCase[] = [
      baseCase({ id: 'rc1', testCaseId: 'c1', suiteId: 's1', status: 'PASSED' }),
    ];
    const run = baseRun({ caseIds: ['c1', 'c2'] });
    const suites: Suite[] = [baseSuite({ id: 's1', name: 'Suite A' })];
    const caseToSuiteMap = new Map([['c1', 's1'], ['c2', 's1']]);
    const result = computeSnapshotData(cases, [run], suites, [], caseToSuiteMap);
    expect(result.suiteData).toHaveLength(1);
    // c1 counted once (PASSED from TestRunCase), c2 counted once (UNTESTED from caseIds)
    expect(result.suiteData[0].passed).toBe(1);
    expect(result.suiteData[0].untested).toBe(1);
    expect(result.suiteData[0].passed + result.suiteData[0].untested).toBe(2);
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

  it('enriches legacy snapshot defects with live id/displayId/createdAt', () => {
    const legacySnapshot: SnapshotDefect[] = [{
      id: '',
      title: 'Bug 1',
      severity: 'Major',
      status: 'Open',
      link: 'https://jira/1',
      source: 'Step 1',
      createdAt: '',
    }];

    const resolved = resolveReportDefects(legacySnapshot, [
      baseDefect({ id: 'live-1', displayId: 'DEF-1', createdAt: '2026-01-02' }),
    ]);

    expect(resolved).toHaveLength(1);
    expect(resolved[0].id).toBe('live-1');
    expect(resolved[0].displayId).toBe('DEF-1');
    expect(resolved[0].createdAt).toBe('2026-01-02');
  });
});

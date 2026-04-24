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

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

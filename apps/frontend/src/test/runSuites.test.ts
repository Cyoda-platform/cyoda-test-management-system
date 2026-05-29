import { describe, it, expect } from 'vitest';
import { buildSuitesFromRunCases } from '@/lib/runSuites';
import type { TestRunCase, RepositorySuite } from '@/lib/api';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const steps = [{ stepNumber: 1, action: 'Click login', expectedResult: 'Redirected' }];

const liveRepo: RepositorySuite[] = [
  {
    id: 'suite-1',
    projectId: 'proj-1',
    name: 'Auth Suite',
    sortOrder: 0,
    cases: [
      {
        id: 'case-1',
        suiteId: 'suite-1',
        projectId: 'proj-1',
        title: 'Login test',
        description: 'Live description',
        preconditions: 'Live preconditions',
        priority: 'HIGH',
        displayId: 'AUTH-1',
        status: 'ACTIVE',
        sortOrder: 0,
        deleted: false,
        steps,
      },
    ],
  },
];

function makeRunCase(overrides: Partial<TestRunCase> = {}): TestRunCase {
  return {
    id: 'rc-1',
    testRunId: 'run-1',
    testCaseId: 'case-1',
    status: 'UNTESTED',
    suiteId: 'suite-1',
    title: 'Snapshot title',
    description: 'Snapshot description',
    preconditions: 'Snapshot preconditions',
    priority: 'MEDIUM',
    displayId: 'SNAP-1',
    steps,
    ...overrides,
  };
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('buildSuitesFromRunCases', () => {
  it('uses snapshot fields when present, ignoring live repo values', () => {
    const result = buildSuitesFromRunCases([makeRunCase()], liveRepo);

    expect(result).toHaveLength(1);
    expect(result[0].name).toBe('Auth Suite');
    const c = result[0].cases[0];
    expect(c.id).toBe('case-1');
    expect(c.title).toBe('Snapshot title');
    expect(c.description).toBe('Snapshot description');
    expect(c.preconditions).toBe('Snapshot preconditions');
    expect(c.priority).toBe('MEDIUM');
    expect(c.displayId).toBe('SNAP-1');
  });

  it('still shows a soft-deleted case (not in live repo) using snapshot data', () => {
    // Live repo has NO cases — simulates soft-delete
    const result = buildSuitesFromRunCases([makeRunCase()], [
      { id: 'suite-1', projectId: 'proj-1', name: 'Auth Suite', sortOrder: 0, cases: [] },
    ]);

    expect(result).toHaveLength(1);
    const c = result[0].cases[0];
    expect(c.id).toBe('case-1');
    expect(c.title).toBe('Snapshot title');
    expect(c.steps).toEqual(steps);
  });

  it('falls back to live repo for fields absent in snapshot (old records)', () => {
    const oldRecord = makeRunCase({
      description: undefined,
      preconditions: undefined,
      priority: undefined,
      displayId: undefined,
      steps: undefined,
    });

    const result = buildSuitesFromRunCases([oldRecord], liveRepo);
    const c = result[0].cases[0];

    expect(c.description).toBe('Live description');
    expect(c.preconditions).toBe('Live preconditions');
    expect(c.priority).toBe('HIGH');
    expect(c.displayId).toBe('AUTH-1');
    expect(c.steps).toEqual(steps);
  });

  it('uses "Unknown Suite" when the suite has been deleted from live repo', () => {
    const result = buildSuitesFromRunCases([makeRunCase()], []);

    expect(result[0].name).toBe('Unknown Suite');
    expect(result[0].cases[0].title).toBe('Snapshot title');
  });

  it('groups cases by suiteId', () => {
    const rc1 = makeRunCase({ id: 'rc-1', testCaseId: 'case-1', suiteId: 'suite-1', title: 'Case A' });
    const rc2: TestRunCase = { ...makeRunCase(), id: 'rc-2', testCaseId: 'case-2', suiteId: 'suite-2', title: 'Case B' };

    const result = buildSuitesFromRunCases([rc1, rc2], []);

    expect(result).toHaveLength(2);
    expect(result.find(s => s.id === 'suite-1')?.cases[0].title).toBe('Case A');
    expect(result.find(s => s.id === 'suite-2')?.cases[0].title).toBe('Case B');
  });

  it('returns empty array when runCases is empty', () => {
    expect(buildSuitesFromRunCases([], liveRepo)).toEqual([]);
  });
});

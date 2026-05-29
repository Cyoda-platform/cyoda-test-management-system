import type { TestRunCase, RepositorySuite } from '@/lib/api';

export interface RunSuiteCase {
  id: string;
  title: string;
  description: string;
  preconditions: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  displayId: string;
  steps: NonNullable<TestRunCase['steps']>;
  suiteId: string;
  projectId: string;
  status: string;
  sortOrder: number;
  deleted: boolean;
}

export interface RunSuite {
  id: string;
  name: string;
  cases: RunSuiteCase[];
}

/**
 * Build the suite/case tree for a test run from TestRunCase snapshot data.
 *
 * Snapshot fields (title, description, preconditions, priority, displayId,
 * steps) were copied at initialize_run time and survive soft-deletion of the
 * original repository records. For fields absent in old snapshots (created
 * before the processor started copying them), falls back to live repository
 * data. If the case has also been deleted from the repository, only snapshot
 * data is used — the case still appears in the run as intended.
 */
export function buildSuitesFromRunCases(
  runCases: TestRunCase[],
  repositorySuites: RepositorySuite[],
): RunSuite[] {
  const suiteNameById = new Map(repositorySuites.map((s) => [s.id, s.name]));
  const liveCaseById = new Map(
    repositorySuites.flatMap((s) => s.cases).map((c) => [c.id, c]),
  );

  const bySuite = new Map<string, TestRunCase[]>();
  for (const rc of runCases) {
    const sid = rc.suiteId ?? 'unknown';
    if (!bySuite.has(sid)) bySuite.set(sid, []);
    bySuite.get(sid)!.push(rc);
  }

  return Array.from(bySuite.entries()).map(([suiteId, cases]) => ({
    id: suiteId,
    name: suiteNameById.get(suiteId) ?? 'Unknown Suite',
    cases: cases.map((rc) => {
      const live = liveCaseById.get(rc.testCaseId);
      return {
        id: rc.testCaseId,
        title: rc.title || live?.title || '',
        description: rc.description ?? live?.description ?? '',
        preconditions: rc.preconditions ?? live?.preconditions ?? '',
        priority: ((rc.priority ?? live?.priority ?? 'MEDIUM') as 'HIGH' | 'MEDIUM' | 'LOW'),
        displayId: rc.displayId ?? live?.displayId ?? '',
        steps: rc.steps?.length ? rc.steps : (live?.steps ?? []),
        suiteId,
        projectId: live?.projectId ?? '',
        status: rc.status,
        sortOrder: live?.sortOrder ?? 0,
        deleted: false,
      };
    }),
  }));
}

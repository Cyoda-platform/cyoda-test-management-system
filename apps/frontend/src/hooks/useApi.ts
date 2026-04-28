/**
 * TanStack Query hooks for all API domains.
 *
 * Convention:
 *  - useXxx()        → useQuery  (read)
 *  - useCreateXxx()  → useMutation (POST)
 *  - useUpdateXxx()  → useMutation (PUT)
 *  - useDeleteXxx()  → useMutation (DELETE)
 *
 * Query key factories live alongside their hooks so invalidation
 * is always consistent.
 */

import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  projectsApi,
  suitesApi,
  testCasesApi,

  testRunsApi,
  testRunCasesApi,
  testRunStepsRunApi,
  defectsApi,
  reportsApi,
  attachmentsApi,
  repositoryApi,
  type Project,
  type Suite,
  type TestCase,

  type TestRun,
  type TestRunCase,
  type TestRunDetail,
  type TestRunStep,
  type Defect,
  type Report,
} from '@/lib/api';

// ── Query key factories ───────────────────────────────────────────────────────

export const keys = {
  projects: {
    all:    ()           => ['projects']                      as const,
    lists:  ()           => ['projects', 'list']              as const,  // For invalidating ALL pages
    list:   (page = 0)   => ['projects', 'list', page]       as const,
    detail: (id: string) => ['projects', 'detail', id]       as const,
  },
  suites: {
    all:    (projectId: string)              => ['suites', projectId]             as const,
    detail: (projectId: string, id: string) => ['suites', projectId, 'detail', id] as const,
  },
  cases: {
    all:    (projectId: string, suiteId: string)              => ['cases', projectId, suiteId]             as const,
    detail: (projectId: string, suiteId: string, id: string) => ['cases', projectId, suiteId, 'detail', id] as const,
  },
  steps: {
    all: (projectId: string, suiteId: string, caseId: string) =>
      ['steps', projectId, suiteId, caseId] as const,
  },
  runs: {
    all:    (projectId: string)              => ['runs', projectId]             as const,
    list:   (projectId: string, page = 0)   => ['runs', projectId, 'list', page] as const,
    detail: (projectId: string, id: string) => ['runs', projectId, 'detail', id] as const,
  },
  defects: {
    all:    (projectId: string)              => ['defects', projectId]             as const,
    list:   (projectId: string, page = 0)   => ['defects', projectId, 'list', page] as const,
    detail: (projectId: string, id: string) => ['defects', projectId, 'detail', id] as const,
  },
  reports: {
    all:    (projectId: string)              => ['reports', projectId]               as const,
    list:   (projectId: string, page = 0)   => ['reports', projectId, 'list', page] as const,
    detail: (projectId: string, id: string) => ['reports', projectId, 'detail', id] as const,
  },
  attachments: {
    all: (projectId: string) => ['attachments', projectId] as const,
  },
  repository: {
    all: (projectId: string) => ['repository', projectId] as const,
  },
  runCases: {
    all: (projectId: string, runId: string) =>
      ['runCases', projectId, runId] as const,
  },
  runSteps: {
    all: (projectId: string, runId: string, runCaseId: string) =>
      ['runSteps', projectId, runId, runCaseId] as const,
  },
  runDefects: {
    all: (projectId: string, runId: string) =>
      ['runDefects', projectId, runId] as const,
  },
};

// ── Projects ──────────────────────────────────────────────────────────────────

export function useProjects(page = 0, size = 10) {
  return useQuery({
    queryKey: keys.projects.list(page),
    queryFn:  () => projectsApi.list(page, size),
    select:   (res) => res.data,
  });
}

export function useProject(id: string) {
  return useQuery({
    queryKey: keys.projects.detail(id),
    queryFn:  () => projectsApi.get(id),
    enabled:  !!id,
  });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Pick<Project, 'name' | 'description'>) => projectsApi.create(body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: keys.projects.lists() }),
  });
}

export function useUpdateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: Partial<Project> }) =>
      projectsApi.update(id, body),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: keys.projects.lists() });
      qc.invalidateQueries({ queryKey: keys.projects.detail(id) });
    },
  });
}

export function useDeleteProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => projectsApi.delete(id),
    onSuccess:  () => qc.invalidateQueries({ queryKey: keys.projects.lists() }),
  });
}

// ── Suites ────────────────────────────────────────────────────────────────────

export function useSuites(projectId: string) {
  return useQuery({
    queryKey: keys.suites.all(projectId),
    queryFn:  () => suitesApi.list(projectId),
    enabled:  !!projectId,
    select:   (res) => res.data,
  });
}

export function useSuite(projectId: string, suiteId: string) {
  return useQuery({
    queryKey: keys.suites.detail(projectId, suiteId),
    queryFn:  () => suitesApi.get(projectId, suiteId),
    enabled:  !!projectId && !!suiteId,
  });
}

export function useCreateSuite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      body,
    }: {
      projectId: string;
      body: Pick<Suite, 'name' | 'description'>;
    }) => suitesApi.create(projectId, body),
    onSuccess: (_data, { projectId }) => {
      qc.invalidateQueries({ queryKey: keys.suites.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

export function useUpdateSuite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      id,
      body,
    }: {
      projectId: string;
      id: string;
      body: Partial<Suite>;
    }) => suitesApi.update(projectId, id, body),
    onSuccess: (_data, { projectId }) => {
      qc.invalidateQueries({ queryKey: keys.suites.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

export function useDeleteSuite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      suitesApi.delete(projectId, id),
    onSuccess: (_data, { projectId }) => {
      qc.invalidateQueries({ queryKey: keys.suites.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

export function useReorderSuites() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, items }: { projectId: string; items: { id: string; sortOrder: number }[] }) =>
      suitesApi.reorder(projectId, items),
    onSuccess: (_data, { projectId }) => {
      qc.invalidateQueries({ queryKey: keys.suites.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

// ── Test Cases ────────────────────────────────────────────────────────────────

export function useTestCases(projectId: string, suiteId: string) {
  return useQuery({
    queryKey: keys.cases.all(projectId, suiteId),
    queryFn:  () => testCasesApi.list(projectId, suiteId),
    enabled:  !!projectId && !!suiteId,
    select:   (res) => res.data,
  });
}

export function useTestCase(projectId: string, suiteId: string, caseId: string) {
  return useQuery({
    queryKey: keys.cases.detail(projectId, suiteId, caseId),
    queryFn:  () => testCasesApi.get(projectId, suiteId, caseId),
    enabled:  !!projectId && !!suiteId && !!caseId,
  });
}

export function useCreateTestCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      body,
    }: {
      projectId: string;
      suiteId: string;
      body: Partial<TestCase>;
    }) => testCasesApi.create(projectId, suiteId, body),
    onSuccess: (_data, { projectId, suiteId }) => {
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, suiteId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

export function useUpdateTestCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      id,
      body,
    }: {
      projectId: string;
      suiteId: string;
      id: string;
      body: Partial<TestCase>;
    }) => testCasesApi.update(projectId, suiteId, id, body),
    onSuccess: (_data, { projectId, suiteId, id }) => {
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, suiteId) });
      qc.invalidateQueries({ queryKey: keys.cases.detail(projectId, suiteId, id) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

export function useDeleteTestCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      id,
    }: {
      projectId: string;
      suiteId: string;
      id: string;
    }) => testCasesApi.delete(projectId, suiteId, id),
    onSuccess: (_data, { projectId, suiteId }) => {
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, suiteId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

export function useReorderTestCases() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      items,
    }: {
      projectId: string;
      suiteId: string;
      items: { id: string; sortOrder: number }[];
    }) => testCasesApi.reorder(projectId, suiteId, items),
    onSuccess: (_data, { projectId, suiteId }) => {
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, suiteId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

export function useMoveTestCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      id,
      targetSuiteId,
      sortOrder,
    }: {
      projectId: string;
      suiteId: string;
      id: string;
      targetSuiteId: string;
      sortOrder: number;
    }) => testCasesApi.move(projectId, suiteId, id, { targetSuiteId, sortOrder }),
    onSuccess: (_data, { projectId, suiteId, targetSuiteId }) => {
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, suiteId) });
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, targetSuiteId) });
      qc.invalidateQueries({ queryKey: keys.repository.all(projectId) });
    },
  });
}

// ── Test Runs ─────────────────────────────────────────────────────────────────

export function useTestRuns(projectId: string, page = 0) {
  return useQuery({
    queryKey: keys.runs.list(projectId, page),
    queryFn:  () => testRunsApi.list(projectId, page),
    enabled:  !!projectId,
    select:   (res) => res.data,
    // Treat data as fresh for 30 seconds. Mutations (complete, update, delete)
    // already call invalidateQueries so the list stays accurate after user
    // actions without needing a refetch on every mount or tab focus.
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}

export function useTestRun(projectId: string, runId: string) {
  return useQuery({
    queryKey: keys.runs.detail(projectId, runId),
    queryFn:  () => testRunsApi.get(projectId, runId),
    enabled:  !!projectId && !!runId,
    // Always fetch fresh execution state when the component mounts so that
    // aggregate counters saved on the previous session are immediately visible.
    staleTime: 0,
    refetchOnMount: true,
  });
}

/**
 * Fetches the run AND all its DB-backed TestRunCase records in a single HTTP
 * request (`GET /runs/{id}/details`), eliminating the two-request waterfall
 * that caused 10+ pending requests and >10 s latency in the Run Execution view.
 *
 * Returns `{ run, runCases, isLoading, isError }` so callers can destructure
 * what they need without fetching the same data twice.
 */
export function useTestRunDetails(projectId: string, runId: string) {
  return useQuery({
    queryKey: [...keys.runs.detail(projectId, runId), 'details'] as const,
    queryFn:  () => testRunsApi.getDetails(projectId, runId),
    enabled:  !!projectId && !!runId,
    // ── staleTime rationale ───────────────────────────────────────────────────
    // 10 s gives us a fresh fetch on mount (first open / page refresh) while
    // preventing the re-fetch cascade that caused the infinite-loop:
    //
    //   click step → updateRun.onSuccess → invalidateQueries(details)
    //   → staleTime:0 triggers immediate refetch
    //   → runDetails changes → component re-renders
    //   → next click invalidates again → loop
    //
    // Step-status display is driven entirely by local `stepStatuses` state, so
    // a short cache window is perfectly safe.  Full-structure refetches only
    // happen on meaningful state transitions (complete / unlock) which
    // explicitly invalidate this key.
    staleTime: 10_000,
    // Only auto-refetch when the component actually mounts (first open / tab
    // switch back to the page), not on every query invalidation.
    refetchOnMount: 'always',
    refetchOnWindowFocus: false,
    select: (data: TestRunDetail) => data,
  });
}

export function useCreateTestRun() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      body,
    }: {
      projectId: string;
      body: Partial<TestRun>;
    }) => testRunsApi.create(projectId, body),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.runs.all(projectId) }),
  });
}

export function useUpdateTestRun() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      id,
      body,
    }: {
      projectId: string;
      id: string;
      body: Partial<TestRun>;
    }) => testRunsApi.update(projectId, id, body),
    /**
     * Optimistic update: immediately reflect name/environment/status changes in the
     * Test Runs table so the user sees the edit take effect without waiting for the
     * server round-trip. We snapshot and roll back on error.
     */
    onMutate: async ({ projectId, id, body }) => {
      // Cancel any in-flight refetches to prevent race conditions
      await qc.cancelQueries({ queryKey: keys.runs.all(projectId) });
      // Snapshot all runs list queries for rollback
      const previousQueries = qc.getQueriesData<{ data: TestRun[] }>({
        queryKey: keys.runs.all(projectId),
      });
      // Optimistically patch every cached page that contains this run
      qc.setQueriesData<{ data: TestRun[] }>(
        { queryKey: keys.runs.all(projectId) },
        (old) => {
          if (!old) return old;
          const typed = old as { data?: TestRun[] };
          if (!Array.isArray(typed.data)) return old;
          return {
            ...typed,
            data: typed.data.map((r) => (r.id === id ? { ...r, ...body } : r)),
          };
        },
      );
      return { previousQueries };
    },
    onError: (_err, { projectId }, context) => {
      // Roll back every affected query to its pre-mutation snapshot
      context?.previousQueries?.forEach(([queryKey, data]) => {
        qc.setQueryData(queryKey, data);
      });
    },
    onSuccess: (_data, { projectId, id }) => {
      // Invalidate the list so the TestRuns table reflects updated counters.
      qc.invalidateQueries({ queryKey: keys.runs.all(projectId) });
      // Invalidate the scalar detail (used by useTestRun callers outside RunExecution).
      qc.invalidateQueries({ queryKey: keys.runs.detail(projectId, id) });
      // ── DO NOT invalidate the batch-details key here ──────────────────────
      // updateTestRun fires on every single step-status click. Invalidating the
      // details query at staleTime:0 would trigger an immediate network refetch
      // after every click, creating an O(n-clicks) request flood (the infinite
      // loop visible in the Network tab).  Step-status changes are reflected in
      // local React state immediately; the details query is only needed for the
      // initial mount and for structural transitions (complete / unlock) which
      // explicitly invalidate it below.
    },
  });
}

export function useCompleteTestRun() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      testRunsApi.complete(projectId, id),
    onSuccess: (_data, { projectId, id }) => {
      qc.invalidateQueries({ queryKey: keys.runs.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.runs.detail(projectId, id) });
      qc.invalidateQueries({ queryKey: [...keys.runs.detail(projectId, id), 'details'] });
    },
  });
}

export function useUnlockTestRun() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      testRunsApi.unlock(projectId, id),
    onSuccess: (_data, { projectId, id }) => {
      qc.invalidateQueries({ queryKey: keys.runs.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.runs.detail(projectId, id) });
      qc.invalidateQueries({ queryKey: [...keys.runs.detail(projectId, id), 'details'] });
    },
  });
}

export function useDeleteTestRun() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      testRunsApi.delete(projectId, id),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.runs.all(projectId) }),
  });
}

// ── Defects ───────────────────────────────────────────────────────────────────

export function useDefects(projectId: string, page = 0) {
  return useQuery({
    queryKey: keys.defects.list(projectId, page),
    queryFn:  () => defectsApi.list(projectId, page),
    enabled:  !!projectId,
    select:   (res) => res.data,
    staleTime: 30_000,
  });
}

export function useDefect(projectId: string, defectId: string) {
  return useQuery({
    queryKey: keys.defects.detail(projectId, defectId),
    queryFn:  () => defectsApi.get(projectId, defectId),
    enabled:  !!projectId && !!defectId,
    staleTime: 30_000,
  });
}

export function useCreateDefect() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      body,
    }: {
      projectId: string;
      body: Partial<Defect>;
    }) => defectsApi.create(projectId, body),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.defects.all(projectId) }),
  });
}

export function useUpdateDefect() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      id,
      body,
    }: {
      projectId: string;
      id: string;
      body: Partial<Defect>;
    }) => defectsApi.update(projectId, id, body),
    /**
     * Optimistic update: immediately reflect the new field values in the cache so
     * the Defects table shows the changed severity/status without a loading delay.
     * We snapshot all matching list queries beforehand and roll back on error.
     */
    onMutate: async ({ projectId, id, body }) => {
      // Cancel any in-flight refetches to prevent race conditions
      await qc.cancelQueries({ queryKey: keys.defects.all(projectId) });
      // Snapshot all defect list/detail queries for rollback
      const previousQueries = qc.getQueriesData<{ data: Defect[] }>({
        queryKey: keys.defects.all(projectId),
      });
      // Optimistically patch every cached page that contains this defect
      qc.setQueriesData<{ data: Defect[] }>(
        { queryKey: keys.defects.all(projectId) },
        (old) => {
          if (!old) return old;
          const typed = old as { data?: Defect[] };
          if (!Array.isArray(typed.data)) return old;
          return {
            ...typed,
            data: typed.data.map((d) => (d.id === id ? { ...d, ...body } : d)),
          };
        },
      );
      return { previousQueries };
    },
    onError: (_err, { projectId }, context) => {
      // Roll back every affected query to its pre-mutation snapshot
      context?.previousQueries?.forEach(([queryKey, data]) => {
        qc.setQueryData(queryKey, data);
      });
    },
    onSuccess: (_data, { projectId, id }) => {
      qc.invalidateQueries({ queryKey: keys.defects.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.defects.detail(projectId, id) });
    },
  });
}

export function useDeleteDefect() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      defectsApi.delete(projectId, id),
    onMutate: async ({ projectId, id }) => {
      await qc.cancelQueries({ queryKey: keys.defects.all(projectId) });
      const previousQueries = qc.getQueriesData<{ data: Defect[] }>({
        queryKey: keys.defects.all(projectId),
      });
      // Optimistically remove the deleted item from every cached list page
      qc.setQueriesData<{ data: Defect[] }>(
        { queryKey: keys.defects.all(projectId) },
        (old) => {
          if (!old) return old;
          const typed = old as { data?: Defect[] };
          if (!Array.isArray(typed.data)) return old;
          return { ...old, data: typed.data.filter((d) => d.id !== id) };
        },
      );
      return { previousQueries };
    },
    onError: (_err, { projectId }, context) => {
      if (context?.previousQueries) {
        context.previousQueries.forEach(([queryKey, data]) =>
          qc.setQueryData(queryKey, data),
        );
      }
      qc.invalidateQueries({ queryKey: keys.defects.all(projectId) });
    },
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.defects.all(projectId) }),
  });
}

/**
 * Fetches all defects that were raised during a specific test run.
 *
 * The backend filters by the {@code testRunId} field on DefectDTO, which is now
 * persisted during defect creation in the Run Execution view. This hook replaces
 * the in-memory {@code createdDefects} state that previously reset on page reload.
 */
export function useDefectsByRun(projectId: string, runId: string) {
  return useQuery({
    queryKey: keys.runDefects.all(projectId, runId),
    queryFn:  () => defectsApi.listByRun(projectId, runId),
    enabled:  !!projectId && !!runId,
    select:   (res) => res.data,
    staleTime: 5_000,
    refetchOnMount: 'always',
  });
}

// ── Reports ───────────────────────────────────────────────────────────────────

export function useReports(projectId: string, page = 0) {
  return useQuery({
    queryKey: keys.reports.list(projectId, page),
    queryFn:  () => reportsApi.list(projectId, page),
    enabled:  !!projectId,
    select:   (res) => res.data,
    staleTime: 30_000,
  });
}

export function useReport(projectId: string, reportId: string) {
  return useQuery({
    queryKey: keys.reports.detail(projectId, reportId),
    queryFn:  () => reportsApi.get(projectId, reportId),
    enabled:  !!projectId && !!reportId,
    staleTime: 30_000,
  });
}

export function useCreateReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      body,
    }: {
      projectId: string;
      body: Partial<Report>;
    }) => reportsApi.create(projectId, body),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.reports.all(projectId) }),
  });
}

export function useUpdateReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      id,
      body,
    }: {
      projectId: string;
      id: string;
      body: Partial<Report>;
    }) => reportsApi.update(projectId, id, body),
    onSuccess: (_data, { projectId, id }) => {
      qc.invalidateQueries({ queryKey: keys.reports.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.reports.detail(projectId, id) });
    },
  });
}

export function useDeleteReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      reportsApi.delete(projectId, id),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.reports.all(projectId) }),
  });
}

// ── Test Run Cases (run-scoped, DB-backed) ────────────────────────────────────

/**
 * Fetches all TestRunCase records for a run.  Each record links the run to an
 * original TestCase and carries a DB-persisted execution status so that progress
 * survives page refreshes.
 */
export function useTestRunCases(projectId: string, runId: string) {
  return useQuery({
    queryKey: keys.runCases.all(projectId, runId),
    queryFn:  () => testRunCasesApi.list(projectId, runId),
    enabled:  !!projectId && !!runId,
    select:   (res) => res.data,
    // Always fresh — execution state changes with every step click.
    staleTime: 0,
    refetchOnMount: true,
  });
}

/**
 * Fetches TestRunCase records for multiple runs in parallel.
 * Returns one query result per runId in the same order.
 */
export function useTestRunCasesForRuns(projectId: string, runIds: string[]) {
  return useQueries({
    queries: runIds.map(runId => ({
      queryKey: keys.runCases.all(projectId, runId),
      queryFn:  () => testRunCasesApi.list(projectId, runId),
      enabled:  !!projectId && !!runId,
      select:   (res: { data: TestRunCase[] }) => res.data,
    })),
  });
}

export function useUpdateTestRunCaseStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      runId,
      id,
      status,
    }: {
      projectId: string;
      runId: string;
      id: string;
      status: string;
    }) => testRunCasesApi.update(projectId, runId, id, { status }),
    onSuccess: (_data, { projectId, runId }) => {
      qc.invalidateQueries({ queryKey: keys.runCases.all(projectId, runId) });
      // ── DO NOT invalidate batch-details here ──────────────────────────────
      // updateTestRunCaseStatus fires on every step click alongside updateTestRun.
      // Two simultaneous details invalidations would double the refetch rate and
      // recreate the infinite-loop.  The details cache is updated directly via
      // setQueryData inside ensureRunCaseId when a new record is lazily created.
    },
  });
}

export function useCreateTestRunCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      runId,
      testCaseId,
    }: {
      projectId: string;
      runId: string;
      testCaseId: string;
    }) => testRunCasesApi.create(projectId, runId, testCaseId),
    onSuccess: (_data, { projectId, runId }) => {
      qc.invalidateQueries({ queryKey: keys.runCases.all(projectId, runId) });
    },
  });
}

// ── Test Run Steps (run-scoped, DB-backed) ────────────────────────────────────

/**
 * Fetches all TestRunStep records for a TestRunCase.
 * @param runCaseId – the TestRunCase.id (NOT the original TestCase.id)
 */
export function useTestRunCaseSteps(
  projectId: string,
  runId: string,
  runCaseId: string,
) {
  return useQuery({
    queryKey: keys.runSteps.all(projectId, runId, runCaseId),
    queryFn:  () => testRunStepsRunApi.list(projectId, runId, runCaseId),
    enabled:  !!projectId && !!runId && !!runCaseId,
    select:   (res) => res.data,
    staleTime: 0,
    refetchOnMount: true,
  });
}

export function useUpdateTestRunStepStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      runId,
      runCaseId,
      id,
      status,
    }: {
      projectId: string;
      runId: string;
      runCaseId: string;
      id: string;
      status: string;
    }) => testRunStepsRunApi.update(projectId, runId, runCaseId, id, { status }),
    onSuccess: (_data, { projectId, runId, runCaseId }) => {
      qc.invalidateQueries({ queryKey: keys.runSteps.all(projectId, runId, runCaseId) });
    },
  });
}

// ── Repository aggregate ──────────────────────────────────────────────────────

/**
 * Fetches all suites + cases for a project in a single round-trip.
 * Use this in the Repository and RunExecution views to avoid the 1+N serial waterfall
 * (suites first, then one cases request per suite).
 * Mutations that create/update/delete suites or cases invalidate this key automatically.
 */
export function useRepository(projectId: string) {
  return useQuery({
    queryKey: keys.repository.all(projectId),
    queryFn:  () => repositoryApi.get(projectId),
    enabled:  !!projectId,
    staleTime: 30_000,
  });
}

// ── Attachments ───────────────────────────────────────────────────────────────

export function useAttachmentsByCase(projectId: string, caseId: string) {
  return useQuery({
    queryKey: ['attachments', projectId, 'case', caseId],
    queryFn:  () => attachmentsApi.listByCase(projectId, caseId),
    enabled:  !!projectId && !!caseId,
    staleTime: 30_000,
  });
}

export function useAttachments(projectId: string) {
  return useQuery({
    queryKey: keys.attachments.all(projectId),
    queryFn:  () => attachmentsApi.list(projectId),
    enabled:  !!projectId,
    select:   (res) => res.data,
    staleTime: 30_000,
  });
}

export function useUploadAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, file, caseId }: { projectId: string; file: File; caseId?: string }) =>
      attachmentsApi.upload(projectId, file, caseId),
    onSuccess: (_data, { projectId, caseId }) => {
      qc.invalidateQueries({ queryKey: keys.attachments.all(projectId) });
      if (caseId) {
        qc.invalidateQueries({ queryKey: ['attachments', projectId, 'case', caseId] });
        qc.invalidateQueries({ queryKey: ['attachments', projectId, caseId] });
      }
    },
  });
}

export function useDeleteAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      attachmentsApi.delete(projectId, id),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.attachments.all(projectId) }),
  });
}

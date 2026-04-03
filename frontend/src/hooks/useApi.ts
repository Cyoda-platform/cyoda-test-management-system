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

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  projectsApi,
  suitesApi,
  testCasesApi,
  testStepsApi,
  testRunsApi,
  defectsApi,
  reportsApi,
  attachmentsApi,
  type Project,
  type Suite,
  type TestCase,
  type TestStep,
  type TestRun,
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
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.suites.all(projectId) }),
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
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.suites.all(projectId) }),
  });
}

export function useDeleteSuite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, id }: { projectId: string; id: string }) =>
      suitesApi.delete(projectId, id),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.suites.all(projectId) }),
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
    onSuccess: (_data, { projectId, suiteId }) =>
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, suiteId) }),
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
    onSuccess: (_data, { projectId, suiteId }) =>
      qc.invalidateQueries({ queryKey: keys.cases.all(projectId, suiteId) }),
  });
}

// ── Test Steps ────────────────────────────────────────────────────────────────

export function useTestSteps(projectId: string, suiteId: string, caseId: string) {
  return useQuery({
    queryKey: keys.steps.all(projectId, suiteId, caseId),
    queryFn:  () => testStepsApi.list(projectId, suiteId, caseId),
    enabled:  !!projectId && !!suiteId && !!caseId,
  });
}

export function useCreateTestStep() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      caseId,
      body,
    }: {
      projectId: string;
      suiteId: string;
      caseId: string;
      body: Partial<TestStep>;
    }) => testStepsApi.create(projectId, suiteId, caseId, body),
    onSuccess: (_data, { projectId, suiteId, caseId }) =>
      qc.invalidateQueries({ queryKey: keys.steps.all(projectId, suiteId, caseId) }),
  });
}

export function useUpdateTestStep() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      caseId,
      id,
      body,
    }: {
      projectId: string;
      suiteId: string;
      caseId: string;
      id: string;
      body: Partial<TestStep>;
    }) => testStepsApi.update(projectId, suiteId, caseId, id, body),
    onSuccess: (_data, { projectId, suiteId, caseId }) =>
      qc.invalidateQueries({ queryKey: keys.steps.all(projectId, suiteId, caseId) }),
  });
}

export function useDeleteTestStep() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      projectId,
      suiteId,
      caseId,
      id,
    }: {
      projectId: string;
      suiteId: string;
      caseId: string;
      id: string;
    }) => testStepsApi.delete(projectId, suiteId, caseId, id),
    onSuccess: (_data, { projectId, suiteId, caseId }) =>
      qc.invalidateQueries({ queryKey: keys.steps.all(projectId, suiteId, caseId) }),
  });
}

// ── Test Runs ─────────────────────────────────────────────────────────────────

export function useTestRuns(projectId: string, page = 0) {
  return useQuery({
    queryKey: keys.runs.list(projectId, page),
    queryFn:  () => testRunsApi.list(projectId, page),
    enabled:  !!projectId,
    select:   (res) => res.data,
    // Override global staleTime so the list always re-fetches on mount,
    // ensuring progress/status reflect changes made inside a run execution.
    staleTime: 0,
    refetchOnWindowFocus: true,
  });
}

export function useTestRun(projectId: string, runId: string) {
  return useQuery({
    queryKey: keys.runs.detail(projectId, runId),
    queryFn:  () => testRunsApi.get(projectId, runId),
    enabled:  !!projectId && !!runId,
    // Always fetch fresh so re-entering a run sees latest saved counts
    staleTime: 0,
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
    onSuccess: (_data, { projectId, id }) => {
      qc.invalidateQueries({ queryKey: keys.runs.all(projectId) });
      qc.invalidateQueries({ queryKey: keys.runs.detail(projectId, id) });
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
  });
}

export function useDefect(projectId: string, defectId: string) {
  return useQuery({
    queryKey: keys.defects.detail(projectId, defectId),
    queryFn:  () => defectsApi.get(projectId, defectId),
    enabled:  !!projectId && !!defectId,
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
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.defects.all(projectId) }),
  });
}

// ── Reports ───────────────────────────────────────────────────────────────────

export function useReports(projectId: string, page = 0) {
  return useQuery({
    queryKey: keys.reports.list(projectId, page),
    queryFn:  () => reportsApi.list(projectId, page),
    enabled:  !!projectId,
    select:   (res) => res.data,
  });
}

export function useReport(projectId: string, reportId: string) {
  return useQuery({
    queryKey: keys.reports.detail(projectId, reportId),
    queryFn:  () => reportsApi.get(projectId, reportId),
    enabled:  !!projectId && !!reportId,
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

// ── Attachments ───────────────────────────────────────────────────────────────

export function useAttachmentsByCase(projectId: string, caseId: string) {
  return useQuery({
    queryKey: ['attachments', projectId, 'case', caseId],
    queryFn:  () => attachmentsApi.listByCase(projectId, caseId),
    enabled:  !!projectId && !!caseId,
  });
}

export function useAttachments(projectId: string) {
  return useQuery({
    queryKey: keys.attachments.all(projectId),
    queryFn:  () => attachmentsApi.list(projectId),
    enabled:  !!projectId,
    select:   (res) => res.data,
  });
}

export function useUploadAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, file }: { projectId: string; file: File }) =>
      attachmentsApi.upload(projectId, file),
    onSuccess: (_data, { projectId }) =>
      qc.invalidateQueries({ queryKey: keys.attachments.all(projectId) }),
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

/**
 * Central API client.
 * Base URL:
 *  - Dev: http://localhost:8080/api (direct connection to backend)
 *  - Prod: /api (served by backend, same domain)
 *
 * NOTE: In dev mode with CORS, we use Authorization header instead of httpOnly cookie
 * because HttpOnly cookies don't work across different origins/ports.
 */
const BASE_URL = import.meta.env.VITE_API_URL ??
  (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

// Store token in localStorage for development (when using Authorization header)
let storedToken: string | null = null;

export function setAuthToken(token: string | null) {
  storedToken = token;
  if (token) {
    localStorage.setItem('auth_token', token);
  } else {
    localStorage.removeItem('auth_token');
  }
}

export function getAuthToken(): string | null {
  if (!storedToken) {
    storedToken = localStorage.getItem('auth_token');
  }
  return storedToken;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const url = `${BASE_URL}${path}`;
  const method = options.method || 'GET';

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  // In development mode, use Authorization header instead of cookies (CORS limitation)
  const token = getAuthToken();
  if (token && import.meta.env.DEV) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(url, {
    ...options,
    credentials: 'include',
    headers,
  });

  if (res.status === 401) {
    // Session expired or no token
    setAuthToken(null);
    // Don't redirect here - let the caller handle it (AuthContext or ProtectedRoute)
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`API ${res.status}: ${body}`);
  }

  // 204 No Content — or 200/2xx with an empty body (e.g. reorder endpoints)
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  if (!text) return undefined as T;

  return JSON.parse(text) as T;
}

export const api = {
  get:    <T>(path: string)                  => request<T>(path),
  post:   <T>(path: string, body: unknown)   => request<T>(path, { method: 'POST',   body: JSON.stringify(body) }),
  put:    <T>(path: string, body: unknown)   => request<T>(path, { method: 'PUT',    body: JSON.stringify(body) }),
  patch:  <T>(path: string, body: unknown)   => request<T>(path, { method: 'PATCH',  body: JSON.stringify(body) }),
  delete: <T>(path: string)                  => request<T>(path, { method: 'DELETE' }),
};

// ── Auth ─────────────────────────────────────────────────────────────────────

export interface AuthUser {
  username: string;
  role: string;
}

interface LoginResponse extends AuthUser {
  token: string;
  expiresAt: string;
}

export const authApi = {
  login: async (username: string, password: string): Promise<AuthUser> => {
    const response = await api.post<LoginResponse>('/auth/login', { username, password });
    // Save token for Authorization header usage in dev mode
    if (response.token) {
      setAuthToken(response.token);
    }
    return response;
  },
  logout: () =>
    api.post<void>('/auth/logout', {}),
  me:     () =>
    api.get<AuthUser>('/auth/me'),
};

// ── Projects ─────────────────────────────────────────────────────────────────

export interface Project {
  id: string;
  name: string;
  description: string;
  createdAt: string;
  deleted: boolean;
}

export const projectsApi = {
  list:   (page = 0, size = 20) =>
    api.get<{ data: Project[] }>(`/projects?page=${page}&size=${size}`),
  get:    (id: string) =>
    api.get<Project>(`/projects/${id}`),
  create: (body: Pick<Project, 'name' | 'description'>) =>
    api.post<Project>('/projects', body),
  update: (id: string, body: Partial<Project>) =>
    api.put<Project>(`/projects/${id}`, body),
  delete: (id: string) =>
    api.delete<void>(`/projects/${id}`),
  search: (query: string) =>
    api.get<Project[]>(`/projects/search?query=${encodeURIComponent(query)}`),
};

// ── Global Search ────────────────────────────────────────────────────────────

export interface SearchResult {
  type: 'project' | 'suite' | 'testcase' | 'testrun' | 'testruncases' | 'testrunchstep' | 'defect' | 'report';
  id: string;
  displayId?: string;
  title: string;
  description?: string;
  metadata?: string;
  parentProjectId?: string;
  matchedFields?: string[];
  score: number;
  externalLink?: string;
  createdAt?: string;
}

export interface GlobalSearchResponse {
  query: string;
  results: SearchResult[];
  resultsByType?: Record<string, SearchResult[]>;
  totalResults: number;
  pageNumber: number;
  pageSize: number;
  executionTimeMs: number;
}

export const searchApi = {
  global: (query: string, pageNumber = 0, pageSize = 10) =>
    api.get<GlobalSearchResponse>(`/v1/search?query=${encodeURIComponent(query)}&pageNumber=${pageNumber}&pageSize=${pageSize}`),
};

// ── Suites ───────────────────────────────────────────────────────────────────

export interface Suite {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  sortOrder?: number;
}

export const suitesApi = {
  list:   (projectId: string) =>
    api.get<{ data: Suite[] }>(`/projects/${projectId}/suites`),
  get:    (projectId: string, id: string) =>
    api.get<Suite>(`/projects/${projectId}/suites/${id}`),
  create: (projectId: string, body: Omit<Suite, 'id' | 'projectId'>) =>
    api.post<Suite>(`/projects/${projectId}/suites`, { ...body, projectId }),
  update: (projectId: string, id: string, body: Partial<Suite>) =>
    api.put<Suite>(`/projects/${projectId}/suites/${id}`, body),
  delete: (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/suites/${id}`),
  /** Persist a new vertical order for all suites in a project. */
  reorder: (projectId: string, items: { id: string; sortOrder: number }[]) =>
    api.patch<void>(`/projects/${projectId}/suites/reorder`, items),
};

// ── Test Cases ────────────────────────────────────────────────────────────────

export interface TestCase {
  id: string;
  displayId?: string;
  shortId?: string;
  projectId: string;
  suiteId: string;
  title: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  preconditions: string;
  deleted: boolean;
  steps?: BatchImportStep[];
}

export interface BatchImportStep {
  stepNumber?: number;
  action: string;
  expectedResult: string;
}

export interface BatchImportCase {
  title: string;
  description?: string;
  preconditions?: string;
  priority?: 'HIGH' | 'MEDIUM' | 'LOW';
  steps?: BatchImportStep[];
}

export const testCasesApi = {
  list:   (projectId: string, suiteId: string) =>
    api.get<{ data: TestCase[] }>(`/projects/${projectId}/suites/${suiteId}/cases`),
  get:    (projectId: string, suiteId: string, id: string) =>
    api.get<TestCase>(`/projects/${projectId}/suites/${suiteId}/cases/${id}`),
  create: (projectId: string, suiteId: string, body: Partial<TestCase>) =>
    api.post<TestCase>(`/projects/${projectId}/suites/${suiteId}/cases`, body),
  /**
   * Batch-create multiple cases (with optional embedded steps) in a single HTTP call.
   * The backend pre-allocates all display IDs in one counter round-trip, eliminating
   * the N+1 counter-table hit pattern that makes row-by-row import slow.
   */
  batchCreate: (projectId: string, suiteId: string, items: BatchImportCase[]) =>
    api.post<TestCase[]>(`/projects/${projectId}/suites/${suiteId}/cases/batch`, items),
  update: (projectId: string, suiteId: string, id: string, body: Partial<TestCase>) =>
    api.put<TestCase>(`/projects/${projectId}/suites/${suiteId}/cases/${id}`, body),
  delete: (projectId: string, suiteId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/suites/${suiteId}/cases/${id}`),
  /** Persist a new display order for all cases in a suite. */
  reorder: (projectId: string, suiteId: string, items: { id: string; sortOrder: number }[]) =>
    api.patch<void>(`/projects/${projectId}/suites/${suiteId}/cases/reorder`, items),
  /** Move a case to a different suite and assign it a position in that suite. */
  move: (projectId: string, suiteId: string, id: string, body: { targetSuiteId: string; sortOrder: number }) =>
    api.patch<TestCase>(`/projects/${projectId}/suites/${suiteId}/cases/${id}/move`, body),
};


// ── Test Runs ─────────────────────────────────────────────────────────────────

export interface TestRun {
  id: string;
  displayId?: string;
  projectId: string;
  name: string;
  environment: string;
  buildVersion: string;
  description: string;
  status: 'initial' | 'active' | 'completed';
  passed: number;
  failed: number;
  skipped: number;
  untested: number;
  createdAt: string;
  /**
   * IDs of the test cases selected for this run (snapshot taken at creation).
   * Used by RunExecution to filter the repository to only this run's cases.
   */
  caseIds?: string[];
  /**
   * Flat step-level execution state persisted on the run entity.
   * Key: "caseId::stepId", Value: uppercase status ("UNTESTED"|"PASSED"|"FAILED"|"SKIPPED").
   * Updated on every case-switch and on page unmount.
   */
  stepStatuses?: Record<string, string>;
}

/**
 * Composite response from `GET /projects/{projectId}/runs/{runId}/details`.
 *
 * Bundles the run entity and all DB-backed TestRunCase records in a single
 * HTTP response — eliminates the two-round-trip waterfall that caused 10+
 * pending requests and 10 s+ latency in the Run Execution view.
 */
export interface TestRunDetail {
  run: TestRun;
  /**
   * All TestRunCase records for this run.
   * Each carries its own `id` (the snapshot ID used to scope defects/steps)
   * and `testCaseId` (the original repo case ID).
   */
  runCases: TestRunCase[];
}

export const testRunsApi = {
  list:     (projectId: string, page = 0, size = 20) =>
    api.get<{ data: TestRun[] }>(`/projects/${projectId}/runs?page=${page}&size=${size}`),
  get:      (projectId: string, id: string) =>
    api.get<TestRun>(`/projects/${projectId}/runs/${id}`),
  /**
   * Fetches the run + all its DB-backed run-case records in a single round-trip.
   * Use this instead of separate `get` + `testRunCasesApi.list` calls.
   */
  getDetails: (projectId: string, id: string) =>
    api.get<TestRunDetail>(`/projects/${projectId}/runs/${id}/details`),
  create:   (projectId: string, body: Partial<TestRun>) =>
    api.post<TestRun>(`/projects/${projectId}/runs`, body),
  update:   (projectId: string, id: string, body: Partial<TestRun>) =>
    api.put<TestRun>(`/projects/${projectId}/runs/${id}`, body),
  complete: (projectId: string, id: string) =>
    api.post<TestRun>(`/projects/${projectId}/runs/${id}/complete`, {}),
  unlock:   (projectId: string, id: string) =>
    api.post<TestRun>(`/projects/${projectId}/runs/${id}/unlock`, {}),
  delete:   (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/runs/${id}`),
};

// ── Defects ───────────────────────────────────────────────────────────────────

export interface Defect {
  id: string;
  displayId?: string;
  projectId: string;
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  link: string;
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  source: string;
  createdAt: string;
  /**
   * ID of the TestRun during which this defect was raised.
   * Persisted by the backend so the defect table survives page reloads.
   */
  testRunId?: string;
  /**
   * ID of the TestRunCase (run-scoped snapshot) this defect was raised against.
   * Used to scope per-case defect counts without an extra query.
   */
  testRunCaseId?: string;
  /**
   * ID of the original TestCase (repository entity) this defect was raised against.
   * Stored directly on the defect so caseId resolution does not depend on the
   * testRunCaseId → TestRunCase → testCaseId lookup chain.
   */
  testCaseId?: string;
}

export const defectsApi = {
  list:   (projectId: string, page = 0, size = 20) =>
    api.get<{ data: Defect[] }>(`/projects/${projectId}/defects?page=${page}&size=${size}`),
  /**
   * Fetches all defects scoped to a specific test run.
   * Calls GET /projects/{projectId}/defects?testRunId={runId} — the backend
   * filters by the testRunId field added to DefectDTO.
   */
  listByRun: (projectId: string, runId: string, page = 0, size = 200) =>
    api.get<{ data: Defect[] }>(
      `/projects/${projectId}/defects?testRunId=${runId}&page=${page}&size=${size}`,
    ),
  get:    (projectId: string, id: string) =>
    api.get<Defect>(`/projects/${projectId}/defects/${id}`),
  create: (projectId: string, body: Partial<Defect>) =>
    api.post<Defect>(`/projects/${projectId}/defects`, body),
  update: (projectId: string, id: string, body: Partial<Defect>) =>
    api.put<Defect>(`/projects/${projectId}/defects/${id}`, body),
  delete: (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/defects/${id}`),
};

// ── Reports ───────────────────────────────────────────────────────────────────

export interface ReportSections {
  executiveSummary: boolean;
  suiteAnalytics:   boolean;
  defectTable:      boolean;
  environmentInfo:  boolean;
}

export interface Report {
  id: string;
  displayId?: string;
  projectId: string;
  name: string;
  type: 'Summary' | 'Regression' | 'Sprint' | 'Custom';
  description: string;
  createdBy: string;
  dateFrom?: string;
  dateTo?: string;
  selectedRuns: string[];
  /** Flattened section flags as stored by the backend */
  sectionExecutiveSummary: boolean;
  sectionSuiteAnalytics:   boolean;
  sectionDefectTable:      boolean;
  sectionEnvironmentInfo:  boolean;
  createdAt: string;
  updatedAt?: string;
  /** JSON-serialized ReportSnapshotData. Present on all new reports; absent on legacy. */
  snapshotData?: string;
}

export const reportsApi = {
  list:   (projectId: string, page = 0, size = 20) =>
    api.get<{ data: Report[] }>(`/projects/${projectId}/reports?page=${page}&size=${size}`),
  get:    (projectId: string, id: string) =>
    api.get<Report>(`/projects/${projectId}/reports/${id}`),
  create: (projectId: string, body: Partial<Report>) =>
    api.post<Report>(`/projects/${projectId}/reports`, body),
  update: (projectId: string, id: string, body: Partial<Report>) =>
    api.put<Report>(`/projects/${projectId}/reports/${id}`, body),
  delete: (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/reports/${id}`),
};

// ── Attachments ───────────────────────────────────────────────────────────────

export interface Attachment {
  id: string;
  projectId: string | null;
  caseId: string | null;
  defectId: string | null;
  fileName: string;
  fileType: string;
  fileSize: number;
  uploadedAt: string;
  messageId: string | null;
  attachmentType: 'CASE' | 'EVIDENCE' | 'DEFECT' | null;
  runId: string | null;
  stepKey: string | null;
}

export const attachmentsApi = {
  list:       (projectId: string) =>
    api.get<{ data: Attachment[] }>(`/projects/${projectId}/attachments`),
  listByRun: (projectId: string, runId: string) =>
    api.get<Attachment[]>(`/projects/${projectId}/attachments/by-run/${runId}`),
  listByCase: (projectId: string, caseId: string) =>
    api.get<Attachment[]>(`/projects/${projectId}/attachments/by-case/${caseId}`),
  listByDefect: (projectId: string, defectId: string) =>
    api.get<Attachment[]>(`/projects/${projectId}/attachments/by-defect/${defectId}`),
  listByRunCaseStep: (projectId: string, runId: string, caseId: string, stepKey: string) =>
    api.get<Attachment[]>(
      `/projects/${projectId}/attachments/by-run/${runId}/case/${caseId}/step/${stepKey}`
    ),
  delete: (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/attachments/${id}`),
  copy: (projectId: string, id: string, toCaseId: string) =>
    api.post<Attachment>(`/projects/${projectId}/attachments/${id}/copy?toCaseId=${toCaseId}`, {}),
  upload: (
    projectId: string,
    file: File,
    caseId?: string,
    defectId?: string,
    attachmentType?: 'CASE' | 'EVIDENCE' | 'DEFECT',
    runId?: string,
    stepKey?: string,
  ) => {
    const form = new FormData();
    form.append('file', file);
    if (caseId)          form.append('caseId', caseId);
    if (defectId)        form.append('defectId', defectId);
    if (attachmentType)  form.append('attachmentType', attachmentType);
    if (runId)           form.append('runId', runId);
    if (stepKey)         form.append('stepKey', stepKey);
    const token = getAuthToken();
    const headers: HeadersInit = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return fetch(`${BASE_URL}/projects/${projectId}/attachments`, {
      method: 'POST',
      credentials: 'include',
      headers,
      body: form,
    }).then(async r => {
      if (!r.ok) {
        const body = await r.text().catch(() => '');
        throw new Error(`Upload failed (${r.status}): ${body}`);
      }
      return r.json() as Promise<Attachment>;
    });
  },
};

// ── Repository aggregate ──────────────────────────────────────────────────────

export interface RepositoryCase {
  id: string;
  displayId?: string;
  suiteId: string;
  projectId: string;
  title: string;
  description: string;
  preconditions: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status: string;
  sortOrder: number;
  deleted: boolean;
  steps?: BatchImportStep[];
}

export interface RepositorySuite {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  sortOrder: number;
  cases: RepositoryCase[];
}

export interface Repository {
  suites: RepositorySuite[];
}

export const repositoryApi = {
  /** Fetches all suites + cases for a project in a single round-trip. */
  get: (projectId: string) =>
    api.get<Repository>(`/projects/${projectId}/repository`),
};

// ── Test Run Cases (run-scoped, DB-backed execution state) ────────────────────

/**
 * A TestRunCase is a per-run snapshot record that tracks the execution status
 * of a specific test case within a specific test run. Created during run
 * creation, it persists across page refreshes.
 */
export interface TestRunCase {
  id: string;
  testRunId: string;
  /** References the original TestCase.id */
  testCaseId: string;
  /** Uppercase: 'UNTESTED' | 'PASSED' | 'FAILED' | 'SKIPPED' */
  status: string;
  bugUrl?: string;
  /** Suite ID from the original test case */
  suiteId: string;
  /** Title from the original test case */
  title: string;
  /** Optional ISO timestamp when execution started */
  startedAt?: string;
  /** Optional ISO timestamp when execution completed */
  completedAt?: string;
  /** Snapshot of steps copied from the test case at run creation time */
  steps?: BatchImportStep[];
}

export const testRunCasesApi = {
  list: (projectId: string, runId: string, page = 0, size = 500) =>
    api.get<{ data: TestRunCase[] }>(
      `/projects/${projectId}/runs/${runId}/cases?page=${page}&size=${size}`,
    ),
  create: (projectId: string, runId: string, testCaseId: string) =>
    api.post<TestRunCase>(`/projects/${projectId}/runs/${runId}/cases`, {
      testCaseId,
    }),
  update: (projectId: string, runId: string, id: string, body: Partial<TestRunCase>) =>
    api.put<TestRunCase>(`/projects/${projectId}/runs/${runId}/cases/${id}`, body),
};

// ── Test Run Steps (run-scoped, DB-backed step status) ────────────────────────

/**
 * A TestRunStep is a per-run snapshot record that tracks the execution status
 * of a specific step within a TestRunCase. The URL's {caseId} segment is the
 * TestRunCase.id (NOT the original TestCase.id).
 */
export interface TestRunStep {
  id: string;
  testRunCaseId: string;
  /** Snapshot fields copied from TestCase.steps[] at run-creation time. */
  stepNumber?: number;
  action?: string;
  expectedResult?: string;
  /** Uppercase: 'UNTESTED' | 'PASSED' | 'FAILED' | 'SKIPPED' */
  status: string;
  actualResult?: string;
}

export const testRunStepsRunApi = {
  /**
   * @param runCaseId – the TestRunCase.id (NOT the original TestCase.id)
   */
  list: (projectId: string, runId: string, runCaseId: string, page = 0, size = 500) =>
    api.get<{ data: TestRunStep[] }>(
      `/projects/${projectId}/runs/${runId}/cases/${runCaseId}/steps?page=${page}&size=${size}`,
    ),
  /**
   * Creates a TestRunStep that links a TestRunCase to an original TestStep.
   * @param runCaseId – the TestRunCase.id
   * @param testStepId – the original TestStep.id
   */
  create: (projectId: string, runId: string, runCaseId: string, testStepId: string) =>
    api.post<TestRunStep>(
      `/projects/${projectId}/runs/${runId}/cases/${runCaseId}/steps`,
      { testStepId },
    ),
  /**
   * @param runCaseId – the TestRunCase.id
   */
  update: (
    projectId: string,
    runId: string,
    runCaseId: string,
    id: string,
    body: Partial<TestRunStep>,
  ) =>
    api.put<TestRunStep>(
      `/projects/${projectId}/runs/${runId}/cases/${runCaseId}/steps/${id}`,
      body,
    ),
};


// ── Search ────────────────────────────────────────────────────────────────────

export interface SearchResult {
  id: string;
  displayId?: string;
  type: string;
  title: string;
  description?: string;
  metadata?: string;
  parentProjectId?: string;
  score: number;
  createdAt?: string;
  matchedFields?: string[];
}

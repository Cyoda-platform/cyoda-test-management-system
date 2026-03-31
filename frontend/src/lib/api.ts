/**
 * Central API client.
 * All requests include credentials: 'include' so the browser sends the
 * httpOnly auth-token cookie automatically on every call.
 *
 * Base URL is read from VITE_API_URL (default: /api for Vite proxy).
 */
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (res.status === 401) {
    // Session expired or no cookie — bounce to login
    window.location.href = '/';
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`API ${res.status}: ${body}`);
  }

  // 204 No Content
  if (res.status === 204) return undefined as T;

  return res.json() as Promise<T>;
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

export const authApi = {
  login:  (username: string, password: string) =>
    api.post<AuthUser>('/auth/login', { username, password }),
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

// ── Suites ───────────────────────────────────────────────────────────────────

export interface Suite {
  id: string;
  projectId: string;
  name: string;
  description?: string;
}

export const suitesApi = {
  list:   (projectId: string) =>
    api.get<{ data: Suite[] }>(`/projects/${projectId}/suites`),
  get:    (projectId: string, id: string) =>
    api.get<Suite>(`/projects/${projectId}/suites/${id}`),
  create: (projectId: string, body: Pick<Suite, 'name' | 'description'>) =>
    api.post<Suite>(`/projects/${projectId}/suites`, body),
  update: (projectId: string, id: string, body: Partial<Suite>) =>
    api.put<Suite>(`/projects/${projectId}/suites/${id}`, body),
  delete: (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/suites/${id}`),
};

// ── Test Cases ────────────────────────────────────────────────────────────────

export interface TestCase {
  id: string;
  projectId: string;
  suiteId: string;
  title: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  preconditions: string;
  deleted: boolean;
}

export const testCasesApi = {
  list:   (projectId: string, suiteId: string) =>
    api.get<{ data: TestCase[] }>(`/projects/${projectId}/suites/${suiteId}/cases`),
  get:    (projectId: string, suiteId: string, id: string) =>
    api.get<TestCase>(`/projects/${projectId}/suites/${suiteId}/cases/${id}`),
  create: (projectId: string, suiteId: string, body: Partial<TestCase>) =>
    api.post<TestCase>(`/projects/${projectId}/suites/${suiteId}/cases`, body),
  update: (projectId: string, suiteId: string, id: string, body: Partial<TestCase>) =>
    api.put<TestCase>(`/projects/${projectId}/suites/${suiteId}/cases/${id}`, body),
  delete: (projectId: string, suiteId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/suites/${suiteId}/cases/${id}`),
};

// ── Test Steps ────────────────────────────────────────────────────────────────

export interface TestStep {
  id: string;
  testCaseId: string;
  stepNumber: number;
  action: string;
  expectedResult: string;
  status: string;
}

export const testStepsApi = {
  list:   (projectId: string, suiteId: string, caseId: string) =>
    api.get<{ data: TestStep[] }>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps`),
  create: (projectId: string, suiteId: string, caseId: string, body: Partial<TestStep>) =>
    api.post<TestStep>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps`, body),
  update: (projectId: string, suiteId: string, caseId: string, id: string, body: Partial<TestStep>) =>
    api.put<TestStep>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps/${id}`, body),
  delete: (projectId: string, suiteId: string, caseId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps/${id}`),
};

// ── Test Runs ─────────────────────────────────────────────────────────────────

export interface TestRun {
  id: string;
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
}

export const testRunsApi = {
  list:     (projectId: string, page = 0, size = 20) =>
    api.get<{ data: TestRun[] }>(`/projects/${projectId}/runs?page=${page}&size=${size}`),
  get:      (projectId: string, id: string) =>
    api.get<TestRun>(`/projects/${projectId}/runs/${id}`),
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
  projectId: string;
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  link: string;
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  source: string;
  createdAt: string;
}

export const defectsApi = {
  list:   (projectId: string, page = 0, size = 20) =>
    api.get<{ data: Defect[] }>(`/projects/${projectId}/defects?page=${page}&size=${size}`),
  get:    (projectId: string, id: string) =>
    api.get<Defect>(`/projects/${projectId}/defects/${id}`),
  create: (projectId: string, body: Partial<Defect>) =>
    api.post<Defect>(`/projects/${projectId}/defects`, body),
  update: (projectId: string, id: string, body: Partial<Defect>) =>
    api.put<Defect>(`/projects/${projectId}/defects/${id}`, body),
  delete: (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/defects/${id}`),
};

// ── Attachments ───────────────────────────────────────────────────────────────

export interface Attachment {
  id: string;
  projectId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  uploadedAt: string;
}

export const attachmentsApi = {
  list:   (projectId: string) =>
    api.get<{ data: Attachment[] }>(`/projects/${projectId}/attachments`),
  delete: (projectId: string, id: string) =>
    api.delete<void>(`/projects/${projectId}/attachments/${id}`),
  upload: (projectId: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return fetch(`${BASE_URL}/projects/${projectId}/attachments`, {
      method: 'POST',
      credentials: 'include',
      body: form,
      // no Content-Type header — browser sets multipart boundary automatically
    }).then(r => r.json() as Promise<Attachment>);
  },
};

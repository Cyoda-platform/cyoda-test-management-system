import { APIRequestContext } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({ path: path.resolve(__dirname, '../../.env.test') });

const BASE = process.env.TEST_API_URL ?? 'http://localhost:8080/api';

function assertOk(status: number, body: unknown, label: string) {
  if (status < 200 || status >= 300) {
    throw new Error(`${label} failed with status ${status}: ${JSON.stringify(body)}`);
  }
}

// ── Auth ─────────────────────────────────────────────────────────────────────

export async function getAuthHeaders(
  api: APIRequestContext,
): Promise<Record<string, string>> {
  const username = process.env.TEST_USERNAME!;
  const password = process.env.TEST_PASSWORD!;

  const res = await api.post(`${BASE}/auth/login`, {
    data: { username, password },
    headers: { 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'getAuthHeaders');
  return { Authorization: `Bearer ${body.token}` };
}

// ── Projects ─────────────────────────────────────────────────────────────────

/**
 * Creates a project.
 * NOTE: requires the TEST_USERNAME account to have ADMIN role.
 * A non-admin token will receive HTTP 403.
 */
export async function createProject(
  api: APIRequestContext,
  headers: Record<string, string>,
): Promise<{ id: string; name: string }> {
  const name = `E2E Defects ${Date.now()}`;
  const res = await api.post(`${BASE}/projects`, {
    data: { name, description: 'Created by Playwright E2E tests' },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'createProject');
  return { id: body.id, name: body.name };
}

/** Deletes a project. NOTE: requires ADMIN role. */
export async function deleteProject(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
): Promise<void> {
  const res = await api.delete(`${BASE}/projects/${projectId}`, { headers });
  assertOk(res.status(), null, 'deleteProject');
}

// ── Suites ────────────────────────────────────────────────────────────────────

export async function createSuite(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
): Promise<{ id: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/suites`, {
    data: { name: 'E2E Suite', description: '', projectId },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'createSuite');
  return { id: body.id };
}

export async function deleteSuite(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
): Promise<void> {
  const res = await api.delete(`${BASE}/projects/${projectId}/suites/${suiteId}`, { headers });
  assertOk(res.status(), null, 'deleteSuite');
}

// ── Cases ─────────────────────────────────────────────────────────────────────

export async function createCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
  title: string,
): Promise<{ id: string }> {
  const res = await api.post(
    `${BASE}/projects/${projectId}/suites/${suiteId}/cases`,
    {
      data: { title, description: 'E2E test case', preconditions: '', priority: 'MEDIUM', suiteId, projectId },
      headers: { ...headers, 'Content-Type': 'application/json' },
    },
  );
  const body = await res.json();
  assertOk(res.status(), body, 'createCase');
  return { id: body.id };
}

export async function createStep(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
  caseId: string,
  stepNumber: number,
  action: string,
  expectedResult: string,
): Promise<{ id: string }> {
  const res = await api.post(
    `${BASE}/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps`,
    {
      data: { stepNumber, action, expectedResult, testCaseId: caseId },
      headers: { ...headers, 'Content-Type': 'application/json' },
    },
  );
  const body = await res.json();
  assertOk(res.status(), body, 'createStep');
  return { id: body.id };
}

export async function deleteCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
  caseId: string,
): Promise<void> {
  const res = await api.delete(
    `${BASE}/projects/${projectId}/suites/${suiteId}/cases/${caseId}`,
    { headers },
  );
  assertOk(res.status(), null, 'deleteCase');
}

// ── Test Runs ─────────────────────────────────────────────────────────────────

export async function createTestRun(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseIds: string[],
): Promise<{ id: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/runs`, {
    data: {
      name: `E2E Run ${Date.now()}`,
      environment: 'E2E',
      buildVersion: '1.0.0',
      description: 'Created by Playwright E2E tests',
      caseIds,
    },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'createTestRun');
  return { id: body.id };
}

export async function deleteTestRun(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  runId: string,
): Promise<void> {
  const res = await api.delete(`${BASE}/projects/${projectId}/runs/${runId}`, { headers });
  assertOk(res.status(), null, 'deleteTestRun');
}

// ── Defects ───────────────────────────────────────────────────────────────────

export async function createDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  body: {
    title: string;
    description?: string;
    severity?: string;
    status?: string;
    source?: string;
    testRunId?: string;
    testRunCaseId?: string;
  },
): Promise<{ id: string; displayId?: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/defects`, {
    data: {
      title: body.title,
      description: body.description ?? '',
      severity: body.severity ?? 'Major',
      status: body.status ?? 'Open',
      source: body.source ?? '',
      link: '',
      ...(body.testRunId ? { testRunId: body.testRunId } : {}),
      ...(body.testRunCaseId ? { testRunCaseId: body.testRunCaseId } : {}),
    },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const json = await res.json();
  assertOk(res.status(), json, 'createDefect');
  return { id: json.id, displayId: json.displayId };
}

export async function deleteDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  defectId: string,
): Promise<void> {
  const res = await api.delete(`${BASE}/projects/${projectId}/defects/${defectId}`, { headers });
  assertOk(res.status(), null, 'deleteDefect');
}

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
  const username = process.env.TEST_USERNAME ?? 'test_admin';
  const password = process.env.TEST_PASSWORD ?? 'admin123';

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
  name?: string,
  description = 'Created by Playwright E2E tests',
): Promise<{ id: string; name: string }> {
  const projectName = name ?? `E2E Project ${Date.now()}`;
  const res = await api.post(`${BASE}/projects`, {
    data: { name: projectName, description },
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
  name = 'E2E Suite',
): Promise<{ id: string; name: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/suites`, {
    data: { name, description: '', projectId },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'createSuite');
  return { id: body.id, name: body.name };
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

/**
 * Creates a test case with optional steps using batch endpoint.
 * This is the new API - steps are embedded in the case creation.
 */
export async function createCaseWithSteps(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
  title: string,
  steps?: Array<{ stepNumber: number; action: string; expectedResult: string }>,
): Promise<{ id: string; steps?: Array<{ id: string }> }> {
  const batchItem = {
    title,
    description: 'E2E test case',
    preconditions: '',
    priority: 'MEDIUM',
    steps: steps || [],
  };

  const res = await api.post(
    `${BASE}/projects/${projectId}/suites/${suiteId}/cases/batch`,
    {
      data: [batchItem],
      headers: { ...headers, 'Content-Type': 'application/json' },
    },
  );
  const body = await res.json();
  assertOk(res.status(), body, 'createCaseWithSteps');
  // Return the first (and only) created case
  const createdCase = Array.isArray(body) ? body[0] : body;
  return { id: createdCase.id, steps: createdCase.steps };
}

/**
 * @deprecated Use createCaseWithSteps instead - the old step endpoint no longer exists
 * Creates a test case step (legacy - endpoint removed in favor of embedded steps)
 */
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
  throw new Error(
    'createStep endpoint no longer exists. Use createCaseWithSteps instead to create cases with embedded steps.',
  );
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

  const runId = body.id;

  // Trigger the initialize_run workflow transition by updating the run
  // This causes the backend's SnapshotProcessor to create TestRunCase/TestRunStep records
  // Backend detects status='initial' and automatically sends the 'initialize_run' operation
  await api.put(`${BASE}/projects/${projectId}/runs/${runId}`, {
    data: {
      name: body.name,
      environment: body.environment,
      buildVersion: body.buildVersion,
      description: body.description,
      caseIds: body.caseIds,
      stepStatuses: body.stepStatuses || '{}',
    },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });

  // Wait for SnapshotProcessor to complete asynchronously (executionMode: ASYNC_NEW_TX)
  // It runs in a separate transaction and takes variable time depending on system load
  await new Promise(resolve => setTimeout(resolve, 5000));

  return { id: runId };
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

// ── Attachments ───────────────────────────────────────────────────────────────

export async function uploadAttachmentToCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseId: string,
  fileBuffer: Buffer,
  fileName: string,
  mimeType: string,
): Promise<{ id: string; attachmentType: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/attachments`, {
    headers,
    multipart: {
      file: { name: fileName, mimeType, buffer: fileBuffer },
      caseId,
      attachmentType: 'CASE',
    },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'uploadAttachmentToCase');
  return { id: body.id, attachmentType: body.attachmentType };
}

export async function uploadAttachmentToDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  defectId: string,
  fileBuffer: Buffer,
  fileName: string,
  mimeType: string,
): Promise<{ id: string; attachmentType: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/attachments`, {
    headers,
    multipart: {
      file: { name: fileName, mimeType, buffer: fileBuffer },
      defectId,
      attachmentType: 'DEFECT',
    },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'uploadAttachmentToDefect');
  return { id: body.id, attachmentType: body.attachmentType };
}

export async function uploadEvidence(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseId: string,
  runId: string,
  stepKey: string,
  fileBuffer: Buffer,
  fileName: string,
  mimeType: string,
): Promise<{ id: string; attachmentType: string; runId: string; stepKey: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/attachments`, {
    headers,
    multipart: {
      file: { name: fileName, mimeType, buffer: fileBuffer },
      caseId,
      attachmentType: 'EVIDENCE',
      runId,
      stepKey,
    },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'uploadEvidence');
  return { id: body.id, attachmentType: body.attachmentType, runId: body.runId, stepKey: body.stepKey };
}

export async function listAttachmentsByCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseId: string,
): Promise<Array<{ id: string; attachmentType: string; fileName: string }>> {
  const res = await api.get(`${BASE}/projects/${projectId}/attachments/by-case/${caseId}`, { headers });
  const body = await res.json();
  assertOk(res.status(), body, 'listAttachmentsByCase');
  return Array.isArray(body) ? body : [];
}

export async function listAttachmentsByDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  defectId: string,
): Promise<Array<{ id: string; attachmentType: string; fileName: string }>> {
  const res = await api.get(`${BASE}/projects/${projectId}/attachments/by-defect/${defectId}`, { headers });
  const body = await res.json();
  assertOk(res.status(), body, 'listAttachmentsByDefect');
  return Array.isArray(body) ? body : [];
}

export async function listEvidenceByStep(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  runId: string,
  caseId: string,
  stepKey: string,
): Promise<Array<{ id: string; attachmentType: string; stepKey: string; runId: string }>> {
  const res = await api.get(
    `${BASE}/projects/${projectId}/attachments/by-run/${runId}/case/${caseId}/step/${stepKey}`,
    { headers },
  );
  const body = await res.json();
  assertOk(res.status(), body, 'listEvidenceByStep');
  return Array.isArray(body) ? body : [];
}

export async function deleteAttachment(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  attachmentId: string,
): Promise<void> {
  const res = await api.delete(
    `${BASE}/projects/${projectId}/attachments/${attachmentId}`,
    { headers },
  );
  if (res.status() !== 204 && res.status() !== 404) {
    assertOk(res.status(), null, 'deleteAttachment');
  }
}

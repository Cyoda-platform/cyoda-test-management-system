import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCaseWithSteps,
  createTestRun,
  uploadEvidence,
  listAttachmentsByCase,
  listEvidenceByStep,
} from './helpers/api';

test.describe('Evidence attachments', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let caseId: string;
  let runId1: string;
  let runId2: string;
  let evidenceId: string;

  const smallPng = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==',
    'base64',
  );

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    const suiteId = suite.id;
    // Create case with embedded steps (new API)
    const tc = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'Evidence E2E Case',
      [
        { stepNumber: 1, action: 'Step One', expectedResult: 'Expected One' },
        { stepNumber: 2, action: 'Step Two', expectedResult: 'Expected Two' },
      ],
    );
    caseId = tc.id;
    const run1 = await createTestRun(apiCtx, authHeaders, projectId, [caseId]);
    runId1 = run1.id;
    const run2 = await createTestRun(apiCtx, authHeaders, projectId, [caseId]);
    runId2 = run2.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  test('API: upload evidence returns EVIDENCE type with correct runId and stepKey', async () => {
    const ev = await uploadEvidence(
      apiCtx, authHeaders, projectId, caseId, runId1, '1',
      smallPng, 'evidence.png', 'image/png',
    );
    evidenceId = ev.id;
    expect(ev.attachmentType).toBe('EVIDENCE');
    expect(ev.runId).toBe(runId1);
    expect(ev.stepKey).toBe('1');
  });

  test('API: evidence does NOT appear in list-by-case (backend filters CASE-only)', async () => {
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    const evidenceInCaseList = list.filter(a => a.attachmentType === 'EVIDENCE');
    expect(evidenceInCaseList).toHaveLength(0);
  });

  test('API: evidence appears in list-by-run-case-step for the correct step', async () => {
    const list = await listEvidenceByStep(apiCtx, authHeaders, projectId, runId1, caseId, '1');
    expect(list.find(a => a.id === evidenceId)).toBeDefined();
  });

  test('API: evidence does NOT appear for a different run (same case, same step)', async () => {
    const list = await listEvidenceByStep(apiCtx, authHeaders, projectId, runId2, caseId, '1');
    expect(list.find(a => a.id === evidenceId)).toBeUndefined();
  });

  test('API: evidence does NOT appear for a different step (same run, same case)', async () => {
    const list = await listEvidenceByStep(apiCtx, authHeaders, projectId, runId1, caseId, '2');
    expect(list.find(a => a.id === evidenceId)).toBeUndefined();
  });
});

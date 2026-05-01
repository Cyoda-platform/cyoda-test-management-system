import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCaseWithSteps,
  uploadAttachmentToCase,
  listAttachmentsByCase,
  deleteAttachment,
} from './helpers/api';

test.describe('Case attachments', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let suiteId: string;
  let caseId: string;
  let uploadedAttachmentId: string;

  // 1x1 transparent PNG — minimal valid image
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
    suiteId = suite.id;
    // Create case with embedded steps (new API)
    const tc = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'Attachment E2E Case',
      [{ stepNumber: 1, action: 'Do thing', expectedResult: 'Thing done' }],
    );
    caseId = tc.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  test('API: upload case attachment returns attachmentType=CASE', async () => {
    const att = await uploadAttachmentToCase(
      apiCtx, authHeaders, projectId, caseId,
      smallPng, 'test.png', 'image/png',
    );
    uploadedAttachmentId = att.id;
    expect(att.attachmentType).toBe('CASE');
  });

  test('API: list by case returns uploaded attachment with CASE type', async () => {
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    const found = list.find(a => a.id === uploadedAttachmentId);
    expect(found).toBeDefined();
    expect(found!.attachmentType).toBe('CASE');
  });

  test('API: delete attachment — GET by ID returns 404', async () => {
    await deleteAttachment(apiCtx, authHeaders, projectId, uploadedAttachmentId);
    const BASE = process.env.TEST_API_URL ?? 'http://localhost:8080/api';
    const res = await apiCtx.get(
      `${BASE}/projects/${projectId}/attachments/${uploadedAttachmentId}`,
      { headers: authHeaders },
    );
    expect(res.status()).toBe(404);
  });
});

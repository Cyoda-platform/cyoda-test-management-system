import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCase,
  createStep,
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
    const tc = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Attachment E2E Case');
    caseId = tc.id;
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 1, 'Do thing', 'Thing done');
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

  test('API: delete attachment removes it from list', async () => {
    await deleteAttachment(apiCtx, authHeaders, projectId, uploadedAttachmentId);
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    expect(list.find(a => a.id === uploadedAttachmentId)).toBeUndefined();
  });
});

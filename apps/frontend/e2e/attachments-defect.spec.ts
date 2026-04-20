import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCase,
  createDefect,
  uploadAttachmentToDefect,
  listAttachmentsByDefect,
  listAttachmentsByCase,
  deleteAttachment,
} from './helpers/api';

test.describe('Defect attachments', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let caseId: string;
  let defectId: string;
  let defectAttachmentId: string;

  const smallFile = Buffer.from('log content for test', 'utf-8');

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    const tc = await createCase(apiCtx, authHeaders, projectId, suite.id, 'Defect Attachment Case');
    caseId = tc.id;
    const defect = await createDefect(apiCtx, authHeaders, projectId, {
      title: 'E2E Defect With Attachment',
      severity: 'Major',
    });
    defectId = defect.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  test('API: upload to defect returns attachmentType=DEFECT', async () => {
    const att = await uploadAttachmentToDefect(
      apiCtx, authHeaders, projectId, defectId,
      smallFile, 'crash.log', 'text/plain',
    );
    defectAttachmentId = att.id;
    expect(att.attachmentType).toBe('DEFECT');
  });

  test('API: list-by-defect includes the uploaded attachment', async () => {
    const list = await listAttachmentsByDefect(apiCtx, authHeaders, projectId, defectId);
    expect(list.find(a => a.id === defectAttachmentId)).toBeDefined();
    expect(list.find(a => a.id === defectAttachmentId)!.attachmentType).toBe('DEFECT');
  });

  test('API: defect attachment does NOT appear in list-by-case', async () => {
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    expect(list.find(a => a.id === defectAttachmentId)).toBeUndefined();
  });

  test('API: delete defect attachment removes it', async () => {
    await deleteAttachment(apiCtx, authHeaders, projectId, defectAttachmentId);
    const list = await listAttachmentsByDefect(apiCtx, authHeaders, projectId, defectId);
    expect(list.find(a => a.id === defectAttachmentId)).toBeUndefined();
  });
});

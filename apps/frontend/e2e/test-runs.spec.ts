import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createTestRunDirect,
  deleteTestRun,
} from './helpers/api';

test.describe('Test Runs page', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders, 'E2E Test Runs Project', 'For test-runs E2E');
    projectId = proj.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  test('page loads — heading and Create Test Run button visible', async ({ page }) => {
    await page.goto(`/projects/${projectId}/runs`);
    await expect(page.getByRole('heading', { name: 'Test Runs' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Test Run' }).first()).toBeVisible();
  });

  test('delete test run — disappears from table without page refresh', async ({ page }) => {
    const run = await createTestRunDirect(apiCtx, authHeaders, projectId);

    await page.goto(`/projects/${projectId}/runs`);
    const row = page.locator('tr', { hasText: run.name });
    await expect(row).toBeVisible({ timeout: 15_000 });

    await row.getByTestId('run-delete-btn').click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await dialog.getByRole('button', { name: 'Delete' }).click();

    await expect(row).not.toBeVisible({ timeout: 10_000 });

    await deleteTestRun(apiCtx, authHeaders, projectId, run.id).catch(() => {});
  });
});

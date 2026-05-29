import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createReport,
  deleteReport,
} from './helpers/api';

test.describe('Reports page', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders, 'E2E Reports Project', 'For reports E2E');
    projectId = proj.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  test('page loads — heading and Create Report button visible', async ({ page }) => {
    await page.goto(`/projects/${projectId}/reports`);
    await expect(page.getByRole('heading', { name: 'Reports' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Report' }).first()).toBeVisible();
  });

  test('create report via form — appears in list', async ({ page }) => {
    const name = `E2E Report ${Date.now()}`;
    await page.goto(`/projects/${projectId}/reports/create`);
    await page.getByPlaceholder('e.g., Sprint 12 Summary').fill(name);
    await page.getByRole('button', { name: 'Create Report' }).click();

    // After creation, navigates to the report detail page (UUID, not 'create')
    await page.waitForURL(
      url => url.pathname.match(/\/reports\/[0-9a-f-]{36}$/) !== null,
      { timeout: 15_000 },
    );
    const reportId = page.url().match(/\/reports\/([^/]+)/)?.[1];

    // Navigate to list and verify the report appears
    await page.goto(`/projects/${projectId}/reports`);
    await expect(page.locator('tr', { hasText: name })).toBeVisible({ timeout: 15_000 });

    if (reportId) await deleteReport(apiCtx, authHeaders, projectId, reportId).catch(() => {});
  });

  test('delete report — disappears from list without page refresh', async ({ page }) => {
    const report = await createReport(apiCtx, authHeaders, projectId);

    await page.goto(`/projects/${projectId}/reports`);
    const row = page.locator('tr', { hasText: report.name });
    await expect(row).toBeVisible({ timeout: 15_000 });

    await row.getByTestId('report-delete-btn').click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await dialog.getByRole('button', { name: 'Delete' }).click();

    await expect(row).not.toBeVisible({ timeout: 10_000 });

    await deleteReport(apiCtx, authHeaders, projectId, report.id).catch(() => {});
  });
});

import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCase,
} from './helpers/api';

const BASE = process.env.TEST_API_URL ?? 'http://localhost:8080/api';

test.describe('Projects page', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let sharedProjectId: string;
  const SHARED_NAME = 'E2E Shared Project';

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders, SHARED_NAME, 'Shared project for Projects page E2E');
    sharedProjectId = proj.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, sharedProjectId).catch(() => {});
    await apiCtx.dispose();
  });

  test('page loads — heading and Create Project button visible', async ({ page }) => {
    await page.goto('/projects');
    await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Project' }).first()).toBeVisible();
  });

  test('create project via form — appears in table', async ({ page }) => {
    const name = `E2E Create ${Date.now()}`;
    await page.goto('/projects');
    await page.getByRole('button', { name: 'Create Project' }).first().click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await dialog.getByPlaceholder('Enter project name').fill(name);
    await dialog.getByPlaceholder('Describe the scope and objectives...').fill('Created by E2E form test');
    await dialog.getByRole('button', { name: 'Create Project' }).click();

    const row = page.locator('tr', { hasText: name });
    await expect(row).toBeVisible({ timeout: 15_000 });

    // Get project ID from URL to clean up
    await row.getByRole('button', { name }).click();
    await page.waitForURL(/\/projects\/.+\/repository/);
    const createdId = page.url().match(/\/projects\/([^/]+)\//)?.[1];
    if (createdId) await deleteProject(apiCtx, authHeaders, createdId).catch(() => {});
  });

  test('edit project — description change reflected without refresh', async ({ page }) => {
    await page.goto('/projects');
    // Use more specific selector to avoid strict mode violation from multiple matching rows
    const rows = page.locator('tr', { hasText: SHARED_NAME });
    // Get the first row that matches SHARED_NAME exactly (not partial matches like "Updated")
    const row = rows.first();
    await expect(row).toBeVisible({ timeout: 20_000 });

    const actionsCell = row.locator('td').last();
    await actionsCell.locator('button').nth(1).click(); // edit button

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Edit Project');

    const descField = dialog.getByPlaceholder('Describe the scope and objectives...');
    await descField.clear();
    await descField.fill('Updated by E2E edit test');
    await dialog.getByRole('button', { name: 'Save Changes' }).click();

    await expect(row.locator('td').nth(1)).toContainText('Updated by E2E edit test', { timeout: 10_000 });
  });

  test('open project — navigates to repository page', async ({ page }) => {
    await page.goto('/projects');
    const row = page.locator('tr', { hasText: SHARED_NAME });
    await expect(row).toBeVisible({ timeout: 20_000 });

    await row.getByRole('button', { name: SHARED_NAME }).click();
    await page.waitForURL(`**/projects/${sharedProjectId}/repository`);
    expect(page.url()).toContain(`/projects/${sharedProjectId}/repository`);
  });

  test('delete project — DELETE returns 204; cascade GET returns 404', async ({ page }) => {
    const proj = await createProject(apiCtx, authHeaders, 'E2E Cascade Delete', 'Cascade delete target');
    const suite = await createSuite(apiCtx, authHeaders, proj.id);
    await createCase(apiCtx, authHeaders, proj.id, suite.id, 'Cascade Case');

    await page.goto('/projects');
    const row = page.locator('tr', { hasText: 'E2E Cascade Delete' });
    await expect(row).toBeVisible({ timeout: 20_000 });

    const actionsCell = row.locator('td').last();
    await actionsCell.locator('button').nth(2).click(); // delete button

    const deleteDialog = page.getByRole('dialog');
    await expect(deleteDialog).toBeVisible({ timeout: 5_000 });
    await expect(deleteDialog).toContainText('Delete Project');

    // Intercept the DELETE request to verify it fires and succeeds
    const deleteResponsePromise = page.waitForResponse(
      r => r.url().includes(`/projects/${proj.id}`) && r.request().method() === 'DELETE',
    );
    await deleteDialog.getByRole('button', { name: 'Delete' }).click();
    const deleteResponse = await deleteResponsePromise;
    expect(deleteResponse.status()).toBe(204);

    // Cascade verification: direct GET should return 404 (FSM delete is immediately consistent)
    const res = await apiCtx.get(`${BASE}/projects/${proj.id}`, { headers: authHeaders });
    expect(res.status()).toBe(404);
  });
});

import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
} from './helpers/api';

const BASE = process.env.TEST_API_URL ?? 'http://localhost:8080/api';

test.describe('Repository — suites and test cases', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;

  const SUITE_NAME   = 'E2E UI Suite';
  const RENAMED_SUITE = 'E2E Renamed Suite';

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders, 'E2E Repository Project', 'For suite/case E2E tests');
    projectId = proj.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  // ── Suites ────────────────────────────────────────────────────────────────

  test('create suite via empty-state button — appears in sidebar', async ({ page }) => {
    await page.goto(`/projects/${projectId}/repository`);

    // Fresh project → empty-state button visible in the middle panel
    await expect(page.getByRole('button', { name: 'Create your first suite' })).toBeVisible({ timeout: 15_000 });
    await page.getByRole('button', { name: 'Create your first suite' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await dialog.getByPlaceholder('e.g. Authentication, Checkout Flow').fill(SUITE_NAME);
    await dialog.getByRole('button', { name: 'Create Suite' }).click();

    // Suite name appears as a button in the left sidebar tree
    await expect(page.getByRole('button', { name: SUITE_NAME })).toBeVisible({ timeout: 20_000 });
  });

  test('rename suite — new name reflected in suite header', async ({ page }) => {
    await page.goto(`/projects/${projectId}/repository`);

    // Suite header row in the middle panel (has .surface-low class + suite name)
    const suiteHeaderRow = page.locator('.surface-low.group', { hasText: SUITE_NAME });
    await expect(suiteHeaderRow).toBeVisible({ timeout: 20_000 });

    // Pencil = 3rd from last button (Trash=last, Copy=-2, Pencil=-3)
    await suiteHeaderRow.locator('button').nth(-3).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Edit Suite');
    const nameInput = dialog.getByPlaceholder('e.g. Authentication, Checkout Flow');
    await nameInput.clear();
    await nameInput.fill(RENAMED_SUITE);
    await dialog.getByRole('button', { name: 'Save Changes' }).click();

    await expect(page.locator('.surface-low.group', { hasText: RENAMED_SUITE })).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('.surface-low.group', { hasText: SUITE_NAME })).not.toBeVisible();
  });

  test('delete suite — confirmation modal, DELETE returns 204', async ({ page }) => {
    // Create a dedicated suite to delete (keeps shared suite intact)
    const { id: deleteSuiteId } = await createSuite(apiCtx, authHeaders, projectId, 'E2E Delete Suite');

    await page.goto(`/projects/${projectId}/repository`);
    const suiteHeaderRow = page.locator('.surface-low.group', { hasText: 'E2E Delete Suite' });
    await expect(suiteHeaderRow).toBeVisible({ timeout: 20_000 });

    const deleteResPromise = page.waitForResponse(
      r => r.url().includes(`/suites/${deleteSuiteId}`) && r.request().method() === 'DELETE',
    );
    await suiteHeaderRow.locator('button').last().click(); // Trash button

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Delete Suite');
    await dialog.getByRole('button', { name: 'Delete' }).click();

    expect((await deleteResPromise).status()).toBe(204);
  });

  // ── Test Cases ────────────────────────────────────────────────────────────

  test('quick create test case — appears in case list', async ({ page }) => {
    await page.goto(`/projects/${projectId}/repository`);

    const suiteHeaderRow = page.locator('.surface-low.group', { hasText: RENAMED_SUITE });
    await expect(suiteHeaderRow).toBeVisible({ timeout: 20_000 });

    // "+ Create quick test" button is in the suite footer row
    await page.getByRole('button', { name: 'Create quick test' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Quick Create Test Case');
    await dialog.getByPlaceholder('e.g. Verify login with valid credentials').fill('E2E Quick Case');
    await dialog.getByRole('button', { name: 'Create Case' }).click();

    await expect(page.getByText('E2E Quick Case')).toBeVisible({ timeout: 20_000 });
  });

  test('create test case via full form — appears in case list', async ({ page }) => {
    await page.goto(`/projects/${projectId}/repository`);
    await expect(page.locator('.surface-low.group', { hasText: RENAMED_SUITE })).toBeVisible({ timeout: 20_000 });

    // "+ Case" button opens CaseFormPage (full-page form)
    await page.getByRole('button', { name: 'Case' }).click();

    await expect(page.getByPlaceholder('e.g. Login with valid credentials')).toBeVisible({ timeout: 5_000 });
    await page.getByPlaceholder('e.g. Login with valid credentials').fill('E2E Full Form Case');
    await page.getByRole('button', { name: 'Create Case' }).click();

    // CaseFormPage closes → back to repository; case should appear
    await expect(page.getByText('E2E Full Form Case')).toBeVisible({ timeout: 20_000 });
  });
});

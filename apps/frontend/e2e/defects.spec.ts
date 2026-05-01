import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  deleteSuite,
  createCaseWithSteps,
  deleteCase,
  createTestRun,
  deleteTestRun,
  createDefect,
  deleteDefect,
} from './helpers/api';

test.describe('Defects page', () => {
  test.describe.configure({ mode: 'serial' });

  // ── Shared state ──────────────────────────────────────────────────────────────

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let suiteId: string;
  let caseId1: string;
  let caseId2: string;
  let runId: string;
  let sharedDefectId: string;   // used by tests 3, 4, 5
  let sourceDefectId: string;   // used by test 7

  const SHARED_TITLE = 'Shared E2E Defect';
  const SOURCE_TITLE  = 'Source Context Defect';

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);

    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;

    const suite = await createSuite(apiCtx, authHeaders, projectId);
    suiteId = suite.id;

    const case1 = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'E2E Case One',
      [
        { stepNumber: 1, action: 'Open the app', expectedResult: 'App opens' },
        { stepNumber: 2, action: 'Click submit', expectedResult: 'Form submits' },
      ],
    );
    caseId1 = case1.id;

    const case2 = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'E2E Case Two',
      [{ stepNumber: 1, action: 'Navigate to settings', expectedResult: 'Settings visible' }],
    );
    caseId2 = case2.id;

    const run = await createTestRun(apiCtx, authHeaders, projectId, [caseId1, caseId2]);
    runId = run.id;

    // Shared defect for read-only tests
    const shared = await createDefect(apiCtx, authHeaders, projectId, {
      title: SHARED_TITLE,
      description: 'Created by E2E test setup',
      severity: 'Major',
      status: 'Open',
    });
    sharedDefectId = shared.id;

    // Source-context defect: linked to test run with step source
    const source = await createDefect(apiCtx, authHeaders, projectId, {
      title: SOURCE_TITLE,
      description: 'Source context test',
      severity: 'Minor',
      status: 'Open',
      source: 'TC-1 · Step 1',
      testRunId: runId,
    });
    sourceDefectId = source.id;
  });

  test.afterAll(async () => {
    try {
      await deleteDefect(apiCtx, authHeaders, projectId, sharedDefectId).catch(() => {});
      await deleteDefect(apiCtx, authHeaders, projectId, sourceDefectId).catch(() => {});
      await deleteTestRun(apiCtx, authHeaders, projectId, runId).catch(() => {});
      await deleteCase(apiCtx, authHeaders, projectId, suiteId, caseId1).catch(() => {});
      await deleteCase(apiCtx, authHeaders, projectId, suiteId, caseId2).catch(() => {});
      await deleteSuite(apiCtx, authHeaders, projectId, suiteId).catch(() => {});
      await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    } finally {
      await apiCtx.dispose();
    }
  });

  // ── Tests ─────────────────────────────────────────────────────────────────────

  test('page loads and defects table is visible', async ({ page }) => {
    await page.goto(`/projects/${projectId}/defects`);
    await expect(page.getByRole('heading', { name: 'Defects' })).toBeVisible();
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('create defect via form, appears in table with DEF-N id', async ({ page }) => {
    await page.goto(`/projects/${projectId}/defects`);
    await page.getByRole('button', { name: 'Create Defect' }).click();

    await page.getByPlaceholder('Brief summary...').fill('UI Form Defect');
    await page.getByPlaceholder('Steps to reproduce...').fill('Fill in the form');
    await page.getByRole('button', { name: 'Create Defect' }).last().click();

    await expect(page.getByRole('cell', { name: 'UI Form Defect' })).toBeVisible({ timeout: 10_000 });
    const idCell = page.locator('tr', { hasText: 'UI Form Defect' }).locator('td').first();
    await expect(idCell).toHaveText(/DEF-\d+/);

    // Cleanup — get defect id from title attribute on ID cell
    const defectId = await idCell.getAttribute('title');
    if (defectId) await deleteDefect(apiCtx, authHeaders, projectId, defectId).catch(() => {});
  });

  test('change severity inline, reflected without page refresh', async ({ page }) => {
    await page.goto(`/projects/${projectId}/defects`);
    await expect(page.getByRole('cell', { name: SHARED_TITLE })).toBeVisible({ timeout: 10_000 });

    const row = page.locator('tr', { hasText: SHARED_TITLE });
    // Click on the Severity SelectTrigger in this row (stop-propagation cell)
    await row.locator('td').nth(2).click();
    await page.getByRole('option', { name: 'Minor' }).click();

    // Row should now show Minor (not Major) without refresh
    await expect(row.locator('td').nth(2)).toContainText('Minor', { timeout: 10_000 });
  });

  test('change status inline, reflected without page refresh', async ({ page }) => {
    await page.goto(`/projects/${projectId}/defects`);
    await expect(page.getByRole('cell', { name: SHARED_TITLE })).toBeVisible({ timeout: 10_000 });

    const row = page.locator('tr', { hasText: SHARED_TITLE });
    await row.locator('td').nth(3).click();
    await page.getByRole('option', { name: 'Fixed' }).click();

    await expect(row.locator('td').nth(3)).toContainText('Fixed', { timeout: 10_000 });
  });

  test('view defect modal shows correct fields', async ({ page }) => {
    await page.goto(`/projects/${projectId}/defects`);
    await expect(page.getByRole('cell', { name: SHARED_TITLE })).toBeVisible({ timeout: 10_000 });

    const row = page.locator('tr', { hasText: SHARED_TITLE });
    await row.getByTestId('defect-view-btn').click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText(SHARED_TITLE);
    await expect(dialog).toContainText('Description');
    await expect(dialog).toContainText('Created by E2E test setup');
  });

  test('delete defect, disappears from table without page refresh', async ({ page }) => {
    // Create a fresh defect via API to avoid polluting shared state
    const defect = await createDefect(apiCtx, authHeaders, projectId, {
      title: 'Defect To Delete',
      severity: 'Minor',
      status: 'Open',
    });

    await page.goto(`/projects/${projectId}/defects`);
    await expect(page.getByRole('cell', { name: 'Defect To Delete' })).toBeVisible({ timeout: 10_000 });

    const row = page.locator('tr', { hasText: 'Defect To Delete' });
    await row.getByTestId('defect-delete-btn').click();

    const dialog = page.getByRole('dialog', { name: 'Remove Defect' });
    await expect(dialog).toBeVisible();
    await dialog.getByRole('button', { name: 'Remove' }).click();

    await expect(page.getByRole('cell', { name: 'Defect To Delete' })).not.toBeVisible({ timeout: 10_000 });

    // Server-side cleanup in case optimistic delete failed
    await deleteDefect(apiCtx, authHeaders, projectId, defect.id).catch(() => {});
  });

  test('Source column shows TR-N · TC-N · Step N for run-linked defect', async ({ page }) => {
    await page.goto(`/projects/${projectId}/defects`);
    await expect(page.getByRole('cell', { name: SOURCE_TITLE })).toBeVisible({ timeout: 10_000 });

    const row = page.locator('tr', { hasText: SOURCE_TITLE });
    const sourceCell = row.locator('td').nth(4); // Source is 5th column (0-indexed: ID, Title, Severity, Status, Source)
    // Should show "TR-1 · TC-1 · Step 1" or similar — TR-N part comes from runDisplayIdMap
    await expect(sourceCell).toContainText('TR-');
    await expect(sourceCell).toContainText('TC-1 · Step 1');
  });
});

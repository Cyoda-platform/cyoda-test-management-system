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
} from './helpers/api';

/**
 * Verifies that soft-deleting a case or suite from the repository does NOT
 * remove it from an existing test run. The run should display snapshot data
 * (title, steps) that was copied at run-creation time.
 */
test.describe('RunExecution: soft-deleted cases remain visible', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let suiteId: string;
  let caseId1: string;
  let caseId2: string;
  let runId: string;

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);

    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;

    const suite = await createSuite(apiCtx, authHeaders, projectId, 'Soft-Delete Suite');
    suiteId = suite.id;

    const case1 = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'Case To Be Deleted',
      [
        { stepNumber: 1, action: 'Open app', expectedResult: 'App loads' },
        { stepNumber: 2, action: 'Click button', expectedResult: 'Dialog opens' },
      ],
    );
    caseId1 = case1.id;

    const case2 = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'Surviving Case',
      [{ stepNumber: 1, action: 'Check footer', expectedResult: 'Footer visible' }],
    );
    caseId2 = case2.id;

    const run = await createTestRun(apiCtx, authHeaders, projectId, [caseId1, caseId2]);
    runId = run.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  async function goToRun() {
    // Return a fresh page navigation function; caller provides the page.
  }

  test('both cases visible before deletion', async ({ page }) => {
    await page.goto(`/projects/${projectId}/runs/${runId}`);
    await expect(page.getByTestId('steps-container')).toBeVisible({ timeout: 15_000 });

    await expect(page.getByText('Case To Be Deleted').first()).toBeVisible();
    await expect(page.getByText('Surviving Case').first()).toBeVisible();
  });

  test('deleted case still visible after soft-delete', async ({ page }) => {
    // Delete case 1 from the repository via API (soft-delete)
    await deleteCase(apiCtx, authHeaders, projectId, suiteId, caseId1);

    // Navigate fresh to the run — no reload of a stale page
    await page.goto(`/projects/${projectId}/runs/${runId}`);
    await expect(page.getByTestId('steps-container')).toBeVisible({ timeout: 15_000 });

    // The deleted case must still appear in the sidebar using snapshot data (title is snapshotted)
    await expect(page.getByText('Case To Be Deleted').first()).toBeVisible({ timeout: 10_000 });

    // The surviving case is unaffected
    await expect(page.getByText('Surviving Case').first()).toBeVisible();
  });

  test('run still usable after soft-deleting the suite', async ({ page }) => {
    // Delete the whole suite (soft-deletes remaining cases too)
    await deleteSuite(apiCtx, authHeaders, projectId, suiteId);

    await page.goto(`/projects/${projectId}/runs/${runId}`);

    // Both cases must still be present in the sidebar via snapshot titles
    await expect(page.getByText('Case To Be Deleted').first()).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText('Surviving Case').first()).toBeVisible({ timeout: 10_000 });
  });
});

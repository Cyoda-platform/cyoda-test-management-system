import { test, expect, APIRequestContext, type Page } from '@playwright/test';
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
} from './helpers/api';

test.describe('RunExecution defects', () => {
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

    const suite = await createSuite(apiCtx, authHeaders, projectId);
    suiteId = suite.id;

    const case1 = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'Run E2E Case One',
      [
        { stepNumber: 1, action: 'Open homepage', expectedResult: 'Homepage loads' },
        { stepNumber: 2, action: 'Click Login', expectedResult: 'Login form appears' },
      ],
    );
    caseId1 = case1.id;

    const case2 = await createCaseWithSteps(
      apiCtx,
      authHeaders,
      projectId,
      suiteId,
      'Run E2E Case Two',
      [{ stepNumber: 1, action: 'Check sidebar', expectedResult: 'Sidebar visible' }],
    );
    caseId2 = case2.id;

    const run = await createTestRun(apiCtx, authHeaders, projectId, [caseId1, caseId2]);
    runId = run.id;
  });

  test.afterAll(async () => {
    try {
      await deleteTestRun(apiCtx, authHeaders, projectId, runId).catch(() => {});
      await deleteCase(apiCtx, authHeaders, projectId, suiteId, caseId1).catch(() => {});
      await deleteCase(apiCtx, authHeaders, projectId, suiteId, caseId2).catch(() => {});
      await deleteSuite(apiCtx, authHeaders, projectId, suiteId).catch(() => {});
      await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    } finally {
      await apiCtx.dispose();
    }
  });

  async function goToRun(page: Page) {
    await page.goto(`/projects/${projectId}/runs/${runId}`);
    // Wait for the steps container (first case auto-selected on load)
    // Steps are now rendered as cards, not a table
    await expect(page.getByTestId('steps-container')).toBeVisible({ timeout: 15_000 });
  }

  // ── Test 1: Create defect via step "failed" button (auto-opens modal) ─────────

  test('create defect via step fail — modal auto-opens', async ({ page }) => {
    await goToRun(page);

    // Click the "failed" status button on the first step — modal should open automatically
    const failedBtn = page.getByRole('button', { name: /^failed$/i }).first();
    await expect(failedBtn).toBeVisible({ timeout: 10_000 });
    await failedBtn.click();

    // CreateDefectModal should open automatically
    const dialog = page.getByRole('dialog', { name: /create defect/i });
    await expect(dialog).toBeVisible({ timeout: 5_000 });

    // Fill in required title
    await dialog.getByPlaceholder(/brief summary/i).fill('Auto-opened defect from failed step');

    // Submit
    await dialog.getByRole('button', { name: /create defect/i }).click();

    // Dialog should close
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    // Defect table should appear below steps
    await expect(page.locator('table').last()).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('cell', { name: 'Auto-opened defect from failed step' })).toBeVisible();
  });

  // ── Test 2: Create defect via bug button on step ──────────────────────────────

  test('create defect via bug button on step', async ({ page }) => {
    await goToRun(page);

    // Click the bug button on the second step
    const bugBtns = page.getByTestId('step-bug-btn');
    await expect(bugBtns.first()).toBeVisible({ timeout: 10_000 });
    await bugBtns.nth(1).click();

    const dialog = page.getByRole('dialog', { name: /create defect/i });
    await expect(dialog).toBeVisible({ timeout: 5_000 });

    await dialog.getByPlaceholder(/brief summary/i).fill('Defect via bug button step 2');
    await dialog.getByRole('button', { name: /create defect/i }).click();

    await expect(dialog).not.toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('cell', { name: 'Defect via bug button step 2' })).toBeVisible();
  });

  // ── Test 3: Create defect via "Report Case Issue" button ─────────────────────

  test('create defect via Report Case Issue', async ({ page }) => {
    await goToRun(page);

    const reportBtn = page.getByRole('button', { name: /report case issue/i });
    await expect(reportBtn).toBeVisible({ timeout: 10_000 });
    await reportBtn.click();

    const dialog = page.getByRole('dialog', { name: /create defect/i });
    await expect(dialog).toBeVisible({ timeout: 5_000 });

    await dialog.getByPlaceholder(/brief summary/i).fill('Case-level defect via Report Case Issue');
    await dialog.getByRole('button', { name: /create defect/i }).click();

    await expect(dialog).not.toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('cell', { name: 'Case-level defect via Report Case Issue' })).toBeVisible();
  });

  // ── Test 4: View defect modal shows title/severity/status; no TR-N in source ──

  test('view defect modal shows correct fields and no TR-N in source', async ({ page }) => {
    await goToRun(page);

    // Wait for the defect table to appear (defects from previous tests persisted in run state)
    await expect(page.getByTestId('defect-view-btn').first()).toBeVisible({ timeout: 10_000 });
    await page.getByTestId('defect-view-btn').first().click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible({ timeout: 5_000 });

    // Should show title, severity and status
    await expect(dialog).toContainText(/Auto-opened defect from failed step|Defect via bug button|Case-level defect/i);
    // Severity should be present (default Major)
    await expect(dialog).toContainText(/Major|Minor|Critical/i);
    // Status should be present (default Open)
    await expect(dialog).toContainText(/Open|In Progress|Fixed|Closed/i);

    // Source field (if present) must not contain TR-N prefix
    const dialogText = await dialog.textContent() ?? '';
    expect(dialogText).not.toMatch(/TR-\d+/);

    // Close the dialog — prefer a close button over Escape (which can be intercepted)
    const closeBtn = dialog.getByRole('button', { name: /close/i });
    if (await closeBtn.count() > 0) {
      await closeBtn.first().click();
    } else {
      await page.keyboard.press('Escape');
    }
    await expect(dialog).not.toBeVisible({ timeout: 5_000 });
  });

  // ── Test 5: Edit defect — change severity to Critical ─────────────────────────

  test('edit defect — change severity to Critical', async ({ page }) => {
    await goToRun(page);

    // Create a defect first (local state is fresh on each navigation)
    await page.getByRole('button', { name: /report case issue/i }).click();
    const createDialog = page.getByRole('dialog', { name: /create defect/i });
    await expect(createDialog).toBeVisible({ timeout: 5_000 });
    await createDialog.getByPlaceholder(/brief summary/i).fill('Edit Severity Defect');
    await createDialog.getByRole('button', { name: /create defect/i }).click();
    await expect(createDialog).not.toBeVisible({ timeout: 10_000 });
    await expect(page.locator('td', { hasText: 'Edit Severity Defect' })).toBeVisible({ timeout: 10_000 });

    // Open edit modal for that defect
    const row = page.locator('tr', { hasText: 'Edit Severity Defect' });
    await row.getByTestId('defect-edit-btn').click();

    const dialog = page.getByRole('dialog', { name: /edit defect/i });
    await expect(dialog).toBeVisible({ timeout: 5_000 });

    // First combobox in edit dialog is Severity (second would be Status)
    const severitySelect = dialog.getByRole('combobox').first();
    await severitySelect.click();
    await page.getByRole('option', { name: 'Critical' }).click();

    await dialog.getByRole('button', { name: /save changes/i }).click();
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    await expect(row).toContainText(/Critical/i, { timeout: 5_000 });
  });

  // ── Test 6: Delete defect — no confirmation dialog, immediate removal ─────────

  test('delete defect — immediate removal without confirmation', async ({ page }) => {
    await goToRun(page);

    // Count defects before deletion
    await expect(page.getByTestId('defect-delete-btn').first()).toBeVisible({ timeout: 10_000 });
    const beforeCount = await page.getByTestId('defect-delete-btn').count();

    await page.getByTestId('defect-delete-btn').first().click();

    // No confirmation dialog should appear — deletion is immediate
    // Verify row count decreased
    await expect(page.getByTestId('defect-delete-btn')).toHaveCount(beforeCount - 1, { timeout: 5_000 });
  });

  // ── Test 7: Source column shows TC-N · Step N (no TR-N prefix) ───────────────

  test('Source column shows TC-N · Step N without TR-N prefix', async ({ page }) => {
    await goToRun(page);

    // Create defect via step fail — auto-sets source with step info in local state
    const failedBtn = page.getByRole('button', { name: /^failed$/i }).first();
    await expect(failedBtn).toBeVisible({ timeout: 10_000 });
    await failedBtn.click();

    const dialog = page.getByRole('dialog', { name: /create defect/i });
    await expect(dialog).toBeVisible({ timeout: 5_000 });
    await dialog.getByPlaceholder(/brief summary/i).fill('Source Col Defect');
    await dialog.getByRole('button', { name: /create defect/i }).click();
    await expect(page.getByRole('cell', { name: 'Source Col Defect' })).toBeVisible({ timeout: 10_000 });

    // Check the source cell of this specific row (Source is column index 4)
    const row = page.locator('tr', { hasText: 'Source Col Defect' });
    const sourceCell = row.locator('td').nth(4);
    const text = await sourceCell.textContent() ?? '';

    // Source should contain a step reference but NOT a TR-N prefix
    expect(text).toMatch(/Step \d+/);
    expect(text).not.toMatch(/TR-\d+/);
  });
});

import { test, expect, APIRequestContext, type Page } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCase,
  createStep,
  createTestRun,
  uploadEvidence,
} from './helpers/api';

const smallPng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==',
  'base64',
);

// ── Suite 1: Step status persistence ─────────────────────────────────────────

test.describe('Step status persistence after reload', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let caseId: string;
  let runId: string;

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    const suiteId = suite.id;
    const tc = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Status Persistence Case');
    caseId = tc.id;
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 1, 'Open the homepage', 'Homepage loads');
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 2, 'Click Login button', 'Login form appears');
    const run = await createTestRun(apiCtx, authHeaders, projectId, [caseId]);
    runId = run.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  async function goToRun(page: Page) {
    await page.goto(`/projects/${projectId}/runs/${runId}`);
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 });
  }

  test('step statuses persist after page reload', async ({ page }) => {
    await goToRun(page);

    // Set step 1 → passed, step 2 → failed
    const rows = page.locator('tbody tr');
    await rows.first().getByRole('button', { name: /^passed$/i }).click();
    await rows.nth(1).getByRole('button', { name: /^failed$/i }).click();

    // Wait for updateRun mutation to complete before reloading
    await page.waitForTimeout(1_500);

    await page.reload();
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 });

    // persistedStepStatusByKey restores statuses from run.stepStatuses JSON
    const row1PassedBtn = page.locator('tbody tr').first().getByRole('button', { name: /^passed$/i });
    const row2FailedBtn = page.locator('tbody tr').nth(1).getByRole('button', { name: /^failed$/i });

    await expect(row1PassedBtn).toHaveClass(/bg-success/, { timeout: 5_000 });
    await expect(row2FailedBtn).toHaveClass(/bg-destructive/, { timeout: 5_000 });
  });

  test('non-active status buttons keep outline style after reload', async ({ page }) => {
    await goToRun(page);

    // Step 1 active status is "passed" so "failed" button must not be filled
    const row1FailedBtn = page.locator('tbody tr').first().getByRole('button', { name: /^failed$/i });
    await expect(row1FailedBtn).not.toHaveClass(/bg-destructive/, { timeout: 5_000 });

    // Step 2 active status is "failed" so "passed" button must not be filled
    const row2PassedBtn = page.locator('tbody tr').nth(1).getByRole('button', { name: /^passed$/i });
    await expect(row2PassedBtn).not.toHaveClass(/bg-success/, { timeout: 5_000 });
  });
});

// ── Suite 2: Evidence badge persistence ──────────────────────────────────────

test.describe('Evidence badge persistence after reload', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let caseId: string;
  let runId: string;

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    const suiteId = suite.id;
    const tc = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Evidence Badge Case');
    caseId = tc.id;
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 1, 'Open the homepage', 'Homepage loads');
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 2, 'Click Login button', 'Login form appears');
    const run = await createTestRun(apiCtx, authHeaders, projectId, [caseId]);
    runId = run.id;

    // Upload one evidence file to step 1 via API so it is already persisted on first load
    await uploadEvidence(apiCtx, authHeaders, projectId, caseId, runId, '1', smallPng, 'evidence.png', 'image/png');
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  async function goToRun(page: Page) {
    await page.goto(`/projects/${projectId}/runs/${runId}`);
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 });
  }

  test('evidence badge shows count on initial load', async ({ page }) => {
    await goToRun(page);
    const badge = page.locator('tbody tr').first().getByTestId('step-evidence-badge');
    await expect(badge).toBeVisible({ timeout: 10_000 });
    await expect(badge).toHaveText('1');
  });

  test('evidence badge count persists after page reload', async ({ page }) => {
    await goToRun(page);
    await expect(page.locator('tbody tr').first().getByTestId('step-evidence-badge')).toBeVisible({ timeout: 10_000 });

    await page.reload();
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 });

    const badge = page.locator('tbody tr').first().getByTestId('step-evidence-badge');
    await expect(badge).toBeVisible({ timeout: 10_000 });
    await expect(badge).toHaveText('1');
  });

  test('evidence badge does NOT appear on step 2 (no evidence there)', async ({ page }) => {
    await goToRun(page);
    const row2Badge = page.locator('tbody tr').nth(1).getByTestId('step-evidence-badge');
    await expect(row2Badge).not.toBeVisible({ timeout: 5_000 });
  });
});

// ── Suite 3: Defect badge persistence ────────────────────────────────────────

test.describe('Defect badge persistence after reload', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let caseId: string;
  let runId: string;

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    const suiteId = suite.id;
    const tc = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Defect Badge Case');
    caseId = tc.id;
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 1, 'Open the homepage', 'Homepage loads');
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 2, 'Click Login button', 'Login form appears');
    const run = await createTestRun(apiCtx, authHeaders, projectId, [caseId]);
    runId = run.id;
  });

  test.afterAll(async () => {
    await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
    await apiCtx.dispose();
  });

  async function goToRun(page: Page) {
    await page.goto(`/projects/${projectId}/runs/${runId}`);
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 });
  }

  test('defect badge appears after creating defect via step bug button', async ({ page }) => {
    await goToRun(page);

    // Click the bug button on step 1 (first step-bug-btn in the table)
    const bugBtns = page.getByTestId('step-bug-btn');
    await expect(bugBtns.first()).toBeVisible({ timeout: 10_000 });
    await bugBtns.first().click();

    const dialog = page.getByRole('dialog', { name: /create defect/i });
    await expect(dialog).toBeVisible({ timeout: 5_000 });
    await dialog.getByPlaceholder(/brief summary/i).fill('Badge Persistence Defect');
    await dialog.getByRole('button', { name: /create defect/i }).click();
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    const badge = page.locator('tbody tr').first().getByTestId('step-defect-badge');
    await expect(badge).toBeVisible({ timeout: 5_000 });
    await expect(badge).toHaveText('1');
  });

  test('defect badge persists after page reload (stepIdx recovered from source field)', async ({ page }) => {
    await goToRun(page);

    // The defect created in the previous test is persisted to the backend.
    // On reload, stepIdx is reconstructed from the source field regex "· Step N".
    const badge = page.locator('tbody tr').first().getByTestId('step-defect-badge');
    await expect(badge).toBeVisible({ timeout: 10_000 });
    await expect(badge).toHaveText('1');
  });

  test('defect badge does NOT appear on step 2 (defect was filed against step 1)', async ({ page }) => {
    await goToRun(page);
    const row2Badge = page.locator('tbody tr').nth(1).getByTestId('step-defect-badge');
    await expect(row2Badge).not.toBeVisible({ timeout: 5_000 });
  });
});

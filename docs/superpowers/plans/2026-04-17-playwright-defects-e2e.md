# Playwright E2E Tests — Defects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Playwright E2E tests covering the full defect lifecycle on the Defects page and RunExecution page, running against a live Cyoda backend.

**Architecture:** Tests run against a real stack (frontend `localhost:5173`, backend `localhost:8080`). Auth is done once in `global-setup.ts` via UI form login, browser state saved to `e2e/.auth/user.json`. Each spec file creates all required entities (project → suite → cases with steps → test run) in `beforeAll` via backend API, and deletes them in `afterAll`. Icon-only buttons get `data-testid` attributes for reliable selection.

**Tech Stack:** Playwright 1.57 (already installed), TypeScript, dotenv, live Cyoda backend.

**Spec:** `docs/superpowers/specs/2026-04-17-playwright-defects-e2e-design.md`

---

## File Structure

```
apps/frontend/
├── playwright.config.ts            ← rewrite
├── package.json                    ← add test:e2e script
├── .env.test                       ← gitignored, created by developer
├── .env.test.example               ← committed placeholder
└── e2e/
    ├── global-setup.ts             ← create
    ├── .auth/
    │   └── .gitkeep                ← create (directory tracked, contents gitignored)
    └── helpers/
        └── api.ts                  ← create

apps/frontend/src/pages/
├── Defects.tsx                     ← add data-testid to Eye/Pencil/Trash buttons
└── RunExecution.tsx                ← add data-testid to Bug/Eye/Pencil/Trash buttons

apps/frontend/e2e/
├── defects.spec.ts                 ← create
└── run-execution-defects.spec.ts   ← create
```

---

## Context for all tasks

- Backend base URL: `http://localhost:8080/api`
- Frontend base URL: `http://localhost:5173`
- Auth: POST `/auth/login` → `{ token }` → `Authorization: Bearer <token>` header
- Login page selectors: `input[placeholder="Enter your username"]`, `input[type="password"]`, button with text "Sign In"
- Defects page URL: `/projects/{projectId}/defects`
- RunExecution page URL: `/projects/{projectId}/runs/{runId}`
- On RunExecution, `activeCase` defaults to first case (`selectedIdx = 0`) — no sidebar click needed
- Step status buttons have text: `untested`, `passed`, `failed`, `skipped`
- "Report Case Issue" button has that exact text
- Delete confirmation: dialog with "Remove Defect" title, confirm button says "Remove"
- RunExecution defect delete: no dialog — click trash button, row disappears immediately

---

### Task 1: Infrastructure setup

**Files:**
- Modify: `apps/frontend/playwright.config.ts`
- Modify: `apps/frontend/package.json`
- Create: `apps/frontend/.env.test.example`
- Create: `apps/frontend/e2e/.auth/.gitkeep`
- Modify: `.gitignore` (root — add `.env.test.example` exception)

- [ ] **Step 1: Install dotenv**

```bash
cd apps/frontend && npm install --save-dev dotenv
```

Expected: `dotenv` appears in `devDependencies` in `package.json`.

- [ ] **Step 2: Rewrite `apps/frontend/playwright.config.ts`**

Replace the entire file (current contents reference an uninstalled package):

```typescript
import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';

dotenv.config({ path: '.env.test' });

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173',
    storageState: 'e2e/.auth/user.json',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
```

- [ ] **Step 3: Add `test:e2e` script to `apps/frontend/package.json`**

In the `"scripts"` section, add after the existing `"test:watch"` entry:

```json
"test:e2e": "playwright test"
```

- [ ] **Step 4: Create `apps/frontend/.env.test.example`**

```
# Copy this to .env.test and fill in your values.
TEST_USERNAME=your_username
TEST_PASSWORD=your_password
TEST_API_URL=http://localhost:8080/api
PLAYWRIGHT_BASE_URL=http://localhost:5173
```

- [ ] **Step 5: Create `apps/frontend/e2e/.auth/.gitkeep`**

Create an empty file at `apps/frontend/e2e/.auth/.gitkeep` so the directory is tracked but its contents (auth state JSON) are ignored.

- [ ] **Step 6: Update root `.gitignore`**

The root `.gitignore` already has `.env*` gitignored with exceptions for `.env.template` and `.env.example`.

Add `.env.test.example` to the list of exceptions (right after the existing `!.env.example` line):

```
.env*
!.env.template
!.env.example
!apps/frontend/.env.test.example
```

Also add `e2e/.auth/*.json` to ignore the saved auth state:

```
# Playwright auth state
apps/frontend/e2e/.auth/*.json
```

- [ ] **Step 7: Install Playwright browsers**

```bash
cd apps/frontend && npx playwright install chromium
```

Expected: Chromium binary downloads successfully.

- [ ] **Step 8: Verify Playwright config loads**

```bash
cd apps/frontend && npx playwright --version
```

Expected: prints a version number like `Version 1.59.1` with no import errors.

- [ ] **Step 9: Commit**

```bash
cd apps/frontend && git add playwright.config.ts package.json package-lock.json .env.test.example e2e/.auth/.gitkeep
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system && git add .gitignore
git commit -m "feat(e2e): Playwright infrastructure setup — config, dotenv, browser"
```

---

### Task 2: Global setup + API helper

**Files:**
- Create: `apps/frontend/e2e/global-setup.ts`
- Create: `apps/frontend/e2e/helpers/api.ts`

> **Context:** The `global-setup.ts` runs once before all tests. It opens a Chromium browser, navigates to the login page, fills in credentials from env vars, clicks "Sign In", waits for redirect to `/projects`, then saves the full browser storage state (including `localStorage.auth_token`) to `e2e/.auth/user.json`. All test specs then load with this state and skip the login flow entirely.
>
> The `api.ts` helper makes direct HTTP calls to the backend API using Playwright's `APIRequestContext`. It is used in `beforeAll`/`afterAll` blocks to create and delete test entities. Auth is done by POSTing to `/auth/login` and extracting the Bearer token.

- [ ] **Step 1: Create `apps/frontend/e2e/global-setup.ts`**

```typescript
import { chromium } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../.env.test') });

export default async function globalSetup() {
  const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173';
  const username = process.env.TEST_USERNAME;
  const password = process.env.TEST_PASSWORD;

  if (!username || !password) {
    throw new Error('TEST_USERNAME and TEST_PASSWORD must be set in .env.test');
  }

  const browser = await chromium.launch();
  const page = await browser.newPage();

  await page.goto(baseURL);
  await page.locator('input[placeholder="Enter your username"]').fill(username);
  await page.locator('input[type="password"]').fill(password);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL('**/projects**', { timeout: 15_000 });

  await page.context().storageState({ path: 'e2e/.auth/user.json' });
  await browser.close();

  console.log('[global-setup] Auth state saved to e2e/.auth/user.json');
}
```

- [ ] **Step 2: Create `apps/frontend/e2e/helpers/api.ts`**

```typescript
import { APIRequestContext } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env.test') });

const BASE = process.env.TEST_API_URL ?? 'http://localhost:8080/api';

function assertOk(status: number, body: unknown, label: string) {
  if (status < 200 || status >= 300) {
    throw new Error(`${label} failed with status ${status}: ${JSON.stringify(body)}`);
  }
}

// ── Auth ─────────────────────────────────────────────────────────────────────

export async function getAuthHeaders(
  api: APIRequestContext,
): Promise<Record<string, string>> {
  const username = process.env.TEST_USERNAME!;
  const password = process.env.TEST_PASSWORD!;

  const res = await api.post(`${BASE}/auth/login`, {
    data: { username, password },
    headers: { 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'getAuthHeaders');
  return { Authorization: `Bearer ${body.token}` };
}

// ── Projects ─────────────────────────────────────────────────────────────────

export async function createProject(
  api: APIRequestContext,
  headers: Record<string, string>,
): Promise<{ id: string; name: string }> {
  const name = `E2E Defects ${Date.now()}`;
  const res = await api.post(`${BASE}/projects`, {
    data: { name, description: 'Created by Playwright E2E tests' },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'createProject');
  return { id: body.id, name: body.name };
}

export async function deleteProject(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
): Promise<void> {
  await api.delete(`${BASE}/projects/${projectId}`, { headers });
}

// ── Suites ────────────────────────────────────────────────────────────────────

export async function createSuite(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
): Promise<{ id: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/suites`, {
    data: { name: 'E2E Suite', description: '', projectId },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'createSuite');
  return { id: body.id };
}

export async function deleteSuite(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
): Promise<void> {
  await api.delete(`${BASE}/projects/${projectId}/suites/${suiteId}`, { headers });
}

// ── Cases ─────────────────────────────────────────────────────────────────────

export async function createCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
  title: string,
): Promise<{ id: string }> {
  const res = await api.post(
    `${BASE}/projects/${projectId}/suites/${suiteId}/cases`,
    {
      data: { title, description: 'E2E test case', preconditions: '', priority: 'MEDIUM', suiteId, projectId },
      headers: { ...headers, 'Content-Type': 'application/json' },
    },
  );
  const body = await res.json();
  assertOk(res.status(), body, 'createCase');
  return { id: body.id };
}

export async function createStep(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
  caseId: string,
  stepNumber: number,
  action: string,
  expectedResult: string,
): Promise<{ id: string }> {
  const res = await api.post(
    `${BASE}/projects/${projectId}/suites/${suiteId}/cases/${caseId}/steps`,
    {
      data: { stepNumber, action, expectedResult, testCaseId: caseId },
      headers: { ...headers, 'Content-Type': 'application/json' },
    },
  );
  const body = await res.json();
  assertOk(res.status(), body, 'createStep');
  return { id: body.id };
}

export async function deleteCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  suiteId: string,
  caseId: string,
): Promise<void> {
  await api.delete(
    `${BASE}/projects/${projectId}/suites/${suiteId}/cases/${caseId}`,
    { headers },
  );
}

// ── Test Runs ─────────────────────────────────────────────────────────────────

export async function createTestRun(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseIds: string[],
): Promise<{ id: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/runs`, {
    data: {
      name: `E2E Run ${Date.now()}`,
      environment: 'E2E',
      buildVersion: '1.0.0',
      description: 'Created by Playwright E2E tests',
      caseIds,
    },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'createTestRun');
  return { id: body.id };
}

export async function deleteTestRun(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  runId: string,
): Promise<void> {
  await api.delete(`${BASE}/projects/${projectId}/runs/${runId}`, { headers });
}

// ── Defects ───────────────────────────────────────────────────────────────────

export async function createDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  body: {
    title: string;
    description?: string;
    severity?: string;
    status?: string;
    source?: string;
    testRunId?: string;
    testRunCaseId?: string;
  },
): Promise<{ id: string; displayId?: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/defects`, {
    data: {
      title: body.title,
      description: body.description ?? '',
      severity: body.severity ?? 'Major',
      status: body.status ?? 'Open',
      source: body.source ?? '',
      link: '',
      ...(body.testRunId ? { testRunId: body.testRunId } : {}),
      ...(body.testRunCaseId ? { testRunCaseId: body.testRunCaseId } : {}),
    },
    headers: { ...headers, 'Content-Type': 'application/json' },
  });
  const json = await res.json();
  assertOk(res.status(), json, 'createDefect');
  return { id: json.id, displayId: json.displayId };
}

export async function deleteDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  defectId: string,
): Promise<void> {
  await api.delete(`${BASE}/projects/${projectId}/defects/${defectId}`, { headers });
}
```

- [ ] **Step 3: Smoke-test global setup**

First, copy `.env.test.example` to `.env.test` and fill in credentials:

```bash
cp apps/frontend/.env.test.example apps/frontend/.env.test
# then edit .env.test to fill in TEST_USERNAME and TEST_PASSWORD
```

Then run:

```bash
cd apps/frontend && npx playwright test --list
```

Expected: lists test files (even if empty). No import errors from global-setup or config.

- [ ] **Step 4: Commit**

```bash
cd apps/frontend && git add e2e/global-setup.ts e2e/helpers/api.ts
git commit -m "feat(e2e): global setup + API helper for test data lifecycle"
```

---

### Task 3: Add data-testid attributes to UI

**Files:**
- Modify: `apps/frontend/src/pages/Defects.tsx`
- Modify: `apps/frontend/src/pages/RunExecution.tsx`

> **Context:** Playwright needs reliable selectors for icon-only buttons (Eye, Pencil, Trash, Bug). Semantic selectors (`getByRole`, `getByText`) don't work for icon-only buttons. We add `data-testid` on each button so tests can do:
> `row.getByTestId('defect-view-btn')` — scoped to the row, unambiguous.

- [ ] **Step 1: Add testids to defect action buttons in `apps/frontend/src/pages/Defects.tsx`**

Find these three buttons in the `filtered.map((d) => ...)` row (around line 461–478):

```tsx
<button
  onClick={() => { setViewTarget(d); setViewOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  onClick={() => openEdit(d)}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  onClick={() => { setDeleteTarget({ id: d.id, displayId: defectDisplayIdMap[d.id] ?? d.id }); setDeleteOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors"
>
  <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
```

Add `data-testid` to each:

```tsx
<button
  data-testid="defect-view-btn"
  onClick={() => { setViewTarget(d); setViewOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  data-testid="defect-edit-btn"
  onClick={() => openEdit(d)}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  data-testid="defect-delete-btn"
  onClick={() => { setDeleteTarget({ id: d.id, displayId: defectDisplayIdMap[d.id] ?? d.id }); setDeleteOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors"
>
  <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
```

- [ ] **Step 2: Add testids to defect action buttons in RunExecution — defect table rows**

In `apps/frontend/src/pages/RunExecution.tsx`, find the defect table row action buttons (around line 1366–1386):

```tsx
<button
  onClick={() => { setViewDefect(d); setViewDefectOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  onClick={() => { setEditDefect(d); setEditDefectOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  onClick={() => {
    setCreatedDefects(createdDefects.filter(x => x.id !== d.id));
    toast.success('Defect deleted');
  }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors"
>
  <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
```

Add `data-testid`:

```tsx
<button
  data-testid="defect-view-btn"
  onClick={() => { setViewDefect(d); setViewDefectOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  data-testid="defect-edit-btn"
  onClick={() => { setEditDefect(d); setEditDefectOpen(true); }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
>
  <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
<button
  data-testid="defect-delete-btn"
  onClick={() => {
    setCreatedDefects(createdDefects.filter(x => x.id !== d.id));
    toast.success('Defect deleted');
  }}
  className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors"
>
  <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
</button>
```

- [ ] **Step 3: Add testid to step Bug button in RunExecution**

In the step table row (around line 1284–1292), find the Bug icon Button:

```tsx
<Button
  variant="ghost"
  size="icon"
  className={`h-7 w-7 text-muted-foreground hover:text-destructive ${isReadOnly ? 'opacity-50 cursor-not-allowed' : ''}`}
  onClick={() => handleBugClick(activeCase.id, step.stepNumber - 1)}
  disabled={isReadOnly}
>
  <Bug className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
```

Add `data-testid`:

```tsx
<Button
  data-testid="step-bug-btn"
  variant="ghost"
  size="icon"
  className={`h-7 w-7 text-muted-foreground hover:text-destructive ${isReadOnly ? 'opacity-50 cursor-not-allowed' : ''}`}
  onClick={() => handleBugClick(activeCase.id, step.stepNumber - 1)}
  disabled={isReadOnly}
>
  <Bug className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
```

- [ ] **Step 4: Verify build**

```bash
cd apps/frontend && npm run build 2>&1 | tail -5
```

Expected: `✓ built in ...` with no TypeScript errors.

- [ ] **Step 5: Commit**

```bash
cd apps/frontend && git add src/pages/Defects.tsx src/pages/RunExecution.tsx
git commit -m "feat(e2e): add data-testid to icon-only defect/step buttons"
```

---

### Task 4: Defects page spec

**Files:**
- Create: `apps/frontend/e2e/defects.spec.ts`

> **Context:** This spec covers the global Defects page (`/projects/{projectId}/defects`). `beforeAll` creates the full entity tree via API and one shared defect (for read-only tests) plus one source-context defect (for the Source column test). `afterAll` deletes everything in reverse order.
>
> Test independence: tests 1, 5, 7 are read-only on the shared defect. Tests 2 and 6 create their own defect via API/UI and clean up. Tests 3 and 4 mutate the shared defect (acceptable since they run sequentially and the mutations are to different fields).

- [ ] **Step 1: Create `apps/frontend/e2e/defects.spec.ts`**

```typescript
import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  deleteSuite,
  createCase,
  createStep,
  deleteCase,
  createTestRun,
  deleteTestRun,
  createDefect,
  deleteDefect,
} from './helpers/api';

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

  const case1 = await createCase(apiCtx, authHeaders, projectId, suiteId, 'E2E Case One');
  caseId1 = case1.id;
  await createStep(apiCtx, authHeaders, projectId, suiteId, caseId1, 1, 'Open the app', 'App opens');
  await createStep(apiCtx, authHeaders, projectId, suiteId, caseId1, 2, 'Click submit', 'Form submits');

  const case2 = await createCase(apiCtx, authHeaders, projectId, suiteId, 'E2E Case Two');
  caseId2 = case2.id;
  await createStep(apiCtx, authHeaders, projectId, suiteId, caseId2, 1, 'Navigate to settings', 'Settings visible');

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

test('1 — page loads and defects table is visible', async ({ page }) => {
  await page.goto(`/projects/${projectId}/defects`);
  await expect(page.getByRole('heading', { name: 'Defects' })).toBeVisible();
  await expect(page.getByRole('table')).toBeVisible();
});

test('2 — create defect via form, appears in table with DEF-N id', async ({ page }) => {
  await page.goto(`/projects/${projectId}/defects`);
  await page.getByRole('button', { name: 'Create Defect' }).click();

  await page.getByRole('dialog').waitFor();
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

test('3 — change severity inline, reflected without page refresh', async ({ page }) => {
  await page.goto(`/projects/${projectId}/defects`);
  await expect(page.getByRole('cell', { name: SHARED_TITLE })).toBeVisible({ timeout: 10_000 });

  const row = page.locator('tr', { hasText: SHARED_TITLE });
  // Click on the Severity SelectTrigger in this row (stop-propagation cell)
  await row.locator('td').nth(2).click();
  await page.getByRole('option', { name: 'Minor' }).click();

  // Row should now show Minor (not Major) without refresh
  await expect(row.locator('td').nth(2)).toContainText('MINOR', { timeout: 10_000 });
});

test('4 — change status inline, reflected without page refresh', async ({ page }) => {
  await page.goto(`/projects/${projectId}/defects`);
  await expect(page.getByRole('cell', { name: SHARED_TITLE })).toBeVisible({ timeout: 10_000 });

  const row = page.locator('tr', { hasText: SHARED_TITLE });
  await row.locator('td').nth(3).click();
  await page.getByRole('option', { name: 'Fixed' }).click();

  await expect(row.locator('td').nth(3)).toContainText('FIXED', { timeout: 10_000 });
});

test('5 — view defect modal shows correct fields', async ({ page }) => {
  await page.goto(`/projects/${projectId}/defects`);
  await expect(page.getByRole('cell', { name: SHARED_TITLE })).toBeVisible({ timeout: 10_000 });

  const row = page.locator('tr', { hasText: SHARED_TITLE });
  await row.getByTestId('defect-view-btn').click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText(SHARED_TITLE);
  // Check that Severity and Status labels exist
  await expect(dialog).toContainText('Severity');
  await expect(dialog).toContainText('Status');
});

test('6 — delete defect, disappears from table without page refresh', async ({ page }) => {
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

test('7 — Source column shows TR-N · TC-N · Step N for run-linked defect', async ({ page }) => {
  await page.goto(`/projects/${projectId}/defects`);
  await expect(page.getByRole('cell', { name: SOURCE_TITLE })).toBeVisible({ timeout: 10_000 });

  const row = page.locator('tr', { hasText: SOURCE_TITLE });
  const sourceCell = row.locator('td').nth(4); // Source is 5th column (0-indexed: ID, Title, Severity, Status, Source)
  // Should show "TR-1 · TC-1 · Step 1" or similar — TR-N part comes from runDisplayIdMap
  await expect(sourceCell).toContainText('TR-');
  await expect(sourceCell).toContainText('TC-1 · Step 1');
});
```

- [ ] **Step 2: Run defects spec against live stack**

Make sure the dev stack is running (`bash start-dev.sh` from project root), then:

```bash
cd apps/frontend && npx playwright test e2e/defects.spec.ts --headed
```

Expected: 7 tests pass. If any selector mismatches, inspect the HTML in the Playwright trace viewer and adjust selectors.

- [ ] **Step 3: Run headless to confirm**

```bash
cd apps/frontend && npx playwright test e2e/defects.spec.ts
```

Expected: `7 passed`.

- [ ] **Step 4: Commit**

```bash
cd apps/frontend && git add e2e/defects.spec.ts
git commit -m "test(e2e): Defects page — all 7 lifecycle scenarios"
```

---

### Task 5: RunExecution defects spec

**Files:**
- Create: `apps/frontend/e2e/run-execution-defects.spec.ts`

> **Context:** This spec covers the RunExecution page (`/projects/{projectId}/runs/{runId}`). The page auto-selects the first case (`selectedIdx = 0`) on load, so no sidebar click is needed. Steps are shown in the center panel.
>
> Step status buttons have text: `untested`, `passed`, `failed`, `skipped`. Clicking "failed" on a step auto-opens the CreateDefectModal.
>
> The Bug icon button on each step row has `data-testid="step-bug-btn"` (added in Task 3).
>
> "Report Case Issue" button has that exact text.
>
> Defect action buttons in the run's defect table have `data-testid="defect-view-btn"`, `"defect-edit-btn"`, `"defect-delete-btn"` (added in Task 3).
>
> RunExecution defect delete does NOT show a confirmation dialog — it removes immediately from local state.
>
> Tests 1, 2, 3 each create a defect via UI and verify. Tests 4–7 operate on a defect created via UI in `beforeEach` steps within each test (since the defects table is only shown when `activeCaseDefects.length > 0`).

- [ ] **Step 1: Create `apps/frontend/e2e/run-execution-defects.spec.ts`**

```typescript
import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  deleteSuite,
  createCase,
  createStep,
  deleteCase,
  createTestRun,
  deleteTestRun,
  deleteDefect,
} from './helpers/api';

// ── Shared state ──────────────────────────────────────────────────────────────

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

  const case1 = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Run E2E Case One');
  caseId1 = case1.id;
  await createStep(apiCtx, authHeaders, projectId, suiteId, caseId1, 1, 'Open homepage', 'Homepage loads');
  await createStep(apiCtx, authHeaders, projectId, suiteId, caseId1, 2, 'Click Login', 'Login form appears');

  const case2 = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Run E2E Case Two');
  caseId2 = case2.id;
  await createStep(apiCtx, authHeaders, projectId, suiteId, caseId2, 1, 'Check sidebar', 'Sidebar visible');

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

// Helper: navigate to run and wait for first case steps to load
async function goToRun(page: import('@playwright/test').Page) {
  await page.goto(`/projects/${projectId}/runs/${runId}`);
  // Wait for step table to be visible (first case auto-selected)
  await expect(page.locator('table').last()).toBeVisible({ timeout: 15_000 });
}

// ── Tests ─────────────────────────────────────────────────────────────────────

test('1 — create defect via step fail: modal opens, defect visible under case', async ({ page }) => {
  await goToRun(page);

  // Click "failed" on the first step
  const failedBtn = page.getByRole('button', { name: 'failed' }).first();
  await failedBtn.click();

  // Defect modal should open automatically
  const dialog = page.getByRole('dialog', { name: 'Create Defect' });
  await expect(dialog).toBeVisible({ timeout: 5_000 });

  await dialog.getByPlaceholder('Brief summary...').fill('Step Fail Defect');
  await dialog.getByRole('button', { name: 'Create Defect' }).click();

  // Defect row should appear under the case
  await expect(page.getByRole('cell', { name: 'Step Fail Defect' })).toBeVisible({ timeout: 10_000 });
});

test('2 — create defect via bug button on step: modal opens, defect visible', async ({ page }) => {
  await goToRun(page);

  // Click Bug icon on the first step row
  await page.getByTestId('step-bug-btn').first().click();

  const dialog = page.getByRole('dialog', { name: 'Create Defect' });
  await expect(dialog).toBeVisible({ timeout: 5_000 });

  await dialog.getByPlaceholder('Brief summary...').fill('Bug Button Defect');
  await dialog.getByRole('button', { name: 'Create Defect' }).click();

  await expect(page.getByRole('cell', { name: 'Bug Button Defect' })).toBeVisible({ timeout: 10_000 });
});

test('3 — create defect via "Report Case Issue": modal opens, defect visible', async ({ page }) => {
  await goToRun(page);

  await page.getByRole('button', { name: 'Report Case Issue' }).click();

  const dialog = page.getByRole('dialog', { name: 'Create Defect' });
  await expect(dialog).toBeVisible({ timeout: 5_000 });

  await dialog.getByPlaceholder('Brief summary...').fill('Case Issue Defect');
  await dialog.getByRole('button', { name: 'Create Defect' }).click();

  await expect(page.getByRole('cell', { name: 'Case Issue Defect' })).toBeVisible({ timeout: 10_000 });
});

test('4 — view defect modal in run execution: shows correct fields, Source has no TR-N', async ({ page }) => {
  await goToRun(page);

  // Create a defect via Bug button so there is something to view
  await page.getByTestId('step-bug-btn').first().click();
  const createDialog = page.getByRole('dialog', { name: 'Create Defect' });
  await createDialog.getByPlaceholder('Brief summary...').fill('View Modal Defect');
  await createDialog.getByRole('button', { name: 'Create Defect' }).click();
  await expect(page.getByRole('cell', { name: 'View Modal Defect' })).toBeVisible({ timeout: 10_000 });

  // Open view modal
  const row = page.locator('tr', { hasText: 'View Modal Defect' });
  await row.getByTestId('defect-view-btn').click();

  const viewDialog = page.getByRole('dialog');
  await expect(viewDialog).toContainText('View Modal Defect');
  // Source in run execution shows TC-N · Step N (no TR-N prefix)
  await expect(viewDialog).toContainText('Source');
  // The source section is a bordered div after the "SOURCE" label
  const sourceSection = viewDialog.locator('div', { hasText: /TC-\d+/ }).first();
  const sourceText = await sourceSection.textContent() ?? '';
  expect(sourceText).toMatch(/TC-\d+/);
  expect(sourceText).not.toMatch(/TR-\d+/);
});

test('5 — edit defect: change severity, saved and reflected in table', async ({ page }) => {
  await goToRun(page);

  // Create a defect to edit
  await page.getByRole('button', { name: 'Report Case Issue' }).click();
  const createDialog = page.getByRole('dialog', { name: 'Create Defect' });
  await createDialog.getByPlaceholder('Brief summary...').fill('Edit Severity Defect');
  await createDialog.getByRole('button', { name: 'Create Defect' }).click();
  await expect(page.getByRole('cell', { name: 'Edit Severity Defect' })).toBeVisible({ timeout: 10_000 });

  // Open edit modal
  const row = page.locator('tr', { hasText: 'Edit Severity Defect' });
  await row.getByTestId('defect-edit-btn').click();

  const editDialog = page.getByRole('dialog', { name: 'Edit Defect' });
  await expect(editDialog).toBeVisible();

  // Change severity to Critical
  await editDialog.locator('button[role="combobox"]').first().click();
  await page.getByRole('option', { name: 'Critical' }).click();
  await editDialog.getByRole('button', { name: 'Save Changes' }).click();

  // Verify severity updated in row
  await expect(row.locator('td').nth(2)).toContainText('CRITICAL', { timeout: 10_000 });
});

test('6 — delete defect: disappears from table without page refresh', async ({ page }) => {
  await goToRun(page);

  // Create a defect to delete
  await page.getByRole('button', { name: 'Report Case Issue' }).click();
  const createDialog = page.getByRole('dialog', { name: 'Create Defect' });
  await createDialog.getByPlaceholder('Brief summary...').fill('Delete Me Defect');
  await createDialog.getByRole('button', { name: 'Create Defect' }).click();
  await expect(page.getByRole('cell', { name: 'Delete Me Defect' })).toBeVisible({ timeout: 10_000 });

  // Delete via trash button (no confirmation dialog in RunExecution)
  const row = page.locator('tr', { hasText: 'Delete Me Defect' });
  await row.getByTestId('defect-delete-btn').click();

  await expect(page.getByRole('cell', { name: 'Delete Me Defect' })).not.toBeVisible({ timeout: 5_000 });
});

test('7 — Source column in run execution shows TC-N · Step N (no TR-N)', async ({ page }) => {
  await goToRun(page);

  // Create defect via step fail — this sets source with step info
  const failedBtn = page.getByRole('button', { name: 'failed' }).first();
  await failedBtn.click();
  const dialog = page.getByRole('dialog', { name: 'Create Defect' });
  await dialog.getByPlaceholder('Brief summary...').fill('Source Col Defect');
  await dialog.getByRole('button', { name: 'Create Defect' }).click();
  await expect(page.getByRole('cell', { name: 'Source Col Defect' })).toBeVisible({ timeout: 10_000 });

  // Source cell is the 5th column in defect table
  const row = page.locator('tr', { hasText: 'Source Col Defect' });
  const sourceCell = row.locator('td').nth(4);
  // Should contain TC-N and Step N, but NOT TR-N
  await expect(sourceCell).toContainText(/TC-\d+/);
  await expect(sourceCell).toContainText(/Step \d+/);
  const text = await sourceCell.textContent() ?? '';
  expect(text).not.toMatch(/TR-\d+/);
});
```

- [ ] **Step 2: Run RunExecution spec against live stack**

```bash
cd apps/frontend && npx playwright test e2e/run-execution-defects.spec.ts --headed
```

Expected: 7 tests pass. Use the trace viewer if any test fails:

```bash
cd apps/frontend && npx playwright show-report
```

- [ ] **Step 3: Run full suite headless**

```bash
cd apps/frontend && npx playwright test
```

Expected: `14 passed` (7 + 7).

- [ ] **Step 4: Commit**

```bash
cd apps/frontend && git add e2e/run-execution-defects.spec.ts
git commit -m "test(e2e): RunExecution defects — 7 lifecycle scenarios"
```

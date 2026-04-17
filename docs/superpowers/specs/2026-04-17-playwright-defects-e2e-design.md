# Playwright E2E Tests — Defects Design

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Cover defect lifecycle with Playwright E2E tests running against a live Cyoda backend — including the Defects page and RunExecution page.

**Architecture:** Tests run against a real stack (frontend on `localhost:5173`, backend on `localhost:8080`). Auth is handled once in `global-setup.ts` via UI login, with the browser state saved to `e2e/.auth/user.json` and reused by all tests. Each test file creates all required entities (project, suite, cases, test run) in `beforeAll` via the backend API and deletes them in `afterAll`.

**Tech Stack:** Playwright 1.57, TypeScript, `.env.test` for credentials.

---

## File Structure

```
apps/frontend/
├── playwright.config.ts                        ← rewrite (lovable-agent-playwright-config not installed)
├── .env.test                                   ← new, gitignored
└── e2e/
    ├── global-setup.ts                         ← login once, save storageState
    ├── helpers/
    │   └── api.ts                              ← APIRequestContext wrapper: create/delete project, suite, cases, run, defect
    ├── defects.spec.ts                         ← Defects page scenarios
    └── run-execution-defects.spec.ts           ← RunExecution page scenarios
```

---

## Environment Variables (`.env.test`)

```
TEST_USERNAME=<login>
TEST_PASSWORD=<password>
TEST_API_URL=http://localhost:8080/api
PLAYWRIGHT_BASE_URL=http://localhost:5173
```

`.env.test` must be added to `.gitignore`. An `.env.test.example` with placeholder values is committed instead.

---

## Playwright Config (`playwright.config.ts`)

Replace the existing stub entirely:

```ts
import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
dotenv.config({ path: '.env.test' });

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
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

---

## Global Setup (`e2e/global-setup.ts`)

Logs in via the UI login form once and saves browser storage state:

```ts
import { chromium } from '@playwright/test';

export default async function globalSetup() {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto(process.env.PLAYWRIGHT_BASE_URL!);
  await page.getByLabel('Username').fill(process.env.TEST_USERNAME!);
  await page.getByLabel('Password').fill(process.env.TEST_PASSWORD!);
  await page.getByRole('button', { name: /log in/i }).click();
  await page.waitForURL('**/projects**');
  await page.context().storageState({ path: 'e2e/.auth/user.json' });
  await browser.close();
}
```

The `e2e/.auth/` directory is gitignored.

---

## API Helper (`e2e/helpers/api.ts`)

Thin wrapper around Playwright's `APIRequestContext`. Provides typed helpers used in `beforeAll`/`afterAll`:

```ts
import { APIRequestContext } from '@playwright/test';

const BASE = process.env.TEST_API_URL ?? 'http://localhost:8080/api';

export async function getAuthHeaders(api: APIRequestContext): Promise<Record<string, string>> {
  // POST /auth/login, return Authorization header value
}

export async function createProject(api: APIRequestContext, headers: Record<string, string>): Promise<{ id: string; name: string }>;
export async function deleteProject(api: APIRequestContext, headers: Record<string, string>, projectId: string): Promise<void>;

export async function createSuite(api: APIRequestContext, headers: Record<string, string>, projectId: string): Promise<{ id: string }>;
export async function deleteSuite(api: APIRequestContext, headers: Record<string, string>, projectId: string, suiteId: string): Promise<void>;

export async function createCase(api: APIRequestContext, headers: Record<string, string>, projectId: string, suiteId: string): Promise<{ id: string; title: string }>;
export async function deleteCase(api: APIRequestContext, headers: Record<string, string>, projectId: string, caseId: string): Promise<void>;

export async function createTestRun(api: APIRequestContext, headers: Record<string, string>, projectId: string, caseIds: string[]): Promise<{ id: string }>;
export async function deleteTestRun(api: APIRequestContext, headers: Record<string, string>, projectId: string, runId: string): Promise<void>;

export async function deleteDefect(api: APIRequestContext, headers: Record<string, string>, projectId: string, defectId: string): Promise<void>;
```

All helpers throw on non-2xx responses.

---

## Test Data Lifecycle

**`beforeAll`** (both spec files):
1. `getAuthHeaders` — POST `/auth/login` with TEST_USERNAME / TEST_PASSWORD
2. `createProject` — name `E2E Defects ${Date.now()}`
3. `createSuite` — inside the project
4. `createCase` × 2 — with at least 2 steps each
5. `createTestRun` — with both case IDs

**`afterAll`** (both spec files):
1. Delete all defects created during tests (tracked in a `createdDefectIds` array)
2. `deleteTestRun`
3. `deleteCase` × 2
4. `deleteSuite`
5. `deleteProject`

Deletion runs in `try/finally` so partial failures don't leave orphaned data.

---

## Defects Page Scenarios (`e2e/defects.spec.ts`)

Navigation: `page.goto(\`/projects/${projectId}/defects\`)`

| # | Test name | Steps | Assertions |
|---|-----------|-------|------------|
| 1 | Page loads and table is visible | Navigate to defects page | Table element visible; empty state or header row rendered |
| 2 | Create defect via form | Click "+ New Defect" → fill Title, Description, Severity=Critical, Status=Open → Submit | Row with title appears in table; display ID is `DEF-N` format |
| 3 | Change severity inline | Find the row → click Severity select → choose "Minor" | Select shows "Minor" after save; no page refresh |
| 4 | Change status inline | Find the row → click Status select → choose "Fixed" | Select shows "Fixed" after save |
| 5 | View defect modal | Click eye icon on row | Modal opens; Title, Severity, Status, Source fields match created values |
| 6 | Delete defect | Click delete icon → confirm in dialog | Row disappears from table without navigation |
| 7 | Source column shows run context | In `beforeAll`: POST defect directly to API with `testRunId` set and `source = "TC-1 · Step 1"`; navigate to Defects page | Source cell shows `TR-1 · TC-1 · Step 1` format |

---

## RunExecution Page Scenarios (`e2e/run-execution-defects.spec.ts`)

Navigation: `page.goto(\`/projects/${projectId}/runs/${runId}\`)`

The page shows suites → cases → steps. Tests interact with the first case and its steps.

| # | Test name | Steps | Assertions |
|---|-----------|-------|------------|
| 1 | Create defect via step fail | Expand case → click step status → set to Fail | Defect creation modal opens automatically; fill Title → Save; defect row visible under case |
| 2 | Create defect via bug button on step | Expand case → click 🐛 icon on a step | Modal opens; fill Title → Save; defect appears under case |
| 3 | Create defect via "Report case issue" | Expand case → click "Report case issue" button | Modal opens; fill Title → Save; defect appears under case in the list |
| 4 | View defect modal in run execution | Click eye icon on a defect row | Modal opens with correct Title, Severity, Status; Source shows `TC-1 · Step 1` (no TR prefix) |
| 5 | Edit defect | Click edit icon on defect row → change Severity to "Critical" → Save Changes | Defect row shows updated severity |
| 6 | Delete defect | Click delete icon → confirm | Defect row disappears without page refresh |
| 7 | Source column in run execution | Defect created via step fail | Source cell in run's defect table shows `TC-1 · Step 1` format (no TR-N) |

---

## Selector Strategy

Use semantic selectors in priority order:
1. `getByRole` / `getByLabel` / `getByText` — preferred
2. `getByTestId` — only if semantic selectors are ambiguous
3. CSS class selectors — avoid

If ambiguous selectors are needed, add `data-testid` attributes to the relevant elements in the app rather than using fragile class-based selectors.

---

## CI / Running Locally

```bash
# Install browsers (once)
cd apps/frontend && npx playwright install chromium

# Run all E2E tests (requires running stack)
cd apps/frontend && npx playwright test

# Run specific file
cd apps/frontend && npx playwright test e2e/defects.spec.ts

# Show report
cd apps/frontend && npx playwright show-report
```

Add `npm run test:e2e` script to `package.json`:
```json
"test:e2e": "playwright test"
```

Traces are saved on first retry. `e2e/.auth/` and `.env.test` are gitignored.

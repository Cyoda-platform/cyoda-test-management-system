# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: repository.spec.ts >> Repository — suites and test cases >> create test case via full form — appears in case list
- Location: e2e/repository.spec.ts:111:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByText('E2E Full Form Case')
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 20000ms
  - waiting for getByText('E2E Full Form Case')

```

# Test source

```ts
  23  |     authHeaders = await getAuthHeaders(apiCtx);
  24  |     const proj = await createProject(apiCtx, authHeaders, 'E2E Repository Project', 'For suite/case E2E tests');
  25  |     projectId = proj.id;
  26  |   });
  27  | 
  28  |   test.afterAll(async () => {
  29  |     await deleteProject(apiCtx, authHeaders, projectId).catch(() => {});
  30  |     await apiCtx.dispose();
  31  |   });
  32  | 
  33  |   // ── Suites ────────────────────────────────────────────────────────────────
  34  | 
  35  |   test('create suite via empty-state button — appears in sidebar', async ({ page }) => {
  36  |     await page.goto(`/projects/${projectId}/repository`);
  37  | 
  38  |     // Fresh project → empty-state button visible in the middle panel
  39  |     await expect(page.getByRole('button', { name: 'Create your first suite' })).toBeVisible({ timeout: 15_000 });
  40  |     await page.getByRole('button', { name: 'Create your first suite' }).click();
  41  | 
  42  |     const dialog = page.getByRole('dialog');
  43  |     await expect(dialog).toBeVisible();
  44  |     await dialog.getByPlaceholder('e.g. Authentication, Checkout Flow').fill(SUITE_NAME);
  45  |     await dialog.getByRole('button', { name: 'Create Suite' }).click();
  46  | 
  47  |     // Suite name appears as a button in the left sidebar tree
  48  |     await expect(page.getByRole('button', { name: SUITE_NAME })).toBeVisible({ timeout: 20_000 });
  49  |   });
  50  | 
  51  |   test('rename suite — new name reflected in suite header', async ({ page }) => {
  52  |     await page.goto(`/projects/${projectId}/repository`);
  53  | 
  54  |     // Suite header row in the middle panel (has .surface-low class + suite name)
  55  |     const suiteHeaderRow = page.locator('.surface-low.group', { hasText: SUITE_NAME });
  56  |     await expect(suiteHeaderRow).toBeVisible({ timeout: 20_000 });
  57  | 
  58  |     // Pencil = 3rd from last button (Trash=last, Copy=-2, Pencil=-3)
  59  |     await suiteHeaderRow.locator('button').nth(-3).click();
  60  | 
  61  |     const dialog = page.getByRole('dialog');
  62  |     await expect(dialog).toContainText('Edit Suite');
  63  |     const nameInput = dialog.getByPlaceholder('e.g. Authentication, Checkout Flow');
  64  |     await nameInput.clear();
  65  |     await nameInput.fill(RENAMED_SUITE);
  66  |     await dialog.getByRole('button', { name: 'Save Changes' }).click();
  67  | 
  68  |     await expect(page.locator('.surface-low.group', { hasText: RENAMED_SUITE })).toBeVisible({ timeout: 15_000 });
  69  |     await expect(page.locator('.surface-low.group', { hasText: SUITE_NAME })).not.toBeVisible();
  70  |   });
  71  | 
  72  |   test('delete suite — confirmation modal, DELETE returns 204', async ({ page }) => {
  73  |     // Create a dedicated suite to delete (keeps shared suite intact)
  74  |     const { id: deleteSuiteId } = await createSuite(apiCtx, authHeaders, projectId, 'E2E Delete Suite');
  75  | 
  76  |     await page.goto(`/projects/${projectId}/repository`);
  77  |     const suiteHeaderRow = page.locator('.surface-low.group', { hasText: 'E2E Delete Suite' });
  78  |     await expect(suiteHeaderRow).toBeVisible({ timeout: 20_000 });
  79  | 
  80  |     const deleteResPromise = page.waitForResponse(
  81  |       r => r.url().includes(`/suites/${deleteSuiteId}`) && r.request().method() === 'DELETE',
  82  |     );
  83  |     await suiteHeaderRow.locator('button').last().click(); // Trash button
  84  | 
  85  |     const dialog = page.getByRole('dialog');
  86  |     await expect(dialog).toContainText('Delete Suite');
  87  |     await dialog.getByRole('button', { name: 'Delete' }).click();
  88  | 
  89  |     expect((await deleteResPromise).status()).toBe(204);
  90  |   });
  91  | 
  92  |   // ── Test Cases ────────────────────────────────────────────────────────────
  93  | 
  94  |   test('quick create test case — appears in case list', async ({ page }) => {
  95  |     await page.goto(`/projects/${projectId}/repository`);
  96  | 
  97  |     const suiteHeaderRow = page.locator('.surface-low.group', { hasText: RENAMED_SUITE });
  98  |     await expect(suiteHeaderRow).toBeVisible({ timeout: 20_000 });
  99  | 
  100 |     // "+ Create quick test" button is in the suite footer row
  101 |     await page.getByRole('button', { name: 'Create quick test' }).click();
  102 | 
  103 |     const dialog = page.getByRole('dialog');
  104 |     await expect(dialog).toContainText('Quick Create Test Case');
  105 |     await dialog.getByPlaceholder('e.g. Verify login with valid credentials').fill('E2E Quick Case');
  106 |     await dialog.getByRole('button', { name: 'Create Case' }).click();
  107 | 
  108 |     await expect(page.getByText('E2E Quick Case')).toBeVisible({ timeout: 20_000 });
  109 |   });
  110 | 
  111 |   test('create test case via full form — appears in case list', async ({ page }) => {
  112 |     await page.goto(`/projects/${projectId}/repository`);
  113 |     await expect(page.locator('.surface-low.group', { hasText: RENAMED_SUITE })).toBeVisible({ timeout: 20_000 });
  114 | 
  115 |     // "+ Case" button opens CaseFormPage (full-page form)
  116 |     await page.getByRole('button', { name: 'Case' }).click();
  117 | 
  118 |     await expect(page.getByPlaceholder('e.g. Login with valid credentials')).toBeVisible({ timeout: 5_000 });
  119 |     await page.getByPlaceholder('e.g. Login with valid credentials').fill('E2E Full Form Case');
  120 |     await page.getByRole('button', { name: 'Create Case' }).click();
  121 | 
  122 |     // CaseFormPage closes → back to repository; case should appear
> 123 |     await expect(page.getByText('E2E Full Form Case')).toBeVisible({ timeout: 20_000 });
      |                                                        ^ Error: expect(locator).toBeVisible() failed
  124 |   });
  125 | });
  126 | 
```
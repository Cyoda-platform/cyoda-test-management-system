# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: attachments-case.spec.ts >> Case attachments >> API: upload case attachment returns attachmentType=CASE
- Location: e2e/attachments-case.spec.ts:47:3

# Error details

```
Error: createStep failed with status 404: {"type":"about:blank","title":"Not Found","status":404,"detail":"No static resource projects/db423a20-492f-11b3-8032-ce53f08f3645/suites/df2c1a70-492f-11b3-8032-ce53f08f3645/cases/e2906e3c-492f-11b3-8032-ce53f08f3645/steps.","instance":"/api/projects/db423a20-492f-11b3-8032-ce53f08f3645/suites/df2c1a70-492f-11b3-8032-ce53f08f3645/cases/e2906e3c-492f-11b3-8032-ce53f08f3645/steps"}
```

# Test source

```ts
  1   | import { APIRequestContext } from '@playwright/test';
  2   | import dotenv from 'dotenv';
  3   | import path from 'path';
  4   | import { fileURLToPath } from 'url';
  5   | 
  6   | const __filename = fileURLToPath(import.meta.url);
  7   | const __dirname = path.dirname(__filename);
  8   | 
  9   | dotenv.config({ path: path.resolve(__dirname, '../../.env.test') });
  10  | 
  11  | const BASE = process.env.TEST_API_URL ?? 'http://localhost:8080/api';
  12  | 
  13  | function assertOk(status: number, body: unknown, label: string) {
  14  |   if (status < 200 || status >= 300) {
> 15  |     throw new Error(`${label} failed with status ${status}: ${JSON.stringify(body)}`);
      |           ^ Error: createStep failed with status 404: {"type":"about:blank","title":"Not Found","status":404,"detail":"No static resource projects/db423a20-492f-11b3-8032-ce53f08f3645/suites/df2c1a70-492f-11b3-8032-ce53f08f3645/cases/e2906e3c-492f-11b3-8032-ce53f08f3645/steps.","instance":"/api/projects/db423a20-492f-11b3-8032-ce53f08f3645/suites/df2c1a70-492f-11b3-8032-ce53f08f3645/cases/e2906e3c-492f-11b3-8032-ce53f08f3645/steps"}
  16  |   }
  17  | }
  18  | 
  19  | // ── Auth ─────────────────────────────────────────────────────────────────────
  20  | 
  21  | export async function getAuthHeaders(
  22  |   api: APIRequestContext,
  23  | ): Promise<Record<string, string>> {
  24  |   const username = process.env.TEST_USERNAME;
  25  |   const password = process.env.TEST_PASSWORD;
  26  |   if (!username || !password) {
  27  |     throw new Error('TEST_USERNAME and TEST_PASSWORD must be set in .env.test');
  28  |   }
  29  | 
  30  |   const res = await api.post(`${BASE}/auth/login`, {
  31  |     data: { username, password },
  32  |     headers: { 'Content-Type': 'application/json' },
  33  |   });
  34  |   const body = await res.json();
  35  |   assertOk(res.status(), body, 'getAuthHeaders');
  36  |   return { Authorization: `Bearer ${body.token}` };
  37  | }
  38  | 
  39  | // ── Projects ─────────────────────────────────────────────────────────────────
  40  | 
  41  | /**
  42  |  * Creates a project.
  43  |  * NOTE: requires the TEST_USERNAME account to have ADMIN role.
  44  |  * A non-admin token will receive HTTP 403.
  45  |  */
  46  | export async function createProject(
  47  |   api: APIRequestContext,
  48  |   headers: Record<string, string>,
  49  |   name?: string,
  50  |   description = 'Created by Playwright E2E tests',
  51  | ): Promise<{ id: string; name: string }> {
  52  |   const projectName = name ?? `E2E Project ${Date.now()}`;
  53  |   const res = await api.post(`${BASE}/projects`, {
  54  |     data: { name: projectName, description },
  55  |     headers: { ...headers, 'Content-Type': 'application/json' },
  56  |   });
  57  |   const body = await res.json();
  58  |   assertOk(res.status(), body, 'createProject');
  59  |   return { id: body.id, name: body.name };
  60  | }
  61  | 
  62  | /** Deletes a project. NOTE: requires ADMIN role. */
  63  | export async function deleteProject(
  64  |   api: APIRequestContext,
  65  |   headers: Record<string, string>,
  66  |   projectId: string,
  67  | ): Promise<void> {
  68  |   const res = await api.delete(`${BASE}/projects/${projectId}`, { headers });
  69  |   assertOk(res.status(), null, 'deleteProject');
  70  | }
  71  | 
  72  | // ── Suites ────────────────────────────────────────────────────────────────────
  73  | 
  74  | export async function createSuite(
  75  |   api: APIRequestContext,
  76  |   headers: Record<string, string>,
  77  |   projectId: string,
  78  |   name = 'E2E Suite',
  79  | ): Promise<{ id: string; name: string }> {
  80  |   const res = await api.post(`${BASE}/projects/${projectId}/suites`, {
  81  |     data: { name, description: '', projectId },
  82  |     headers: { ...headers, 'Content-Type': 'application/json' },
  83  |   });
  84  |   const body = await res.json();
  85  |   assertOk(res.status(), body, 'createSuite');
  86  |   return { id: body.id, name: body.name };
  87  | }
  88  | 
  89  | export async function deleteSuite(
  90  |   api: APIRequestContext,
  91  |   headers: Record<string, string>,
  92  |   projectId: string,
  93  |   suiteId: string,
  94  | ): Promise<void> {
  95  |   const res = await api.delete(`${BASE}/projects/${projectId}/suites/${suiteId}`, { headers });
  96  |   assertOk(res.status(), null, 'deleteSuite');
  97  | }
  98  | 
  99  | // ── Cases ─────────────────────────────────────────────────────────────────────
  100 | 
  101 | export async function createCase(
  102 |   api: APIRequestContext,
  103 |   headers: Record<string, string>,
  104 |   projectId: string,
  105 |   suiteId: string,
  106 |   title: string,
  107 | ): Promise<{ id: string }> {
  108 |   const res = await api.post(
  109 |     `${BASE}/projects/${projectId}/suites/${suiteId}/cases`,
  110 |     {
  111 |       data: { title, description: 'E2E test case', preconditions: '', priority: 'MEDIUM', suiteId, projectId },
  112 |       headers: { ...headers, 'Content-Type': 'application/json' },
  113 |     },
  114 |   );
  115 |   const body = await res.json();
```
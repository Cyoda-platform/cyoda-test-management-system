# Cyoda TMS — UI ↔ Backend Integration Plan

**Status:** Draft
**Date:** 2026-03-31
**Author:** Engineering

---

## Overview

This document describes the full plan for replacing all mock data in the React frontend with live calls to the Spring Boot backend. The backend exposes a REST API at `/api` (port 8080 by default). The frontend runs on port 5173. Authentication uses httpOnly cookies (`auth-token`), set by the backend on login and automatically sent by the browser on every subsequent request.

The central API client (`frontend/src/lib/api.ts`) and the auth context (`frontend/src/contexts/AuthContext.tsx`) are already in place. The work below is purely wiring pages one by one.

---

## Architecture at a Glance

```
Browser (port 5173)
  └── React + TanStack Query
        └── api.ts  ──credentials: 'include'──►  Spring Boot (port 8080)
                                                     └── /api/...
                                                           └── EntityService → Cyoda Platform
```

**Key conventions:**
- All mutations go through `useMutation` (TanStack Query); on success call `queryClient.invalidateQueries` to refetch.
- Loading skeleton shown via `isLoading` flag; error shown via `isError` + `error.message`.
- Empty state (zero results) gets a friendly "No items yet" UI with a Create button.
- 401 responses from the API client automatically redirect to `/` (login page). No extra handling needed per-page.
- The `toast` utility (already imported in most pages via `sonner`) is used for success/failure feedback on mutations.

---

## Phase 0 — Already Done

| Item | Status |
|------|--------|
| `frontend/src/lib/api.ts` — central fetch client with credentials | ✅ Done |
| `frontend/src/contexts/AuthContext.tsx` — session restore via `GET /auth/me` | ✅ Done |
| `frontend/src/pages/Login.tsx` — calls `authApi.login()` | ✅ Done |
| `frontend/src/App.tsx` — `AuthProvider` + `ProtectedRoute` + `QueryClient` | ✅ Done |
| Root `package.json` — monorepo scripts (`npm run dev`, `npm run build`) | ✅ Done |
| Root `.env` / `.env.example` — single env file for both apps | ✅ Done |
| Backend DTO alignment — DefectDTO, TestRunDTO, TestStepDTO, ProjectDTO fixes | ✅ Done |
| Backend auth — cookie-based `POST /auth/login`, `POST /auth/logout`, `GET /auth/me` | ✅ Done |

---

## Phase 1 — Shared Infrastructure (do this first)

These tasks are prerequisites for all page integrations.

### 1.1 Install TanStack Query (if not already)

```bash
cd frontend && npm install @tanstack/react-query @tanstack/react-query-devtools
```

`QueryClient` is already configured in `App.tsx` with `retry: 1, staleTime: 30_000`.

### 1.2 Create custom query hooks file

Create `frontend/src/hooks/useApi.ts` (or per-domain hook files). Pattern:

```typescript
// Example pattern — replicate for each domain
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectsApi, type Project } from '@/lib/api';

export const projectKeys = {
  all:    () => ['projects']           as const,
  list:   (page: number) => [...projectKeys.all(), 'list', page] as const,
  detail: (id: string)   => [...projectKeys.all(), 'detail', id] as const,
};

export function useProjects(page = 0) {
  return useQuery({
    queryKey: projectKeys.list(page),
    queryFn:  () => projectsApi.list(page),
    select:   (data) => data.data,  // unwrap { data: [...] }
  });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Pick<Project, 'name' | 'description'>) => projectsApi.create(body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: projectKeys.all() }),
  });
}
```

### 1.3 Wire `AppHeader` logout and username

File: `frontend/src/components/AppHeader.tsx`

**Current state:** User icon and logout icon render but are non-functional.

**Changes needed:**
- Import `useAuth` from `AuthContext`
- Display `user.username` next to the user icon
- On logout icon click: call `logout()` from `useAuth()` (which calls `authApi.logout()` and clears state)

```typescript
// AppHeader.tsx additions
import { useAuth } from '@/contexts/AuthContext';
const { user, logout } = useAuth();
// In JSX: show user.username, wire logout button to logout()
```

---

## Phase 2 — Page-by-Page Integration

Work in this order: each page depends on the one before it (Projects → Repository → TestRuns → RunExecution → Defects → Attachments → Reports).

---

### Page 1: Projects (`/projects`)

**File:** `frontend/src/pages/Projects.tsx`

**Current state:** Local `useState<Project[]>(mockProjects)`. CRUD done in-memory. Pagination over local array.

**Target endpoints:**
| Action | Endpoint |
|--------|----------|
| List all | `GET /projects?page=0&size=10` |
| Create | `POST /projects` |
| Update | `PUT /projects/{id}` |
| Delete | `DELETE /projects/{id}` |

**Migration steps:**

1. Remove `import { mockProjects } from '@/data/mockData'`
2. Replace `useState<Project[]>(mockProjects)` with `useQuery`:
   ```typescript
   const { data: projects = [], isLoading, isError } = useProjects(page - 1);
   ```
3. Replace `handleCreate` local logic with `useCreateProject` mutation:
   ```typescript
   const createProject = useCreateProject();
   const handleCreate = (name: string, description: string) =>
     createProject.mutate({ name, description }, {
       onSuccess: () => { setCreateOpen(false); toast.success('Project created'); },
       onError:   (e) => toast.error(e.message),
     });
   ```
4. Replace `handleEdit` with `useUpdateProject` mutation.
5. Replace `handleDelete` with `useDeleteProject` mutation.
6. Update pagination: backend returns total count — use that for `totalPages` instead of `projects.length`.
7. Add loading skeleton (spinner or skeleton rows) while `isLoading`.
8. Add error banner when `isError`.

**Note on `initials`:** The backend `ProjectDTO` does not store `initials`. Compute it client-side: `name.substring(0, 2).toUpperCase()`. The `Project` interface in `api.ts` does not include `initials` — add it as a computed/derived value in the component or extend the interface with an optional `initials?: string` that gets filled in after fetch.

---

### Page 2: Repository (`/projects/:projectId/repository`)

**File:** `frontend/src/pages/Repository.tsx`

**Current state:** Most complex page. Uses `mockSuites`, `mockProjects`, `mockTestRuns`. All state managed locally. Suites contain nested `cases[]` which in turn contain nested `steps[]`.

**Target endpoints:**
| Action | Endpoint |
|--------|----------|
| Get project | `GET /projects/{projectId}` |
| List suites | `GET /projects/{projectId}/suites` |
| Create suite | `POST /projects/{projectId}/suites` |
| Update suite | `PUT /projects/{projectId}/suites/{id}` |
| Delete suite | `DELETE /projects/{projectId}/suites/{id}` |
| List cases for suite | `GET /projects/{projectId}/suites/{suiteId}/cases` |
| Create case | `POST /projects/{projectId}/suites/{suiteId}/cases` |
| Update case | `PUT /projects/{projectId}/suites/{suiteId}/cases/{id}` |
| Delete case | `DELETE /projects/{projectId}/suites/{suiteId}/cases/{id}` |
| List steps for case | `GET /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps` |
| Create step | `POST /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps` |
| Update step | `PUT /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps/{id}` |
| Delete step | `DELETE /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps/{id}` |
| List test runs (for "Run" button) | `GET /projects/{projectId}/runs` |

**Data shape change — important:**
Current mock data nests cases inside suites and steps inside cases: `Suite.cases[].steps[]`. The backend returns them separately (flat lists per endpoint). The frontend query layer must reconstruct the tree view.

**Migration approach:**

1. Fetch suites via `useQuery(['suites', projectId], () => suitesApi.list(projectId))`
2. Fetch cases per suite lazily (when suite is expanded) or fetch all cases for the project in one sweep if a `GET /projects/{id}/cases` (all-cases) endpoint exists. Since it doesn't currently, fetch per suite and merge.
3. Fetch steps per selected case only (when a case is clicked): `useQuery(['steps', projectId, suiteId, caseId], ...)`
4. Remove all inline `mockSuites` / `mockProjects` imports.
5. Keep `localStorage`-based panel sizing (this is not data, just UI state — keep as-is).
6. The `performExport` / `performImport` functions in `exportImport.ts` will need updating to use real API calls rather than mock data after the page is wired, but this can be deferred.

**Query key hierarchy:**
```
['suites', projectId]
['cases', projectId, suiteId]
['steps', projectId, suiteId, caseId]
```

**Cache invalidation rules:**
- Creating/deleting a suite → invalidate `['suites', projectId]`
- Creating/updating/deleting a case → invalidate `['cases', projectId, suiteId]`
- Creating/updating/deleting a step → invalidate `['steps', projectId, suiteId, caseId]`

---

### Page 3: Test Runs (`/projects/:projectId/test-runs`)

**File:** `frontend/src/pages/TestRuns.tsx`

**Current state:** Uses `mockTestRuns`, `mockProjects`. Local state for CRUD. Shows run status badges and progress bars.

**Target endpoints:**
| Action | Endpoint |
|--------|----------|
| List runs | `GET /projects/{projectId}/runs?page=0&size=20` |
| Create run | `POST /projects/{projectId}/runs` |
| Update run | `PUT /projects/{projectId}/runs/{id}` |
| Delete run | `DELETE /projects/{projectId}/runs/{id}` |

**Migration steps:**

1. Replace `mockTestRuns` with `useQuery(['runs', projectId], () => testRunsApi.list(projectId))`
2. Replace `mockProjects.find(...)` with the project loaded by `useQuery(['project', projectId], () => projectsApi.get(projectId))`
3. Wire create/edit forms to `useMutation` with `testRunsApi.create` / `testRunsApi.update`
4. Wire delete with `testRunsApi.delete`, invalidate `['runs', projectId]`
5. The `passed/failed/skipped/untested` counters now come from the backend DTO — render them directly.

---

### Page 4: Create Test Run (`/projects/:projectId/test-runs/new`)

**File:** `frontend/src/pages/CreateTestRun.tsx`

**Current state:** Uses `mockProjects`, `mockSuites`. Allows selecting test cases to include in a run. Posts locally.

**Target endpoints:**
| Action | Endpoint |
|--------|----------|
| Get project | `GET /projects/{projectId}` |
| List suites | `GET /projects/{projectId}/suites` |
| List cases | `GET /projects/{projectId}/suites/{suiteId}/cases` |
| Create run | `POST /projects/{projectId}/runs` |

**Migration steps:**

1. Replace mock project/suites/cases with live queries.
2. The `POST /projects/{projectId}/runs` body should include the selected test case IDs if the backend supports a `testCaseIds` field. Check `TestRunDTO` — if not present, add it as an optional `List<String>` field.
3. After successful creation, navigate to the new run's execution page: `navigate(\`/projects/${projectId}/test-runs/${newRun.id}/execute\`)`.

---

### Page 5: Run Execution (`/projects/:projectId/test-runs/:runId/execute`)

**File:** `frontend/src/pages/RunExecution.tsx`

**Current state:** Uses `mockProjects`, `mockSuites`, `mockTestRuns`. Tracks step-level status in local state. Has "Complete" and "Unlock" actions.

**Target endpoints:**
| Action | Endpoint |
|--------|----------|
| Get run | `GET /projects/{projectId}/runs/{runId}` |
| Get suites/cases/steps | Same as Repository page |
| Update step status | `PUT /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps/{stepId}` |
| Complete run | `POST /projects/{projectId}/runs/{runId}/complete` |
| Unlock run | `POST /projects/{projectId}/runs/{runId}/unlock` |
| Update run counters | `PUT /projects/{projectId}/runs/{runId}` |

**Migration steps:**

1. Load run via `useQuery(['run', projectId, runId], () => testRunsApi.get(projectId, runId))`
2. Load suites/cases/steps using the same pattern as Repository.
3. Each step status change → `useMutation` on `testStepsApi.update`, then recalculate `passed/failed/skipped/untested` counts and `PUT` the run to persist them (or let the backend compute them).
4. "Complete" button → `testRunsApi.complete(projectId, runId)`, invalidate `['run', projectId, runId]`
5. "Unlock" button → `testRunsApi.unlock(projectId, runId)`, invalidate same key.

---

### Page 6: Defects (`/projects/:projectId/defects`)

**File:** `frontend/src/pages/Defects.tsx`

**Current state:** Uses `mockDefects`, `mockProjects`. Full CRUD in local state. Filters by severity/status.

**Target endpoints:**
| Action | Endpoint |
|--------|----------|
| List defects | `GET /projects/{projectId}/defects?page=0&size=20` |
| Create defect | `POST /projects/{projectId}/defects` |
| Update defect | `PUT /projects/{projectId}/defects/{id}` |
| Delete defect | `DELETE /projects/{projectId}/defects/{id}` |

**Migration steps:**

1. Replace mock data with `useQuery(['defects', projectId], () => defectsApi.list(projectId))`
2. Filtering by severity/status can stay client-side (filter the query result) or be pushed to the backend via query params if the list grows large. Start client-side.
3. Wire create/edit to `useMutation` with appropriate API calls.
4. Wire delete with confirmation modal (already present).
5. Invalidate `['defects', projectId]` after every mutation.

---

### Page 7: Attachments (`/projects/:projectId/attachments`)

**File:** `frontend/src/pages/Attachments.tsx`

**Current state:** Uses `mockProjects` for the project name and a local `mockFiles` array. Upload is simulated. Download links are fake.

**Target endpoints:**
| Action | Endpoint |
|--------|----------|
| List attachments | `GET /projects/{projectId}/attachments` |
| Upload file | `POST /projects/{projectId}/attachments` (multipart) |
| Delete attachment | `DELETE /projects/{projectId}/attachments/{id}` |
| Download file | Backend needs a `GET /projects/{projectId}/attachments/{id}/download` endpoint (may need to be added) |

**Migration steps:**

1. Replace mock files with `useQuery(['attachments', projectId], () => attachmentsApi.list(projectId))`
2. File upload: use `attachmentsApi.upload(projectId, file)` (already handles multipart/FormData without Content-Type header).
3. Wire delete with `attachmentsApi.delete`, invalidate `['attachments', projectId]`.
4. For downloads: if backend returns a URL in the `Attachment` object, use it directly. If not, a download endpoint needs to be added to `AttachmentController`.

---

### Page 8: Reports (`/projects/:projectId/reports`)

**File:** `frontend/src/pages/Reports.tsx`

**Current state:** Uses local `MockReport[]` state and `mockProjects`. No real report entity in backend yet.

**Note:** The backend does not currently have a `Report` entity or controller. Reports are likely generated on-demand from test run data. This is the only page requiring a backend addition.

**Two options:**

**Option A — Generate reports client-side (simpler, no new backend code):**
Fetch test runs via `testRunsApi.list`, defects via `defectsApi.list`, compute statistics in the browser, and render the report. The "report" is not persisted — it is regenerated each time.

**Option B — Add a `ReportDTO` + `ReportController` to backend (full persistence):**
Create a `Report` entity with fields: `id`, `projectId`, `name`, `type` (summary/full/defect), `createdAt`, `testRunId` (optional), `status`. Then:
- `GET /projects/{projectId}/reports` — list
- `POST /projects/{projectId}/reports` — create (triggers generation)
- `GET /projects/{projectId}/reports/{id}` — get detail

**Recommendation:** Start with Option A (client-side generation) so the UI is unblocked. Migrate to Option B later if report persistence is required.

---

### Page 9: Create Report (`/projects/:projectId/reports/new`)

**File:** `frontend/src/pages/CreateReport.tsx`

**Current state:** Uses `mockProjects`, `mockTestRuns`. Populates a form.

**Migration steps (Option A):**
1. Load project via `projectsApi.get(projectId)`
2. Load test runs via `testRunsApi.list(projectId)`
3. On form submit, navigate to `/reports/{generatedId}` and pass state (report config) via React Router `state`

---

### Page 10: Report Detail (`/projects/:projectId/reports/:reportId`)

**File:** `frontend/src/pages/ReportDetail.tsx`

**Current state:** Uses `mockProjects`, `mockTestRuns`, `mockDefects`.

**Migration steps (Option A):**
1. Load project, runs, defects via live queries
2. Render aggregated statistics (pass rate, defect counts, etc.) from real data

---

## Phase 3 — Export / Import

**File:** `frontend/src/lib/exportImport.ts`

**Current state:** `performExport` reads from mock data. `performImport` parses CSV and updates local state.

**Migration steps:**
1. Update `performExport` to accept real `Suite[]` data (passed in from Repository page after it fetches live data).
2. Update `performImport` to call `suitesApi.create` / `testCasesApi.create` / `testStepsApi.create` for each imported row.
3. This is the last thing to update since it depends on Repository being live.

---

## Phase 4 — Clean-up

Once all pages are integrated:

1. Delete `frontend/src/data/mockData.ts` (verify no remaining imports first: `grep -r "mockData" frontend/src`)
2. Remove any unused `useState` arrays that previously held mock data
3. Remove the `@tanstack/react-query-devtools` import from production builds (keep in dev)
4. Audit all pages for remaining `console.log` / `console.error` calls

---

## Implementation Order Summary

| # | Task | Effort | Depends on |
|---|------|--------|-----------|
| 0 | Phase 1: AppHeader logout + username | 1h | AuthContext (done) |
| 1 | Phase 2.1: Projects page | 2h | api.ts (done) |
| 2 | Phase 2.2: Repository page (suites + cases) | 4h | Projects |
| 3 | Phase 2.3: Repository steps panel | 2h | Repository suites/cases |
| 4 | Phase 2.4: Test Runs page | 2h | Projects |
| 5 | Phase 2.5: Create Test Run page | 1h | Repository + Test Runs |
| 6 | Phase 2.6: Run Execution page | 3h | Test Runs + Repository |
| 7 | Phase 2.7: Defects page | 2h | Projects |
| 8 | Phase 2.8: Attachments page | 2h | Projects |
| 9 | Phase 2.9: Reports (client-side gen) | 3h | Test Runs + Defects |
| 10 | Phase 3: Export / Import | 2h | Repository live |
| 11 | Phase 4: Clean-up, remove mockData | 1h | All above |

**Total estimated effort:** ~25 hours of focused frontend work

---

## Error Handling Convention

Every page should handle three states:

```typescript
if (isLoading) return <SkeletonTable />;       // show skeleton/spinner
if (isError)   return <ErrorBanner msg={...} />; // show error with retry button
if (!data || data.length === 0) return <EmptyState />; // show empty state with Create CTA
// else: render table/list
```

A shared `<SkeletonTable>` and `<ErrorBanner>` component should be created in `frontend/src/components/` to avoid repetition.

---

## Query Key Conventions

All query keys follow the pattern `[entity, ...scopeIds, action?]`:

| Entity | Key pattern |
|--------|-------------|
| Projects list | `['projects', page]` |
| Single project | `['project', projectId]` |
| Suites | `['suites', projectId]` |
| Cases | `['cases', projectId, suiteId]` |
| Steps | `['steps', projectId, suiteId, caseId]` |
| Runs | `['runs', projectId, page]` |
| Single run | `['run', projectId, runId]` |
| Defects | `['defects', projectId, page]` |
| Attachments | `['attachments', projectId]` |

---

## Testing Checklist (per page)

Before marking a page as complete, verify:

- [ ] Loading state shows while fetching
- [ ] Error state shows when API is unreachable
- [ ] Empty state shows when no items exist
- [ ] Create works end-to-end (form submit → API → list updates)
- [ ] Edit works end-to-end
- [ ] Delete works end-to-end with confirmation
- [ ] No `mockData` imports remaining in the file
- [ ] Session expiry (401) redirects to login
- [ ] Toast notifications appear for success and failure

---

## Running the Full Stack Locally

```bash
# 1. Install root devDependencies (concurrently + dotenv-cli)
npm install

# 2. Install frontend dependencies
npm run install:frontend

# 3. Copy and fill env file (only needed once)
cp .env.example .env
# Edit .env with your CYODA_HOST, CYODA_CLIENT_ID, CYODA_CLIENT_SECRET

# 4. Start both apps together
npm run dev
#  → Frontend: http://localhost:5173
#  → Backend:  http://localhost:8080/api
#  → Swagger:  http://localhost:8080/api (root redirects to Swagger UI)
```

---

*Last updated: 2026-03-31*

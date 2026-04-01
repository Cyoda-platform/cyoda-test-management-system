# Cyoda TMS — UI ↔ Backend Integration Plan

**Status:** ✅ Complete
**Date:** 2026-04-01
**Author:** Engineering

---

## Overview

This document describes the full plan for replacing all mock data in the React frontend with live calls to the Spring Boot backend. The backend exposes a REST API at `/api` (port 8080 by default). The frontend runs on port 5173. Authentication uses httpOnly cookies (`auth-token`) in production; in development, a Bearer token stored in `localStorage` is sent via the `Authorization` header (CORS limitation across ports).

The central API client (`frontend/src/lib/api.ts`) and the auth context (`frontend/src/contexts/AuthContext.tsx`) are in place. All pages are now wired to live endpoints. `mockData.ts` has been deleted.

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
- Loading spinner shown via `isLoading` flag; error shown via `isError` + `error.message`.
- Empty state (zero results) gets a friendly "No items yet" UI with a Create button.
- 401 responses from the API client clear the stored token; `ProtectedRoute` redirects to `/` (login).
- The `toast` utility (via `sonner`) is used for success/failure feedback on all mutations.

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

## Phase 1 — Shared Infrastructure ✅ Complete

### 1.1 Install TanStack Query ✅ Done

```bash
cd frontend && npm install @tanstack/react-query @tanstack/react-query-devtools
```

`QueryClient` is configured in `App.tsx` with `retry: 1, staleTime: 30_000`.

### 1.2 Create custom query hooks file ✅ Done

`frontend/src/hooks/useApi.ts` — typed hooks for every domain (projects, suites, cases, steps, runs, defects, attachments) using `keys.*` factory pattern. All pages import from here.

### 1.3 Wire `AppHeader` logout and username ✅ Done

`frontend/src/components/AppHeader.tsx` and `frontend/src/components/LogoutModal.tsx` — fully wired. `useAuth()` supplies `user.username` and `user.role` for display. Logout icon opens `LogoutModal`, which calls `await logout()` with `isLoggingOut` spinner state.

---

## Phase 2 — Page-by-Page Integration ✅ Complete

---

### Page 1: Projects (`/projects`) ✅ Done

**File:** `frontend/src/pages/Projects.tsx`

No mock data imports. Full CRUD via TanStack Query (`useProjects`, `useCreateProject`, `useUpdateProject`, `useDeleteProject`). Loading / error / empty states present. Pagination uses backend total count.

| Action | Endpoint |
|--------|----------|
| List all | `GET /projects?page=0&size=10` |
| Create | `POST /projects` |
| Update | `PUT /projects/{id}` |
| Delete | `DELETE /projects/{id}` |

---

### Page 2: Repository (`/projects/:projectId/repository`) ✅ Done

**File:** `frontend/src/pages/Repository.tsx`

Fully integrated. Suites, cases, steps, and case-level attachments all use live API. Key fixes applied during integration:
- Fixed `TestCaseDTO` 400 error (Lombok/Jackson `@JsonProperty` conflict on `name` vs `title`)
- Fixed steps not displaying (backend returns plain `List<T>`, not `{ data: [...] }`)
- Attachments upload in case form calls `attachmentsApi.upload` with `caseId` and invalidates cache

| Action | Endpoint |
|--------|----------|
| Get project | `GET /projects/{projectId}` |
| List suites | `GET /projects/{projectId}/suites` |
| Create / Update / Delete suite | `POST/PUT/DELETE /projects/{projectId}/suites/{id}` |
| List cases for suite | `GET /projects/{projectId}/suites/{suiteId}/cases` |
| Create / Update / Delete case | `POST/PUT/DELETE /projects/{projectId}/suites/{suiteId}/cases/{id}` |
| List steps for case | `GET /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps` |
| Create / Update / Delete step | `POST/PUT/DELETE /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps/{id}` |

---

### Page 3: Attachments (`/projects/:projectId/attachments`) ✅ Done

**File:** `frontend/src/pages/Attachments.tsx`

Live list, multipart upload (with auth header), delete, download, and image preview integrated. EdgeMessage fallback: if EdgeMessage API fails, file content is stored as base64 inline in the entity.

| Action | Endpoint |
|--------|----------|
| List attachments | `GET /projects/{projectId}/attachments` |
| Upload file | `POST /projects/{projectId}/attachments` (multipart) |
| Delete attachment | `DELETE /projects/{projectId}/attachments/{id}` |

---

### Page 4: Test Runs (`/projects/:projectId/runs`) ✅ Done

**File:** `frontend/src/pages/TestRuns.tsx`

Mock imports removed. `useTestRuns` for list, `useProject` for project header. `useUpdateTestRun` / `useDeleteTestRun` mutations wired to edit and delete dialogs. Toast feedback on all mutations. Loading / error / empty states present.

| Action | Endpoint |
|--------|----------|
| List runs | `GET /projects/{projectId}/runs?page=0&size=20` |
| Update run | `PUT /projects/{projectId}/runs/{id}` |
| Delete run | `DELETE /projects/{projectId}/runs/{id}` |

---

### Page 5: Create Test Run (`/projects/:projectId/runs/new`) ✅ Done

**File:** `frontend/src/pages/CreateTestRun.tsx`

Mock imports removed. `useProject` for header, `useSuites` for suite list. `useQueries` fetches cases for all suites in parallel (avoids hook-in-loop rule violation). `createTestRun.mutate()` on submit; navigates to new run's execution page on success. Loading spinner in case picker and Create button.

| Action | Endpoint |
|--------|----------|
| Get project | `GET /projects/{projectId}` |
| List suites | `GET /projects/{projectId}/suites` |
| List cases per suite | `GET /projects/{projectId}/suites/{suiteId}/cases` |
| Create run | `POST /projects/{projectId}/runs` |

---

### Page 6: Run Execution (`/projects/:projectId/runs/:runId/execute`) ✅ Done

**File:** `frontend/src/pages/RunExecution.tsx`

Most complex migration. Mock imports removed. `useTestRun` + `useSuites` + `useQueries` (per-suite cases) + `useTestSteps` (active case steps). Key implementation details:
- `activeCase` declared immediately after `allCases` computation so `useTestSteps` (called unconditionally) can receive `activeCase?.suiteId`
- `useEffect` on `[steps, activeCase?.id]` initialises `stepStatuses` map from fetched step data on first load, preserving user changes thereafter
- Each step status toggle calls `updateStep.mutate()` to persist to API instantly
- `step.stepNumber` used (not `step.order` which was a mock artefact)
- Complete Run and Unlock buttons wired to `useCompleteTestRun` / `useUnlockTestRun`

| Action | Endpoint |
|--------|----------|
| Get run | `GET /projects/{projectId}/runs/{runId}` |
| List suites / cases / steps | Same as Repository |
| Update step status | `PUT /projects/{projectId}/suites/{suiteId}/cases/{caseId}/steps/{stepId}` |
| Complete run | `POST /projects/{projectId}/runs/{runId}/complete` |
| Unlock run | `POST /projects/{projectId}/runs/{runId}/unlock` |

---

### Page 7: Defects (`/projects/:projectId/defects`) ✅ Done

**File:** `frontend/src/pages/Defects.tsx`

Mock imports removed. `useDefects` + `useProject` for data. All four CRUD handlers wired to mutations (`useCreateDefect`, `useUpdateDefect`, `useDeleteDefect`). Inline severity/status quick-edits call `updateDefect.mutate()` directly. Removed `viewTarget.files` section (not in API `Defect` type). Loading / error states wrap the table.

| Action | Endpoint |
|--------|----------|
| List defects | `GET /projects/{projectId}/defects?page=0&size=20` |
| Create defect | `POST /projects/{projectId}/defects` |
| Update defect | `PUT /projects/{projectId}/defects/{id}` |
| Delete defect | `DELETE /projects/{projectId}/defects/{id}` |

---

### Page 8: Reports (`/projects/:projectId/reports`) ✅ Done — Option A (client-side)

**File:** `frontend/src/pages/Reports.tsx`

No backend `Report` entity. Report configuration is stored in `localStorage` (Option A). `useProject` provides live project header. Report list rendered from localStorage.

---

### Page 9: Create Report (`/projects/:projectId/reports/new`) ✅ Done

**File:** `frontend/src/pages/CreateReport.tsx`

Mock imports removed. `useProject` + `useTestRuns` provide live data for the form. Submitted report config saved to localStorage. Navigation to report detail on submit.

| Action | Endpoint |
|--------|----------|
| Get project | `GET /projects/{projectId}` |
| List runs | `GET /projects/{projectId}/runs?page=0&size=20` |

---

### Page 10: Report Detail (`/projects/:projectId/reports/:reportId`) ✅ Done

**File:** `frontend/src/pages/ReportDetail.tsx`

Mock imports removed. `ReportSections` interface defined locally (was previously imported from mockData). Report config loaded via `useMemo` from localStorage, with fallback to hardcoded `reportData` for pre-seeded samples. Stats computed from live run data. Defect quick-edit / delete wired to `useUpdateDefect` / `useDeleteDefect`.

| Action | Endpoint |
|--------|----------|
| Get project | `GET /projects/{projectId}` |
| List runs | `GET /projects/{projectId}/runs?page=0&size=20` |
| List defects | `GET /projects/{projectId}/defects?page=0&size=20` |

---

## Phase 3 — Export / Import ✅ Done

**File:** `frontend/src/lib/exportImport.ts`

Already importing from `@/lib/localTypes` (not mockData). `performExport` accepts `suites: Suite[]` passed in from the Repository page (which now provides live data). `performImport` returns `{ updatedSuites, result }` for the caller to apply. No changes required.

---

## Phase 4 — Clean-up ✅ Done

| Task | Status |
|------|--------|
| `GlobalSearch.tsx` migrated from mockData to `projectsApi.search()` | ✅ Done |
| Removed 3 stale `console.log` / `console.warn` calls from `api.ts` | ✅ Done |
| Confirmed zero `from '@/data/mockData'` imports across codebase | ✅ Done |
| Deleted `frontend/src/data/mockData.ts` | ✅ Done |
| `npx tsc --noEmit` — zero errors | ✅ Done |

**GlobalSearch details:** Global search now uses `projectsApi.search(query)` via `useQuery` with 300 ms debounce. Query fires when ≥ 2 characters are typed. Test cases and test runs removed from global search (no cross-project search endpoint exists for those — discoverable within each project's Repository / Runs pages). Spinner replaces the search icon while fetching.

---

## Implementation Order Summary

| # | Task | Effort | Status |
|---|------|--------|--------|
| 0 | Phase 1.1–1.2: TanStack Query + hooks file | 2h | ✅ Done |
| 1 | Phase 1.3: AppHeader logout + username | 1h | ✅ Done |
| 2 | Phase 2.1: Projects page | 2h | ✅ Done |
| 3 | Phase 2.2: Repository page (suites + cases + steps) | 6h | ✅ Done |
| 4 | Phase 2.3: Repository attachments (per-case upload) | 2h | ✅ Done |
| 5 | Phase 2.7: Attachments page (standalone) | 2h | ✅ Done |
| 6 | Phase 2.4: Test Runs page | 2h | ✅ Done |
| 7 | Phase 2.5: Create Test Run page | 1h | ✅ Done |
| 8 | Phase 2.6: Run Execution page | 3h | ✅ Done |
| 9 | Phase 2.8: Defects page | 2h | ✅ Done |
| 10 | Phase 2.9–10: Reports (client-side gen) | 3h | ✅ Done |
| 11 | Phase 3: Export / Import | — | ✅ Done (was already clean) |
| 12 | Phase 4: GlobalSearch migration | 1h | ✅ Done |
| 13 | Phase 4: console.log removal + mockData.ts deletion | 0.5h | ✅ Done |

**Total estimated effort:** ~26.5 hours — **100% complete**

---

## Error Handling Convention

Every page handles three states:

```typescript
if (isLoading) return <Loader2 className="animate-spin" />;      // spinner
if (isError)   return <AlertTriangle /> + error.message;          // error banner
if (!data || data.length === 0) return <EmptyState />;            // empty state with Create CTA
// else: render table/list
```

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
| Global project search | `['global-search-projects', query]` |

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

*Last updated: 2026-04-01 — Integration complete.*

# Role-Based Access Control: TESTER vs ADMIN

**Date:** 2026-05-04
**Status:** Approved

---

## Context

The system has two hardcoded roles: `ADMIN` and `TESTER`. Previously, only project create/edit/delete was restricted to ADMIN. This spec extends restrictions to suites and test cases, fixes a code inconsistency in role comparisons, and adds role-based UI hiding on the frontend.

---

## Role Matrix

| Resource | Action | ADMIN | TESTER |
|---|---|---|---|
| Projects | Read | ✅ | ✅ |
| Projects | Create / Edit / Delete | ✅ | ❌ 403 |
| Suites | Read | ✅ | ✅ |
| Suites | Create / Edit / Delete | ✅ | ❌ 403 |
| Test Cases | Read | ✅ | ✅ |
| Test Cases | Create / Edit / Delete | ✅ | ❌ 403 |
| Test Runs | All operations | ✅ | ✅ |
| Defects | All operations | ✅ | ✅ |
| Reports | All operations | ✅ | ✅ |
| Attachments | All operations | ✅ | ✅ |

---

## Backend Changes

### 1. SuiteController
Add `"ADMIN".equals(role)` guard on:
- `POST /projects/{projectId}/suites` — create suite
- `PUT /projects/{projectId}/suites/{id}` — update suite
- `DELETE /projects/{projectId}/suites/{id}` — delete suite

Return `403 Forbidden` if role is not ADMIN.

### 2. TestCaseController
Add `"ADMIN".equals(role)` guard on:
- `POST /projects/{projectId}/suites/{suiteId}/cases` — create test case
- `PUT /projects/{projectId}/suites/{suiteId}/cases/{id}` — update test case
- `DELETE /projects/{projectId}/suites/{suiteId}/cases/{id}` — delete test case

### 3. TestCaseDirectController
Add `"ADMIN".equals(role)` guard on all mutating endpoints (POST, PUT, DELETE).

### 4. TestRunController — fix inconsistent comparisons
Replace:
```java
role.equalsIgnoreCase("Tester") || role.equalsIgnoreCase("Admin")
```
With:
```java
"TESTER".equals(role) || "ADMIN".equals(role)
```

Tokens always contain uppercase roles (`"ADMIN"`, `"TESTER"`) — `equalsIgnoreCase` is unnecessary and misleading.

---

## Frontend Changes

### Approach
Hide (not disable) create/edit/delete buttons for resources the TESTER cannot manage. Role is already displayed in `AppHeader` — users understand their access level.

### Components to update
For each of the following, hide action buttons when `user.role !== "ADMIN"`:

- `pages/Projects.tsx` — hide "Create project", edit (pencil), delete buttons
- `pages/Repository.tsx` — hide "Create suite", edit suite, delete suite, "Create test case", edit test case, delete test case buttons
- `components/CaseFormPage.tsx` — hide save/submit button (or the whole form) when accessed by TESTER; TESTER should not reach case creation form

### Pattern
Use `useAuth()` hook to get `user.role`, then conditionally render:
```tsx
const { user } = useAuth();
const isAdmin = user?.role === "ADMIN";

// In JSX:
{isAdmin && <Button>Create Suite</Button>}
```

No changes needed for test runs, defects, reports, or attachments — these remain visible and functional for all roles.

---

## What Does NOT Change

- Token generation and validation logic
- Hardcoded user store in `AuthService`
- All GET endpoints — readable by any authenticated user
- `ProjectController` — already correct, no changes needed
- `AppHeader` role display — already shows role, no changes needed

---

## Testing

- Unit tests: add role-enforcement tests to `SuiteController` and `TestCaseController` test classes
- E2E: add Cucumber scenarios for 403 responses when TESTER attempts forbidden mutations

# UI Requirements: Test Management System (TMS) v1.1

---

## 1. Overview

TMS is a web application for managing the complete testing lifecycle. The system is built on strict data isolation by Project ID and uses snapshot copies of test cases when running test executions.

**Base API URL:** `http://<host>/api`
**Stack:** REST API (JSON), JWT (Bearer Token), server-side pagination.

---

## 2. Roles and Access Control (RBAC)

| Role | System Name | Access |
|---|---|---|
| Admin | `Admin` | Full access, project management, unlock test runs. |
| Tester | `Tester` | Create and execute test runs, edit test case repository. |

> **Note:** Roles are case-sensitive (use capitalized names).

---

## 3. Navigation

```
/ (login)
└── /projects
    └── /projects/:id (Project Dashboard)
        ├── /suites (List of test suites)
        │   └── /cases/:id (Repository: view/edit test case)
        ├── /runs (List of test runs)
        │   └── /runs/:id (Execution screen: cases and steps)
        └── /attachments (Project attachment archive)
```

---

## 4. Screen Specifications

### 4.1. Authentication (`/`)

**Elements:** Login, Password, "Sign In" button.

**Logic:** On 401 error, redirect here with `localStorage` cleared.

---

### 4.2. Project List (`/projects`)

**Actions (Admin):** Create project (name, description).
**Actions (All):** Search by name, navigate to project.

---

### 4.3. Repository: Test Suites and Test Cases

**Structure:** List of test suites (single-level). Within each suite — list of test cases.

**TestCase (Details):** Title, Description, Preconditions, Priority (High / Medium / Low).

**TestSteps:** Table of steps (Action / Expected Result).
Steps can be added, deleted, and reordered (drag-and-drop).

**Soft Delete:** "Delete" button for a case changes its status to `deleted`
(case is hidden from the list but remains in the database for history).

---

### 4.4. Test Run List (`/projects/:id/runs`)

**Elements:** Table of runs with progress bar (Passed / Failed / Untested ratio).

**Run Statuses:**

| Status | Description |
|---|---|
| `initial` | Created |
| `active` | In Progress |
| `completed` | Locked (Read-Only) |

**Actions:**

- **Initialize Run** — creates a Snapshot (copy) of all project cases.
- **Complete Run** — captures metrics and changes run to Read-Only.
- **Unlock Run** *(Admin only)* — returns run to `active` status for edits.

---

### 4.5. Test Run Execution (Tester's Workspace)

**This is the most complex screen.** Path: `/projects/:id/runs/:runId`

#### A. Run Header
- Name, environment.
- Overall status and "Complete Run" button (calls `complete_run`).

#### B. TestRunCase List (Left Panel / Table)

- **Case Status:** displayed automatically (Passed / Failed / Skipped / Untested).
  Manual changes are blocked!
- **Link Bug:** URL input field and "Link Bug" button (calls `POST .../link-bug`).
  Available only if run is not locked.

#### C. TestRunStep List (Central Panel when case is selected)

- **Actions:** for each step — Dropdown with statuses: `Passed`, `Failed`, `Skipped`.
- **Atomic Failure:** when status `Failed` is selected for any step, UI must update (refresh)
  the status of the entire case (because backend processor will trigger).
- **Evidence:** "Attach screenshot/log" button next to each step (calls `EdgeMessageProcessor`).

---

### 4.6. Attachments (`/projects/:id/attachments`)

**View:** list of all files uploaded within the project (including those attached to run steps).

**Actions:** Download, Delete (if run is not locked).

---

## 5. UX System Requirements

### Locking (Read-Only Mode)

If `run.status == "completed"`, UI must:

- Disable all step status selection Dropdowns.
- Hide or lock "Upload file" and "Link Bug" buttons.
- Hide "Complete Run" button and show "Unlock" button *(Admin only)*.

### Validation and Errors

- On `400` error (Validation) display messages from the `messages` array under corresponding fields.
- On any system error (`500`) show Toast notification "Server error".

### Pagination

- Use `page` and `size` parameters.
- Display total number of elements (`totalElements`) in table footer.

---

## 6. Key API Calls Table (UI → Backend)

| Screen | Action | Method | Endpoint |
|---|---|---|---|
| Run Execution | Change step status | POST | `/runs/{id}/cases/{cid}/steps/{sid}/status` |
| Run Execution | Link bug to case | POST | `/runs/{id}/cases/{cid}/link-bug` |
| Run Execution | Complete run | POST | `/runs/{id}/complete` |
| Attachments | Upload file | POST | `/projects/{id}/message/upload` (multipart) |
| Repository | Delete case | DELETE | `/projects/{id}/cases/{caseId}` |


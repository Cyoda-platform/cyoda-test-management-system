# Functional Requirements: Test Management System (TMS)

## Module 1: Workspace, Access Control & Projects

* **FR 1.1 Authentication (Predefined):** The system shall not support open registration. Access is restricted to two hardcoded accounts: `admin` (Role: Admin) and `tester` (Role: Tester) with fixed passwords.
* **FR 1.2 Workspace Context:** All operations shall be performed within the context of a single workspace.
* **FR 1.3 Project Management:** Users with the **Admin** role shall be able to Create, Read, Update, and Delete (CRUD) isolated Projects. Each Project shall have:
    * `id`: UUID (auto-generated)
    * `name`: String, unique within workspace (required)
    * `description`: String (optional)
    * `status`: String, one of {Active, Archived} (default: Active)
    * `deleted`: Boolean (soft delete flag, default: false)
    * `createdAt`, `updatedAt`: Timestamps (auto-managed)
* **FR 1.4 Data Isolation:** The system shall strictly restrict data visibility (suites, test cases, runs) to the scope of a single Project. Cross-project data access is prohibited.
* **FR 1.5 Role Permissions:**
    * **Admin:** Full access (CRUD) to all entities within the assigned project.
    * **Tester:** Read-only access to the Test Repository and permission to execute Test Runs. Creation or deletion of entities is prohibited.

---

## Module 2: Test Repository (Management)

* **FR 2.1 Suite Management:** Admins shall be allowed to create single-level folders (**Suites**) to group test cases. Nested folders are strictly prohibited.
* **FR 2.2 Test Case Header:** A Test Case must contain a header with the following fields:
    * `displayId`: Auto-generated readable identifier (e.g., "TC-001", "TC-002"). Generated once and never recomputed.
    * `title`: String, mandatory (max 255 chars)
    * `description`: String, optional (max 2000 chars)
    * `preconditions`: String, optional (max 2000 chars)
    * `priority`: Enum {HIGH, MEDIUM, LOW}, mandatory
    * `deleted`: Boolean, soft delete flag (default: false)
    * `createdAt`, `updatedAt`: Timestamps (auto-managed)
* **FR 2.3 Step Management (Array-based):** The system shall allow Admins to add multiple sequential steps to a Test Case. Each step is a distinct object containing two mandatory fields: **"Action"** and **"Expected Result"**.
* **FR 2.4 Multi-format Attachments (EdgeMessage):** The system shall support uploading various file formats (images, JSON/XML/YAML, documents) via integration with the **EdgeMessage API** (endpoints under `/message/*`). Files shall be linked to test cases or execution steps.
* **FR 2.5 Test Case Export:** Users shall be able to export selected cases or entire suites into **CSV** or **XML** formats, including all metadata and nested step data.
* **FR 2.6 Test Case Import:** Admins shall be able to bulk-import test cases from CSV/XML files. The system must parse the files and automatically generate the TestCase and Step entities within the active Project.
* **FR 2.7 Deep Search:** The system shall provide a global search bar matching keywords against Titles, Descriptions, Pre-conditions, Step Actions, and Expected Results.
* **FR 2.8 Soft Delete:** The system shall implement **"Soft Delete"** for test cases. Deleted cases shall be hidden from the repository and search results but remain preserved in existing Test Run snapshots to maintain historical integrity.
* **FR 2.9 Repository Organization:** Admins shall be able to:
    * Reorder Suites within a Project (PATCH /projects/{id}/suites/reorder)
    * Reorder Test Cases within a Suite (PATCH /projects/{id}/suites/{id}/cases/reorder)
    * Move a Test Case to a different Suite while preserving its content (PATCH /projects/{id}/suites/{id}/cases/{id}/move)
* **FR 2.10 Batch Test Case Import:** In addition to single-case creation, the system shall support batch creation of multiple Test Cases with embedded Steps in a single API call (POST /projects/{id}/suites/{id}/cases/batch). This eliminates the N+1 counter-table hit pattern and improves bulk import performance.
* **FR 2.11 Repository UI - Three-Panel Layout:** The Repository page shall display a three-panel layout:
    * **Left Panel (Suite Tree):** Hierarchical list of all suites with expandable test cases
    * **Middle Panel (Case List):** Full list of test cases within selected suite(s) with search and filtering
    * **Right Panel (Case Details):** Opens when a case is selected, showing full details, steps, attachments, and edit capabilities. Closes when no case is selected.
* **FR 2.12 Bulk Selection & Batch Operations:** Admins shall be able to:
    * Select individual test cases via checkbox
    * Select all test cases across all suites with "Select All" checkbox
    * Perform bulk operations on selected cases:
        * **Bulk Delete:** Delete multiple cases simultaneously
        * **Bulk Copy:** Duplicate selected cases with "(Copy)" suffix in same suite
    * Display count of selected cases in toolbar
* **FR 2.13 Suite Operations:** Admins shall be able to:
    * **Create Suite:** Add new test suite with name and optional description
    * **Edit Suite:** Modify suite name and description
    * **Delete Suite:** Remove suite and all its test cases (with confirmation)
    * **Copy Suite:** Duplicate entire suite with all its test cases (with "(Copy)" suffix)
    * **Reorder Suites:** Drag & drop suites to reorder within project
* **FR 2.14 Test Case Operations:** Admins shall be able to:
    * **Create Test Case:** Full form with title, description, preconditions, priority, and steps
    * **Quick Create:** Fast creation with just a title (opens case for full editing after creation)
    * **Edit Test Case:** Modify all case properties inline or in dedicated form
    * **Delete Test Case:** Remove case with soft delete (marked as deleted, hidden from UI)
    * **Copy Test Case:** Duplicate case in same suite with "(Copy)" suffix in title
    * **Move Test Case:** Transfer case to different suite while preserving all content
    * **Reorder Test Cases:** Drag & drop cases within same suite
* **FR 2.15 Step Management:** Admins shall be able to:
    * **Add Steps:** Insert new step with action and expected result
    * **Edit Steps:** Modify action and expected result inline or in form
    * **Delete Steps:** Remove step from test case
    * **Reorder Steps:** Drag & drop steps to change order within test case
    * **Display Step Numbers:** Show sequential step numbers (1, 2, 3, etc.)
* **FR 2.16 Attachments in Case Details:** Testers and Admins shall be able to:
    * **Upload Attachments:** Attach files to test cases via EdgeMessage API
    * **View Attachments:** Display list of attached files with names and types
    * **Preview Attachments:** Click to preview file content (for images, JSON, XML, etc.)
    * **Delete Attachments:** Remove attachment from case
* **FR 2.17 Import/Export with Conflict Resolution:** Admins shall be able to:
    * **Export Test Cases:** Export selected cases or entire suite(s) to CSV/JSON/XML with all metadata
    * **Import Test Cases:** Import cases from CSV/JSON/XML files with parsing and validation
    * **Conflict Resolution:** When importing, provide options for handling duplicate cases:
        * **Skip:** Do not import if case with same displayId exists
        * **Overwrite:** Replace existing case with imported version
        * **Create New:** Always create new case with new displayId regardless of duplicates
    * **Download Template:** Provide CSV template showing expected format for import
    * **Drag & Drop Import:** Allow dropping CSV/JSON/XML files into import zone
* **FR 2.18 Repository Search & Filter:** Users (Testers) and Admins shall be able to:
    * **Local Search:** Filter cases by title in real-time within current view
    * **Deep Search (FR 2.7):** Global search matching across titles, descriptions, preconditions, and step content (TODO: implement backend)

---

## Module 3: Test Execution

**Implementation Status:** ✅ ~60% Complete (See MODULE_3_REQUIREMENTS.md for detailed status)

* **FR 3.1 Run Initialization:** ✅ WORKING
  Admins and Testers can create a Test Run with the following properties:
    * `displayId`: Auto-generated readable identifier (e.g., "RUN-001", "RUN-002") ✅
    * `name`: String, mandatory (max 255 chars) ✅
    * `environment`: String (e.g., "Staging", "Production", optional) ✅
    * `buildVersion`: String, optional (e.g., "v2.5.0", for CI/CD tracking) ✅
    * `description`: String, optional (max 2000 chars) ✅
    * `caseIds`: List of selected Test Case IDs (snapshot at creation time) ✅
    * The system shall create an immutable **"snapshot"** of all selected test cases and their steps ✅

* **FR 3.2 Step-Level Execution:** ✅ WORKING
  During execution, Testers shall be able to set individual status for each step:
    * Accepted statuses: **Passed**, **Failed**, or **Skipped** ✅
    * Status change is immediate (no refresh needed) ✅
    * Each step status is separately tracked ✅
    * Status is persisted to database ✅

* **FR 3.3 Atomic Failure Logic:** ✅ WORKING
  The system shall automatically aggregate step statuses:
    * IF ANY step = Failed → Case status = Failed ✅
    * IF ALL steps = Passed → Case status = Passed ✅
    * IF some/all steps = Skipped → Case status = Skipped (no Failed) ✅
    * Case status auto-calculated in real-time ✅

* **FR 3.4 Execution Artifacts:** ⚠️ PARTIAL - NEEDS FIX
  Testers shall be able to upload evidence (screenshots, logs) via EdgeMessage:
    * File upload works for TestRunStep ⚠️ (uploads to correct location)
    * Files stored via EdgeMessage API ✅
    * BUT: Evidence display needs fix - currently shows case attachments instead of run evidence ❌
    * ISSUE: Need to separate "Case Attachments" (from repository) from "Test Evidence" (from run)
    * REQUIRED FIX: Display evidence in separate section with clear labeling

* **FR 3.5 Defect Linking:** ✅ WORKING
  The system shall support linking defects to failed test cases and steps:
    * Failed step status triggers defect creation dialog ✅
    * Defect links to test case result ✅
    * Defect properties stored correctly ✅
    * Defect visible in run report ✅
    * (See FR 3.9 for full Defect Management)

* **FR 3.6 Run Locking & Unlocking:** ⚠️ PARTIAL - NEEDS VERIFICATION
  * **Lock:** Upon marking run as "Completed", lock into read-only state ⚠️ (needs test)
  * **Unlock:** Provide "Unlock" function for Admins to return to active state ⚠️ (needs test)
  * **Verification Needed:** Test that locked runs cannot be modified

* **FR 3.7 Test Run Metrics:** ✅ WORKING
  Each Test Run shall automatically track:
    * `status`: One of {draft, active, completed} ✅
    * `passed`, `failed`, `skipped`, `untested`: Integer counters ✅
    * Auto-calculated from step statuses ✅
    * Progress bar updated real-time ✅
    * Success rate = (passed / total) * 100% ✅
    * `startedAt`, `completedAt`: Timestamps ✅

* **FR 3.8 Test Run Details Endpoint:** ⚠️ PARTIAL
  The system shall provide composite endpoint (GET /projects/{id}/runs/{id}):
    * Returns Test Run entity ✅
    * Returns TestRunCase records ✅
    * Includes snapshot IDs and original case IDs ✅
    * Includes step results with evidence ⚠️ (evidence display needs fix)

* **FR 3.9 Execution Audit Trail:** ⚠️ NOT FULLY IMPLEMENTED
  The system shall record audit trail for all status changes:
    * Who changed the status (username) ⚠️ (needs verification)
    * When changed (timestamp) ⚠️ (needs verification)
    * Previous status value ⚠️ (needs verification)
    * New status value ⚠️ (needs verification)
    * Optional: reason for change

---

## Module 3+: Defect Management

**Implementation Status:** ✅ ~70% Complete

* **FR 3.9 Defect Entities:** ✅ WORKING
  The system shall support full Defect tracking with the following properties:
    * `displayId`: Auto-generated readable identifier (e.g., "DEF-001", "DEF-002") ✅
    * `title`: String, mandatory (max 255 chars) ✅
    * `description`: String, optional (max 2000 chars) ✅
    * `severity`: Enum {Critical, Major, Minor}, mandatory ✅
    * `link`: String, optional (URL to external bug tracker like Jira or GitHub) ✅
    * `status`: Enum {Open, In Progress, Fixed, Closed} (default: Open) ✅
    * `source`: String (reference to the source entity) ✅
    * `testRunId`: UUID, optional (link to specific Test Run) ✅
    * `testRunCaseId`: UUID, optional (link to specific TestRunCase) ✅
    * `createdAt`, `updatedAt`: Timestamps (auto-managed) ✅

* **FR 3.10 Defect Lifecycle:** ✅ WORKING
  Admins and Testers shall be able to:
    * Create new Defects from failed step (POST /projects/{id}/defects) ✅
    * View Defects scoped to a Project (GET /projects/{id}/defects) ✅
    * View Defects in table with all fields displayed correctly ✅
    * Update Defect properties (PUT /projects/{id}/defects/{id}) ✅
    * Delete Defects (DELETE /projects/{id}/defects/{id}) ✅
    * Filter Defects by status ✅
    * Link Defects to test results via failed step ✅
    * Defect attachments stored separately (not with test evidence) ✅

---

## Module 4: Reporting & Metrics

* **FR 4.1 Real-time Metrics:** The system shall automatically calculate aggregate statuses within a run: **Total count**, **Passed**, **Failed**, and **Untested** (both in absolute numbers and percentages). (See FR 3.7 for TestRun counters)
* **FR 4.2 Test Run Export:** Users shall be able to export Test Run results into **PDF** or **CSV**, including metrics and a list of failed cases with their associated Defects and step-level details.

---

## Module 4+: Advanced Reporting

* **FR 4.3 Custom Report Creation:** The system shall support creation of custom QA Reports with the following properties:
    * `displayId`: Auto-generated readable identifier (e.g., "REP-001", "REP-002")
    * `name`: String, mandatory (max 255 chars)
    * `type`: Enum {Summary, Regression, Sprint, Custom} (mandatory)
    * `description`: String, optional (max 2000 chars)
    * `createdBy`: String (username of report creator)
    * `dateFrom`, `dateTo`: Strings (optional date range for filtering)
    * `selectedRuns`: List of Test Run IDs to include in the report
    * `sectionExecutiveSummary`: Boolean (default: true)
    * `sectionSuiteAnalytics`: Boolean (default: true)
    * `sectionDefectTable`: Boolean (default: true)
    * `sectionEnvironmentInfo`: Boolean (default: true)
    * `createdAt`, `updatedAt`: Timestamps (auto-managed)

* **FR 4.4 Report Generation and Retrieval:** Admins and Users shall be able to:
    * Create custom reports (POST /projects/{id}/reports)
    * List all reports for a project (GET /projects/{id}/reports)
    * View a specific report with all selected metrics and sections (GET /projects/{id}/reports/{id})
    * Delete reports (DELETE /projects/{id}/reports/{id})
    * Export reports to PDF/CSV with selected sections

* **FR 4.5 Metrics Aggregation:** Reports shall aggregate metrics from selected Test Runs:
    * Total test cases, passed, failed, skipped counts
    * Pass rate percentage
    * Defect summary by severity
    * Suite-level analytics
    * Environment and build version tracking
# Agile User Stories (Final MVP Version)

## Epic 1: Workspace & Security

* **US 1.1: Predefined Access (No Registration)**
    * **As a** system user,
    * **I want to** log in using one of the two predefined accounts (admin or tester),
    * **So that** the system remains simple and secure without the overhead of public registration.
* **US 1.2: User Roles & Permissions**
    * **As a** logged-in user,
    * **I want** the system to enforce my role-based permissions (Admin: full CRUD; Tester: read/execute only),
    * **So that** the test repository is protected from unauthorized changes by non-admin users.
* **US 1.3: Create Isolated Project**
    * **As an** Admin,
    * **I want to** create a new isolated project workspace,
    * **So that** I can keep test data for different products strictly separated.
    * **Acceptance Criteria:** Data from Project A is never visible to users working in Project B.

---

## Epic 2: The Test Repository

* **US 2.1: Single-Level Suites**
    * **As an** Admin,
    * **I want to** group test cases into simple, non-nested folders (Suites),
    * **So that** the team can navigate the repository without complex hierarchies.
    * **Acceptance Criteria:**
        * [ ] Can create suite with name and optional description
        * [ ] Suites appear in left panel tree view
        * [ ] Suites can be expanded to show nested test cases
        * [ ] Suite displayId auto-generated (e.g., "SUITE-001")

* **US 2.2: Structured Test Cases & Sequential Steps**
    * **As an** Admin,
    * **I want to** create test cases with a header (Title, Description, Pre-conditions, Priority) and a list of sequential steps (Action and Expected Result),
    * **So that** testers have clear, granular instructions for execution.
    * **Acceptance Criteria:**
        * [ ] Test case form has all required fields (title, priority)
        * [ ] Optional fields available (description, preconditions)
        * [ ] Priority selector with HIGH/MEDIUM/LOW options
        * [ ] Can add multiple steps with action and expected result
        * [ ] Steps display with sequential numbers
        * [ ] Case displayId auto-generated (e.g., "TC-001")

* **US 2.3: Attachments via EdgeMessage**
    * **As an** Admin,
    * **I want to** upload reference files (images, logs, configs) to a Test Case using the EdgeMessage API,
    * **So that** all necessary materials are stored securely in the cloud and accessible during execution.
    * **Acceptance Criteria:**
        * [ ] Attachments section visible in case details panel
        * [ ] Can upload multiple file types (images, JSON, XML, PDFs)
        * [ ] Attached files shown with name and type
        * [ ] Can preview file content inline
        * [ ] Can delete attachments

* **US 2.4: Deep Search**
    * **As** any user,
    * **I want to** search for test cases using keywords that match titles, descriptions, pre-conditions, or any text within steps,
    * **So that** I can find relevant tests even if I only remember a specific action or expected result.
    * **Acceptance Criteria:**
        * [ ] Global search bar matches against titles
        * [ ] Search results filter in real-time
        * [ ] Matching text highlighted in results
        * [ ] Can search across all cases in all suites
        * [ ] (TODO) Search backend matches descriptions, preconditions, and step content

* **US 2.5: Bulk Import/Export**
    * **As an** Admin,
    * **I want to** import and export test cases via CSV/XML (including all steps),
    * **So that** I can quickly migrate data or create backups.
    * **Acceptance Criteria:**
        * [ ] Export button exports selected or all cases to CSV/JSON/XML
        * [ ] Exported file includes all metadata and steps
        * [ ] Import dialog supports drag & drop of CSV/JSON/XML
        * [ ] Conflict resolution options: Skip/Overwrite/Create New
        * [ ] CSV template available for download
        * [ ] Import validation reports errors
        * [ ] Success message shows count of imported cases

* **US 2.6: Three-Panel Repository UI**
    * **As an** Admin or Tester,
    * **I want to** view the test repository in a three-panel layout (Suites, Cases, Details),
    * **So that** I can efficiently browse and manage test cases.
    * **Acceptance Criteria:**
        * [ ] Left panel shows suite tree with create/edit/delete/copy buttons
        * [ ] Middle panel shows test cases with checkboxes for selection
        * [ ] Right panel opens when case selected, shows full details
        * [ ] Right panel closes when no case selected
        * [ ] Panels are resizable

* **US 2.7: Suite Operations (CRUD & Copy)**
    * **As an** Admin,
    * **I want to** create, edit, delete, and copy suites,
    * **So that** I can manage test organization efficiently.
    * **Acceptance Criteria:**
        * [ ] Create suite button opens form
        * [ ] Edit button allows changing name and description
        * [ ] Delete button with confirmation removes suite
        * [ ] Copy button duplicates suite with all cases and "(Copy)" suffix
        * [ ] All operations show success notifications

* **US 2.8: Test Case Operations (CRUD, Copy, Move)**
    * **As an** Admin,
    * **I want to** create, edit, delete, copy, and move test cases,
    * **So that** I can manage test cases efficiently.
    * **Acceptance Criteria:**
        * [ ] Create button opens full form
        * [ ] Quick create button allows title-only creation
        * [ ] Edit button opens case details in right panel
        * [ ] Delete button soft-deletes with confirmation
        * [ ] Copy button duplicates case with "(Copy)" suffix
        * [ ] Move button transfers case to different suite
        * [ ] All operations show success notifications

* **US 2.9: Drag & Drop Reordering**
    * **As an** Admin,
    * **I want to** drag and drop to reorder suites, test cases, and steps,
    * **So that** I can organize them without complex dialogs.
    * **Acceptance Criteria:**
        * [ ] Suites can be dragged in left panel
        * [ ] Cases can be dragged within same suite
        * [ ] Steps can be dragged within test case
        * [ ] Visual feedback during drag
        * [ ] Order persists on refresh

* **US 2.10: Bulk Selection & Batch Operations**
    * **As an** Admin,
    * **I want to** select multiple test cases and perform batch operations,
    * **So that** I can manage many cases efficiently.
    * **Acceptance Criteria:**
        * [ ] Checkboxes on all cases for individual selection
        * [ ] "Select All" checkbox selects all cases in all suites
        * [ ] Selected count displayed in toolbar
        * [ ] Bulk delete button removes all selected
        * [ ] Bulk copy button duplicates all selected
        * [ ] Delete confirmation shows count
        * [ ] Clear selection deselects all

---

## Epic 3: Test Execution (The Player)

* **US 3.1: Initialize Targeted Run**
    * **As a** Tester or Admin,
    * **I want to** create a Test Run by selecting specific suites or cases and defining the environment,
    * **So that** I can start a focused QA cycle based on a "snapshot" of the current test repository.
    * **Acceptance Criteria:**
        * [ ] Can select one or more suites/cases for test run
        * [ ] displayId auto-generates (e.g., "RUN-001")
        * [ ] Name field required (max 255 chars)
        * [ ] Environment field optional (e.g., "Staging", "Production")
        * [ ] buildVersion field optional (e.g., "v2.5.0")
        * [ ] Description field optional (max 2000 chars)
        * [ ] Snapshot created at run creation (immutable copy of cases + steps)
        * [ ] Run status set to "draft" initially
        * [ ] Start timestamp recorded
        * [ ] Run cannot be edited after snapshot locked

* **US 3.2: Step-Level Validation (Atomic Failure)**
    * **As a** Tester,
    * **I want to** mark individual steps as Passed, Failed, or Skipped,
    * **So that** the system can automatically fail the entire test case if any single step fails.
    * **Acceptance Criteria:**
        * [ ] Each step shows three status buttons: Passed, Failed, Skipped
        * [ ] Only one status can be selected per step
        * [ ] Status change is immediate (no refresh needed)
        * [ ] If ANY step is Failed → case status = Failed
        * [ ] If ALL steps are Passed → case status = Passed
        * [ ] If some/all steps are Skipped → case status = Skipped (if no Failed)
        * [ ] Status history/audit trail recorded (who, when, previous status)
        * [ ] Cannot change step status after run is locked

* **US 3.3: Execution Evidence & Bug Linking**
    * **As a** Tester,
    * **I want to** attach screenshots/logs to a specific step and link a Bug URL to a failed case,
    * **So that** developers have all the context needed to fix the issue.
    * **Acceptance Criteria:**
        * [ ] Upload button in step detail allows file attachment (images, logs, etc.)
        * [ ] Files stored via EdgeMessage API
        * [ ] Multiple files can be attached to one step
        * [ ] Defect/Bug URL field in failed case details
        * [ ] Bug link opens in new window
        * [ ] Attachments remain accessible in run history
        * [ ] Can delete attachment before run is locked

* **US 3.4: Lock and Unlock Test Runs**
    * **As an** Admin or Tester,
    * **I want to** mark a run as "Completed" to lock it, but also have the ability to "Unlock" it if corrections are needed,
    * **So that** we maintain a balance between data integrity and flexibility.
    * **Acceptance Criteria:**
        * [ ] Complete button changes run status to "completed"
        * [ ] Completion timestamp recorded
        * [ ] Run becomes read-only after completion
        * [ ] "Unlock" button available for Admins
        * [ ] Unlock returns run to "active" state
        * [ ] Unlocking records audit trail
        * [ ] Cannot modify run details while locked

* **US 3.5: Real-time Progress Metrics**
    * **As a** Tester,
    * **I want to** see live counts of Passed/Failed/Skipped/Untested cases,
    * **So that** I can quickly assess test execution progress.
    * **Acceptance Criteria:**
        * [ ] Progress bar shows percentage complete
        * [ ] Counters show: Passed count, Failed count, Skipped count, Untested count
        * [ ] Total cases count displayed
        * [ ] Metrics update in real-time as steps are marked
        * [ ] Success rate calculated (Passed / Total)
        * [ ] Failed cases highlighted visually

* **US 3.6: Browse Execution Results**
    * **As a** Tester or Admin,
    * **I want to** view all cases and their step results in a test run,
    * **So that** I can review what was tested and what failed.
    * **Acceptance Criteria:**
        * [ ] Cases list shows status badge (Passed/Failed/Skipped/Untested)
        * [ ] Click case shows detailed step results
        * [ ] Step results include: status, timestamp, evidence (attachments)
        * [ ] Failed steps highlighted in red
        * [ ] Can filter by status (show only failed, etc.)
        * [ ] Search cases by title

* **US 3.7: View Run Summary & Metrics**
    * **As a** Admin or Tester,
    * **I want to** see a run summary with total metrics and execution timeline,
    * **So that** I can understand the overall execution results and performance.
    * **Acceptance Criteria:**
        * [ ] Summary shows: Run name, environment, build version, status
        * [ ] Execution duration displayed (started at → completed at)
        * [ ] Total metrics: Total cases, Passed, Failed, Skipped, Untested
        * [ ] Success rate percentage (Passed / Total * 100)
        * [ ] Created by, Last modified by displayed
        * [ ] Printable/exportable summary

* **US 3.8: Snapshot Immutability & Case History**
    * **As a** Tester or Admin,
    * **I want to** see that test cases in a run cannot be modified after execution starts,
    * **So that** I maintain historical integrity of the test record.
    * **Acceptance Criteria:**
        * [ ] Snapshot locked when run status = "active"
        * [ ] Cases show original snapshot details, not current repo state
        * [ ] If case deleted in repo, run still shows archived case version
        * [ ] Snapshot includes all steps as they were at run creation
        * [ ] Cannot edit case details during active run
        * [ ] Both snapshot ID and original case ID stored

* **US 3.9: Defect Linking (Basic)**
    * **As a** Tester,
    * **I want to** link failed test cases to defects/bugs,
    * **So that** developers know which cases failed and can investigate.
    * **Acceptance Criteria:**
        * [ ] Failed case shows "Link Defect" button
        * [ ] Can enter defect ID and URL
        * [ ] Defect link stored with failed case
        * [ ] Can view linked defects in run report

* **US 3.10: Audit Trail for Execution**
    * **As an** Admin,
    * **I want to** see a complete audit trail of who changed what status when,
    * **So that** I can track test execution decisions.
    * **Acceptance Criteria:**
        * [ ] Audit log shows: timestamp, user, step, old status, new status
        * [ ] View audit in step detail or separate audit log
        * [ ] Sortable by date, user, case
        * [ ] Cannot modify audit log
        * [ ] Includes unlock events

---

## Epic 4: Reporting

* **US 4.1: Real-time Progress Metrics**
    * **As a** user,
    * **I want to** see an instant summary (Total, Passed, Failed, Untested) in units and percentages within a run,
    * **So that** I can quickly assess the current quality status of the release.
* **US 4.2: Exportable Execution Reports**
    * **As a** user,
    * **I want to** download a PDF or CSV report of the Test Run results,
    * **So that** I can share the final quality sign-off with stakeholders.
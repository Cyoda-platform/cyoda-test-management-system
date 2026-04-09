# Module 3: Test Execution - Detailed Requirements & User Stories

## Overview
Module 3 handles the execution phase of test management. Users (Testers and Admins) create Test Runs, execute individual test steps, and capture results with evidence. The system maintains an immutable snapshot of test cases to preserve historical integrity.

## Implementation Status

| Feature | Status | Notes |
|---------|--------|-------|
| Create Test Run UI | ✅ WORKING | Select cases, env, build, description all functional |
| Test Run List | ✅ WORKING | Displays all runs with filters |
| Progress Display | ✅ WORKING | Progress bar in list + in run detail |
| Step Status Update | ✅ WORKING | Passed/Failed/Skipped statuses save correctly |
| Defect Creation | ✅ WORKING | Failed status triggers defect dialog |
| Defect Linking | ✅ WORKING | Defect attaches to test case result |
| Evidence Upload | ✅ PARTIAL | Upload UI works but only for test run step |
| Run Lock/Unlock | ❌ NOT TESTED | Probably works but needs verification |
| Case Attachments Display | ❌ NEEDS FIX | Shows repo case attachments, NOT run step evidence |
| Evidence Association | ❌ NEEDS FIX | Evidence should attach to TestRunStep, not TestCase |
| Defect Attachments | ❌ NEEDS FIX | Defect files store with defect, not with test evidence |
| Metrics in Real-time | ⚠️ PARTIAL | Works but may need optimization |

---

## Key Concepts

### Test Run Lifecycle
```
Draft → Active → Completed (Locked)
        ↑________________↓
        (Can Unlock)
```

### Snapshot
- Created at run initialization time
- Contains immutable copy of: TestCase metadata + all Steps
- Preserves state even if case/step deleted from repository
- Links to both snapshot ID and original case ID

### Atomic Failure Logic
- IF ANY step status = Failed → Case status = Failed
- IF ALL steps status = Passed → Case status = Passed
- IF ANY step status = Skipped → Case status = Skipped (no Failed)
- IF NO steps executed → Case status = Untested

### Evidence & Defect Association
- **Evidence (Test Results):** Attaches to TestRunStep result, NOT TestCase
- **Defect Attachments:** Attaches to Defect record, NOT Test Evidence
- **Case Attachments:** Are from Repository TestCase, visible separately

---

## Detailed Requirements

### 3.1: Run Initialization & Snapshot Creation

**Status:** ✅ WORKING

**What needs to be done:**
- Verify TestRunDTO has all fields
- Verify SnapshotProcessor creates TestRunCase and TestRunStep records
- Verify immutable snapshot storage

**Acceptance Criteria:**
- [x] POST /projects/{projectId}/runs creates run with caseIds
- [x] displayId auto-generated (RUN-001, RUN-002, etc.)
- [x] Name required (max 255), environment optional
- [x] Snapshot created immediately
- [x] TestRunCase records created for each selected case
- [x] TestRunStep records created for each step in each case
- [x] Original case/step IDs stored alongside snapshot IDs
- [x] Run status = "draft" → "active"
- [x] startedAt timestamp recorded
- [x] All snapshot data immutable after creation

---

### 3.2: Step-Level Execution & Status Management

**Status:** ✅ WORKING

**What needs to be verified:**
- Step status update endpoints working
- Audit trail recorded correctly
- Case status auto-calculated from step statuses

**Acceptance Criteria:**
- [x] PUT /projects/{projectId}/runs/{runId}/steps/{stepId} with status
- [x] Accepted statuses: PASSED, FAILED, SKIPPED
- [x] Status change immediate (no refresh)
- [ ] Audit trail recorded: user, timestamp, previous status (NEEDS VERIFICATION)
- [x] Case status auto-calculated from step statuses
- [ ] Cannot change status if run locked (NEEDS VERIFICATION)
- [x] Response includes updated case status

---

### 3.3: Execution Evidence & Defect Linking

**Status:** ⚠️ PARTIAL - NEEDS FIXES

**Current Issues:**
1. **Evidence Association WRONG:** Evidence uploads to TestRunStep (CORRECT) but UI shows TestCase attachments (WRONG)
   - ISSUE: Upload Evidence dialog appears correctly for step
   - ISSUE: But displayed attachments are from original TestCase repo
   - FIX NEEDED: Separate display of:
     - Test Case Attachments (from repository)
     - Test Run Step Evidence (from execution)

2. **Defect Attachments:** Defect files attach to Defect, NOT to evidence
   - CURRENT: This is CORRECT per design
   - STATUS: No fix needed - this is working as designed

**Acceptance Criteria:**
- [x] File upload to steps via EdgeMessage API (WORKS)
- [x] Multiple files per step allowed (WORKS)
- [x] Failed case shows defect linking option (WORKS)
- [x] Defect creation on Failed status (WORKS)
- [ ] Evidence display SEPARATED from case attachments (NEEDS FIX)
- [ ] Evidence only shows step-specific files (NEEDS FIX)

---

### 3.4: Run Completion & Locking

**Status:** ⚠️ LIKELY WORKING - NEEDS VERIFICATION

**What needs to be tested:**
- Run completion endpoint
- Run lock state after completion
- Run unlock for admins

**Acceptance Criteria:**
- [ ] POST /projects/{projectId}/runs/{runId}/complete locks run (NEEDS TEST)
- [ ] Run status = "completed" (NEEDS TEST)
- [ ] completedAt timestamp recorded (NEEDS TEST)
- [ ] All metrics finalized (Passed/Failed/Skipped/Untested) (NEEDS TEST)
- [ ] Run becomes read-only (NEEDS TEST)
- [ ] POST /projects/{projectId}/runs/{runId}/unlock available for Admins (NEEDS TEST)
- [ ] Unlock records audit entry (NEEDS TEST)

---

### 3.5: Metrics & Progress Tracking

**Status:** ✅ WORKING

**What is implemented:**
- Progress bar in test run list
- Real-time metric updates
- Success rate calculation

**Acceptance Criteria:**
- [x] GET /projects/{projectId}/runs/{runId}/metrics (or inline)
- [x] Returns: total, passed, failed, skipped, untested
- [x] Success rate = (passed / total) * 100%
- [x] Metrics updated real-time as steps change
- [x] Progress bar reflects completion %

---

## Issues to Fix (From Code Review)

### CRITICAL ISSUES

**Issue #1: Evidence Display Confusion**
- **Problem:** Upload Evidence dialog for TestRunStep (correct), but displayed attachments show TestCase repo attachments (wrong)
- **Location:** Test Run detail page, Test Case attachment section
- **What's Wrong:** User sees case attachments from repository, not from test execution evidence
- **What Should Happen:**
  - Show TWO separate sections:
    - "Case Attachments" (from repository TestCase)
    - "Test Evidence" (from TestRunStep execution)
- **To Fix:**
  1. Modify TestCase detail view to separate attachments by source
  2. Query both TestCase attachments AND TestRunStep evidence
  3. Display each in separate section with clear labels

**Issue #2: Evidence Not Persisting Correctly**
- **Problem:** Evidence uploads but may not be retrievable later
- **Location:** Upload Evidence modal + retrieval
- **What Should Happen:**
  1. File uploaded via EdgeMessage
  2. URL stored with TestRunStep record
  3. On reload, evidence shows in TestRunStep
  4. NOT accessible in TestCase attachments view
- **To Fix:**
  1. Verify TestRunStep.evidence array stores URLs
  2. Verify API returns evidence with step results
  3. Verify frontend stores evidence URLs on upload

**Issue #3: Defect Attachments Storage**
- **Problem:** Defect attachment associations need clarity
- **Current Design:** ✅ CORRECT - Defect attachments belong to Defect, not evidence
- **Status:** No fix needed, this is working correctly

---

## Data Model Requirements

### TestRunCase (Snapshot)
- id: UUID (snapshot case ID)
- testRunId: UUID (reference to run)
- originalCaseId: UUID (reference to original case)
- snapshotCaseData: TestCaseDTO (complete case copy)
- status: UNTESTED | PASSED | FAILED | SKIPPED
- createdAt: Timestamp

### TestRunStep (Snapshot)
- id: UUID (snapshot step ID)
- testRunCaseId: UUID (reference to snapshot case)
- originalStepId: UUID (reference to original step)
- snapshotStepData: TestStepDTO (complete step copy)
- status: UNTESTED | PASSED | FAILED | SKIPPED
- executedBy: String (tester username)
- executedAt: Timestamp
- evidence: List<String> (attachment URLs)
- createdAt, updatedAt: Timestamp

### StepStatusAudit
- id: UUID
- stepId: UUID
- previousStatus: Enum
- newStatus: Enum
- changedBy: String (username)
- changedAt: Timestamp
- reason: String (optional)

---

## API Endpoints Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /projects/{id}/runs | Create test run |
| GET | /projects/{id}/runs | List all runs |
| GET | /projects/{id}/runs/{id} | Get run details |
| PUT | /projects/{id}/runs/{id} | Update run |
| PUT | /projects/{id}/runs/{id}/steps/{stepId} | Update step status |
| POST | /projects/{id}/runs/{id}/complete | Complete and lock |
| POST | /projects/{id}/runs/{id}/unlock | Unlock run |
| GET | /projects/{id}/runs/{id}/metrics | Get metrics |
| GET | /projects/{id}/runs/{id}/audit | Get audit trail |

---

## Implementation Priority

### Phase 1 (CRITICAL - Blocks execution)
1. Create TestRunCase and TestRunStep entities/DTOs
2. Implement SnapshotProcessor (copy cases + steps)
3. Implement step status update endpoint + logic
4. Implement case status auto-calculation

### Phase 2 (IMPORTANT - Core functionality)
5. Implement run completion logic
6. Implement run unlocking
7. Implement metrics aggregation
8. Add audit trail support

### Phase 3 (NICE-TO-HAVE)
9. Evidence/attachments
10. Defect linking
11. Detailed reporting

---

## Testing Requirements

- Unit tests for SnapshotProcessor
- Unit tests for status auto-calculation logic
- Integration tests for step update workflow
- Integration tests for run completion
- Controller tests for all endpoints
- Edge case tests (empty runs, all skipped, etc.)

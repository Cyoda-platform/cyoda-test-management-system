# Module 3: Test Execution - Implementation & Fixes TODO List

## Current Status
- ✅ 60% Implemented
- ⚠️ 20% Partially Working (needs fixes)
- ❌ 20% Not Verified/Tested

---

## CRITICAL FIXES NEEDED

### FIX #1: Evidence Display Separation (BLOCKING)
**Priority:** CRITICAL
**Effort:** 2-3 hours
**What:** Separate TestCase attachments from TestRunStep evidence in UI

**Steps:**
1. [ ] Identify where TestCase attachments display in test run detail
2. [ ] Verify TestRunStep.evidence array structure in database
3. [ ] Create separate "Test Evidence" section in UI
4. [ ] Query both sources: TestCase AND TestRunStep
5. [ ] Display with clear labels:
   - "Case Attachments" (from repository)
   - "Test Evidence" (from this run's execution)
6. [ ] Test upload/retrieve evidence in test run
7. [ ] Test case attachments still visible

### FIX #2: Evidence Persistence Verification
**Priority:** CRITICAL
**Effort:** 1-2 hours
**What:** Ensure evidence uploads persist and retrieval works

**Steps:**
1. [ ] Check TestRunStep entity has evidence field
2. [ ] Verify API stores evidence URLs with step
3. [ ] Verify API returns evidence on GET step
4. [ ] Test upload → reload → evidence still there
5. [ ] Test evidence not mixed with case attachments

### FIX #3: Run Lock/Unlock Verification
**Priority:** HIGH
**Effort:** 1 hour
**What:** Test completion and lock functionality

**Steps:**
1. [ ] Create test run
2. [ ] Execute some steps
3. [ ] Complete run (POST .../complete)
4. [ ] Verify run locked (cannot modify)
5. [ ] Unlock run (POST .../unlock)
6. [ ] Verify can modify again
7. [ ] Check audit trail for these events

---

## VERIFICATION TESTS NEEDED

### Module 3.1: Run Initialization
- [ ] Create run with suite selection
- [ ] Verify all fields saved (name, env, build, desc)
- [ ] Verify snapshot created with correct cases
- [ ] Verify original + snapshot IDs stored
- [ ] Verify run status = active after creation

### Module 3.2: Step Execution & Status
- [ ] Mark step Passed → verify saved
- [ ] Mark step Failed → verify saved
- [ ] Mark step Skipped → verify saved
- [ ] Verify case status auto-calculated:
  - Any Failed → Case Failed
  - All Passed → Case Passed
  - Some/All Skipped → Case Skipped
- [ ] Verify cannot change after run locked

### Module 3.3: Defect Management
- [ ] Failed step triggers defect dialog ✅ (working)
- [ ] Defect links to test case result ✅ (working)
- [ ] Defect has correct fields (title, severity, status)
- [ ] Defect link visible in run report

### Module 3.4: Progress Metrics
- [ ] Progress bar updates real-time ✅ (working)
- [ ] Counters correct (passed, failed, skipped, untested)
- [ ] Success rate calculated correctly
- [ ] Metrics consistent across list and detail views

### Module 3.5: Run Completion
- [ ] Complete run endpoint works
- [ ] Run becomes read-only after completion
- [ ] Cannot modify locked run
- [ ] Unlock works for admin
- [ ] Unlock allows modifications again
- [ ] Audit trail records completion + unlock

---

## NEW FEATURES NEEDED

### Audit Trail Display
**Priority:** MEDIUM
**Effort:** 2-3 hours
**What:** Show who changed what when for each step

- [ ] API endpoint: GET /projects/{id}/runs/{id}/audit
- [ ] Returns: user, timestamp, step, old_status, new_status
- [ ] Display audit log in UI
- [ ] Filter/sort options (by date, user, step)

### Detailed Run Report
**Priority:** MEDIUM
**Effort:** 2-3 hours
**What:** Exportable run report

- [ ] Endpoint: GET /projects/{id}/runs/{id}/report
- [ ] Returns PDF/CSV with:
  - Run summary
  - All cases + statuses
  - Failed cases with details
  - Defects linked
- [ ] Download button in UI

---

## Testing Requirements

### Unit Tests Needed
- [ ] SnapshotProcessor logic
- [ ] Case status auto-calculation
- [ ] Evidence file association
- [ ] Defect creation from step failure
- [ ] Metrics calculation

### Integration Tests Needed
- [ ] Full test run workflow (create → execute → complete)
- [ ] Evidence upload + retrieve
- [ ] Run lock/unlock
- [ ] Audit trail recording

### Manual Tests Needed
- [ ] Test UI responsiveness with many cases
- [ ] Test evidence upload with large files
- [ ] Test run completion with metrics
- [ ] Test unlock functionality

---

## Timeline Estimate
- Critical Fixes: 4-5 hours
- Verification Tests: 2-3 hours
- New Features: 4-6 hours
- Manual Testing: 1-2 hours
- **Total: 11-16 hours of work**

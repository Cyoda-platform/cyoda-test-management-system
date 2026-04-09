# Module 3: Test Coverage Analysis

## Current Test Status

**Overall Coverage:** 🔴 **30%** - Critical gaps

**Test Files Existing:**
- ✅ TestRunServiceTest.java (4 tests)

**Test Files Missing:**
- ❌ TestRunControllerTest.java
- ❌ TestRunCaseServiceTest.java
- ❌ TestRunStepServiceTest.java
- ❌ StepStatusUpdateTest.java
- ❌ AtomicFailureProcessorTest.java
- ❌ SnapshotProcessorTest.java
- ❌ DefectServiceTest.java
- ❌ DefectControllerTest.java
- ❌ StatusCalculationTest.java
- ❌ MetricsAggregationTest.java

---

## Requirements vs Tests Coverage

### FR 3.1: Run Initialization
**Requirement:** Create TestRun with fields, snapshot creation
- ✅ testCreateTestRun() - basic creation
- ❌ testCreateTestRunWithAllFields() - missing (env, buildVersion, desc)
- ❌ testSnapshotCreated() - NO TEST
- ❌ testSnapshotContainsAllCases() - NO TEST
- ❌ testSnapshotContainsAllSteps() - NO TEST
- ❌ testDisplayIdGenerated() - NO TEST
- ❌ testStartedAtRecorded() - partial

**Coverage:** 🔴 20% (1 basic test, missing advanced)

### FR 3.2: Step-Level Execution
**Requirement:** Mark steps Passed/Failed/Skipped, persist status
- ❌ testUpdateStepToPassed() - NO TEST
- ❌ testUpdateStepToFailed() - NO TEST
- ❌ testUpdateStepToSkipped() - NO TEST
- ❌ testStatusChangeImmediate() - NO TEST
- ❌ testStatusPersisted() - NO TEST
- ❌ testUpdateStepReturnsUpdatedCase() - NO TEST

**Coverage:** 🔴 0% - CRITICAL GAP

### FR 3.3: Atomic Failure Logic
**Requirement:** Case status auto-calculated from steps
- ❌ testAnyStepFailedCaseFails() - NO TEST
- ❌ testAllStepsPassedCasePasses() - NO TEST
- ❌ testSkippedStepsCalculation() - NO TEST
- ❌ testUntestedStepsCalculation() - NO TEST
- ❌ testMixedStatusCalculation() - NO TEST

**Coverage:** 🔴 0% - CRITICAL GAP

### FR 3.4: Execution Artifacts (Evidence)
**Requirement:** Upload files to TestRunStep
- ❌ testUploadEvidenceToStep() - NO TEST
- ❌ testMultipleFilesPerStep() - NO TEST
- ❌ testEvidenceRetrieved() - NO TEST
- ❌ testEvidenceNotMixedWithCaseAttachments() - NO TEST

**Coverage:** 🔴 0% - CRITICAL GAP

### FR 3.5: Defect Linking
**Requirement:** Link defects to failed steps
- ❌ testCreateDefectFromFailedStep() - NO TEST
- ❌ testDefectLinkedToTestRunCase() - NO TEST
- ❌ testDefectPropertiesStored() - NO TEST
- ❌ testLinkMultipleDefects() - NO TEST

**Coverage:** 🔴 0% - CRITICAL GAP

### FR 3.6: Run Locking & Unlocking
**Requirement:** Lock on completion, unlock for admin
- ❌ testCompleteRunLocks() - NO TEST
- ❌ testCompletedRunIsReadOnly() - NO TEST
- ❌ testUnlockRun() - NO TEST
- ❌ testUnlockedRunIsEditable() - NO TEST
- ❌ testOnlyAdminCanUnlock() - NO TEST

**Coverage:** 🔴 0% - CRITICAL GAP

### FR 3.7: Test Run Metrics
**Requirement:** Real-time passed/failed/skipped/untested counts
- ❌ testMetricsUpdatedOnStatusChange() - NO TEST
- ❌ testPassedCountCorrect() - NO TEST
- ❌ testFailedCountCorrect() - NO TEST
- ❌ testSkippedCountCorrect() - NO TEST
- ❌ testUntestedCountCorrect() - NO TEST
- ❌ testSuccessRateCalculated() - NO TEST
- ❌ testProgressBarPercentage() - NO TEST

**Coverage:** 🔴 0% - CRITICAL GAP

### FR 3.8: Test Run Details Endpoint
**Requirement:** GET /projects/{id}/runs/{id} with cases + steps
- ✅ testGetTestRunById() - basic retrieval
- ❌ testRunDetailsIncludeCases() - NO TEST
- ❌ testRunDetailsIncludeSteps() - NO TEST
- ❌ testBothSnapshotAndOriginalIds() - NO TEST

**Coverage:** 🔴 25% (basic only)

### FR 3.9: Audit Trail
**Requirement:** Record who changed status when, previous value
- ❌ testAuditTrailRecorded() - NO TEST
- ❌ testAuditIncludesUser() - NO TEST
- ❌ testAuditIncludesTimestamp() - NO TEST
- ❌ testAuditIncludesPreviousStatus() - NO TEST

**Coverage:** 🔴 0% - CRITICAL GAP

---

## Tests to Create (Priority Order)

### CRITICAL (Blocks functionality)

**1. Step Status Update Tests**
- [ ] testUpdateStepToPassed()
- [ ] testUpdateStepToFailed()
- [ ] testUpdateStepToSkipped()
- [ ] testStatusUpdateReturnsUpdatedCase()
- Effort: 2 hours

**2. Atomic Failure Logic Tests**
- [ ] testAnyStepFailedCaseFails()
- [ ] testAllStepsPassedCasePasses()
- [ ] testSkippedStepsLogic()
- [ ] testMixedStatusCalculation()
- Effort: 2 hours

**3. Snapshot Creation Tests**
- [ ] testSnapshotCreatedWithAllCases()
- [ ] testSnapshotCreatedWithAllSteps()
- [ ] testSnapshotIsImmutable()
- [ ] testDisplayIdGenerated()
- Effort: 2 hours

### IMPORTANT

**4. Defect Tests** (2 hours)
**5. Metrics Tests** (2 hours)
**6. Lock/Unlock Tests** (1.5 hours)
**7. Evidence Tests** (1.5 hours)

### NICE-TO-HAVE

**8. Audit Trail Tests** (1 hour)
**9. Edge Case Tests** (1.5 hours)

---

## Timeline to Full Coverage

- Create CRITICAL tests: **6 hours**
- Create IMPORTANT tests: **8 hours**
- Create NICE-TO-HAVE tests: **2.5 hours**
- **Total: ~16 hours**

## Test Strategy

Use existing TestRunServiceTest as template:
- Mock EntityService
- Use TestRunDTO builders
- Test both positive + negative cases
- Test edge cases (empty lists, null values, etc.)

# Module 3: Test Implementation Plan
## REQUIREMENTS-DRIVEN APPROACH

---

## ⚠️ IMPORTANT: Test Mapping Rules

**Each test MUST map to ONE Acceptance Criterion from User Stories**

- Source: `userstories.md` → Module 3 User Stories (US 3.1-3.10)
- Test Name = AC Description
- Test Code = Verification of AC
- One AC = One @Test method

---

## Current State
- ✅ TestRunServiceTest.java (4 tests)
- ❌ All Module 3 tests missing (45 tests needed)

---

## Phase 1: CRITICAL (6 hours, 16 tests)

### File 1: TestRunInitializationTest.java (6 tests)
**Source:** US 3.1 Acceptance Criteria
```
✓ testSelectCasesForRun() - AC: Can select suites/cases
✓ testDisplayIdGenerated() - AC: displayId auto-generated
✓ testNameValidation() - AC: Name required, max 255
✓ testSnapshotCreated() - AC: Snapshot created immediately
✓ testInitialStatus() - AC: Status = draft → active
✓ testStartedAtRecorded() - AC: startedAt timestamp recorded
```

### File 2: StepStatusTest.java (5 tests)
**Source:** US 3.2 Acceptance Criteria
```
✓ testMarkStepAsPassed() - AC: Mark steps Passed
✓ testMarkStepAsFailed() - AC: Mark steps Failed
✓ testMarkStepAsSkipped() - AC: Mark steps Skipped
✓ testImmediateUpdate() - AC: Status change immediate
✓ testPersistStatus() - AC: Status persisted
```

### File 3: CaseStatusCalculationTest.java (5 tests)
**Source:** US 3.2, 3.8 Acceptance Criteria
```
✓ testAnyStepFailedMakesCaseFailed() - AC: Any Failed → Case Failed
✓ testAllStepsPassedMakesCasePassed() - AC: All Passed → Case Passed
✓ testSkippedStepsLogic() - AC: Skipped → Case Skipped
✓ testUntestedLogic() - AC: No steps → Untested
✓ testMixedStatusCalculation() - AC: Mixed status handling
```

---

## Phase 2: IMPORTANT (8 hours, 20 tests)

### File 4: DefectLinkingTest.java (4 tests)
**Source:** US 3.9 Acceptance Criteria

### File 5: MetricsTest.java (7 tests)
**Source:** US 3.5, 3.7 Acceptance Criteria

### File 6: RunLockingTest.java (5 tests)
**Source:** US 3.4 Acceptance Criteria

### File 7: EvidenceUploadTest.java (4 tests)
**Source:** US 3.3 Acceptance Criteria

---

## Phase 3: NICE-TO-HAVE (2.5 hours, 8 tests)

### File 8: AuditTrailTest.java (4 tests)
**Source:** US 3.10 Acceptance Criteria

### File 9: EdgeCasesTest.java (4 tests)
**Source:** General edge cases from requirements

---

## Success Criteria

1. ✅ Each test has @DisplayName matching AC description
2. ✅ Test code directly verifies AC requirement
3. ✅ All tests compile without errors
4. ✅ All tests pass
5. ✅ One test = One AC (not grouped)
6. ✅ References to US in comments/javadoc

Timeline: ~20 hours total (3 days intensive or 1 week part-time)

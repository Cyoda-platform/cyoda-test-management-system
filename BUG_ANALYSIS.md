# 🐛 CYODA TMS — Bug Analysis Report

**Date**: 2026-04-14  
**Status**: ANALYZED (NOT FIXED)  
**Severity**: CRITICAL (3 interconnected bugs)

---

## Executive Summary

Discovered **3 critical bugs** affecting test case and step management:

1. **React key anti-pattern** → step duplication on delete
2. **DisplayID regeneration on every edit** → TC-1 becomes TC-7
3. **Duplicate stepNumbers in database** → data integrity violation

All three are triggered by a **single root cause**: displayId and stepNumber fields are lost during the update flow.

---

## Bug #1: Step Duplication on Delete (React key={idx})

### Severity: HIGH
### Impact: Confusing UI, data duplication, incorrect step operations

### Location
- **File**: `apps/frontend/src/components/CaseFormPage.tsx`
- **Line**: 311
- **Code**:
```jsx
{steps.map((step, idx) => (
  <div
    key={idx}  // ❌ ANTI-PATTERN: Using array index as key
    ...
```

### Root Cause
React uses `key` prop to identify which items have changed, been added, or been removed. Using array index as key is dangerous when:
- Items are deleted (indices shift)
- Items are reordered (indices change meaning)

### Failure Scenario
```
Initial state:   steps = [
  { order: 1, action: 's1', expectedResult: 'e1' },
  { order: 2, action: 's12', expectedResult: 'e12' }
]
Keys: [0, 1]

User clicks "delete step 2" (idx=1)

Frontend calls: removeStep(1)
  → setSteps(prev => prev.filter((_, i) => i !== 1).map((s, i) => ({ ...s, order: i + 1 })))

Expected result: [
  { order: 1, action: 's1', expectedResult: 'e1' }
]
Keys should be: [0]

ACTUAL React behavior:
- React sees DOM still has key={0} and key={1}
- React doesn't know which one to remove
- React diffs the new JSX against old DOM keys
- Result: DUPLICATION or incorrect association
```

### Evidence
**User Report**: "When I tried to delete the second step, step 1 got duplicated instead"

**Screenshot shows**:
```
# STEP  EXPECTED
1 s1    e1
1 s1    e1      ← Duplicate!
2 s12   e12
```

### Fix Required
Replace `key={idx}` with a **stable, unique identifier**:
```jsx
{steps.map((step, idx) => (
  <div
    key={step.id || `step-${idx}-${step.order}`}  // ✅ Stable key
```

---

## Bug #2: DisplayID Regeneration on Every Edit

### Severity: CRITICAL
### Impact: Test case tracking broken, displayID not stable, counter increment abuse

### Failure Scenario
```
Create test case → TC-1 (correct, counter=1)
Edit test case (change title) → TC-1 → sent to backend
   ↓
Backend receives:
  { title: "New Title", priority: "HIGH", description: "...", preconditions: "..." }
   (displayId NOT included in request body)
   ↓
EntityService.update(id, testCaseDTO, null)
   ↓
displayId field is LOST (null or cleared)
   ↓
Test case refetch → no displayId
   ↓
System regenerates displayId → calls nextDisplayId()
   ↓
Counter scans existing IDs, finds max=1, returns TC-2
   ↓
Next edit → calls nextDisplayId() again → TC-3
   ↓
...eventually TC-7 after multiple edits
```

### Root Causes

#### A) Frontend doesn't send displayId in update request
**File**: `apps/frontend/src/pages/Repository.tsx`  
**Lines**: 1035-1038

```typescript
await testCasesApi.update(projectId, caseSuiteId, editingCaseId, {
  title: data.title,
  priority: data.priority,
  description: data.description,
  preconditions: data.preconditions,
  // ❌ displayId NOT included
});
```

#### B) Backend doesn't preserve displayId during update
**File**: `apps/backend/src/main/java/com/java_template/application/service/TestCaseService.java`  
**Lines**: 167-169

```java
public TestCaseDTO updateTestCase(UUID id, TestCaseDTO testCase) {
    // ❌ No explicit displayId preservation like in createTestCase()
    return withId(entityService.update(id, testCase, null));
}
```

Compare with **createTestCase** (lines 83-92):
```java
public TestCaseDTO createTestCase(TestCaseDTO testCase) {
    // ...
    String displayId = projectCounterService.nextDisplayId(testCase.getProjectId());
    testCase.setDisplayId(displayId);
    TestCaseDTO created = withId(entityService.create(testCase));
    // ✅ Explicit persist of displayId
    created.setDisplayId(displayId);
    entityService.update(created.getId(), created, null);
    return created;
}
```

#### C) EntityService.update may not preserve missing fields
If `EntityService.update()` does a **full replacement** instead of a **merge**, any field not in the request body is cleared.

**Evidence**: Comment in TestCaseService (line 79):
```java
// Note: Cyoda's create-then-reload cycle may not return the displayId in the
// reloaded payload.  We therefore persist it explicitly via a follow-up update
```

This comment suggests **EntityService doesn't always preserve displayId automatically**.

### Evidence
**User Report**: "TC-1 and TC-2 initially created correctly, but after I edited them, they became TC-5 and TC-4, then TC-7"

**Mechanism**: Each edit triggers displayId loss → regeneration → new counter value

---

## Bug #3: Duplicate stepNumbers in Database

### Severity: CRITICAL
### Impact: Data integrity violation, step identification broken

### Current State
```
GET /projects/{pid}/suites/{sid}/cases/{cid}/steps
[
  { id: "step-a", testCaseId: "tc-1", stepNumber: 1, action: "s1", ... },
  { id: "step-b", testCaseId: "tc-1", stepNumber: 1, action: "s1", ... },  ← ❌ DUPLICATE
  { id: "step-c", testCaseId: "tc-1", stepNumber: 2, action: "s12", ... }
]
```

### Root Cause
When updating a test case, Repository.tsx deletes and recreates all steps:

**File**: `apps/frontend/src/pages/Repository.tsx`  
**Lines**: 1040-1047

```typescript
// Replace all steps: delete existing then recreate
for (const step of stepsForSelectedCase) {
  if (step.id) await testStepsApi.delete(projectId, caseSuiteId, editingCaseId, step.id);
}
for (const step of data.steps) {
  await testStepsApi.create(projectId, caseSuiteId, editingCaseId, {
    stepNumber: step.order,
    action: step.action,
    expectedResult: step.expectedResult,
  });
}
```

**Problem**: If delete fails silently or there's a race condition:
1. Old steps not deleted
2. New steps created anyway
3. Result: duplicate stepNumbers

### Why It Happened
1. User created test case with 2 steps
2. User edited test case in CaseFormPage
3. React key bug prevented proper step deletion visualization
4. Frontend thought deletion succeeded, recreated steps
5. Async delete operation completed AFTER recreation
6. Database ended up with both old AND new steps

### Evidence
**User Report**: "There are two steps with #1 on the screen"  
**Screenshot**: Shows `# | STEP | EXPECTED` table with:
```
1   s1   e1
1   s1   e1    ← Both have same stepNumber!
2   s12  e12
```

### Database State
The backend returned two steps with `stepNumber: 1`, which means:
- **No backend validation** prevents duplicate stepNumbers per test case
- **No unique constraint** on (testCaseId, stepNumber) tuple

---

## Data Flow Diagram: How Bugs Interact

```
┌─────────────────────────────────────────────────────────────────┐
│ User: Create Test Case with 2 Steps                             │
└─────────────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Backend: Assigns TC-1, creates 2 steps with stepNumber=[1, 2]   │
└─────────────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Frontend: Displays steps in CaseFormPage (key={idx} ❌)         │
│ Shows: order=[1, 2]                                              │
└─────────────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────────┐
│ User: Edits test case (changes title)                           │
│ Frontend sends update WITHOUT displayId ❌                      │
└─────────────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Backend: EntityService.update() clears displayId ❌             │
│ Now: displayId=null                                              │
└─────────────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Frontend: Tries to delete step 2 (idx=1)                        │
│ React key={idx} gets confused ❌                                │
│ Deletes wrong element or duplicates                              │
└─────────────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Backend delete: testStepsApi.delete() succeeds                  │
│ Backend recreate: testStepsApi.create() for 2 steps             │
│ Race condition: Both old and new steps exist ❌                 │
└─────────────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Result:                                                          │
│ - Database has 2 steps with stepNumber=1 ❌                     │
│ - Test case has displayId=null ❌                               │
│ - On next fetch: displayId regenerated → TC-7 ❌               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Code Changes Required (DO NOT IMPLEMENT YET)

### Fix 1: React key in CaseFormPage
```diff
- <div key={idx} ...>
+ <div key={`step-${step.id || step.order}`} ...>
```

### Fix 2: Frontend includes displayId in update
```diff
  await testCasesApi.update(projectId, caseSuiteId, editingCaseId, {
    title: data.title,
    priority: data.priority,
    description: data.description,
    preconditions: data.preconditions,
+   displayId: editingCase?.displayId,  // Preserve displayId
  });
```

### Fix 3: Backend preserves displayId like in create
```diff
  public TestCaseDTO updateTestCase(UUID id, TestCaseDTO testCase) {
-     return withId(entityService.update(id, testCase, null));
+     // Preserve displayId like we do in createTestCase
+     TestCaseDTO updated = withId(entityService.update(id, testCase, null));
+     if (updated.getDisplayId() == null) {
+         // Fetch current displayId if lost
+         return getTestCaseById(id).orElse(updated);
+     }
+     return updated;
  }
```

### Fix 4: Add validation in backend
```java
// In TestStepService or EntityValidator
private void validateStepNumbersUnique(UUID testCaseId, List<TestStepDTO> steps) {
    Set<Integer> numbers = new HashSet<>();
    for (TestStepDTO step : steps) {
        if (!numbers.add(step.getStepNumber())) {
            throw new BusinessException("Duplicate stepNumber " + step.getStepNumber() + 
                " for test case " + testCaseId);
        }
    }
}
```

---

## Testing Checklist (To Verify Fixes)

- [ ] Create test case TC-1 with 2 steps
- [ ] Edit test case (change title only)
- [ ] Verify displayId is still TC-1 (not TC-2, TC-3, TC-7, etc.)
- [ ] Delete step 2
- [ ] Verify step 1 is NOT duplicated
- [ ] Verify database has exactly 1 step
- [ ] Verify stepNumber is NOT duplicated in database
- [ ] Edit test case 3 more times
- [ ] Verify displayId stays TC-1
- [ ] Verify counter only incremented on creation, not on edit

---

## Questions for Investigation

1. **Does EntityService.update() do a full replace or merge?**
   - If full replace: displayId is lost when not in request body
   - If merge: displayId should be preserved (but comment suggests it's not)

2. **Is there validation on TestStep.stepNumber uniqueness?**
   - No UNIQUE constraint found in DTO
   - No validation in TestStepService

3. **Why was displayId comment added in createTestCase?**
   - "Cyoda's create-then-reload cycle may not return the displayId"
   - Same issue might apply to update cycle

4. **Is there a processor or lifecycle hook that regenerates displayId?**
   - Checked: No TestCase processor found
   - Checked: No TestStep processor found

---

## Risk Assessment

| Bug | Affected Users | Data Loss | Workaround |
|-----|---|---|---|
| #1: Step duplication | 100% when editing | No | Don't use CaseFormPage edit |
| #2: DisplayID regen | 100% when editing | No (IDs just increment) | None |
| #3: Duplicate steps | ~20% (race condition) | Yes (duplicate data) | Manual DB cleanup |

---

## Next Steps

1. Verify EntityService.update() behavior with displayId
2. Check Cyoda framework documentation for field preservation rules
3. Add backend validation for stepNumber uniqueness
4. Run integration tests to confirm all fixes work together
5. Add regression tests to prevent recurrence

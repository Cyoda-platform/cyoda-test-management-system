# Auto-Advance to Next Test Case

**Date:** 2026-05-04
**Status:** Approved

---

## Context

During test run execution, after a tester fills in all step statuses for a case, they must manually click the next case in the sidebar. This spec adds automatic progression to the next case when the current one reaches a terminal non-failed status.

---

## Behaviour

When the **last untested step** of a case is given any status, the system computes the new case-level status. If:

- All steps are now non-`untested`, **AND**
- The computed case status is `passed` or `skipped` (not `failed`, not `untested`)

→ after a **600 ms delay**, automatically select the next case (`selectedIdx + 1`).

The delay lets the tester see the final step status before the view moves.

### Cases that do NOT trigger auto-advance

| Situation | Reason |
|---|---|
| Case status = `failed` | Tester may need to log a defect, review evidence |
| Not all steps filled yet | Case is still in progress |
| Already on last case | No next case to advance to |
| Case has no steps | `computeCaseStatus([])` returns `'untested'`; condition never met |

---

## Implementation

**Single file change:** `apps/frontend/src/pages/RunExecution.tsx`

Inside `setStepStatus`, after the local `stepStatuses` state is updated and `computeCaseStatus` is called, add:

```ts
const newStatuses = updatedStepStatuses[caseId];
const allFilled = newStatuses.every(s => s !== 'untested');
const newCaseStatus = computeCaseStatus(newStatuses);

if (allFilled && newCaseStatus !== 'failed' && newCaseStatus !== 'untested') {
  if (selectedIdx < allCases.length - 1) {
    setTimeout(() => setSelectedIdx(selectedIdx + 1), 600);
  }
}
```

No changes to server persistence, defect modal, or any other logic.

---

## Testing

One new vitest test in `apps/frontend/src/test/` verifying:

1. When last step is set to `passed` and all steps are now non-untested → `setSelectedIdx` called with `selectedIdx + 1` after 600 ms
2. When last step is set to `skipped` and all steps are now non-untested → same
3. When a step is set to `failed` → `setSelectedIdx` not called
4. When not all steps are filled → `setSelectedIdx` not called
5. When on last case (`selectedIdx === allCases.length - 1`) → `setSelectedIdx` not called

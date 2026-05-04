# Auto-Advance Test Case Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the last step of a test case is given a non-failed status, automatically select the next case in the run after a 600 ms delay.

**Architecture:** Extract the advance-decision logic into a pure helper `shouldAutoAdvance` in `src/lib/runUtils.ts` (testable in isolation). Import and call it inside `setStepStatus` in `RunExecution.tsx` after step-status state is updated. No backend changes.

**Tech Stack:** React 18 + TypeScript, Vitest.

---

## Files

| File | Change |
|---|---|
| `apps/frontend/src/lib/runUtils.ts` | Create — pure `shouldAutoAdvance` helper |
| `apps/frontend/src/test/runUtils.test.ts` | Create — vitest unit tests |
| `apps/frontend/src/pages/RunExecution.tsx` | Modify — import helper, add auto-advance call in `setStepStatus` |

---

## Task 1: Write failing tests for `shouldAutoAdvance` (TDD red)

**Files:**
- Create: `apps/frontend/src/test/runUtils.test.ts`

- [ ] **Step 1.1: Create the test file**

Create `apps/frontend/src/test/runUtils.test.ts` with this content:

```ts
import { describe, it, expect } from 'vitest';
import { shouldAutoAdvance } from '@/lib/runUtils';

describe('shouldAutoAdvance', () => {
  it('returns true when all steps passed and not last case', () => {
    expect(shouldAutoAdvance(['passed', 'passed', 'passed'], 0, 5)).toBe(true);
  });

  it('returns true when all steps skipped and not last case', () => {
    expect(shouldAutoAdvance(['skipped', 'skipped'], 2, 5)).toBe(true);
  });

  it('returns true when steps are mixed passed+skipped and not last case', () => {
    expect(shouldAutoAdvance(['passed', 'skipped', 'passed'], 1, 3)).toBe(true);
  });

  it('returns false when any step is failed', () => {
    expect(shouldAutoAdvance(['passed', 'failed', 'passed'], 0, 5)).toBe(false);
  });

  it('returns false when not all steps are filled (untested present)', () => {
    expect(shouldAutoAdvance(['passed', 'untested', 'passed'], 0, 5)).toBe(false);
  });

  it('returns false when on last case', () => {
    expect(shouldAutoAdvance(['passed', 'passed'], 4, 5)).toBe(false);
  });

  it('returns false for empty statuses array', () => {
    expect(shouldAutoAdvance([], 0, 5)).toBe(false);
  });
});
```

- [ ] **Step 1.2: Run tests and confirm they FAIL**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run test -- --run src/test/runUtils.test.ts 2>&1 | tail -15
```

Expected: all 7 tests FAIL with "Cannot find module '@/lib/runUtils'".

---

## Task 2: Implement `shouldAutoAdvance` (TDD green)

**Files:**
- Create: `apps/frontend/src/lib/runUtils.ts`

- [ ] **Step 2.1: Create the helper**

Create `apps/frontend/src/lib/runUtils.ts`:

```ts
/**
 * Returns true if the test run executor should automatically advance to the
 * next test case after setting a step status.
 *
 * Conditions for auto-advance:
 * - All steps have a non-untested status
 * - No step is failed (failed cases require defect logging — stay put)
 * - The current case is not the last one in the run
 */
export function shouldAutoAdvance(
  statuses: string[],
  selectedIdx: number,
  totalCases: number
): boolean {
  if (statuses.length === 0) return false;
  if (selectedIdx >= totalCases - 1) return false;
  const allFilled = statuses.every((s) => s !== 'untested');
  if (!allFilled) return false;
  return !statuses.some((s) => s === 'failed');
}
```

- [ ] **Step 2.2: Run tests and confirm they all PASS**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run test -- --run src/test/runUtils.test.ts 2>&1 | tail -15
```

Expected: 7 tests pass, BUILD SUCCESSFUL.

- [ ] **Step 2.3: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/frontend/src/lib/runUtils.ts \
        apps/frontend/src/test/runUtils.test.ts
git commit -m "$(cat <<'EOF'
feat: add shouldAutoAdvance helper with tests

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Wire auto-advance into RunExecution.tsx

**Files:**
- Modify: `apps/frontend/src/pages/RunExecution.tsx`

The function `setStepStatus` lives around line 766. After updating local state (step 1 in the function), computing `updated` (line 771), and calling `updateTestRunCaseStatus.mutate()` (around line 821), we add the auto-advance call.

- [ ] **Step 3.1: Add the import**

Find the existing imports at the top of `RunExecution.tsx`. Add after the last `@/lib/...` import:

```ts
import { shouldAutoAdvance } from '@/lib/runUtils';
```

- [ ] **Step 3.2: Add auto-advance call inside `setStepStatus`**

In `setStepStatus`, after the `// ── 5. Auto-trigger defect modal on 'failed'` block (which ends around line 853), add a new section **between step 4 and step 5** (i.e., after the `updateTestRunCaseStatus.mutate()` call, before the defect modal block):

Find this exact line (end of step 4):
```ts
    }

    // ── 5. Auto-trigger defect modal on 'failed' — opens synchronously ────────
```

Replace with:
```ts
    }

    // ── 5. Auto-advance to next case on pass/skip ─────────────────────────────
    if (shouldAutoAdvance(updated, selectedIdx, allCases.length)) {
      setTimeout(() => setSelectedIdx(selectedIdx + 1), 600);
    }

    // ── 6. Auto-trigger defect modal on 'failed' — opens synchronously ────────
```

Note: the defect modal block comment number becomes 6 but the code inside does not change.

- [ ] **Step 3.3: Run the full frontend test suite**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system/apps/frontend
npm run test -- --run 2>&1 | tail -15
```

Expected: all existing tests still pass, nothing broken.

- [ ] **Step 3.4: Run lint**

```bash
npm run lint 2>&1 | grep "RunExecution.tsx\|runUtils"
```

Expected: no new errors on these files.

- [ ] **Step 3.5: Commit**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
git add apps/frontend/src/pages/RunExecution.tsx
git commit -m "$(cat <<'EOF'
feat: auto-advance to next case after pass/skip in test run

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

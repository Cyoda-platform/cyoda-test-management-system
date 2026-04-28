# UI Persistence: Panel Sizes & Sidebar State — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist Repository panel widths and sidebar collapsed state in localStorage per-project so they survive page refresh.

**Architecture:** A single generic `useLocalStorage<T>` hook reads from localStorage on mount and writes on every setter call. `ProjectLayout` uses it for sidebar state; `Repository` uses it for panel sizes and adds an `onLayout` callback to capture resize events.

**Tech Stack:** React 18, TypeScript, vitest + jsdom + @testing-library/react

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `apps/frontend/src/hooks/useLocalStorage.ts` | Create | Generic localStorage-backed state hook |
| `apps/frontend/src/hooks/useLocalStorage.test.ts` | Create | Unit tests for the hook |
| `apps/frontend/src/components/ProjectLayout.tsx` | Modify | Use hook for sidebar collapsed state |
| `apps/frontend/src/pages/Repository.tsx` | Modify | Use hook for panel sizes + add `onLayout` |

---

## Task 1: `useLocalStorage` hook — test + implementation

**Files:**
- Create: `apps/frontend/src/hooks/useLocalStorage.ts`
- Create: `apps/frontend/src/hooks/useLocalStorage.test.ts`

- [ ] **Step 1.1: Write the failing tests**

Create `apps/frontend/src/hooks/useLocalStorage.test.ts`:

```ts
import { renderHook, act } from '@testing-library/react';
import { beforeEach, describe, it, expect } from 'vitest';
import useLocalStorage from './useLocalStorage';

describe('useLocalStorage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('returns default value when key is absent', () => {
    const { result } = renderHook(() => useLocalStorage('k', 42));
    expect(result.current[0]).toBe(42);
  });

  it('reads existing value from localStorage on mount', () => {
    localStorage.setItem('k', JSON.stringify(99));
    const { result } = renderHook(() => useLocalStorage('k', 42));
    expect(result.current[0]).toBe(99);
  });

  it('setter updates state', () => {
    const { result } = renderHook(() => useLocalStorage('k', 0));
    act(() => result.current[1](7));
    expect(result.current[0]).toBe(7);
  });

  it('setter persists to localStorage', () => {
    const { result } = renderHook(() => useLocalStorage('k', 0));
    act(() => result.current[1](7));
    expect(JSON.parse(localStorage.getItem('k')!)).toBe(7);
  });

  it('setter supports functional update form', () => {
    localStorage.setItem('k', JSON.stringify({ a: 1, b: 2 }));
    const { result } = renderHook(() =>
      useLocalStorage<{ a: number; b: number }>('k', { a: 0, b: 0 })
    );
    act(() => result.current[1](prev => ({ ...prev, a: 99 })));
    expect(result.current[0]).toEqual({ a: 99, b: 2 });
    expect(JSON.parse(localStorage.getItem('k')!)).toEqual({ a: 99, b: 2 });
  });

  it('falls back to default on invalid JSON', () => {
    localStorage.setItem('k', 'not{valid');
    const { result } = renderHook(() => useLocalStorage('k', 'fallback'));
    expect(result.current[0]).toBe('fallback');
  });
});
```

- [ ] **Step 1.2: Run tests — verify they fail**

```bash
cd apps/frontend && npm run test -- useLocalStorage
```

Expected: fail with "Cannot find module './useLocalStorage'"

- [ ] **Step 1.3: Implement the hook**

Create `apps/frontend/src/hooks/useLocalStorage.ts`:

```ts
import { useState, useCallback } from 'react';

function useLocalStorage<T>(
  key: string,
  defaultValue: T,
): [T, (value: T | ((prev: T) => T)) => void] {
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item !== null ? (JSON.parse(item) as T) : defaultValue;
    } catch {
      return defaultValue;
    }
  });

  const setValue = useCallback(
    (value: T | ((prev: T) => T)) => {
      setStoredValue(prev => {
        const next =
          typeof value === 'function' ? (value as (prev: T) => T)(prev) : value;
        try {
          window.localStorage.setItem(key, JSON.stringify(next));
        } catch {
          // private/incognito mode or quota exceeded — state still updates
        }
        return next;
      });
    },
    [key],
  );

  return [storedValue, setValue];
}

export default useLocalStorage;
```

- [ ] **Step 1.4: Run tests — verify they pass**

```bash
cd apps/frontend && npm run test -- useLocalStorage
```

Expected: 6 tests pass, 0 fail.

- [ ] **Step 1.5: Commit**

```bash
git add apps/frontend/src/hooks/useLocalStorage.ts apps/frontend/src/hooks/useLocalStorage.test.ts
git commit -m "feat(frontend): add useLocalStorage hook with per-key persistence"
```

---

## Task 2: Sidebar state persistence — `ProjectLayout.tsx`

**Files:**
- Modify: `apps/frontend/src/components/ProjectLayout.tsx`

- [ ] **Step 2.1: Add the import**

In `apps/frontend/src/components/ProjectLayout.tsx`, replace line 1:
```ts
import { useState } from 'react';
```
with:
```ts
import useLocalStorage from '@/hooks/useLocalStorage';
```

(Remove `useState` from the import — it's no longer used after the next step. If `useState` is used elsewhere in the file, keep it.)

- [ ] **Step 2.2: Replace sidebar state**

Replace line 11:
```ts
const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
```
with:
```ts
const [sidebarCollapsed, setSidebarCollapsed] = useLocalStorage(
  `tms.sidebar.collapsed.${projectId}`,
  false,
);
```

`projectId` is already available on line 10 from `useParams()`.

- [ ] **Step 2.3: Verify the toggle still works**

No change needed to the toggle — `() => setSidebarCollapsed(c => !c)` already passes a function, which the hook's setter supports.

- [ ] **Step 2.4: Type-check**

```bash
cd apps/frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 2.5: Commit**

```bash
git add apps/frontend/src/components/ProjectLayout.tsx
git commit -m "feat(frontend): persist sidebar collapsed state per project in localStorage"
```

---

## Task 3: Panel sizes persistence — `Repository.tsx`

**Files:**
- Modify: `apps/frontend/src/pages/Repository.tsx`

- [ ] **Step 3.1: Add the import**

Near the top of `apps/frontend/src/pages/Repository.tsx`, after the existing hook imports (around line 1), add:

```ts
import useLocalStorage from '@/hooks/useLocalStorage';
```

- [ ] **Step 3.2: Replace hardcoded panelSizes with persisted state**

Find line 417–418:
```ts
  // Fixed panel sizes
  const panelSizes = { left: 15, middle: 50, right: 35 };
```

Replace with:
```ts
  const [panelSizes, setPanelSizes] = useLocalStorage(
    `tms.repository.panelSizes.${projectId}`,
    { left: 15, middle: 50, right: 35 },
  );
```

- [ ] **Step 3.3: Add `onLayout` to `ResizablePanelGroup`**

Find the `ResizablePanelGroup` opening tag (around line 1277):
```tsx
        <ResizablePanelGroup
          direction="horizontal"
          className="h-full"
        >
```

Replace with:
```tsx
        <ResizablePanelGroup
          direction="horizontal"
          className="h-full"
          onLayout={(sizes) => {
            if (sizes.length === 3) {
              setPanelSizes({ left: sizes[0], middle: sizes[1], right: sizes[2] });
            } else if (sizes.length === 2) {
              setPanelSizes(prev => ({ ...prev, left: sizes[0] }));
            }
          }}
        >
```

**Why:** The right panel is conditionally mounted (only when `selectedCase` is set). When absent, `onLayout` fires with 2 values — we update only `left` and leave `middle`/`right` unchanged so they restore correctly when a case is selected.

- [ ] **Step 3.4: Type-check**

```bash
cd apps/frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3.5: Run full test suite**

```bash
cd apps/frontend && npm run test
```

Expected: all existing tests pass.

- [ ] **Step 3.6: Commit**

```bash
git add apps/frontend/src/pages/Repository.tsx
git commit -m "feat(frontend): persist Repository panel sizes per project in localStorage"
```

---

## Manual Smoke Test

After all tasks are complete, verify in browser:

1. Open any project → Repository page.
2. Drag the resizer between left/middle panels to change widths.
3. Refresh the page — widths should be restored.
4. Collapse the left sidebar (PanelLeftClose button).
5. Refresh the page — sidebar should remain collapsed.
6. Open a different project — it should have its own independent panel/sidebar state.
7. Open DevTools → Application → Local Storage and confirm keys like `tms.sidebar.collapsed.{id}` and `tms.repository.panelSizes.{id}` exist with correct values.

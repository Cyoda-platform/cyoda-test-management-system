# UI Persistence: Panel Sizes & Sidebar State

**Date:** 2026-04-27  
**Scope:** Frontend only — Repository page + ProjectLayout  

---

## Problem

Two pieces of UI state reset on every page refresh:

1. **Repository panel widths** — the three-pane split (suites tree / case list / case detail) always resets to hardcoded defaults `{ left: 15, middle: 50, right: 35 }`.
2. **Sidebar collapsed state** — `ProjectLayout` initialises with `useState(false)`, so the sidebar always opens expanded on refresh even if the user had collapsed it.

---

## Solution

Persist both values in `localStorage` under per-project keys, initialised via a shared `useLocalStorage` hook.

---

## Architecture

### Hook: `useLocalStorage<T>`

**File:** `src/hooks/useLocalStorage.ts`

```ts
function useLocalStorage<T>(key: string, defaultValue: T): [T, (value: T) => void]
```

- Reads from `localStorage` on mount; falls back to `defaultValue` on parse error or missing key.
- Setter writes to both React state and `localStorage` atomically.
- Errors are swallowed silently (private/incognito mode, storage quota).

---

### Change 1 — Sidebar (`ProjectLayout.tsx`)

Replace:
```ts
const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
```
With:
```ts
const [sidebarCollapsed, setSidebarCollapsed] = useLocalStorage(
  `tms.sidebar.collapsed.${projectId}`, false
);
```

The toggle callback `() => setSidebarCollapsed(c => !c)` stays unchanged — the hook setter handles persistence.

**Storage key:** `tms.sidebar.collapsed.{projectId}`  
**Value type:** `boolean`

---

### Change 2 — Panel sizes (`Repository.tsx`)

Replace hardcoded:
```ts
const panelSizes = { left: 15, middle: 50, right: 35 };
```
With:
```ts
const [panelSizes, setPanelSizes] = useLocalStorage(
  `tms.repository.panelSizes.${projectId}`,
  { left: 15, middle: 50, right: 35 }
);
```

Add `onLayout` to `ResizablePanelGroup`:
```ts
onLayout={(sizes) => {
  if (sizes.length === 3) {
    setPanelSizes({ left: sizes[0], middle: sizes[1], right: sizes[2] });
  } else if (sizes.length === 2) {
    setPanelSizes(prev => ({ ...prev, left: sizes[0] }));
  }
}}
```

**Why 2 vs 3 sizes:** The right panel is conditionally mounted (only when `selectedCase` is set). When it's absent, `onLayout` fires with 2 values — we update only `left` and leave `middle`/`right` unchanged so the right panel restores to its last saved width when a case is selected.

**Storage key:** `tms.repository.panelSizes.{projectId}`  
**Value type:** `{ left: number; middle: number; right: number }`

---

## Files Changed

| File | Change |
|---|---|
| `src/hooks/useLocalStorage.ts` | New file — generic hook |
| `src/components/ProjectLayout.tsx` | Use hook for sidebar collapsed state |
| `src/pages/Repository.tsx` | Use hook for panel sizes + add `onLayout` |

---

## Non-Goals

- No server-side persistence — localStorage is sufficient.
- No migration of existing saved values — defaults apply for all users on first load.
- No cross-tab sync (`storage` event) — not required.

# Attachments Fix — Design Spec
**Date:** 2026-04-20  
**Status:** Approved

---

## Problem Summary

Five bugs in the Attachments subsystem:

| # | Bug | Root Cause |
|---|-----|-----------|
| 1 | Evidence shows in "Attachments" section on RunExecution page | No `attachmentType` field — all uploads share the same `caseId`-based query |
| 2 | Evidence not isolated to Evidence modal | Same as above |
| 3 | Defect attachments save to case, not defect | `RunExecution.tsx:936` uses `defect.caseId` instead of `created.id` |
| 4 | Image preview broken (401 Unauthorized) | `<img src="...">` does not send `Authorization` header |
| 5 | No Playwright E2E tests for any attachment flow | — |

---

## Scope

- Backend: `AttachmentDTO`, entity schema JSON, `AttachmentController`, `AttachmentService`
- Frontend: `api.ts`, `RunExecution.tsx`, `Attachments.tsx`, new `useAuthenticatedImageUrl` hook
- Tests: 3 new Playwright E2E spec files
- Infrastructure: Cyoda re-import (delete tenant data → delete models → import-schemas.sh)

---

## Data Model Changes

### `AttachmentDTO.java` — 3 new fields

```java
private String attachmentType;  // "CASE" | "EVIDENCE" | "DEFECT"
private UUID   runId;           // set only for EVIDENCE
private String stepKey;         // step number as string, e.g. "2" — set only for EVIDENCE
```

**Defaults:** `attachmentType` defaults to `"CASE"` when not provided. `runId` and `stepKey` are nullable.

### `attachment.json` (entity schema sample)

Add new fields with realistic values so Cyoda infers correct types:

```json
{
  "attachmentType": "CASE",
  "runId": "550e8400-e29b-41d4-a716-446655440010",
  "stepKey": "2"
}
```

The workflow JSON (`Attachment.json`) requires no changes — the FSM states/transitions are unchanged.

---

## Backend Changes

### `AttachmentController` — upload endpoint

`POST /projects/{projectId}/attachments` gains three optional `@RequestParam`:
- `attachmentType` (String, default `"CASE"`)
- `runId` (UUID, optional)
- `stepKey` (String, optional)

These are passed through to `AttachmentService.uploadAttachment()`.

### `AttachmentController` — new Evidence query endpoint

```
GET /projects/{projectId}/attachments/by-run/{runId}/case/{caseId}/step/{stepKey}
```

Returns `List<AttachmentMetadataDTO>` — all Evidence for a specific step in a specific run.

### `AttachmentService`

- `uploadAttachment(projectId, caseId, defectId, file, attachmentType, runId, stepKey)` — sets new fields before save
- `getEvidenceByRunCaseStep(runId, caseId, stepKey)` — queries by `runId` + `caseId` + `stepKey` + `attachmentType = "EVIDENCE"`

---

## Frontend Changes

### `api.ts`

- `attachmentsApi.upload()` — add optional params: `attachmentType`, `runId`, `stepKey`; appended to FormData
- `attachmentsApi.listByRunCaseStep(projectId, runId, caseId, stepKey)` — new method calling the new endpoint
- `Attachment` type — add `attachmentType`, `runId`, `stepKey` fields

### `RunExecution.tsx`

**Fix 1 — Case attachments section (line ~1263):**  
Filter `serverAttachments` to show only `attachmentType === "CASE"` or `attachmentType == null` (null = legacy record created before this migration, treat as CASE).

**Fix 2 — Evidence modal:**  
- Evidence upload: pass `attachmentType: "EVIDENCE"`, `runId: runId!`, `stepKey: String(evidenceTarget.stepIdx + 1)`
- Evidence list in modal: use `useQuery` calling `listByRunCaseStep` instead of local `stepEvidence` state (so Evidence survives page reload)
- Evidence badge count: derive from server query result, not local state

**Fix 3 — Defect attachment upload (line 936):**  
```typescript
// Before:
attachmentsApi.upload(projectId!, f, defect.caseId)
// After:
attachmentsApi.upload(projectId!, f, undefined, created.id, 'DEFECT')
```

### `useAuthenticatedImageUrl` hook (new file)

```typescript
// hooks/useAuthenticatedImageUrl.ts
// Fetches a URL with Authorization header, returns a blob: object URL
// Returns null while loading, revokes the blob URL on unmount
```

Used in:
- `Attachments.tsx` preview modal (`<img src={blobUrl}>`)
- `Attachments.tsx` grid thumbnail (`<img src={blobUrl}>`)
- `RunExecution.tsx` if evidence preview is added in the future

---

## Re-import Procedure

Required because `AttachmentDTO` entity schema gains new fields.

1. **Delete tenant entities** (bottom-up per `SCHEMA_AND_WORKFLOW_IMPORT.md`):  
   TestRunStep → TestRunCase → TestRun → Attachment → Defect → TestStep → TestCase → Suite → Report → ProjectCounter → Project

2. **Unlock + Delete all models**

3. **Update files:**
   - `apps/backend/src/main/resources/entity/attachment/version_1/attachment.json` (add 3 fields)
   - `apps/backend/src/main/java/com/java_template/application/dto/AttachmentDTO.java` (add 3 fields)

4. **Run `./import-schemas.sh`** from `apps/backend/`

---

## E2E Playwright Tests (written first — TDD)

Three new spec files under `apps/frontend/e2e/`:

### `attachments-case.spec.ts`
- Upload a file to a test case → appears in case detail
- Upload a file to a test case → appears in project Attachments page
- Delete an attachment → disappears from both views
- Preview an image attachment → image renders (not 401)

### `attachments-evidence.spec.ts`
- Upload evidence to a step during RunExecution → badge count on step increases
- Reload page → evidence still visible in Evidence modal for that step
- Evidence for step X does NOT appear in the "Attachments" section of RunExecution
- Evidence for run A does NOT appear in Evidence modal of run B (same case, different run)

### `attachments-defect.spec.ts`
- Create a defect with an attached file → file visible in defect view modal
- Open defect edit → existing attachment shown; can add new file; can remove file
- Defect attachment does NOT appear in case Attachments section

---

## Success Criteria

- [ ] All 3 Playwright spec files pass
- [ ] Evidence visible only inside Evidence modal, not in RunExecution Attachments section
- [ ] Defect attachments stored with `defectId`, visible in defect view/edit modals
- [ ] Image preview works (no 401) in both Attachments page and any preview modal
- [ ] `./gradlew :apps:backend:test` passes
- [ ] `./gradlew :apps:backend:build` passes

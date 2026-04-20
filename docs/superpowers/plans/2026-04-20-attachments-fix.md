# Attachments Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 5 attachment bugs: evidence/case mixing, defect attachments wrong parent, broken image preview (401), and add Playwright E2E coverage.

**Architecture:** Add `attachmentType` ("CASE"/"EVIDENCE"/"DEFECT"), `runId`, and `stepKey` to `AttachmentDTO` + entity schema; update service/controller; fix 3 frontend bugs; add `useAuthenticatedImageUrl` hook for authenticated image preview; write Playwright E2E tests.

**Tech Stack:** Java 21 / Spring Boot 3.5 / Mockito JUnit 5 (backend); React 18 / TypeScript / Playwright (frontend); Cyoda EntityService + EdgeMessage (persistence).

---

## File Map

| File | Action | What changes |
|------|--------|-------------|
| `apps/backend/src/main/java/com/java_template/application/dto/AttachmentDTO.java` | Modify | Add 3 fields: `attachmentType`, `runId`, `stepKey` |
| `apps/backend/src/main/java/com/java_template/application/dto/AttachmentMetadataDTO.java` | Modify | Rebuild record to include `attachmentType`, `runId`, `stepKey`, `defectId` |
| `apps/backend/src/main/resources/entity/attachment/version_1/attachment.json` | Modify | Add 3 fields with realistic sample values |
| `apps/backend/src/main/java/com/java_template/application/service/AttachmentService.java` | Modify | Update `uploadAttachment()` signature; add `getEvidenceByRunCaseStep()` |
| `apps/backend/src/main/java/com/java_template/application/controller/AttachmentController.java` | Modify | Add 3 optional params to upload; add new GET endpoint |
| `apps/backend/src/test/java/com/java_template/application/service/AttachmentServiceTest.java` | Create | Unit tests for new service method + upload with type |
| `apps/frontend/src/lib/api.ts` | Modify | Add fields to `Attachment` type; update `upload()`; add `listByRunCaseStep()` |
| `apps/frontend/src/hooks/useAuthenticatedImageUrl.ts` | Create | Fetch image with Bearer token → blob URL |
| `apps/frontend/src/pages/RunExecution.tsx` | Modify | 3 bug fixes: case-only filter, evidence upload with runId+stepKey, defect upload with defectId |
| `apps/frontend/src/pages/Attachments.tsx` | Modify | Replace `<img src>` with authenticated blob URL |
| `apps/frontend/e2e/helpers/api.ts` | Modify | Add `uploadAttachment()`, `listAttachmentsByCase()`, `listAttachmentsByDefect()`, `listEvidenceByStep()` helper functions |
| `apps/frontend/e2e/attachments-case.spec.ts` | Create | Playwright E2E: case attachment upload/delete/preview |
| `apps/frontend/e2e/attachments-evidence.spec.ts` | Create | Playwright E2E: evidence isolation per step/run |
| `apps/frontend/e2e/attachments-defect.spec.ts` | Create | Playwright E2E: defect attachment linkage |

---

## Task 1: Update AttachmentDTO and entity schema

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/dto/AttachmentDTO.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/dto/AttachmentMetadataDTO.java`
- Modify: `apps/backend/src/main/resources/entity/attachment/version_1/attachment.json`

- [ ] **Step 1.1: Add 3 fields to AttachmentDTO**

Open `apps/backend/src/main/java/com/java_template/application/dto/AttachmentDTO.java`.

Add after `private String uploadedAt;`:
```java
private String attachmentType;  // "CASE" | "EVIDENCE" | "DEFECT"
private UUID runId;             // non-null only for EVIDENCE
private String stepKey;         // step number string, e.g. "2" — non-null only for EVIDENCE
```

Full updated class (only the field section, rest unchanged):
```java
private UUID id;
private UUID projectId;
private UUID caseId;
private UUID defectId;
private String fileName;
private String fileType;
private long fileSize;
private String uploadedAt;
private String attachmentType;
private UUID runId;
private String stepKey;
/** ID of the corresponding EdgeMessage in Cyoda that holds the file content */
private UUID messageId;
/** Base64-encoded file content — used as fallback when EdgeMessage is unavailable */
private String content;
```

- [ ] **Step 1.2: Rebuild AttachmentMetadataDTO record**

Replace the entire `AttachmentMetadataDTO.java` content:

```java
package com.java_template.application.dto;

import java.util.UUID;

/**
 * Lightweight response DTO for attachment list and metadata endpoints.
 * Omits {@code content} to avoid bloating responses with base64 data.
 */
public record AttachmentMetadataDTO(
        UUID id,
        UUID projectId,
        UUID caseId,
        UUID defectId,
        String fileName,
        String fileType,
        long fileSize,
        String uploadedAt,
        UUID messageId,
        String attachmentType,
        UUID runId,
        String stepKey
) {
    public static AttachmentMetadataDTO from(AttachmentDTO dto) {
        return new AttachmentMetadataDTO(
                dto.getId(),
                dto.getProjectId(),
                dto.getCaseId(),
                dto.getDefectId(),
                dto.getFileName(),
                dto.getFileType(),
                dto.getFileSize(),
                dto.getUploadedAt(),
                dto.getMessageId(),
                dto.getAttachmentType(),
                dto.getRunId(),
                dto.getStepKey()
        );
    }
}
```

- [ ] **Step 1.3: Update entity schema JSON**

Replace `apps/backend/src/main/resources/entity/attachment/version_1/attachment.json`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "projectId": "550e8400-e29b-41d4-a716-446655440001",
  "caseId": "550e8400-e29b-41d4-a716-446655440003",
  "defectId": "550e8400-e29b-41d4-a716-446655440009",
  "fileName": "checkout_screenshot.png",
  "fileType": "image/png",
  "fileSize": 256000,
  "uploadedAt": "2024-02-15T09:45:30",
  "attachmentType": "CASE",
  "runId": "550e8400-e29b-41d4-a716-446655440010",
  "stepKey": "2",
  "messageId": "550e8400-e29b-41d4-a716-446655440002",
  "content": null
}
```

- [ ] **Step 1.4: Verify compilation**

```bash
./gradlew :apps:backend:compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 1.5: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/dto/AttachmentDTO.java \
        apps/backend/src/main/java/com/java_template/application/dto/AttachmentMetadataDTO.java \
        apps/backend/src/main/resources/entity/attachment/version_1/attachment.json
git commit -m "feat(attachments): add attachmentType/runId/stepKey fields to AttachmentDTO"
```

---

## Task 2: Backend service — write failing test, then implement

**Files:**
- Create: `apps/backend/src/test/java/com/java_template/application/service/AttachmentServiceTest.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/service/AttachmentService.java`

- [ ] **Step 2.1: Write failing unit tests**

Create `apps/backend/src/test/java/com/java_template/application/service/AttachmentServiceTest.java`:

```java
package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.AttachmentDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EdgeMessageService;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentService unit tests")
class AttachmentServiceTest {

    @Mock private EntityService entityService;
    @Mock private EdgeMessageService edgeMessageService;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private AttachmentService attachmentService;

    private UUID projectId;
    private UUID caseId;
    private UUID defectId;
    private UUID runId;

    private EntityWithMetadata<AttachmentDTO> wrap(AttachmentDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        caseId    = UUID.randomUUID();
        defectId  = UUID.randomUUID();
        runId     = UUID.randomUUID();
    }

    @Test
    @DisplayName("uploadAttachment sets attachmentType=CASE when not provided")
    void uploadFile_defaultsToCase() throws Exception {
        UUID attId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "shot.png", "image/png", new byte[]{1, 2, 3});
        when(edgeMessageService.createMessage(any(), any(), any())).thenReturn(UUID.randomUUID());
        when(entityService.create(any())).thenAnswer(inv -> wrap(inv.getArgument(0), attId));

        AttachmentDTO result = attachmentService.uploadAttachment(
                projectId, caseId, null, file, null, null, null);

        assertEquals("CASE", result.getAttachmentType());
        assertNull(result.getRunId());
        assertNull(result.getStepKey());
    }

    @Test
    @DisplayName("uploadAttachment sets EVIDENCE type with runId and stepKey")
    void uploadFile_setsEvidenceFields() throws Exception {
        UUID attId  = UUID.randomUUID();
        String stepKey = "2";
        MockMultipartFile file = new MockMultipartFile("file", "shot.png", "image/png", new byte[]{1, 2, 3});
        when(edgeMessageService.createMessage(any(), any(), any())).thenReturn(UUID.randomUUID());
        when(entityService.create(any())).thenAnswer(inv -> wrap(inv.getArgument(0), attId));

        AttachmentDTO result = attachmentService.uploadAttachment(
                projectId, caseId, null, file, "EVIDENCE", runId, stepKey);

        assertEquals("EVIDENCE", result.getAttachmentType());
        assertEquals(runId, result.getRunId());
        assertEquals(stepKey, result.getStepKey());
    }

    @Test
    @DisplayName("uploadAttachment sets DEFECT type with defectId")
    void uploadFile_setsDefectFields() throws Exception {
        UUID attId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "log.txt", "text/plain", new byte[]{1});
        when(edgeMessageService.createMessage(any(), any(), any())).thenReturn(UUID.randomUUID());
        when(entityService.create(any())).thenAnswer(inv -> wrap(inv.getArgument(0), attId));

        AttachmentDTO result = attachmentService.uploadAttachment(
                projectId, null, defectId, file, "DEFECT", null, null);

        assertEquals("DEFECT", result.getAttachmentType());
        assertEquals(defectId, result.getDefectId());
    }

    @Test
    @DisplayName("getEvidenceByRunCaseStep queries with 4-field compound condition")
    void getEvidenceByRunCaseStep_queriesCorrectly() {
        UUID attId  = UUID.randomUUID();
        String stepKey = "3";

        AttachmentDTO evidence = new AttachmentDTO();
        evidence.setAttachmentType("EVIDENCE");
        evidence.setRunId(runId);
        evidence.setCaseId(caseId);
        evidence.setStepKey(stepKey);

        PageResult<EntityWithMetadata<AttachmentDTO>> page =
                PageResult.of(null, List.of(wrap(evidence, attId)), 0, 20, 1L);

        when(entityService.search(any(), any(GroupConditionDto.class), eq(AttachmentDTO.class)))
                .thenReturn(page);

        List<AttachmentDTO> result = attachmentService.getEvidenceByRunCaseStep(runId, caseId, stepKey);

        assertEquals(1, result.size());
        assertEquals("EVIDENCE", result.get(0).getAttachmentType());
        assertEquals(stepKey, result.get(0).getStepKey());
        verify(entityService).search(any(), any(GroupConditionDto.class), eq(AttachmentDTO.class));
    }
}
```

- [ ] **Step 2.2: Run test to confirm failure**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.service.AttachmentServiceTest" 2>&1 | tail -20
```

Expected: compilation error — `uploadAttachment` doesn't accept the new params yet.

- [ ] **Step 2.3: Update AttachmentService**

In `apps/backend/src/main/java/com/java_template/application/service/AttachmentService.java`:

**Replace** the existing `uploadAttachment(UUID projectId, UUID caseId, UUID defectId, MultipartFile file)` method signature and body with:

```java
public AttachmentDTO uploadAttachment(UUID projectId, UUID caseId, UUID defectId,
                                      MultipartFile file,
                                      String attachmentType, UUID runId, String stepKey)
        throws IOException {

    long fileSizeBytes = file.getSize();
    logger.info("📤 Starting attachment upload: fileName='{}', size={}KB",
            file.getOriginalFilename(), fileSizeBytes / 1024);

    AttachmentDTO attachment = new AttachmentDTO();
    attachment.setProjectId(projectId);
    attachment.setCaseId(caseId);
    attachment.setDefectId(defectId);
    attachment.setFileName(file.getOriginalFilename());
    attachment.setFileType(file.getContentType());
    attachment.setFileSize(fileSizeBytes);
    attachment.setUploadedAt(Instant.now().toString());
    attachment.setAttachmentType(attachmentType != null ? attachmentType : "CASE");
    attachment.setRunId(runId);
    attachment.setStepKey(stepKey);

    try {
        logger.info("📝 Creating EdgeMessage with file content for '{}'...", file.getOriginalFilename());
        ObjectNode content = objectMapper.createObjectNode();
        content.put("fileName", file.getOriginalFilename());
        content.put("fileType", file.getContentType());
        content.put("fileSize", fileSizeBytes);
        content.put("uploadedAt", attachment.getUploadedAt());
        content.put("data", Base64.getEncoder().encodeToString(file.getBytes()));

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("projectId", projectId.toString());
        if (caseId != null) metadata.put("caseId", caseId.toString());
        if (defectId != null) metadata.put("defectId", defectId.toString());
        metadata.put("contentType", file.getContentType());

        UUID messageId = edgeMessageService.createMessage(EDGE_MESSAGE_SUBJECT, content, metadata);
        attachment.setMessageId(messageId);
        logger.info("✅ EdgeMessage created for file '{}': messageId={}", file.getOriginalFilename(), messageId);
    } catch (Exception e) {
        logger.error("❌ EdgeMessage creation failed for file '{}': {}", file.getOriginalFilename(), e.getMessage());
        logger.warn("⚠️  Will proceed without EdgeMessage reference");
    }

    logger.info("💾 Creating Attachment entity in Cyoda...");
    AttachmentDTO result = withId(entityService.create(attachment));
    logger.info("✅ Attachment entity created successfully");
    return result;
}
```

**Add** the new `getEvidenceByRunCaseStep` method after `getAttachmentsByDefectId`:

```java
/**
 * Retrieves Evidence attachments for a specific step in a specific test run.
 * Filters by attachmentType=EVIDENCE, runId, caseId, and stepKey.
 */
public List<AttachmentDTO> getEvidenceByRunCaseStep(UUID runId, UUID caseId, String stepKey) {
    // Build a 4-condition AND group
    SimpleConditionDto c1 = new SimpleConditionDto()
            .jsonPath("$.attachmentType").operation(OperatorTypeDto.EQUALS)
            .value(objectMapper.valueToTree("EVIDENCE"));
    c1.setType(QueryConditionTypeDto.SIMPLE);

    SimpleConditionDto c2 = new SimpleConditionDto()
            .jsonPath("$.runId").operation(OperatorTypeDto.EQUALS)
            .value(objectMapper.valueToTree(runId.toString()));
    c2.setType(QueryConditionTypeDto.SIMPLE);

    SimpleConditionDto c3 = new SimpleConditionDto()
            .jsonPath("$.caseId").operation(OperatorTypeDto.EQUALS)
            .value(objectMapper.valueToTree(caseId.toString()));
    c3.setType(QueryConditionTypeDto.SIMPLE);

    SimpleConditionDto c4 = new SimpleConditionDto()
            .jsonPath("$.stepKey").operation(OperatorTypeDto.EQUALS)
            .value(objectMapper.valueToTree(stepKey));
    c4.setType(QueryConditionTypeDto.SIMPLE);

    GroupConditionDto group = new GroupConditionDto()
            .operator(GroupOperatorDto.AND)
            .conditions(List.of(c1, c2, c3, c4));
    group.setType(QueryConditionTypeDto.GROUP);

    return entityService.search(MODEL_SPEC, group, AttachmentDTO.class).data()
            .stream().map(this::withId).toList();
}
```

- [ ] **Step 2.4: Run tests to confirm pass**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.service.AttachmentServiceTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 4 tests passing.

- [ ] **Step 2.5: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/service/AttachmentService.java \
        apps/backend/src/test/java/com/java_template/application/service/AttachmentServiceTest.java
git commit -m "feat(attachments): update service — new upload params, getEvidenceByRunCaseStep"
```

---

## Task 3: Backend controller — update upload endpoint and add evidence endpoint

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/controller/AttachmentController.java`

- [ ] **Step 3.1: Update upload endpoint signature**

In `AttachmentController.java`, replace the `uploadAttachment` method:

```java
@PostMapping(consumes = "multipart/form-data")
@Operation(summary = "Upload attachment file to Cyoda EdgeMessage")
public ResponseEntity<?> uploadAttachment(
        @PathVariable UUID projectId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "caseId",          required = false) UUID caseId,
        @RequestParam(value = "defectId",         required = false) UUID defectId,
        @RequestParam(value = "attachmentType",   required = false) String attachmentType,
        @RequestParam(value = "runId",            required = false) UUID runId,
        @RequestParam(value = "stepKey",          required = false) String stepKey) {
    try {
        AttachmentDTO uploaded = attachmentService.uploadAttachment(
                projectId, caseId, defectId, file, attachmentType, runId, stepKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentMetadataDTO.from(uploaded));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Upload failed"));
    }
}
```

- [ ] **Step 3.2: Add evidence query endpoint**

Add after `getAttachmentsByDefect`:

```java
@GetMapping("/by-run/{runId}/case/{caseId}/step/{stepKey}")
@Operation(summary = "Get Evidence attachments for a specific step in a test run")
public ResponseEntity<List<AttachmentMetadataDTO>> getEvidenceByRunCaseStep(
        @PathVariable UUID projectId,
        @PathVariable UUID runId,
        @PathVariable UUID caseId,
        @PathVariable String stepKey) {
    try {
        List<AttachmentMetadataDTO> result = attachmentService
                .getEvidenceByRunCaseStep(runId, caseId, stepKey)
                .stream()
                .map(AttachmentMetadataDTO::from)
                .toList();
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        logger.warn("Failed to fetch evidence for run={} case={} step={}: {}",
                runId, caseId, stepKey, e.getMessage());
        return ResponseEntity.ok(List.of());
    }
}
```

- [ ] **Step 3.3: Compile and run all backend tests**

```bash
./gradlew :apps:backend:compileJava && ./gradlew :apps:backend:test 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 3.4: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/controller/AttachmentController.java
git commit -m "feat(attachments): add uploadType params to upload endpoint; add evidence query endpoint"
```

---

## Task 4: Cyoda re-import (infrastructure — manual steps)

**This task must be run with a live Cyoda instance. No code changes.**

- [ ] **Step 4.1: Get auth token**

```bash
cd apps/backend
set -a; source .env; set +a
TOKEN=$(curl -s -u "$CYODA_CLIENT_ID:$CYODA_CLIENT_SECRET" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&scope=ROLE_M2M' \
  "https://$CYODA_HOST/api/oauth/token" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("access_token",""))')
echo "Token OK: ${TOKEN:0:20}..."
```

- [ ] **Step 4.2: Delete all tenant entities (bottom-up)**

```bash
MODELS_ORDER=("TestRunStep" "TestRunCase" "TestRun" "Attachment" "Defect" "TestStep" "TestCase" "Suite" "Report" "ProjectCounter" "Project")
for model in "${MODELS_ORDER[@]}"; do
  echo "Deleting entities: $model"
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/entity/$model/1" > /dev/null 2>&1
done
echo "Entity deletion complete"
```

- [ ] **Step 4.3: Unlock all models**

```bash
MODELS=("Project" "Suite" "TestCase" "TestStep" "Defect" "TestRun" "TestRunCase" "TestRunStep" "Attachment" "Report" "ProjectCounter")
for model in "${MODELS[@]}"; do
  curl -s -X PUT -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/model/$model/1/unlock"
done
echo "Unlock complete"
```

- [ ] **Step 4.4: Delete all models**

```bash
for model in "${MODELS[@]}"; do
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/model/$model/1"
done
echo "Model deletion complete"
```

- [ ] **Step 4.5: Run import script**

```bash
./import-schemas.sh
```

Expected: all 11 schemas and 11 workflows imported, all models locked. Check output for any errors.

- [ ] **Step 4.6: Verify Attachment schema has new fields**

In Cyoda UI or via API, confirm the `Attachment` model has fields `attachmentType`, `runId`, `stepKey`.

---

## Task 5: Frontend — update api.ts

**Files:**
- Modify: `apps/frontend/src/lib/api.ts`

- [ ] **Step 5.1: Update Attachment type**

Find the `export interface Attachment` (around line 397) and replace it:

```typescript
export interface Attachment {
  id: string;
  projectId: string;
  caseId: string | null;
  defectId: string | null;
  fileName: string;
  fileType: string;
  fileSize: number;
  uploadedAt: string;
  messageId: string | null;
  attachmentType: 'CASE' | 'EVIDENCE' | 'DEFECT' | null;
  runId: string | null;
  stepKey: string | null;
}
```

- [ ] **Step 5.2: Update upload() to send new params**

Replace `attachmentsApi.upload`:

```typescript
upload: (
  projectId: string,
  file: File,
  caseId?: string,
  defectId?: string,
  attachmentType?: 'CASE' | 'EVIDENCE' | 'DEFECT',
  runId?: string,
  stepKey?: string,
) => {
  const form = new FormData();
  form.append('file', file);
  if (caseId)          form.append('caseId', caseId);
  if (defectId)        form.append('defectId', defectId);
  if (attachmentType)  form.append('attachmentType', attachmentType);
  if (runId)           form.append('runId', runId);
  if (stepKey)         form.append('stepKey', stepKey);
  const token = getAuthToken();
  const headers: HeadersInit = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return fetch(`${BASE_URL}/projects/${projectId}/attachments`, {
    method: 'POST',
    credentials: 'include',
    headers,
    body: form,
  }).then(async r => {
    if (!r.ok) {
      const body = await r.text().catch(() => '');
      throw new Error(`Upload failed (${r.status}): ${body}`);
    }
    return r.json() as Promise<Attachment>;
  });
},
```

- [ ] **Step 5.3: Add listByRunCaseStep method**

Inside `attachmentsApi`, add after `listByDefect`:

```typescript
listByRunCaseStep: (projectId: string, runId: string, caseId: string, stepKey: string) =>
  api.get<Attachment[]>(
    `/projects/${projectId}/attachments/by-run/${runId}/case/${caseId}/step/${stepKey}`
  ),
```

- [ ] **Step 5.4: TypeScript compile check**

```bash
cd apps/frontend && npx tsc --noEmit 2>&1 | head -40
```

Expected: no errors.

- [ ] **Step 5.5: Commit**

```bash
git add apps/frontend/src/lib/api.ts
git commit -m "feat(attachments): update Attachment type and api — new fields, upload params, listByRunCaseStep"
```

---

## Task 6: Frontend — useAuthenticatedImageUrl hook

**Files:**
- Create: `apps/frontend/src/hooks/useAuthenticatedImageUrl.ts`

- [ ] **Step 6.1: Write the hook**

Create `apps/frontend/src/hooks/useAuthenticatedImageUrl.ts`:

```typescript
import { useState, useEffect } from 'react';

function getAuthToken(): string | null {
  return localStorage.getItem('token') ?? sessionStorage.getItem('token') ?? null;
}

/**
 * Fetches a protected URL with the Bearer token and returns a blob: object URL.
 * Returns null while loading or on error.
 * Automatically revokes the blob URL on unmount to prevent memory leaks.
 */
export function useAuthenticatedImageUrl(url: string | null | undefined): string | null {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!url) {
      setBlobUrl(null);
      return;
    }

    let revoked = false;
    let objectUrl: string | null = null;

    const token = getAuthToken();
    const headers: HeadersInit = token ? { Authorization: `Bearer ${token}` } : {};

    fetch(url, { credentials: 'include', headers })
      .then(r => {
        if (!r.ok) throw new Error(`Image fetch failed: ${r.status}`);
        return r.blob();
      })
      .then(blob => {
        if (revoked) return;
        objectUrl = URL.createObjectURL(blob);
        setBlobUrl(objectUrl);
      })
      .catch(() => {
        if (!revoked) setBlobUrl(null);
      });

    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [url]);

  return blobUrl;
}
```

- [ ] **Step 6.2: TypeScript compile check**

```bash
cd apps/frontend && npx tsc --noEmit 2>&1 | head -20
```

Expected: no errors.

- [ ] **Step 6.3: Commit**

```bash
git add apps/frontend/src/hooks/useAuthenticatedImageUrl.ts
git commit -m "feat(attachments): add useAuthenticatedImageUrl hook for Bearer-authenticated image preview"
```

---

## Task 7: Frontend — fix RunExecution.tsx (3 bugs)

**Files:**
- Modify: `apps/frontend/src/pages/RunExecution.tsx`

Bugs to fix:
1. `serverAttachments` shows Evidence — filter to CASE only
2. Evidence upload uses wrong params — no runId/stepKey/attachmentType
3. Defect attachment upload uses `defect.caseId` instead of `created.id`

- [ ] **Step 7.1: Fix Bug 1 — filter serverAttachments to CASE only**

Find the section around line 1262:
```tsx
{/* Attachments Section — loaded from server so they survive page refresh */}
{serverAttachments.length > 0 && (
```

Change the `serverAttachments.map(...)` render to first filter:

```tsx
{/* Attachments Section — only CASE type; Evidence stays in Evidence modal */}
{serverAttachments.filter(
  (att: Attachment) => !att.attachmentType || att.attachmentType === 'CASE'
).length > 0 && (
```

And update the `.map` call inside to use the filtered array:

```tsx
{serverAttachments
  .filter((att: Attachment) => !att.attachmentType || att.attachmentType === 'CASE')
  .map((item: Attachment) => {
```

- [ ] **Step 7.2: Fix Bug 2 — Evidence upload with type/runId/stepKey**

Find `handleEvidenceUpload` function (around line 858). Replace the `attachmentsApi.upload` call:

```typescript
// Before:
fileArray.map((f) => attachmentsApi.upload(projectId!, f, evidenceTarget.caseId))

// After:
fileArray.map((f) =>
  attachmentsApi.upload(
    projectId!,
    f,
    evidenceTarget.caseId,
    undefined,
    'EVIDENCE',
    runId!,
    String(evidenceTarget.stepIdx + 1),
  )
)
```

Also update the `qc.invalidateQueries` to invalidate the evidence query:

```typescript
qc.invalidateQueries({
  queryKey: ['attachments', projectId!, 'run', runId!, 'case', evidenceTarget.caseId, 'step', String(evidenceTarget.stepIdx + 1)],
});
```

- [ ] **Step 7.3: Add evidence query hook and use server data for Evidence modal**

Add this near the other `useAttachmentsByCase` hook (around line 487):

```typescript
// Evidence for the active step — loaded from server
const { data: serverEvidence = [] } = useQuery({
  queryKey: [
    'attachments', projectId!, 'run', runId!,
    'case', activeCase?.id ?? '',
    'step', evidenceTarget ? String(evidenceTarget.stepIdx + 1) : '',
  ],
  queryFn: () =>
    evidenceTarget
      ? attachmentsApi.listByRunCaseStep(
          projectId!,
          runId!,
          evidenceTarget.caseId,
          String(evidenceTarget.stepIdx + 1),
        )
      : Promise.resolve([]),
  enabled: !!projectId && !!runId && !!evidenceTarget,
});
```

In the Evidence modal (around line 1523), replace the local `getEvidenceFiles(...)` list render with `serverEvidence`:

```tsx
{serverEvidence.length > 0 && (
  <div className="space-y-1.5">
    {serverEvidence.map((att: Attachment) => {
      const IconComp = att.fileType?.startsWith('image/') ? Image
        : att.fileType?.includes('pdf') ? FileText : File;
      return (
        <div key={att.id} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
          <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
          <span className="text-sm text-foreground truncate flex-1">{att.fileName}</span>
          <span className="text-xs text-muted-foreground shrink-0">{att.fileSize} B</span>
        </div>
      );
    })}
  </div>
)}
```

Also update the evidence badge count to use server data. Find:
```tsx
{evidenceFiles.length > 0 && (
  <span ...>{evidenceFiles.length}</span>
```

Note: For the badge count per step, keep a local count from `stepEvidence` state for immediate feedback during the session, and let server data take over after reload. No change needed here — the local state still reflects newly uploaded files in the current session.

- [ ] **Step 7.4: Fix Bug 3 — defect attachment upload uses defectId**

Find line ~936 (inside `handleCreateDefect`):

```typescript
// Before:
attachmentsApi.upload(projectId!, f, defect.caseId)

// After:
attachmentsApi.upload(projectId!, f, undefined, created.id, 'DEFECT')
```

- [ ] **Step 7.5: TypeScript compile check**

```bash
cd apps/frontend && npx tsc --noEmit 2>&1 | head -40
```

Expected: no errors.

- [ ] **Step 7.6: Commit**

```bash
git add apps/frontend/src/pages/RunExecution.tsx
git commit -m "fix(attachments): filter Evidence from case section; fix evidence upload params; fix defect attachment parent"
```

---

## Task 8: Frontend — fix image preview in Attachments.tsx

**Files:**
- Modify: `apps/frontend/src/pages/Attachments.tsx`

The bug: `<img src={viewUrl(f)}>` sends no auth header → 401. Fix: use `useAuthenticatedImageUrl`.

- [ ] **Step 8.1: Import the hook**

At the top of `Attachments.tsx`, add:
```typescript
import { useAuthenticatedImageUrl } from '@/hooks/useAuthenticatedImageUrl';
```

- [ ] **Step 8.2: Create a PreviewImage sub-component**

Add this small component inside the file, before the main `Attachments` component:

```typescript
const AuthenticatedImage = ({ url, alt, className }: { url: string; alt: string; className?: string }) => {
  const blobUrl = useAuthenticatedImageUrl(url);
  if (!blobUrl) return <div className="w-full h-full flex items-center justify-center text-xs text-muted-foreground">Loading…</div>;
  return <img src={blobUrl} alt={alt} className={className} />;
};
```

- [ ] **Step 8.3: Replace img tags in preview modal**

In the preview modal (around line 246), replace:
```tsx
<img
  src={viewUrl(previewFile)}
  alt={previewFile.fileName}
  className="max-w-full max-h-[60vh] object-contain"
  onLoad={...}
  onError={...}
/>
```

With:
```tsx
<AuthenticatedImage
  url={viewUrl(previewFile)}
  alt={previewFile.fileName}
  className="max-w-full max-h-[60vh] object-contain"
/>
```

- [ ] **Step 8.4: Replace img tag in grid view thumbnail**

In the grid view (around line 205), replace:
```tsx
<img src={downloadUrl(f)} alt={f.fileName} className="w-full h-full object-cover" />
```

With:
```tsx
<AuthenticatedImage url={downloadUrl(f)} alt={f.fileName} className="w-full h-full object-cover" />
```

- [ ] **Step 8.5: TypeScript compile check**

```bash
cd apps/frontend && npx tsc --noEmit 2>&1 | head -20
```

Expected: no errors.

- [ ] **Step 8.6: Commit**

```bash
git add apps/frontend/src/pages/Attachments.tsx
git commit -m "fix(attachments): use authenticated blob URL for image preview — fixes 401 on img src"
```

---

## Task 9: Playwright E2E tests

**Files:**
- Modify: `apps/frontend/e2e/helpers/api.ts`
- Create: `apps/frontend/e2e/attachments-case.spec.ts`
- Create: `apps/frontend/e2e/attachments-evidence.spec.ts`
- Create: `apps/frontend/e2e/attachments-defect.spec.ts`

- [ ] **Step 9.1: Add attachment helpers to e2e/helpers/api.ts**

Append to `apps/frontend/e2e/helpers/api.ts`:

```typescript
// ── Attachments ───────────────────────────────────────────────────────────────

export async function uploadAttachmentToCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseId: string,
  fileBuffer: Buffer,
  fileName: string,
  mimeType: string,
): Promise<{ id: string; attachmentType: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/attachments`, {
    headers,
    multipart: {
      file: { name: fileName, mimeType, buffer: fileBuffer },
      caseId,
      attachmentType: 'CASE',
    },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'uploadAttachmentToCase');
  return { id: body.id, attachmentType: body.attachmentType };
}

export async function uploadAttachmentToDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  defectId: string,
  fileBuffer: Buffer,
  fileName: string,
  mimeType: string,
): Promise<{ id: string; attachmentType: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/attachments`, {
    headers,
    multipart: {
      file: { name: fileName, mimeType, buffer: fileBuffer },
      defectId,
      attachmentType: 'DEFECT',
    },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'uploadAttachmentToDefect');
  return { id: body.id, attachmentType: body.attachmentType };
}

export async function uploadEvidence(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseId: string,
  runId: string,
  stepKey: string,
  fileBuffer: Buffer,
  fileName: string,
  mimeType: string,
): Promise<{ id: string; attachmentType: string; runId: string; stepKey: string }> {
  const res = await api.post(`${BASE}/projects/${projectId}/attachments`, {
    headers,
    multipart: {
      file: { name: fileName, mimeType, buffer: fileBuffer },
      caseId,
      attachmentType: 'EVIDENCE',
      runId,
      stepKey,
    },
  });
  const body = await res.json();
  assertOk(res.status(), body, 'uploadEvidence');
  return { id: body.id, attachmentType: body.attachmentType, runId: body.runId, stepKey: body.stepKey };
}

export async function listAttachmentsByCase(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  caseId: string,
): Promise<Array<{ id: string; attachmentType: string; fileName: string }>> {
  const res = await api.get(`${BASE}/projects/${projectId}/attachments/by-case/${caseId}`, { headers });
  const body = await res.json();
  assertOk(res.status(), body, 'listAttachmentsByCase');
  return Array.isArray(body) ? body : [];
}

export async function listAttachmentsByDefect(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  defectId: string,
): Promise<Array<{ id: string; attachmentType: string; fileName: string }>> {
  const res = await api.get(`${BASE}/projects/${projectId}/attachments/by-defect/${defectId}`, { headers });
  const body = await res.json();
  assertOk(res.status(), body, 'listAttachmentsByDefect');
  return Array.isArray(body) ? body : [];
}

export async function listEvidenceByStep(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  runId: string,
  caseId: string,
  stepKey: string,
): Promise<Array<{ id: string; attachmentType: string; stepKey: string; runId: string }>> {
  const res = await api.get(
    `${BASE}/projects/${projectId}/attachments/by-run/${runId}/case/${caseId}/step/${stepKey}`,
    { headers },
  );
  const body = await res.json();
  assertOk(res.status(), body, 'listEvidenceByStep');
  return Array.isArray(body) ? body : [];
}

export async function deleteAttachment(
  api: APIRequestContext,
  headers: Record<string, string>,
  projectId: string,
  attachmentId: string,
): Promise<void> {
  const res = await api.delete(
    `${BASE}/projects/${projectId}/attachments/${attachmentId}`,
    { headers },
  );
  if (res.status() !== 204 && res.status() !== 404) {
    assertOk(res.status(), null, 'deleteAttachment');
  }
}
```

- [ ] **Step 9.2: Write attachments-case.spec.ts**

Create `apps/frontend/e2e/attachments-case.spec.ts`:

```typescript
import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCase,
  createStep,
  uploadAttachmentToCase,
  listAttachmentsByCase,
  deleteAttachment,
} from './helpers/api';
import { Buffer } from 'buffer';

test.describe('Case attachments', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let suiteId: string;
  let caseId: string;
  let uploadedAttachmentId: string;

  const smallPng = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==',
    'base64',
  );

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    suiteId = suite.id;
    const tc = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Attachment E2E Case');
    caseId = tc.id;
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 1, 'Do thing', 'Thing done');
  });

  test.afterAll(async () => {
    await apiCtx.dispose();
    // Project cleanup via admin API
    const cleanCtx = await (await import('@playwright/test')).request.newContext();
    const h = await getAuthHeaders(cleanCtx);
    await deleteProject(cleanCtx, h, projectId).catch(() => {});
    await cleanCtx.dispose();
  });

  test('API: upload case attachment returns attachmentType=CASE', async () => {
    const att = await uploadAttachmentToCase(
      apiCtx, authHeaders, projectId, caseId,
      smallPng, 'test.png', 'image/png',
    );
    uploadedAttachmentId = att.id;
    expect(att.attachmentType).toBe('CASE');
  });

  test('API: list by case returns uploaded attachment', async () => {
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    const found = list.find(a => a.id === uploadedAttachmentId);
    expect(found).toBeDefined();
    expect(found!.attachmentType).toBe('CASE');
  });

  test('UI: case attachment appears on Attachments page', async ({ page }) => {
    await page.goto(`/projects/${projectId}/attachments`);
    await expect(page.getByText('test.png')).toBeVisible({ timeout: 15_000 });
  });

  test('UI: image preview opens without auth error', async ({ page }) => {
    await page.goto(`/projects/${projectId}/attachments`);
    await page.getByText('test.png').click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible({ timeout: 5_000 });
    // img should load (no broken image icon)
    const img = dialog.locator('img');
    await expect(img).toBeVisible({ timeout: 10_000 });
    const naturalWidth = await img.evaluate((el: HTMLImageElement) => el.naturalWidth);
    expect(naturalWidth).toBeGreaterThan(0);
  });

  test('API: delete attachment removes it from list', async () => {
    await deleteAttachment(apiCtx, authHeaders, projectId, uploadedAttachmentId);
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    expect(list.find(a => a.id === uploadedAttachmentId)).toBeUndefined();
  });
});
```

- [ ] **Step 9.3: Write attachments-evidence.spec.ts**

Create `apps/frontend/e2e/attachments-evidence.spec.ts`:

```typescript
import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCase,
  createStep,
  createTestRun,
  uploadEvidence,
  listAttachmentsByCase,
  listEvidenceByStep,
} from './helpers/api';
import { Buffer } from 'buffer';

test.describe('Evidence attachments', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let caseId: string;
  let runId1: string;
  let runId2: string;
  let evidenceId: string;

  const smallPng = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==',
    'base64',
  );

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    const suiteId = suite.id;
    const tc = await createCase(apiCtx, authHeaders, projectId, suiteId, 'Evidence E2E Case');
    caseId = tc.id;
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 1, 'Step One', 'Expected One');
    await createStep(apiCtx, authHeaders, projectId, suiteId, caseId, 2, 'Step Two', 'Expected Two');
    const run1 = await createTestRun(apiCtx, authHeaders, projectId, [caseId]);
    runId1 = run1.id;
    const run2 = await createTestRun(apiCtx, authHeaders, projectId, [caseId]);
    runId2 = run2.id;
  });

  test.afterAll(async () => {
    await apiCtx.dispose();
    const cleanCtx = await (await import('@playwright/test')).request.newContext();
    const h = await getAuthHeaders(cleanCtx);
    await deleteProject(cleanCtx, h, projectId).catch(() => {});
    await cleanCtx.dispose();
  });

  test('API: upload evidence returns EVIDENCE type with correct runId and stepKey', async () => {
    const ev = await uploadEvidence(
      apiCtx, authHeaders, projectId, caseId, runId1, '1',
      smallPng, 'evidence.png', 'image/png',
    );
    evidenceId = ev.id;
    expect(ev.attachmentType).toBe('EVIDENCE');
    expect(ev.runId).toBe(runId1);
    expect(ev.stepKey).toBe('1');
  });

  test('API: evidence does NOT appear in list-by-case (only CASE type should)', async () => {
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    const evidenceInCaseList = list.filter(a => a.attachmentType === 'EVIDENCE');
    expect(evidenceInCaseList).toHaveLength(0);
  });

  test('API: evidence appears in list-by-run-case-step for the correct step', async () => {
    const list = await listEvidenceByStep(apiCtx, authHeaders, projectId, runId1, caseId, '1');
    expect(list.find(a => a.id === evidenceId)).toBeDefined();
  });

  test('API: evidence does NOT appear for a different run (same case, same step)', async () => {
    const list = await listEvidenceByStep(apiCtx, authHeaders, projectId, runId2, caseId, '1');
    expect(list.find(a => a.id === evidenceId)).toBeUndefined();
  });

  test('API: evidence does NOT appear for a different step (same run, same case)', async () => {
    const list = await listEvidenceByStep(apiCtx, authHeaders, projectId, runId1, caseId, '2');
    expect(list.find(a => a.id === evidenceId)).toBeUndefined();
  });

  test('UI: Evidence modal shows uploaded evidence after reload', async ({ page }) => {
    await page.goto(`/projects/${projectId}/runs/${runId1}`);
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 });

    // Click the paperclip button for step 1
    const paperclip = page.locator('[aria-label*="evidence" i], button').filter({ has: page.locator('[data-lucide="paperclip"]') }).first();
    await paperclip.click();

    const dialog = page.getByRole('dialog', { name: /evidence/i });
    await expect(dialog).toBeVisible({ timeout: 5_000 });
    await expect(dialog.getByText('evidence.png')).toBeVisible({ timeout: 10_000 });
  });

  test('UI: Evidence does NOT appear in the Attachments section of RunExecution', async ({ page }) => {
    await page.goto(`/projects/${projectId}/runs/${runId1}`);
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 });

    // The Attachments section heading
    const attachmentsSection = page.locator('h3').filter({ hasText: /^attachments$/i });
    // If the section exists, evidence.png must not be there
    const sectionCount = await attachmentsSection.count();
    if (sectionCount > 0) {
      await expect(page.locator('h3').filter({ hasText: /^attachments$/i })
        .locator('..').getByText('evidence.png')).not.toBeVisible();
    }
  });
});
```

- [ ] **Step 9.4: Write attachments-defect.spec.ts**

Create `apps/frontend/e2e/attachments-defect.spec.ts`:

```typescript
import { test, expect, APIRequestContext } from '@playwright/test';
import {
  getAuthHeaders,
  createProject,
  deleteProject,
  createSuite,
  createCase,
  createDefect,
  uploadAttachmentToDefect,
  listAttachmentsByDefect,
  listAttachmentsByCase,
  deleteAttachment,
} from './helpers/api';
import { Buffer } from 'buffer';

test.describe('Defect attachments', () => {
  test.describe.configure({ mode: 'serial' });

  let apiCtx: APIRequestContext;
  let authHeaders: Record<string, string>;
  let projectId: string;
  let caseId: string;
  let defectId: string;
  let defectAttachmentId: string;

  const smallFile = Buffer.from('log content for test', 'utf-8');

  test.beforeAll(async ({ playwright }) => {
    apiCtx = await playwright.request.newContext();
    authHeaders = await getAuthHeaders(apiCtx);
    const proj = await createProject(apiCtx, authHeaders);
    projectId = proj.id;
    const suite = await createSuite(apiCtx, authHeaders, projectId);
    const tc = await createCase(apiCtx, authHeaders, projectId, suite.id, 'Defect Attachment Case');
    caseId = tc.id;
    const defect = await createDefect(apiCtx, authHeaders, projectId, {
      title: 'E2E Defect With Attachment',
      severity: 'Major',
    });
    defectId = defect.id;
  });

  test.afterAll(async () => {
    await apiCtx.dispose();
    const cleanCtx = await (await import('@playwright/test')).request.newContext();
    const h = await getAuthHeaders(cleanCtx);
    await deleteProject(cleanCtx, h, projectId).catch(() => {});
    await cleanCtx.dispose();
  });

  test('API: upload to defect returns attachmentType=DEFECT', async () => {
    const att = await uploadAttachmentToDefect(
      apiCtx, authHeaders, projectId, defectId,
      smallFile, 'crash.log', 'text/plain',
    );
    defectAttachmentId = att.id;
    expect(att.attachmentType).toBe('DEFECT');
  });

  test('API: list-by-defect includes the uploaded attachment', async () => {
    const list = await listAttachmentsByDefect(apiCtx, authHeaders, projectId, defectId);
    expect(list.find(a => a.id === defectAttachmentId)).toBeDefined();
    expect(list.find(a => a.id === defectAttachmentId)!.attachmentType).toBe('DEFECT');
  });

  test('API: defect attachment does NOT appear in list-by-case', async () => {
    const list = await listAttachmentsByCase(apiCtx, authHeaders, projectId, caseId);
    expect(list.find(a => a.id === defectAttachmentId)).toBeUndefined();
  });

  test('UI: defect attachment visible in View Defect modal', async ({ page }) => {
    await page.goto(`/projects/${projectId}/defects`);
    await page.getByText('E2E Defect With Attachment').click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible({ timeout: 5_000 });
    await expect(dialog.getByText('crash.log')).toBeVisible({ timeout: 10_000 });
  });

  test('API: delete defect attachment removes it', async () => {
    await deleteAttachment(apiCtx, authHeaders, projectId, defectAttachmentId);
    const list = await listAttachmentsByDefect(apiCtx, authHeaders, projectId, defectId);
    expect(list.find(a => a.id === defectAttachmentId)).toBeUndefined();
  });
});
```

- [ ] **Step 9.5: Run Playwright tests**

```bash
cd apps/frontend && npx playwright test e2e/attachments-case.spec.ts e2e/attachments-evidence.spec.ts e2e/attachments-defect.spec.ts --reporter=line 2>&1 | tail -40
```

Expected: all tests pass. If any fail, debug and fix before proceeding.

- [ ] **Step 9.6: Commit**

```bash
git add apps/frontend/e2e/helpers/api.ts \
        apps/frontend/e2e/attachments-case.spec.ts \
        apps/frontend/e2e/attachments-evidence.spec.ts \
        apps/frontend/e2e/attachments-defect.spec.ts
git commit -m "test(e2e): add Playwright specs for case attachments, evidence isolation, and defect attachments"
```

---

## Task 10: Final verification

- [ ] **Step 10.1: Full backend build + tests**

```bash
./gradlew :apps:backend:build 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 10.2: Frontend type check + lint**

```bash
cd apps/frontend && npx tsc --noEmit && npm run lint 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 10.3: Run all Playwright tests**

```bash
cd apps/frontend && npx playwright test --reporter=line 2>&1 | tail -40
```

Expected: all suites pass (including existing defects + run-execution-defects specs).

- [ ] **Step 10.4: Manual smoke test** (requires running app)

1. Open `/projects/{id}/runs/{runId}`
2. Upload a file via Evidence modal on Step 1 — badge shows count
3. Reload page — Evidence still shows in modal for Step 1
4. Confirm Evidence does NOT appear in the "Attachments" section
5. Open `/projects/{id}/attachments` — upload an image, click it → preview shows (not broken)
6. Open a defect, attach a file via Edit modal — file visible in View modal

- [ ] **Step 10.5: Final commit**

```bash
git add -A
git commit -m "feat(attachments): complete attachments fix — type separation, evidence isolation, defect linkage, preview fix"
```

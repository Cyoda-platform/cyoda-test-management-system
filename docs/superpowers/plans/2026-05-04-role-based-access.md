# Role-Based Access Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend ADMIN-only restrictions to suite and test-case mutations; fix an equalsIgnoreCase inconsistency in TestRunController; hide admin-only UI buttons for TESTER in Projects and Repository pages.

**Architecture:** Backend guards follow the existing `ProjectController` pattern — extract `"role"` from `request.getAttribute("role")` and return `403` if not `"ADMIN"`. Frontend derives `isAdmin = user?.role === "ADMIN"` from `useAuth()` and conditionally renders buttons.

**Tech Stack:** Java 21 + Spring Boot 3.5, React 18 + TypeScript, JUnit 5 + MockMvc, Vitest.

---

## Task 1: Write failing role tests for SuiteController (TDD red)

**Files:**
- Modify: `apps/backend/src/test/java/com/java_template/application/controller/SuiteControllerTest.java`

- [ ] **Step 1.1: Add ADMIN role to existing `testCreateSuite` test**

The existing test has no role attribute. Once the guard is added in Task 2 it will return 403 instead of 201. Fix it now so the suite of existing tests stays green after Task 2.

In `testCreateSuite`, add `.requestAttr("role", "ADMIN")` to the perform call:

```java
mockMvc.perform(post("/projects/" + projectId + "/suites")
        .requestAttr("role", "ADMIN")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(newSuite)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(suiteId.toString()))
        .andExpect(jsonPath("$.name").value("Test Suite"));
```

- [ ] **Step 1.2: Add three new role-enforcement tests at the bottom of `SuiteControllerTest`**

These three tests expect `403` but currently receive `201`/`200`/`204` — they will fail until Task 2:

```java
@Test
@DisplayName("POST /suites — TESTER role — returns 403")
public void testCreateSuiteRequiresAdminRole() throws Exception {
    SuiteDTO newSuite = new SuiteDTO();
    newSuite.setName("Should Fail");

    mockMvc.perform(post("/projects/" + projectId + "/suites")
            .requestAttr("role", "TESTER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newSuite)))
            .andExpect(status().isForbidden());
}

@Test
@DisplayName("PUT /suites/{id} — TESTER role — returns 403")
public void testUpdateSuiteRequiresAdminRole() throws Exception {
    SuiteDTO update = new SuiteDTO();
    update.setName("Updated");

    mockMvc.perform(put("/projects/" + projectId + "/suites/" + suiteId)
            .requestAttr("role", "TESTER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isForbidden());
}

@Test
@DisplayName("DELETE /suites/{id} — TESTER role — returns 403")
public void testDeleteSuiteRequiresAdminRole() throws Exception {
    mockMvc.perform(delete("/projects/" + projectId + "/suites/" + suiteId)
            .requestAttr("role", "TESTER"))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 1.3: Run only SuiteControllerTest and confirm the 3 new tests FAIL**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
./gradlew :apps:backend:test --tests "com.java_template.application.controller.SuiteControllerTest" 2>&1 | tail -30
```

Expected: 3 failures (`testCreateSuiteRequiresAdminRole`, `testUpdateSuiteRequiresAdminRole`, `testDeleteSuiteRequiresAdminRole`). Existing tests pass.

---

## Task 2: Implement ADMIN guard in SuiteController (TDD green)

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/controller/SuiteController.java`

- [ ] **Step 2.1: Add `jakarta.servlet.http.HttpServletRequest` import**

Add to the import block (after the existing imports):
```java
import jakarta.servlet.http.HttpServletRequest;
```

- [ ] **Step 2.2: Add role guard to `createSuite`**

Replace:
```java
public ResponseEntity<SuiteDTO> createSuite(@PathVariable UUID projectId, @Valid @RequestBody SuiteDTO suite) {
    suite.setProjectId(projectId);
```
With:
```java
public ResponseEntity<SuiteDTO> createSuite(@PathVariable UUID projectId, @Valid @RequestBody SuiteDTO suite, HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    suite.setProjectId(projectId);
```

- [ ] **Step 2.3: Add role guard to `updateSuite`**

Replace:
```java
public ResponseEntity<SuiteDTO> updateSuite(@PathVariable UUID projectId, @PathVariable UUID id, @Valid @RequestBody SuiteDTO suite) {
    if (!suiteService.suiteExists(id)) {
```
With:
```java
public ResponseEntity<SuiteDTO> updateSuite(@PathVariable UUID projectId, @PathVariable UUID id, @Valid @RequestBody SuiteDTO suite, HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    if (!suiteService.suiteExists(id)) {
```

- [ ] **Step 2.4: Add role guard to `deleteSuite`**

Replace:
```java
public ResponseEntity<Void> deleteSuite(@PathVariable UUID projectId, @PathVariable UUID id) {
    if (suiteService.deleteSuite(id)) {
```
With:
```java
public ResponseEntity<Void> deleteSuite(@PathVariable UUID projectId, @PathVariable UUID id, HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    if (suiteService.deleteSuite(id)) {
```

- [ ] **Step 2.5: Run SuiteControllerTest and confirm all tests PASS**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.SuiteControllerTest" 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Step 2.6: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/controller/SuiteController.java \
        apps/backend/src/test/java/com/java_template/application/controller/SuiteControllerTest.java
git commit -m "$(cat <<'EOF'
feat: restrict suite create/edit/delete to ADMIN role

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Write failing role tests for TestCaseController + TestCaseDirectController (TDD red)

**Files:**
- Modify: `apps/backend/src/test/java/com/java_template/application/controller/TestCaseControllerTest.java`
- Create: `apps/backend/src/test/java/com/java_template/application/controller/TestCaseDirectControllerTest.java`

- [ ] **Step 3.1: Add ADMIN role to existing `testCreateTestCaseUsesPathVariablesInsteadOfBodyIds`**

Add `.requestAttr("role", "ADMIN")` to the perform call:

```java
mockMvc.perform(post("/projects/{projectId}/suites/{suiteId}/cases", projectId, suiteId)
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Test1","priority":"MEDIUM","description":"","preconditions":""}
                        """))
        .andExpect(status().isCreated())
        ...
```

- [ ] **Step 3.2: Add role-enforcement tests to `TestCaseControllerTest`**

Add these imports at the top if not present:
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.junit.jupiter.api.DisplayName;
```

Add tests:
```java
@Test
@DisplayName("POST /cases — TESTER role — returns 403")
public void testCreateTestCaseRequiresAdminRole() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID suiteId = UUID.randomUUID();

    mockMvc.perform(post("/projects/{projectId}/suites/{suiteId}/cases", projectId, suiteId)
                    .requestAttr("role", "TESTER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"title":"Blocked","priority":"MEDIUM","description":"","preconditions":""}
                            """))
            .andExpect(status().isForbidden());
}

@Test
@DisplayName("PUT /cases/{id} — TESTER role — returns 403")
public void testUpdateTestCaseRequiresAdminRole() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID suiteId = UUID.randomUUID();
    UUID caseId = UUID.randomUUID();

    mockMvc.perform(put("/projects/{projectId}/suites/{suiteId}/cases/{id}", projectId, suiteId, caseId)
                    .requestAttr("role", "TESTER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"title":"Blocked","priority":"MEDIUM","description":"","preconditions":""}
                            """))
            .andExpect(status().isForbidden());
}

@Test
@DisplayName("DELETE /cases/{id} — TESTER role — returns 403")
public void testDeleteTestCaseRequiresAdminRole() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID suiteId = UUID.randomUUID();
    UUID caseId = UUID.randomUUID();

    mockMvc.perform(delete("/projects/{projectId}/suites/{suiteId}/cases/{id}", projectId, suiteId, caseId)
                    .requestAttr("role", "TESTER"))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 3.3: Create `TestCaseDirectControllerTest.java`**

```java
package com.java_template.application.controller;

import com.java_template.application.service.TestCaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestCaseDirectController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class TestCaseDirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestCaseService testCaseService;

    @Test
    @DisplayName("DELETE /projects/{projectId}/cases/{caseId} — ADMIN role — returns 204")
    public void testDeleteTestCaseAsAdmin() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        org.mockito.Mockito.when(testCaseService.deleteTestCase(caseId)).thenReturn(true);

        mockMvc.perform(delete("/projects/{projectId}/cases/{caseId}", projectId, caseId)
                        .requestAttr("role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/cases/{caseId} — TESTER role — returns 403")
    public void testDeleteTestCaseAsTesterIsForbidden() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/{projectId}/cases/{caseId}", projectId, caseId)
                        .requestAttr("role", "TESTER"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 3.4: Run tests and confirm new tests FAIL**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestCaseControllerTest" \
    --tests "com.java_template.application.controller.TestCaseDirectControllerTest" 2>&1 | tail -30
```

Expected: `testCreateTestCaseRequiresAdminRole`, `testUpdateTestCaseRequiresAdminRole`, `testDeleteTestCaseRequiresAdminRole`, `testDeleteTestCaseAsTesterIsForbidden` fail. `testDeleteTestCaseAsAdmin` fails too (no guard yet, but we'll fix that).

---

## Task 4: Implement ADMIN guard in TestCaseController + TestCaseDirectController (TDD green)

**Files:**
- Modify: `apps/backend/src/main/java/com/java_template/application/controller/TestCaseController.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/controller/TestCaseDirectController.java`

- [ ] **Step 4.1: Add import to `TestCaseController.java`**

Add to imports:
```java
import jakarta.servlet.http.HttpServletRequest;
```

- [ ] **Step 4.2: Add role guard to `createTestCase`**

Replace:
```java
public ResponseEntity<TestCaseDTO> createTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @Valid @RequestBody TestCaseDTO testCase) {
    testCase.setProjectId(projectId);
```
With:
```java
public ResponseEntity<TestCaseDTO> createTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @Valid @RequestBody TestCaseDTO testCase, HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    testCase.setProjectId(projectId);
```

Add the import for `HttpStatus` if not already present:
```java
import org.springframework.http.HttpStatus;
```

- [ ] **Step 4.3: Add role guard to `updateTestCase`**

Replace:
```java
public ResponseEntity<TestCaseDTO> updateTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @PathVariable UUID id, @Valid @RequestBody TestCaseDTO testCase) {
    if (!testCaseService.testCaseExists(id)) {
```
With:
```java
public ResponseEntity<TestCaseDTO> updateTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @PathVariable UUID id, @Valid @RequestBody TestCaseDTO testCase, HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    if (!testCaseService.testCaseExists(id)) {
```

- [ ] **Step 4.4: Add role guard to `deleteTestCase`**

Replace:
```java
public ResponseEntity<Void> deleteTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @PathVariable UUID id) {
    if (testCaseService.softDeleteTestCase(id)) {
```
With:
```java
public ResponseEntity<Void> deleteTestCase(@PathVariable UUID projectId, @PathVariable UUID suiteId, @PathVariable UUID id, HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    if (testCaseService.softDeleteTestCase(id)) {
```

- [ ] **Step 4.5: Add role guard to `batchCreateTestCases`**

Replace:
```java
public ResponseEntity<List<TestCaseDTO>> batchCreateTestCases(
        @PathVariable UUID projectId,
        @PathVariable UUID suiteId,
        @RequestBody List<BatchImportCaseDTO> items) {
    List<TestCaseDTO> created = testCaseService.batchCreateTestCases(projectId, suiteId, items);
```
With:
```java
public ResponseEntity<List<TestCaseDTO>> batchCreateTestCases(
        @PathVariable UUID projectId,
        @PathVariable UUID suiteId,
        @RequestBody List<BatchImportCaseDTO> items,
        HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    List<TestCaseDTO> created = testCaseService.batchCreateTestCases(projectId, suiteId, items);
```

- [ ] **Step 4.6: Add import and role guard to `TestCaseDirectController.java`**

Add import:
```java
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
```

Replace:
```java
public ResponseEntity<Void> deleteTestCase(
        @PathVariable UUID projectId,
        @PathVariable UUID caseId) {
    if (testCaseService.deleteTestCase(caseId)) {
```
With:
```java
public ResponseEntity<Void> deleteTestCase(
        @PathVariable UUID projectId,
        @PathVariable UUID caseId,
        HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    if (testCaseService.deleteTestCase(caseId)) {
```

- [ ] **Step 4.7: Run tests and confirm all pass**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestCaseControllerTest" \
    --tests "com.java_template.application.controller.TestCaseDirectControllerTest" 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4.8: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/controller/TestCaseController.java \
        apps/backend/src/main/java/com/java_template/application/controller/TestCaseDirectController.java \
        apps/backend/src/test/java/com/java_template/application/controller/TestCaseControllerTest.java \
        apps/backend/src/test/java/com/java_template/application/controller/TestCaseDirectControllerTest.java
git commit -m "$(cat <<'EOF'
feat: restrict test-case create/edit/delete to ADMIN role

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Fix TestRunController — replace equalsIgnoreCase with equals

**Files:**
- Modify: `apps/backend/src/test/java/com/java_template/application/controller/TestRunControllerTest.java`
- Modify: `apps/backend/src/main/java/com/java_template/application/controller/TestRunController.java`

- [ ] **Step 5.1: Update existing unlock tests to use uppercase roles**

In `TestRunControllerTest`, find the two tests at lines ~207–230 that use `"Admin"` and `"Tester"` (mixed case). Update them to use uppercase:

```java
// unlockTestRun_adminRole_returns200 — change:
.requestAttr("role", "Admin")
// to:
.requestAttr("role", "ADMIN")

// unlockTestRun_testerRole_returns200 — change:
.requestAttr("role", "Tester")
// to:
.requestAttr("role", "TESTER")
```

- [ ] **Step 5.2: Verify these tests still pass (equalsIgnoreCase accepts uppercase too)**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestRunControllerTest" 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL — `equalsIgnoreCase` still matches uppercase strings.

- [ ] **Step 5.3: Fix the equalsIgnoreCase in TestRunController**

In `TestRunController.java` line ~119, replace:
```java
if (role == null || (!role.equalsIgnoreCase("Tester") && !role.equalsIgnoreCase("Admin"))) {
    log.warn("===== UNLOCK_RUN FORBIDDEN ===== role={} is not Tester or Admin", role);
```
With:
```java
if (role == null || (!"TESTER".equals(role) && !"ADMIN".equals(role))) {
    log.warn("===== UNLOCK_RUN FORBIDDEN ===== role={} is not TESTER or ADMIN", role);
```

Also remove the debug warn log at line ~117 while you're here (it was a temporary debug statement):
```java
log.warn("===== UNLOCK_RUN ===== role={}", role);
```

- [ ] **Step 5.4: Run tests and confirm they still pass**

```bash
./gradlew :apps:backend:test --tests "com.java_template.application.controller.TestRunControllerTest" 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5.5: Commit**

```bash
git add apps/backend/src/main/java/com/java_template/application/controller/TestRunController.java \
        apps/backend/src/test/java/com/java_template/application/controller/TestRunControllerTest.java
git commit -m "$(cat <<'EOF'
refactor: use case-sensitive equals for role checks in TestRunController

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Frontend — hide admin-only buttons in Projects.tsx

**Files:**
- Modify: `apps/frontend/src/pages/Projects.tsx`

- [ ] **Step 6.1: Add `useAuth` import**

Add after the existing imports (e.g. after `import { formatDate } from '@/lib/utils';`):
```tsx
import { useAuth } from '@/contexts/AuthContext';
```

- [ ] **Step 6.2: Add `isAdmin` constant inside the `Projects` component**

Add after the existing `useState` declarations (after line ~36 `const deleteProjectMutation = useDeleteProject();`):
```tsx
const { user } = useAuth();
const isAdmin = user?.role === 'ADMIN';
```

- [ ] **Step 6.3: Wrap the header "Create Project" button**

Find (around line 95):
```tsx
<Button
  onClick={() => setCreateOpen(true)}
  className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-2 border-0"
>
  <Plus className="h-4 w-4" strokeWidth={1.5} />
  Create Project
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button
    onClick={() => setCreateOpen(true)}
    className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-2 border-0"
  >
    <Plus className="h-4 w-4" strokeWidth={1.5} />
    Create Project
  </Button>
)}
```

- [ ] **Step 6.4: Wrap the empty-state "Create Project" button**

Find (around line 125):
```tsx
<Button
  onClick={() => setCreateOpen(true)}
  className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-2 border-0"
>
  <Plus className="h-4 w-4" strokeWidth={1.5} />
  Create Project
</Button>
```
(This is inside the `!isLoading && !isError && projects.length === 0` block)

Replace with:
```tsx
{isAdmin && (
  <Button
    onClick={() => setCreateOpen(true)}
    className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-2 border-0"
  >
    <Plus className="h-4 w-4" strokeWidth={1.5} />
    Create Project
  </Button>
)}
```

- [ ] **Step 6.5: Wrap the edit (Pencil) and delete (Trash2) buttons in the table row**

Find (around lines 190–205) the two icon buttons with `onClick={() => setEditProject(project)}` and `onClick={() => setDeleteProject(project)}`:
```tsx
<Button
  variant="ghost"
  size="icon"
  className="h-8 w-8 text-muted-foreground hover:text-foreground"
  onClick={() => setEditProject(project)}
>
  <Pencil className="h-4 w-4" strokeWidth={1.5} />
</Button>
<Button
  variant="ghost"
  size="icon"
  className="h-8 w-8 text-muted-foreground hover:text-destructive"
  onClick={() => setDeleteProject(project)}
>
  <Trash2 className="h-4 w-4" strokeWidth={1.5} />
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button
    variant="ghost"
    size="icon"
    className="h-8 w-8 text-muted-foreground hover:text-foreground"
    onClick={() => setEditProject(project)}
  >
    <Pencil className="h-4 w-4" strokeWidth={1.5} />
  </Button>
)}
{isAdmin && (
  <Button
    variant="ghost"
    size="icon"
    className="h-8 w-8 text-muted-foreground hover:text-destructive"
    onClick={() => setDeleteProject(project)}
  >
    <Trash2 className="h-4 w-4" strokeWidth={1.5} />
  </Button>
)}
```

- [ ] **Step 6.6: Run frontend lint**

```bash
cd apps/frontend && npm run lint 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 6.7: Commit**

```bash
git add apps/frontend/src/pages/Projects.tsx
git commit -m "$(cat <<'EOF'
feat: hide project create/edit/delete buttons for TESTER role

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Frontend — hide admin-only buttons in Repository.tsx

**Files:**
- Modify: `apps/frontend/src/pages/Repository.tsx`

- [ ] **Step 7.1: Add `useAuth` import**

Add after the last import line in the file (around line 32):
```tsx
import { useAuth } from '@/contexts/AuthContext';
```

- [ ] **Step 7.2: Add `isAdmin` constant inside the `Repository` component**

Find the start of the component function body (search for `const Repository = () => {` or `function Repository`). Add after the first `const` declarations (e.g. after the `useParams` / `useNavigate` calls):
```tsx
const { user } = useAuth();
const isAdmin = user?.role === 'ADMIN';
```

- [ ] **Step 7.3: Wrap the toolbar "Case" create button (line ~1138)**

Find:
```tsx
<Button
  size="sm"
  className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-1.5 border-0"
  onClick={() => openCreateCase()}
>
  <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Case
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button
    size="sm"
    className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-1.5 border-0"
    onClick={() => openCreateCase()}
  >
    <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Case
  </Button>
)}
```

- [ ] **Step 7.4: Wrap the sidebar "+" create suite button (line ~1237)**

Find:
```tsx
<Button variant="ghost" size="icon" className="h-6 w-6" onClick={openCreateSuite}>
  <Plus className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button variant="ghost" size="icon" className="h-6 w-6" onClick={openCreateSuite}>
    <Plus className="h-3.5 w-3.5" strokeWidth={1.5} />
  </Button>
)}
```

- [ ] **Step 7.5: Wrap the empty-state "Create your first suite" button (line ~1314)**

Find:
```tsx
<Button variant="outline" size="sm" onClick={openCreateSuite} className="mt-1">
  <Plus className="h-3.5 w-3.5 mr-1.5" strokeWidth={1.5} />
  Create your first suite
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button variant="outline" size="sm" onClick={openCreateSuite} className="mt-1">
    <Plus className="h-3.5 w-3.5 mr-1.5" strokeWidth={1.5} />
    Create your first suite
  </Button>
)}
```

- [ ] **Step 7.6: Wrap the per-suite action buttons (lines ~1364–1374)**

Find the `<div className="flex items-center gap-1">` block inside the suite header row that contains the three icon buttons:
```tsx
<Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => openCreateCase(suite.id)}>
  <Plus className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
<Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => openEditSuite(suite)}>
  <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
<Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => copySuite(suite)}>
  <Copy className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
<Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-destructive" onClick={() => confirmDeleteSuite(suite)}>
  <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
```
Replace with (keep Copy button visible for all roles — copying a suite is read-like):
```tsx
{isAdmin && (
  <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => openCreateCase(suite.id)}>
    <Plus className="h-3.5 w-3.5" strokeWidth={1.5} />
  </Button>
)}
{isAdmin && (
  <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => openEditSuite(suite)}>
    <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
  </Button>
)}
<Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => copySuite(suite)}>
  <Copy className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
{isAdmin && (
  <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-destructive" onClick={() => confirmDeleteSuite(suite)}>
    <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
  </Button>
)}
```

- [ ] **Step 7.7: Wrap per-case edit and delete buttons in the case row (lines ~1444–1451)**

Find (inside the case row's actions div):
```tsx
<Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground" onClick={(e) => { e.stopPropagation(); openEditCase(tc); }}>
  <Pencil className="h-3 w-3" strokeWidth={1.5} />
</Button>
<Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground" onClick={(e) => { e.stopPropagation(); copyCase(tc); }}>
  <Copy className="h-3 w-3" strokeWidth={1.5} />
</Button>
<Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground hover:text-destructive" onClick={(e) => { e.stopPropagation(); confirmDeleteCase(tc); }}>
  <Trash2 className="h-3 w-3" strokeWidth={1.5} />
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground" onClick={(e) => { e.stopPropagation(); openEditCase(tc); }}>
    <Pencil className="h-3 w-3" strokeWidth={1.5} />
  </Button>
)}
<Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground" onClick={(e) => { e.stopPropagation(); copyCase(tc); }}>
  <Copy className="h-3 w-3" strokeWidth={1.5} />
</Button>
{isAdmin && (
  <Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground hover:text-destructive" onClick={(e) => { e.stopPropagation(); confirmDeleteCase(tc); }}>
    <Trash2 className="h-3 w-3" strokeWidth={1.5} />
  </Button>
)}
```

- [ ] **Step 7.8: Wrap the "Create quick test" suite-footer button (line ~1465)**

Find:
```tsx
<Button
  variant="ghost"
  size="sm"
  className="text-xs text-muted-foreground gap-1"
  onClick={() => { setQuickCreateSuiteId(suite.id); setQuickCreateTitle(''); setQuickCreateOpen(true); }}
>
  <Plus className="h-3 w-3" strokeWidth={1.5} /> Create quick test
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button
    variant="ghost"
    size="sm"
    className="text-xs text-muted-foreground gap-1"
    onClick={() => { setQuickCreateSuiteId(suite.id); setQuickCreateTitle(''); setQuickCreateOpen(true); }}
  >
    <Plus className="h-3 w-3" strokeWidth={1.5} /> Create quick test
  </Button>
)}
```

- [ ] **Step 7.9: Wrap the case-detail-panel edit button (line ~1506)**

Find:
```tsx
<Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-foreground" onClick={() => openEditCase(selectedCase)}>
  <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
</Button>
```
Replace with:
```tsx
{isAdmin && (
  <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-foreground" onClick={() => openEditCase(selectedCase)}>
    <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
  </Button>
)}
```

- [ ] **Step 7.10: Run frontend lint**

```bash
cd apps/frontend && npm run lint 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 7.11: Commit**

```bash
git add apps/frontend/src/pages/Repository.tsx
git commit -m "$(cat <<'EOF'
feat: hide suite and test-case mutate buttons for TESTER role

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Final verification

- [ ] **Step 8.1: Run full backend test suite**

```bash
cd /Users/Victoria/PycharmProjects/cyoda-test-management-system
./gradlew :apps:backend:test 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL, 0 failures.

- [ ] **Step 8.2: Run frontend lint**

```bash
cd apps/frontend && npm run lint 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 8.3: Run frontend unit tests**

```bash
cd apps/frontend && npm run test -- --run 2>&1 | tail -20
```

Expected: all pass.

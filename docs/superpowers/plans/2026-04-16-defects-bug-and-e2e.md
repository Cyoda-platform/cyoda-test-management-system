# Defects — Bug Fix + Cucumber E2E Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the `keys.testRunDetails is not a function` crash in RunExecution.tsx, then add Cucumber E2E test coverage for the Defects REST API.

**Architecture:** Two independent changes. (1) A two-line typo fix in the frontend query key factory call. (2) A new Cucumber feature file + step-definitions class mirroring the existing `project-crud.feature` / `ProjectApiSteps` pattern; auth/generic HTTP steps are reused from existing classes — no duplication.

**Tech Stack:** React 18 + TypeScript (frontend fix); Java 21 + Spring Boot 3.5 + Cucumber 7 (E2E tests); `TmsHttpClient` HTTP wrapper; JUnit 5 assertions.

---

## File map

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `apps/frontend/src/pages/RunExecution.tsx` | Fix wrong query key factory call |
| Create | `apps/backend/src/test/resources/features/defect-crud.feature` | Gherkin scenarios for Defects API |
| Create | `apps/backend/src/test/java/e2e/steps/DefectApiSteps.java` | Step definitions for defect-crud.feature |

---

## Task 1: Fix the `keys.testRunDetails` crash in RunExecution.tsx

**Files:**
- Modify: `apps/frontend/src/pages/RunExecution.tsx:1018,1041`

`keys.testRunDetails` does not exist on the `keys` object exported from `useApi.ts`. The correct factory is `keys.runs.detail`. This error fires inside the `onSuccess` callback of the `unlockRun` and `completeRun` mutations, causing an unhandled promise rejection that traps the user on the page.

- [ ] **Step 1: Fix line 1018 — unlockRun onSuccess**

Open `apps/frontend/src/pages/RunExecution.tsx`. Find the block around line 1018 and change:

```ts
// BEFORE (line ~1018)
qc.invalidateQueries({ queryKey: keys.testRunDetails(projectId!, runId!) });
```
```ts
// AFTER
qc.invalidateQueries({ queryKey: keys.runs.detail(projectId!, runId!) });
```

- [ ] **Step 2: Fix line 1041 — completeRun onSuccess**

Same file, around line 1041:

```ts
// BEFORE
qc.invalidateQueries({ queryKey: keys.testRunDetails(projectId!, runId!) });
```
```ts
// AFTER
qc.invalidateQueries({ queryKey: keys.runs.detail(projectId!, runId!) });
```

- [ ] **Step 3: Verify the fix compiles**

```bash
cd apps/frontend && npm run build
```

Expected: build completes with no TypeScript errors. If you see `Property 'testRunDetails' does not exist` somewhere else, fix that too. If you see `Property 'detail' does not exist on type...`, double-check the `keys.runs` shape in `apps/frontend/src/hooks/useApi.ts`.

- [ ] **Step 4: Commit**

```bash
git add apps/frontend/src/pages/RunExecution.tsx
git commit -m "fix: replace non-existent keys.testRunDetails with keys.runs.detail in RunExecution

keys.testRunDetails does not exist on the keys object in useApi.ts.
The TypeError thrown inside onSuccess prevented navigation after
adding a defect to a step."
```

---

## Task 2: Write defect-crud.feature (TDD: write the failing tests first)

**Files:**
- Create: `apps/backend/src/test/resources/features/defect-crud.feature`

The Cucumber runner picks up ALL `.feature` files under `src/test/resources/features/` (see `@SelectClasspathResource("features")` in `GherkinE2eTest`). Writing the feature file before the step definitions makes the test run report "undefined step" — this is the TDD "red" state.

- [ ] **Step 1: Create the feature file**

Create `apps/backend/src/test/resources/features/defect-crud.feature` with this exact content:

```gherkin
@e2e @tms
Feature: Defects REST API — CRUD lifecycle
  As a Test Manager I need to create, read, update and delete defects
  so that I can track bugs found during test runs.

  # Auth model is shared with project-crud.feature (see ProjectApiSteps).
  #
  # Defects API (DefectController):
  #   POST   /api/projects/{projectId}/defects              → 201 Created
  #   GET    /api/projects/{projectId}/defects              → 200 { data:[], totalElements, ... }
  #   GET    /api/projects/{projectId}/defects?testRunId=.. → 200 filtered by run
  #   GET    /api/projects/{projectId}/defects/{id}         → 200 | 404
  #   PUT    /api/projects/{projectId}/defects/{id}         → 200 | 404
  #   DELETE /api/projects/{projectId}/defects/{id}         → 204
  #
  # Validation rules on DefectDTO:
  #   title    @NotBlank, @Size(max=255)
  #   severity @NotBlank
  #
  # @smoke    — does NOT call EntityService; no live Cyoda instance required.
  # @requires-cyoda — calls EntityService; needs a live Cyoda instance.

  # ── Authentication ────────────────────────────────────────────────────────

  @smoke
  Scenario: Unauthenticated POST to defects returns 401
    Given I am not authenticated
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/defects" with body:
      """
      {"title":"Bug","severity":"Major"}
      """
    Then the response HTTP status is 401

  # ── Input validation (no Cyoda needed — fails at @Valid boundary) ─────────

  @smoke
  Scenario: Creating a defect with a blank title returns 400
    Given I am logged in as admin
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/defects" with body:
      """
      {"title":"","severity":"Major"}
      """
    Then the response HTTP status is 400

  @smoke
  Scenario: Creating a defect with a blank severity returns 400
    Given I am logged in as admin
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/defects" with body:
      """
      {"title":"Login button is broken","severity":""}
      """
    Then the response HTTP status is 400

  # ── Full CRUD (requires live Cyoda instance) ──────────────────────────────

  @requires-cyoda
  Scenario: Admin creates a defect — response must be 201 with all required fields
    Given I am logged in as admin
    And I have created a defect testing project named "Defect CRUD Project"
    When I create a defect with title "Login button is broken" and severity "Major"
    Then the create defect response HTTP status is 201
    And the defect response body contains field "id"
    And the defect response body contains field "title" with value "Login button is broken"
    And the defect response body contains field "severity" with value "Major"
    And the defect response body contains field "displayId"
    And the defect response body contains field "createdAt"

  @requires-cyoda
  Scenario: Admin can retrieve the defect just created by its ID
    Given I am logged in as admin
    And I have created a defect testing project named "Defect GetById Project"
    And I have created a defect with title "Fetch Defect" and severity "Minor"
    When I GET the last created defect by ID
    Then the get defect response HTTP status is 200
    And the defect response body contains field "title" with value "Fetch Defect"

  @requires-cyoda
  Scenario: GET all defects for a project returns a paged response
    Given I am logged in as admin
    And I have created a defect testing project named "List Defects Project"
    And I have created a defect with title "Listed Defect" and severity "Critical"
    When I GET defects for the last created project
    Then the response HTTP status is 200
    And the paged response body contains the "data" array field
    And the paged response body contains the "totalElements" field

  @requires-cyoda
  Scenario: Admin can update a defect severity and status
    Given I am logged in as admin
    And I have created a defect testing project named "Update Defect Project"
    And I have created a defect with title "Original Defect" and severity "Minor"
    When I update the last created defect severity to "Critical" and status to "In Progress"
    Then the update defect response HTTP status is 200
    And the defect response body contains field "severity" with value "Critical"
    And the defect response body contains field "status" with value "In Progress"

  @requires-cyoda
  Scenario: Admin can delete a defect and it returns 204 No Content
    Given I am logged in as admin
    And I have created a defect testing project named "Delete Defect Project"
    And I have created a defect with title "Delete Me" and severity "Minor"
    When I delete the last created defect
    Then the delete defect response HTTP status is 204

  @requires-cyoda
  Scenario: Create a defect linked to a test run — testRunId is persisted
    Given I am logged in as admin
    And I have created a defect testing project named "Run-linked Defect Project"
    When I create a defect with title "Run-linked Bug" and severity "Major" and testRunId "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    Then the create defect response HTTP status is 201
    And the defect response body contains field "testRunId" with value "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"

  @requires-cyoda
  Scenario: GET defects filtered by testRunId returns only matching defects
    Given I am logged in as admin
    And I have created a defect testing project named "Filter Defects Project"
    And I have created a defect with title "Run Defect" and severity "Major" and testRunId "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    And I have created a defect with title "Unrelated Defect" and severity "Minor"
    When I GET defects for the last created project filtered by testRunId "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    Then the response HTTP status is 200
    And the filtered defect list contains 1 item
    And the first filtered defect has title "Run Defect"
```

- [ ] **Step 2: Run the smoke tests to confirm "undefined step" failures**

```bash
./gradlew :apps:backend:cucumberTest -Dcucumber.filter.tags="@smoke"
```

Expected: test run reports **UNDEFINED** steps for `I have created a defect testing project named`, `I create a defect with title...`, `the create defect response HTTP status is`, etc. Steps shared from existing classes (`I am logged in as admin`, `I POST to ... with body:`, `the response HTTP status is {int}`) will be matched. This is the expected TDD red state — proceed to Task 3.

---

## Task 3: Implement DefectApiSteps.java

**Files:**
- Create: `apps/backend/src/test/java/e2e/steps/DefectApiSteps.java`

Steps shared from existing classes (do NOT redefine them here):
- `I am logged in as admin` — `ProjectApiSteps`
- `I am not authenticated` — `ProjectApiSteps`
- `I POST to {string} with body:` — `ProjectApiSteps`
- `the response HTTP status is {int}` — `HealthCheckSteps`
- `the paged response body contains the {string} array field` — `ProjectApiSteps`
- `the paged response body contains the {string} field` — `ProjectApiSteps`

- [ ] **Step 1: Create DefectApiSteps.java**

Create `apps/backend/src/test/java/e2e/steps/DefectApiSteps.java`:

```java
package e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.GherkinE2eTest;
import e2e.support.TmsHttpClient;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for {@code defect-crud.feature}.
 *
 * <p>Auth steps ({@code I am logged in as admin} etc.) and generic HTTP assertion
 * steps ({@code the response HTTP status is {int}}) are shared from
 * {@code ProjectApiSteps} and {@code HealthCheckSteps} — globally available in
 * the same Cucumber run and NOT redefined here.
 *
 * <p><b>Lifecycle:</b> Scenarios that create Cyoda entities use
 * "I have created a defect testing project named ..." to own a project.
 * The {@code @After} hook deletes all tracked defects then the project
 * (bottom-up order per {@code apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md}).
 */
public class DefectApiSteps {

    private static final Logger log = LoggerFactory.getLogger(DefectApiSteps.class);
    private static final String CONTEXT_PATH = "/api";

    @Autowired
    private TmsHttpClient http;

    @Autowired
    private GherkinE2eTest runner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** IDs of defects created during a scenario — deleted in @After. */
    private final List<String> createdDefectIds = new ArrayList<>();

    /** Project created by this class — deleted in @After after defects are gone. */
    private String createdProjectId;

    /** Last response from a defect-specific API call (create / get / update). */
    private ResponseEntity<String> lastDefectResponse;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Before
    public void configureHttpClient() {
        http.setServerPort(runner.serverPort);
        http.reset();
        createdDefectIds.clear();
        createdProjectId = null;
        lastDefectResponse = null;
    }

    @After
    public void cleanupCreatedDefects() {
        // Bottom-up deletion: defects before project (per SCHEMA_AND_WORKFLOW_IMPORT.md)
        if (createdProjectId != null) {
            for (String defectId : createdDefectIds) {
                try {
                    http.delete(CONTEXT_PATH + "/projects/" + createdProjectId + "/defects/" + defectId);
                    log.info("[Cleanup] Deleted defect {}", defectId);
                } catch (Exception e) {
                    log.warn("[Cleanup] Failed to delete defect {}: {}", defectId, e.getMessage());
                }
            }
            try {
                http.delete(CONTEXT_PATH + "/projects/" + createdProjectId);
                log.info("[Cleanup] Deleted project {}", createdProjectId);
            } catch (Exception e) {
                log.warn("[Cleanup] Failed to delete project {}: {}", createdProjectId, e.getMessage());
            }
        }
        createdDefectIds.clear();
        createdProjectId = null;
    }

    // ── Given ─────────────────────────────────────────────────────────────

    @Given("I have created a defect testing project named {string}")
    public void i_have_created_a_defect_testing_project(String name) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"description\":\"E2E defect test project\"}";
        ResponseEntity<String> response = http.post(CONTEXT_PATH + "/projects", body);
        assertEquals(201, response.getStatusCode().value(),
                "Prerequisite project creation failed. Status: " + response.getStatusCode().value()
                        + ", Body: " + response.getBody());
        createdProjectId = extractField(response.getBody(), "id");
        assertNotNull(createdProjectId,
                "Project creation response did not contain 'id'. Body: " + response.getBody());
    }

    @Given("I have created a defect with title {string} and severity {string}")
    public void i_have_created_a_defect(String title, String severity) throws Exception {
        assertNotNull(createdProjectId, "Cannot create defect — no project has been created yet");
        String body = buildDefectBody(title, severity, null);
        lastDefectResponse = http.post(
                CONTEXT_PATH + "/projects/" + createdProjectId + "/defects", body);
        assertEquals(201, http.lastStatus(),
                "Prerequisite defect creation failed. Status: " + http.lastStatus()
                        + ", Body: " + http.lastBody());
        String id = extractField(http.lastBody(), "id");
        if (id != null) createdDefectIds.add(id);
    }

    @Given("I have created a defect with title {string} and severity {string} and testRunId {string}")
    public void i_have_created_a_defect_with_run(String title, String severity, String testRunId) throws Exception {
        assertNotNull(createdProjectId, "Cannot create defect — no project has been created yet");
        String body = buildDefectBody(title, severity, testRunId);
        lastDefectResponse = http.post(
                CONTEXT_PATH + "/projects/" + createdProjectId + "/defects", body);
        assertEquals(201, http.lastStatus(),
                "Prerequisite defect creation (with testRunId) failed. Status: " + http.lastStatus()
                        + ", Body: " + http.lastBody());
        String id = extractField(http.lastBody(), "id");
        if (id != null) createdDefectIds.add(id);
    }

    // ── When ──────────────────────────────────────────────────────────────

    @When("I create a defect with title {string} and severity {string}")
    public void i_create_a_defect(String title, String severity) {
        assertNotNull(createdProjectId, "Cannot create defect — no project has been created yet");
        String body = buildDefectBody(title, severity, null);
        lastDefectResponse = http.post(
                CONTEXT_PATH + "/projects/" + createdProjectId + "/defects", body);
        String id = extractField(http.lastBody(), "id");
        if (id != null) createdDefectIds.add(id);
    }

    @When("I create a defect with title {string} and severity {string} and testRunId {string}")
    public void i_create_a_defect_with_run(String title, String severity, String testRunId) {
        assertNotNull(createdProjectId, "Cannot create defect — no project has been created yet");
        String body = buildDefectBody(title, severity, testRunId);
        lastDefectResponse = http.post(
                CONTEXT_PATH + "/projects/" + createdProjectId + "/defects", body);
        String id = extractField(http.lastBody(), "id");
        if (id != null) createdDefectIds.add(id);
    }

    @When("I GET the last created defect by ID")
    public void i_get_the_last_created_defect_by_id() throws Exception {
        assertNotNull(lastDefectResponse, "Cannot GET by ID — no defect has been created yet");
        String id = extractField(lastDefectResponse.getBody(), "id");
        assertNotNull(id, "Cannot GET by ID — defect id was not found in last response");
        lastDefectResponse = http.get(
                CONTEXT_PATH + "/projects/" + createdProjectId + "/defects/" + id);
    }

    @When("I GET defects for the last created project")
    public void i_get_defects_for_the_last_created_project() {
        assertNotNull(createdProjectId, "Cannot list defects — no project has been created yet");
        http.get(CONTEXT_PATH + "/projects/" + createdProjectId + "/defects");
    }

    @When("I GET defects for the last created project filtered by testRunId {string}")
    public void i_get_defects_filtered_by_run(String testRunId) {
        assertNotNull(createdProjectId, "Cannot list defects — no project has been created yet");
        http.get(CONTEXT_PATH + "/projects/" + createdProjectId + "/defects?testRunId=" + testRunId);
    }

    @When("I update the last created defect severity to {string} and status to {string}")
    public void i_update_the_last_created_defect(String newSeverity, String newStatus) throws Exception {
        assertNotNull(lastDefectResponse, "Cannot update — no defect has been created yet");
        String id = extractField(lastDefectResponse.getBody(), "id");
        assertNotNull(id, "Cannot update — defect id not found in last response");

        // Backend uses full-replacement PUT — read current fields and override only severity+status
        JsonNode current = objectMapper.readTree(lastDefectResponse.getBody());
        String title = current.has("title") ? current.get("title").asText() : "Defect";
        String description = current.has("description") && !current.get("description").isNull()
                ? current.get("description").asText() : "";
        String link = current.has("link") && !current.get("link").isNull()
                ? current.get("link").asText() : "";
        String source = current.has("source") && !current.get("source").isNull()
                ? current.get("source").asText() : "";

        String updateBody = String.format(
                "{\"title\":\"%s\",\"description\":\"%s\",\"severity\":\"%s\",\"status\":\"%s\",\"link\":\"%s\",\"source\":\"%s\"}",
                escapeJson(title), escapeJson(description),
                newSeverity, newStatus,
                escapeJson(link), escapeJson(source));

        lastDefectResponse = http.put(
                CONTEXT_PATH + "/projects/" + createdProjectId + "/defects/" + id, updateBody);
    }

    @When("I delete the last created defect")
    public void i_delete_the_last_created_defect() throws Exception {
        assertNotNull(lastDefectResponse, "Cannot DELETE — no defect has been created yet");
        String id = extractField(lastDefectResponse.getBody(), "id");
        assertNotNull(id, "Cannot DELETE — defect id not found in last response");
        http.delete(CONTEXT_PATH + "/projects/" + createdProjectId + "/defects/" + id);
        createdDefectIds.remove(id);  // already deleted — skip @After cleanup
    }

    // ── Then ──────────────────────────────────────────────────────────────

    @Then("the create defect response HTTP status is {int}")
    public void the_create_defect_response_http_status_is(int expected) {
        assertNotNull(lastDefectResponse, "No defect create call has been made");
        assertEquals(expected, lastDefectResponse.getStatusCode().value(),
                "Create defect status mismatch. Body: " + lastDefectResponse.getBody());
    }

    @Then("the get defect response HTTP status is {int}")
    public void the_get_defect_response_http_status_is(int expected) {
        assertNotNull(lastDefectResponse, "No defect GET call has been made");
        assertEquals(expected, lastDefectResponse.getStatusCode().value(),
                "Get defect status mismatch. Body: " + lastDefectResponse.getBody());
    }

    @Then("the update defect response HTTP status is {int}")
    public void the_update_defect_response_http_status_is(int expected) {
        assertNotNull(lastDefectResponse, "No defect PUT call has been made");
        assertEquals(expected, lastDefectResponse.getStatusCode().value(),
                "Update defect status mismatch. Body: " + lastDefectResponse.getBody());
    }

    @Then("the delete defect response HTTP status is {int}")
    public void the_delete_defect_response_http_status_is(int expected) {
        assertEquals(expected, http.lastStatus(),
                "Delete defect status mismatch. Body: " + http.lastBody());
    }

    @Then("the defect response body contains field {string}")
    public void the_defect_response_body_contains_field(String field) throws Exception {
        assertNotNull(lastDefectResponse, "No defect response available");
        JsonNode root = objectMapper.readTree(lastDefectResponse.getBody());
        assertTrue(root.has(field) && !root.get(field).isNull(),
                "Expected non-null field '" + field + "' in defect response: "
                        + lastDefectResponse.getBody());
    }

    @Then("the defect response body contains field {string} with value {string}")
    public void the_defect_response_body_contains_field_with_value(String field, String expected) throws Exception {
        assertNotNull(lastDefectResponse, "No defect response available");
        JsonNode root = objectMapper.readTree(lastDefectResponse.getBody());
        assertTrue(root.has(field),
                "Expected field '" + field + "' in defect response: " + lastDefectResponse.getBody());
        assertEquals(expected, root.get(field).asText(),
                "Field '" + field + "' value mismatch. Body: " + lastDefectResponse.getBody());
    }

    @Then("the filtered defect list contains {int} item(s)")
    public void the_filtered_defect_list_contains(int count) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        JsonNode data = root.get("data");
        assertNotNull(data, "Expected 'data' array in response: " + http.lastBody());
        assertTrue(data.isArray(), "'data' should be a JSON array: " + http.lastBody());
        assertEquals(count, data.size(),
                "Expected " + count + " defect(s) in 'data'. Body: " + http.lastBody());
    }

    @Then("the first filtered defect has title {string}")
    public void the_first_filtered_defect_has_title(String expectedTitle) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        JsonNode data = root.get("data");
        assertNotNull(data, "Expected 'data' array in response: " + http.lastBody());
        assertTrue(data.isArray() && data.size() > 0,
                "'data' array should be non-empty. Body: " + http.lastBody());
        assertEquals(expectedTitle, data.get(0).get("title").asText(),
                "Expected first defect title '" + expectedTitle + "'. Body: " + http.lastBody());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String buildDefectBody(String title, String severity, String testRunId) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\"");
        sb.append(",\"severity\":\"").append(escapeJson(severity)).append("\"");
        sb.append(",\"status\":\"Open\"");
        if (testRunId != null) {
            sb.append(",\"testRunId\":\"").append(testRunId).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /** Minimal JSON string escaping for ASCII test data. */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractField(String json, String field) {
        if (json == null) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode node = root.get(field);
            return (node != null && !node.isNull()) ? node.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 2: Compile the test sources**

```bash
./gradlew :apps:backend:compileTestJava
```

Expected: `BUILD SUCCESSFUL`. If there are compilation errors, fix them before continuing. Common causes: wrong import, method signature mismatch, missing `throws Exception` on a step method.

- [ ] **Step 3: Run smoke tests**

```bash
./gradlew :apps:backend:cucumberTest -Dcucumber.filter.tags="@smoke"
```

Expected: all 3 `@smoke` scenarios **PASS**:
- `Unauthenticated POST to defects returns 401` — auth filter short-circuits before controller
- `Creating a defect with a blank title returns 400` — `@Valid` rejects empty title
- `Creating a defect with a blank severity returns 400` — `@Valid` rejects empty severity

If a smoke test fails with `404` instead of the expected status, check that the controller mapping `/api/projects/{projectId}/defects` is live (the app starts in test mode with `WebEnvironment.RANDOM_PORT`).

- [ ] **Step 4: (If Cyoda available) Run full E2E suite**

```bash
./gradlew :apps:backend:cucumberTest
```

Expected: `@smoke` scenarios pass. `@requires-cyoda` scenarios pass if a Cyoda instance is configured in `apps/backend/src/test/resources/application-cucumber.yaml`. If Cyoda is not available, `@requires-cyoda` scenarios fail with a connection error — this is expected.

- [ ] **Step 5: Commit E2E tests**

```bash
git add apps/backend/src/test/resources/features/defect-crud.feature \
        apps/backend/src/test/java/e2e/steps/DefectApiSteps.java
git commit -m "test(e2e): add Cucumber E2E coverage for Defects REST API

Covers CRUD lifecycle, testRunId linking, and testRunId filter.
Smoke scenarios run without Cyoda (validation-layer only).
Requires-cyoda scenarios need a live Cyoda instance."
```

---

## Self-review checklist

- [x] Bug fix: both occurrences of `keys.testRunDetails` replaced → `keys.runs.detail`
- [x] @smoke 401: uses `I am not authenticated` (ProjectApiSteps) + generic `the response HTTP status is {int}` (HealthCheckSteps) — no new steps needed
- [x] @smoke 400: uses `I am logged in as admin` + `I POST to {string} with body:` (ProjectApiSteps) — no new steps needed
- [x] `I have created a defect testing project named {string}` — unique phrase, no conflict with ProjectApiSteps' `I have created a project named {string} with description {string}`
- [x] @After deletes defects before project (bottom-up per SCHEMA_AND_WORKFLOW_IMPORT.md)
- [x] Full PUT body reconstruction in update step matches DefectController's full-replacement semantics
- [x] `buildDefectBody` includes `"status":"Open"` so the required non-blank status is always set
- [x] Filter scenario creates 2 defects (one with testRunId, one without) and asserts exactly 1 returned
- [x] No placeholder text, no TBD sections, all code is complete

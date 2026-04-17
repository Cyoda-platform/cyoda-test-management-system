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
 *
 * <p><b>@Before notes:</b> Both this class and {@code ProjectApiSteps} have
 * unscoped {@code @Before} hooks. Both call {@code http.reset()} and
 * {@code http.setServerPort()} — these are idempotent so running them twice
 * per scenario is harmless. The Bearer token is set by the {@code "I am logged
 * in as admin"} Given step, which runs after all {@code @Before} hooks complete.
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
    public void configureDefectHttpClient() {
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
        // Return value not captured: Then steps use shared `the response HTTP status is {int}`
        // and `the paged response body contains...` steps that read http.lastStatus()/lastBody() directly.
        http.get(CONTEXT_PATH + "/projects/" + createdProjectId + "/defects");
    }

    @When("I GET defects for the last created project filtered by testRunId {string}")
    public void i_get_defects_filtered_by_run(String testRunId) {
        assertNotNull(createdProjectId, "Cannot list defects — no project has been created yet");
        // Return value not captured: Then steps (the_filtered_defect_list_contains,
        // the_first_filtered_defect_has_title) read http.lastBody() directly.
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

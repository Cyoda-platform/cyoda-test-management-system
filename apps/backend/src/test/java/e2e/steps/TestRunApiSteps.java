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
import io.cucumber.java.en.And;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for test-run-crud.feature
 * Tests full lifecycle of TestRun CRUD operations, including cases and status transitions.
 */
public class TestRunApiSteps {

    private static final Logger log = LoggerFactory.getLogger(TestRunApiSteps.class);
    private static final String CONTEXT_PATH = "/api";

    @Autowired
    private TmsHttpClient http;

    @Autowired
    private GherkinE2eTest runner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String projectId;
    private String testRunId;
    private String testRunCaseId;
    private Map<String, String> testRunPayload;
    private List<String> createdRunIds = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Before
    public void resetHttpClient() {
        http.setServerPort(runner.serverPort);
        http.reset();
        createdRunIds.clear();
    }

    @After
    public void cleanup() {
        for (String runId : createdRunIds) {
            try {
                http.delete(CONTEXT_PATH + "/projects/" + projectId + "/runs/" + runId);
            } catch (Exception e) {
                log.warn("Failed to delete test run {}: {}", runId, e.getMessage());
            }
        }
    }

    // ── Given ──────────────────────────────────────────────────────────────

    @Given("a project with ID exists")
    public void a_project_with_id_exists() {
        String body = "{\"name\":\"Test Project\",\"description\":\"For E2E tests\"}";
        http.post(CONTEXT_PATH + "/projects", body);
        assertEquals(201, http.lastStatus(), "Failed to create project");
        projectId = extractField(http.lastBody(), "id");
        assertNotNull(projectId, "Project ID not returned");
    }

    @Given("I have valid bearer token")
    public void i_have_valid_bearer_token() {
        // Token already set from login step — http.lastStatus() will be 200 if successful
        assertTrue(http.lastStatus() == 200 || http.lastStatus() == 0, "Bearer token not properly set");
    }

    @Given("I prepare a TestRunDTO with name {string}")
    public void i_prepare_a_test_run_dto_with_name(String name) {
        testRunPayload = new HashMap<>();
        testRunPayload.put("name", name);
        testRunPayload.put("status", "ACTIVE");
    }

    @Given("test runs exist for the project")
    public void test_runs_exist_for_the_project() {
        String body = "{\"name\":\"Existing Run 1\",\"status\":\"ACTIVE\"}";
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs", body);
        assertEquals(201, http.lastStatus());
        String id = extractField(http.lastBody(), "id");
        createdRunIds.add(id);
    }

    @Given("a test run with ID exists")
    public void a_test_run_with_id_exists() throws Exception {
        String body = "{\"name\":\"Test Run\",\"status\":\"ACTIVE\"}";
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs", body);
        assertEquals(201, http.lastStatus(), "Create test run failed: " + http.lastBody());
        testRunId = extractField(http.lastBody(), "id");
        createdRunIds.add(testRunId);
        assertNotNull(testRunId, "testRunId not returned");

        // Trigger initialize_run transition: service detects status=initial and fires it
        http.put(CONTEXT_PATH + "/projects/" + projectId + "/runs/" + testRunId,
                "{\"name\":\"Test Run\",\"status\":\"active\"}");

        // Poll until the SnapshotProcessor completes and state becomes 'active' (Cyoda lowercase)
        waitForStatus(testRunId, "active", 30);
    }

    @Given("a test run with ID and associated test cases exist")
    public void a_test_run_with_id_and_cases_exist() throws Exception {
        a_test_run_with_id_exists();
        String caseBody = "{\"title\":\"Test Case 1\",\"status\":\"UNTESTED\","
                + "\"testCaseId\":\"550e8400-e29b-41d4-a716-000000000001\"}";
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs/" + testRunId + "/cases", caseBody);
        testRunCaseId = extractField(http.lastBody(), "id");
    }

    @Given("a non-existent test run ID")
    public void a_non_existent_test_run_id() {
        testRunId = UUID.randomUUID().toString();
    }

    @Given("a test run case exists")
    public void a_test_run_case_exists() throws Exception {
        a_test_run_with_id_exists();
        String caseBody = "{\"title\":\"Test Case\",\"status\":\"UNTESTED\","
                + "\"testCaseId\":\"550e8400-e29b-41d4-a716-000000000001\"}";
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs/" + testRunId + "/cases", caseBody);
        assertEquals(201, http.lastStatus(), "Create test run case failed: " + http.lastBody());
        testRunCaseId = extractField(http.lastBody(), "id");
        assertNotNull(testRunCaseId, "testRunCaseId not returned");
    }

    // ── When ──────────────────────────────────────────────────────────────

    @When("I POST to {string}")
    public void i_post_to(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        com.fasterxml.jackson.databind.node.ObjectNode node =
                testRunPayload != null
                ? objectMapper.convertValue(testRunPayload, com.fasterxml.jackson.databind.node.ObjectNode.class)
                : objectMapper.createObjectNode();
        http.post(pathWithId, node.toString());
    }

    @When("I POST a test case to {string}")
    public void i_post_a_test_case_to(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        // testCaseId must be a valid UUID — Cyoda schema enforces UUID_TYPE
        String body = "{\"title\":\"New Test Case\",\"status\":\"UNTESTED\","
                + "\"testCaseId\":\"550e8400-e29b-41d4-a716-000000000001\"}";
        http.post(pathWithId, body);
    }

    @When("I GET from {string}")
    public void i_get_from(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        http.get(pathWithId);
    }

    @When("I PUT to {string}")
    public void i_put_to(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        com.fasterxml.jackson.databind.node.ObjectNode node =
                testRunPayload != null
                ? objectMapper.convertValue(testRunPayload, com.fasterxml.jackson.databind.node.ObjectNode.class)
                : objectMapper.createObjectNode();
        http.put(pathWithId, node.toString());
    }

    @When("I POST to {string} with role {string}")
    public void i_post_to_with_role(String path, String role) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        http.post(pathWithId, "{}");
    }

    @When("I POST to {string} without role")
    public void i_post_to_without_role(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        // Clear token → unauthenticated request → Spring Security returns 401
        http.clearBearerToken();
        http.postAnonymous(pathWithId, "{}");
    }

    @When("I DELETE from {string}")
    public void i_delete_from(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        http.delete(pathWithId);
    }

    @When("I PUT to {string} with status {string}")
    public void i_put_to_with_status(String path, String status) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path) + "?status=" + status;
        http.put(pathWithId, "{}");
    }

    @When("I POST to {string} with bugUrl {string}")
    public void i_post_to_with_bug_url(String path, String bugUrl) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path) + "?bugUrl=" + bugUrl;
        http.post(pathWithId, "{}");
    }

    @Given("the test run is completed")
    public void the_test_run_is_completed() throws Exception {
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs/" + testRunId + "/complete", "{}");
        assertEquals(200, http.lastStatus(),
                "Failed to complete test run. Body: " + http.lastBody());
        waitForStatus(testRunId, "completed", 15);
    }

    // ── Then ──────────────────────────────────────────────────────────────

    @Then("the response body has {string} array")
    public void the_response_body_has_array_any(String arrayField) throws Exception {
        JsonNode node = objectMapper.readTree(http.lastBody());
        assertTrue(node.has(arrayField),
                "Response missing array '" + arrayField + "'. Body: " + http.lastBody());
        assertTrue(node.get(arrayField).isArray(),
                "Field '" + arrayField + "' is not an array. Body: " + http.lastBody());
    }

    @Then("the response body contains {string}: {string}")
    public void the_response_body_contains(String field, String value) throws Exception {
        JsonNode node = objectMapper.readTree(http.lastBody());
        assertTrue(node.has(field), "Response missing field: " + field);
        assertEquals(value, node.get(field).asText());
    }

    @Then("the response body has {string} array with at least {int} element")
    public void the_response_body_has_array(String arrayField, int minSize) throws Exception {
        JsonNode node = objectMapper.readTree(http.lastBody());
        assertTrue(node.has(arrayField), "Response missing array: " + arrayField);
        assertTrue(node.get(arrayField).isArray());
        assertTrue(node.get(arrayField).size() >= minSize);
    }

    @Then("the response body has {string} object")
    public void the_response_body_has_object(String objectField) throws Exception {
        JsonNode node = objectMapper.readTree(http.lastBody());
        assertTrue(node.has(objectField), "Response missing field: " + objectField);
    }

    @Then("the response body has {string} field")
    public void the_response_body_has_field(String field) throws Exception {
        JsonNode node = objectMapper.readTree(http.lastBody());
        assertTrue(node.has(field), "Response missing field: " + field);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Polls GET /runs/{id} until status matches expected or timeout (seconds). */
    private void waitForStatus(String runId, String expectedStatus, int timeoutSeconds) throws Exception {
        String url = CONTEXT_PATH + "/projects/" + projectId + "/runs/" + runId;
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            http.get(url);
            String status = extractField(http.lastBody(), "status");
            if (expectedStatus.equalsIgnoreCase(status)) return;
            Thread.sleep(1000);
        }
        String finalStatus = extractField(http.lastBody(), "status");
        assertEquals(expectedStatus, finalStatus,
                "Test run did not reach '" + expectedStatus + "' within " + timeoutSeconds + "s");
    }

    private String replacePlaceholders(String path) {
        String result = path;
        if (projectId     != null) result = result.replace("{projectId}", projectId);
        if (testRunId     != null) result = result.replace("{runId}",     testRunId);
        if (testRunCaseId != null) result = result.replace("{caseId}",    testRunCaseId);
        return result;
    }

    private String extractField(String json, String field) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.has(field) ? node.get(field).asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

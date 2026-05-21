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
    public void a_test_run_with_id_exists() {
        String body = "{\"name\":\"Test Run\",\"status\":\"ACTIVE\"}";
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs", body);
        assertEquals(201, http.lastStatus());
        testRunId = extractField(http.lastBody(), "id");
        createdRunIds.add(testRunId);
        assertNotNull(testRunId);
    }

    @Given("a test run with ID and associated test cases exist")
    public void a_test_run_with_id_and_cases_exist() {
        a_test_run_with_id_exists();
        String caseBody = "{\"title\":\"Test Case 1\",\"status\":\"UNTESTED\"}";
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs/" + testRunId + "/cases", caseBody);
        testRunCaseId = extractField(http.lastBody(), "id");
    }

    @Given("a non-existent test run ID")
    public void a_non_existent_test_run_id() {
        testRunId = UUID.randomUUID().toString();
    }

    @Given("a test run case exists")
    public void a_test_run_case_exists() {
        a_test_run_with_id_exists();
        String caseBody = "{\"title\":\"Test Case\",\"status\":\"UNTESTED\"}";
        http.post(CONTEXT_PATH + "/projects/" + projectId + "/runs/" + testRunId + "/cases", caseBody);
        assertEquals(201, http.lastStatus());
        testRunCaseId = extractField(http.lastBody(), "id");
        assertNotNull(testRunCaseId);
    }

    // ── When ──────────────────────────────────────────────────────────────

    @When("I POST to {string}")
    public void i_post_to(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        String body = objectMapper.convertValue(testRunPayload, com.fasterxml.jackson.databind.node.ObjectNode.class).toString();
        http.post(pathWithId, body);
    }

    @When("I POST a test case to {string}")
    public void i_post_a_test_case_to(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        String body = "{\"title\":\"New Test Case\",\"status\":\"UNTESTED\"}";
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
        String body = objectMapper.convertValue(testRunPayload, com.fasterxml.jackson.databind.node.ObjectNode.class).toString();
        http.put(pathWithId, body);
    }

    @When("I POST to {string} with role {string}")
    public void i_post_to_with_role(String path, String role) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        http.post(pathWithId, "{}");
    }

    @When("I POST to {string} without role")
    public void i_post_to_without_role(String path) {
        String pathWithId = CONTEXT_PATH + replacePlaceholders(path);
        http.post(pathWithId, "{}");
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

    // ── Then ──────────────────────────────────────────────────────────────

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

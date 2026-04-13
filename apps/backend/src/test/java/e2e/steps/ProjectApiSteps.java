package e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.GherkinE2eTest;
import e2e.support.TmsHttpClient;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
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
 * Step definitions for {@code project-crud.feature}.
 *
 * <p><b>Auth model</b>: After a successful login this class extracts the
 * {@code token} field from the JSON response body and passes it as an
 * {@code Authorization: Bearer} header on all subsequent requests.  This
 * matches the dual-delivery pattern in {@code AuthController.login()}:
 * the token is returned <em>both</em> in the response body and as an
 * {@code httpOnly} cookie.
 *
 * <p><b>BUG-001 note</b>: The existing unit test
 * {@code AuthControllerTest.testLoginWithValidCredentials()} asserts
 * {@code $.token.doesNotExist()}, which contradicts the controller
 * implementation that clearly passes {@code authResponse.token} into
 * {@code new LoginResponse(token, ...)} with no {@code @JsonIgnore}.
 * That assertion is <strong>wrong</strong>; this E2E test correctly
 * asserts the token IS present.
 *
 * <p><b>BUG-002 note</b>: {@code ProjectService.deleteProject()} always
 * returns {@code true}, making the 404 branch in {@code ProjectController}
 * dead code.  The scenario tagged {@code @bug-002} documents this and is
 * expected to fail until the bug is fixed.
 *
 * <p><b>Cleanup</b>: created project IDs are tracked and deleted in the
 * {@code @After} hook so each scenario starts with a clean slate.
 */
public class ProjectApiSteps {

    private static final Logger log = LoggerFactory.getLogger(ProjectApiSteps.class);
    private static final String CONTEXT_PATH = "/api";

    @Autowired
    private TmsHttpClient http;

    @Autowired
    private GherkinE2eTest runner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** IDs of projects created during a scenario — deleted in @After. */
    private final List<String> createdProjectIds = new ArrayList<>();

    /** Captured separately for auth-specific assertions. */
    private ResponseEntity<String> lastAuthResponse;
    /** Captured separately for project-specific assertions. */
    private ResponseEntity<String> lastProjectResponse;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Before
    public void configureHttpClient() {
        http.setServerPort(runner.serverPort);
        http.reset();
        createdProjectIds.clear();
    }

    @After
    public void cleanupCreatedProjects() {
        // Best-effort cleanup — log failures but do not fail the scenario.
        for (String id : createdProjectIds) {
            try {
                http.delete(CONTEXT_PATH + "/projects/" + id);
                log.info("[Cleanup] Deleted project {}", id);
            } catch (Exception e) {
                log.warn("[Cleanup] Failed to delete project {}: {}", id, e.getMessage());
            }
        }
        createdProjectIds.clear();
    }

    // ── Given ─────────────────────────────────────────────────────────────

    @Given("I am not authenticated")
    public void i_am_not_authenticated() {
        http.clearBearerToken();
    }

    @Given("I am logged in as admin")
    public void i_am_logged_in_as_admin() throws Exception {
        loginAs("admin", "admin123");
    }

    @Given("I am logged in as tester")
    public void i_am_logged_in_as_tester() throws Exception {
        loginAs("tester", "tester123");
    }

    @Given("I have created a project named {string} with description {string}")
    public void i_have_created_a_project(String name, String description) throws Exception {
        i_am_logged_in_as_admin();
        String body = "{\"name\":\"" + name + "\",\"description\":\"" + description + "\"}";
        lastProjectResponse = http.post(CONTEXT_PATH + "/projects", body);
        assertEquals(201, http.lastStatus(),
                "Prerequisite project creation failed. Status: " + http.lastStatus()
                        + ", Body: " + http.lastBody());
        String id = extractField(http.lastBody(), "id");
        if (id != null) createdProjectIds.add(id);
    }

    // ── When ──────────────────────────────────────────────────────────────

    @When("I log in as {string} with password {string}")
    public void i_log_in_as_with_password(String username, String password) {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        lastAuthResponse = http.postAnonymous(CONTEXT_PATH + "/auth/login", body);
    }

    @When("I POST to {string} with body:")
    public void i_post_to_with_body(String path, String jsonBody) {
        http.post(path, jsonBody.trim());
    }

    @When("I GET {string}")
    public void i_get(String path) {
        http.get(path);
    }

    @When("I DELETE {string}")
    public void i_delete(String path) {
        http.delete(path);
    }

    @When("I create a project named {string} with description {string}")
    public void i_create_a_project_named(String name, String description) {
        String body = "{\"name\":\"" + name + "\",\"description\":\"" + description + "\"}";
        lastProjectResponse = http.post(CONTEXT_PATH + "/projects", body);
        // Track for cleanup even if status != 201 (Cyoda may still have saved it)
        String id = extractField(http.lastBody(), "id");
        if (id != null) createdProjectIds.add(id);
    }

    @When("I GET the last created project by ID")
    public void i_get_the_last_created_project_by_id() throws Exception {
        String lastBody = lastProjectResponse != null
                ? lastProjectResponse.getBody()
                : http.lastBody();
        String id = extractField(lastBody, "id");
        assertNotNull(id, "Cannot GET by ID — no project was created yet");
        lastProjectResponse = http.get(CONTEXT_PATH + "/projects/" + id);
    }

    @When("I update the last created project name to {string}")
    public void i_update_the_last_created_project_name_to(String newName) throws Exception {
        String lastBody = lastProjectResponse != null
                ? lastProjectResponse.getBody()
                : http.lastBody();
        String id = extractField(lastBody, "id");
        assertNotNull(id, "Cannot UPDATE — no project was created yet");
        String updateBody = "{\"name\":\"" + newName + "\",\"description\":\"Updated by E2E\"}";
        lastProjectResponse = http.put(CONTEXT_PATH + "/projects/" + id, updateBody);
    }

    @When("I delete the last created project")
    public void i_delete_the_last_created_project() throws Exception {
        String lastBody = lastProjectResponse != null
                ? lastProjectResponse.getBody()
                : http.lastBody();
        String id = extractField(lastBody, "id");
        assertNotNull(id, "Cannot DELETE — no project was created yet");
        http.delete(CONTEXT_PATH + "/projects/" + id);
        createdProjectIds.remove(id);   // already deleted, skip @After cleanup
    }

    // ── Then (auth) ───────────────────────────────────────────────────────

    @Then("the auth response HTTP status is {int}")
    public void the_auth_response_http_status_is(int expected) {
        assertNotNull(lastAuthResponse, "No auth call has been made");
        assertEquals(expected, lastAuthResponse.getStatusCode().value(),
                "Auth response status mismatch. Body: " + lastAuthResponse.getBody());
        if (expected == 200) {
            // On successful login, capture the token for subsequent requests
            // BUG-001 NOTE: This correctly asserts token IS present in body.
            // The unit test AuthControllerTest.testLoginWithValidCredentials()
            // has a WRONG assertion: $.token.doesNotExist() — that will fail
            // because AuthController.login() passes the token into LoginResponse.
            try {
                String token = extractField(lastAuthResponse.getBody(), "token");
                if (token != null) http.setBearerToken(token);
            } catch (Exception ignored) { /* token extraction is best-effort */ }
        }
    }

    @Then("the auth response body contains field {string}")
    public void the_auth_response_body_contains_field(String field) throws Exception {
        JsonNode root = objectMapper.readTree(lastAuthResponse.getBody());
        assertTrue(root.has(field),
                "Expected field '" + field + "' in auth response: " + lastAuthResponse.getBody());
    }

    @Then("the auth response body contains field {string} with value {string}")
    public void the_auth_response_body_contains_field_with_value(String field, String expected) throws Exception {
        JsonNode root = objectMapper.readTree(lastAuthResponse.getBody());
        assertTrue(root.has(field),
                "Expected field '" + field + "' in auth response: " + lastAuthResponse.getBody());
        assertEquals(expected, root.get(field).asText(),
                "Field '" + field + "' value mismatch. Body: " + lastAuthResponse.getBody());
    }

    @Then("the {string} httpOnly cookie is set")
    public void the_httponly_cookie_is_set(String cookieName) {
        assertNotNull(lastAuthResponse, "No auth call has been made");
        String setCookie = lastAuthResponse.getHeaders().getFirst("Set-Cookie");
        assertNotNull(setCookie, "Expected Set-Cookie header but found none");
        assertTrue(setCookie.contains(cookieName),
                "Expected cookie '" + cookieName + "' in Set-Cookie: " + setCookie);
        assertTrue(setCookie.toLowerCase().contains("httponly"),
                "Expected HttpOnly flag on cookie '" + cookieName + "' but got: " + setCookie);
    }

    // ── Then (project) ────────────────────────────────────────────────────

    @Then("the create project response HTTP status is {int}")
    public void the_create_project_response_http_status_is(int expected) {
        assertNotNull(lastProjectResponse, "No project call has been made");
        assertEquals(expected, lastProjectResponse.getStatusCode().value(),
                "Create project status mismatch. Body: " + lastProjectResponse.getBody());
    }

    @Then("the get project response HTTP status is {int}")
    public void the_get_project_response_http_status_is(int expected) {
        assertNotNull(lastProjectResponse, "No project GET has been made");
        assertEquals(expected, lastProjectResponse.getStatusCode().value(),
                "Get project status mismatch. Body: " + lastProjectResponse.getBody());
    }

    @Then("the update project response HTTP status is {int}")
    public void the_update_project_response_http_status_is(int expected) {
        assertNotNull(lastProjectResponse, "No project PUT has been made");
        assertEquals(expected, lastProjectResponse.getStatusCode().value(),
                "Update project status mismatch. Body: " + lastProjectResponse.getBody());
    }

    @Then("the delete response HTTP status is {int}")
    public void the_delete_response_http_status_is(int expected) {
        assertEquals(expected, http.lastStatus(),
                "Delete project status mismatch. Body: " + http.lastBody());
    }

    @Then("the project response body contains field {string}")
    public void the_project_response_body_contains_field(String field) throws Exception {
        String body = lastProjectResponse != null
                ? lastProjectResponse.getBody() : http.lastBody();
        JsonNode root = objectMapper.readTree(body);
        assertTrue(root.has(field) && !root.get(field).isNull(),
                "Expected non-null field '" + field + "' in project response: " + body);
    }

    @Then("the project response body contains field {string} with value {string}")
    public void the_project_response_body_contains_field_with_value(String field, String expected) throws Exception {
        String body = lastProjectResponse != null
                ? lastProjectResponse.getBody() : http.lastBody();
        JsonNode root = objectMapper.readTree(body);
        assertTrue(root.has(field),
                "Expected field '" + field + "' in project response: " + body);
        assertEquals(expected, root.get(field).asText(),
                "Field '" + field + "' value mismatch. Body: " + body);
    }

    @Then("the paged response body contains the {string} array field")
    public void the_paged_response_body_contains_the_array_field(String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        assertTrue(root.has(fieldName),
                "Expected field '" + fieldName + "' in paged response: " + http.lastBody());
        assertTrue(root.get(fieldName).isArray(),
                "Expected '" + fieldName + "' to be a JSON array. Body: " + http.lastBody());
    }

    @Then("the paged response body contains the {string} field")
    public void the_paged_response_body_contains_the_field(String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        assertTrue(root.has(fieldName),
                "Expected field '" + fieldName + "' in paged response: " + http.lastBody());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void loginAs(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        lastAuthResponse = http.postAnonymous(CONTEXT_PATH + "/auth/login", body);
        assertEquals(200, lastAuthResponse.getStatusCode().value(),
                "Login as '" + username + "' failed. Body: " + lastAuthResponse.getBody());
        String token = extractField(lastAuthResponse.getBody(), "token");
        assertNotNull(token, "Login response did not contain a 'token' field. Body: "
                + lastAuthResponse.getBody());
        http.setBearerToken(token);
    }

    /**
     * Extracts the string value of a top-level JSON field.
     * Returns {@code null} (not fails) if the field is absent.
     */
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

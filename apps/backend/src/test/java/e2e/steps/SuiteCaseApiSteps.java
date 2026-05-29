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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SuiteCaseApiSteps {

    private static final Logger log = LoggerFactory.getLogger(SuiteCaseApiSteps.class);
    private static final String CTX = "/api";

    @Autowired private TmsHttpClient http;
    @Autowired private GherkinE2eTest runner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String projectId;
    private String lastSuiteId;
    private String lastCaseId;
    private final List<String> suiteIds = new ArrayList<>();

    @Before
    public void resetSuiteSteps() {
        http.setServerPort(runner.serverPort);
        http.reset();
        projectId = null;
        lastSuiteId = null;
        lastCaseId = null;
        suiteIds.clear();
    }

    @After
    public void cleanupSuites() {
        if (projectId == null) return;
        loginAs("test_admin", "admin123");
        for (String id : suiteIds) {
            try { http.delete(CTX + "/projects/" + projectId + "/suites/" + id); }
            catch (Exception e) { log.warn("[Cleanup] suite {}: {}", id, e.getMessage()); }
        }
        try { http.delete(CTX + "/projects/" + projectId); }
        catch (Exception e) { log.warn("[Cleanup] project {}: {}", projectId, e.getMessage()); }
    }

    @Given("I have a suite testing project")
    public void i_have_a_suite_testing_project() {
        String body = "{\"name\":\"Suite E2E Project\",\"description\":\"Suite E2E\"}";
        http.post(CTX + "/projects", body);
        assertEquals(201, http.lastStatus(), "Project creation failed: " + http.lastBody());
        projectId = extractField(http.lastBody(), "id");
        assertNotNull(projectId);
    }

    @When("I log in as tester and POST a suite to the project")
    public void tester_posts_a_suite() {
        loginAs("test_tester", "tester123");
        String body = "{\"name\":\"Tester Suite\",\"projectId\":\"" + projectId + "\"}";
        http.post(CTX + "/projects/" + projectId + "/suites", body);
    }

    @When("I create a suite named {string}")
    public void i_create_a_suite_named(String name) {
        String body = "{\"name\":\"" + name + "\",\"projectId\":\"" + projectId + "\"}";
        http.post(CTX + "/projects/" + projectId + "/suites", body);
        String id = extractField(http.lastBody(), "id");
        if (id != null) { lastSuiteId = id; suiteIds.add(id); }
    }

    @Given("I have created a suite named {string}")
    public void i_have_created_a_suite_named(String name) {
        loginAs("test_admin", "admin123");
        i_create_a_suite_named(name);
        assertEquals(201, http.lastStatus(), "Suite creation failed: " + http.lastBody());
    }

    @When("I GET all suites for the project")
    public void i_get_all_suites() {
        http.get(CTX + "/projects/" + projectId + "/suites");
    }

    @When("I create a test case titled {string} in that suite")
    public void i_create_a_test_case(String title) {
        String body = "{\"title\":\"" + title + "\",\"description\":\"\",\"priority\":\"MEDIUM\","
                + "\"suiteId\":\"" + lastSuiteId + "\",\"projectId\":\"" + projectId + "\"}";
        http.post(CTX + "/projects/" + projectId + "/suites/" + lastSuiteId + "/cases", body);
        String id = extractField(http.lastBody(), "id");
        if (id != null) lastCaseId = id;
    }

    @When("I log in as tester and DELETE that suite")
    public void tester_deletes_suite() {
        loginAs("test_tester", "tester123");
        http.delete(CTX + "/projects/" + projectId + "/suites/" + lastSuiteId);
    }

    @When("I delete that suite as admin")
    public void i_delete_that_suite_as_admin() {
        loginAs("test_admin", "admin123");
        http.delete(CTX + "/projects/" + projectId + "/suites/" + lastSuiteId);
        suiteIds.remove(lastSuiteId);
    }

    @Then("the suite response body contains {string}: {string}")
    public void suite_body_contains(String field, String value) {
        String actual = extractField(http.lastBody(), field);
        assertEquals(value, actual, "Field '" + field + "' mismatch. Body: " + http.lastBody());
    }

    @Then("the suite response body has a {string} array")
    public void suite_body_has_array(String field) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        assertTrue(root.has(field) && root.get(field).isArray(),
                "Expected '" + field + "' array in: " + http.lastBody());
    }

    private void loginAs(String username, String password) {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        http.postAnonymous(CTX + "/auth/login", body);
        String token = extractField(http.lastBody(), "token");
        assertNotNull(token, "Login failed for " + username + ": " + http.lastBody());
        http.setBearerToken(token);
    }

    private String extractField(String json, String field) {
        if (json == null) return null;
        try {
            JsonNode node = objectMapper.readTree(json).get(field);
            return (node != null && !node.isNull()) ? node.asText() : null;
        } catch (Exception e) { return null; }
    }
}

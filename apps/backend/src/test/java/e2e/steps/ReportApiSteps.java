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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportApiSteps {

    private static final Logger log = LoggerFactory.getLogger(ReportApiSteps.class);
    private static final String CTX = "/api";

    @Autowired private TmsHttpClient http;
    @Autowired private GherkinE2eTest runner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String projectId;
    private String lastReportId;
    private final List<String> reportIds = new ArrayList<>();

    @Before
    public void resetReportSteps() {
        http.setServerPort(runner.serverPort);
        http.reset();
        projectId = null;
        lastReportId = null;
        reportIds.clear();
    }

    @After
    public void cleanupReports() {
        if (projectId == null) return;
        loginAs("test_admin", "admin123");
        for (String id : reportIds) {
            try { http.delete(CTX + "/projects/" + projectId + "/reports/" + id); }
            catch (Exception e) { log.warn("[Cleanup] report {}: {}", id, e.getMessage()); }
        }
        try { http.delete(CTX + "/projects/" + projectId); }
        catch (Exception e) { log.warn("[Cleanup] project {}: {}", projectId, e.getMessage()); }
    }

    @Given("I have a report testing project")
    public void i_have_a_report_testing_project() {
        String body = "{\"name\":\"Report E2E Project\",\"description\":\"Report E2E\"}";
        http.post(CTX + "/projects", body);
        assertEquals(201, http.lastStatus(), "Project creation failed: " + http.lastBody());
        projectId = extractField(http.lastBody(), "id");
        assertNotNull(projectId);
    }

    @Given("I have created a report named {string}")
    public void i_have_created_a_report_named(String name) {
        i_create_a_report_named(name);
        assertEquals(201, http.lastStatus(), "Report creation failed: " + http.lastBody());
    }

    @When("I create a report named {string}")
    public void i_create_a_report_named(String name) {
        String body = "{\"name\":\"" + name + "\",\"type\":\"Summary\",\"projectId\":\"" + projectId + "\"}";
        http.post(CTX + "/projects/" + projectId + "/reports", body);
        String id = extractField(http.lastBody(), "id");
        if (id != null) { lastReportId = id; reportIds.add(id); }
    }

    @When("I GET that report by ID")
    public void i_get_that_report_by_id() {
        assertNotNull(lastReportId, "No report created yet");
        http.get(CTX + "/projects/" + projectId + "/reports/" + lastReportId);
    }

    @When("I GET all reports for the project")
    public void i_get_all_reports_for_the_project() {
        http.get(CTX + "/projects/" + projectId + "/reports");
    }

    @When("I delete that report")
    public void i_delete_that_report() {
        assertNotNull(lastReportId, "No report created yet");
        http.delete(CTX + "/projects/" + projectId + "/reports/" + lastReportId);
        reportIds.remove(lastReportId);
    }

    @Then("the report response body contains {string}: {string}")
    public void report_body_contains(String field, String value) {
        String actual = extractField(http.lastBody(), field);
        assertEquals(value, actual, "Field '" + field + "' mismatch. Body: " + http.lastBody());
    }

    @Then("the report response body has a {string} array")
    public void report_body_has_array(String field) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        assertTrue(root.has(field) && root.get(field).isArray(),
                "Expected '" + field + "' array in: " + http.lastBody());
    }

    private void loginAs(String username, String password) {
        http.postAnonymous(CTX + "/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        String token = extractField(http.lastBody(), "token");
        if (token != null) http.setBearerToken(token);
    }

    private String extractField(String json, String field) {
        if (json == null) return null;
        try {
            JsonNode node = objectMapper.readTree(json).get(field);
            return (node != null && !node.isNull()) ? node.asText() : null;
        } catch (Exception e) { return null; }
    }
}

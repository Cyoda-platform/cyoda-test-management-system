package e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.GherkinE2eTest;
import e2e.support.TmsHttpClient;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for {@code health-check.feature}.
 *
 * <p>All steps call the Spring Actuator health endpoint
 * ({@code GET /api/actuator/health}) without any authentication header.
 * The endpoint is explicitly whitelisted as public in
 * {@code AuthorizationFilter.isPublicEndpoint()}.
 *
 * <p>These steps do <em>not</em> perform any Cyoda entity operations.
 */
public class HealthCheckSteps {

    @Autowired
    private TmsHttpClient http;

    @Autowired
    private GherkinE2eTest runner;        // used only to read serverPort

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void resetHttpClient() {
        http.setServerPort(runner.serverPort);
        http.reset();
    }

    // ── When ──────────────────────────────────────────────────────────────

    @When("I call the health endpoint without authentication")
    public void i_call_the_health_endpoint_without_authentication() {
        http.getAnonymous("/api/actuator/health");
    }

    @When("I call a non-existent path {string} without authentication")
    public void i_call_a_non_existent_path_without_authentication(String path) {
        http.getAnonymous(path);
    }

    // ── Then ──────────────────────────────────────────────────────────────

    @Then("the health response HTTP status is {int}")
    public void the_health_response_http_status_is(int expected) {
        assertEquals(expected, http.lastStatus(),
                "Expected health endpoint to return HTTP " + expected
                        + " but got " + http.lastStatus()
                        + ". Body: " + http.lastBody());
    }

    @Then("the response HTTP status is {int}")
    public void the_response_http_status_is(int expected) {
        assertEquals(expected, http.lastStatus(),
                "Expected HTTP " + expected + " but got " + http.lastStatus()
                        + ". Body: " + http.lastBody());
    }

    @Then("the health response body contains a {string} field")
    public void the_health_response_body_contains_a_field(String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        assertTrue(root.has(fieldName),
                "Expected JSON field '" + fieldName + "' in body: " + http.lastBody());
    }

    @Then("the health status value is {string}")
    public void the_health_status_value_is(String expected) throws Exception {
        JsonNode root = objectMapper.readTree(http.lastBody());
        assertTrue(root.has("status"),
                "Expected 'status' field in health response body: " + http.lastBody());
        assertEquals(expected, root.get("status").asText(),
                "Health status mismatch. Body: " + http.lastBody());
    }
}

package e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.GherkinE2eTest;
import e2e.support.TmsHttpClient;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for {@code user-auth.feature}.
 *
 * Intentionally does NOT redefine steps already present in {@link ProjectApiSteps}:
 *   - "I log in as {string} with password {string}"
 *   - "I am logged in as admin"
 *   - "I GET {string}"
 * Those steps are shared and are provided by ProjectApiSteps.
 *
 * All @Then assertions read from TmsHttpClient.getLastResponse() so they work
 * regardless of which @When step triggered the HTTP call.
 */
public class UserAuthSteps {

    private static final String CTX = "/api";

    @Autowired
    private TmsHttpClient http;

    @Autowired
    private GherkinE2eTest runner;

    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        http.setServerPort(runner.serverPort);
        http.reset();
    }

    // ── When — auth-specific actions not covered by ProjectApiSteps ───────────

    @When("I call the login endpoint with empty credentials")
    public void i_call_the_login_endpoint_with_empty_credentials() {
        http.postAnonymous(CTX + "/auth/login", "{\"username\":\"\",\"password\":\"\"}");
    }

    @When("I call the logout endpoint")
    public void i_call_the_logout_endpoint() {
        http.postAnonymous(CTX + "/auth/logout", "");
    }

    @When("I GET {string} without a token")
    public void i_get_without_a_token(String path) {
        http.getAnonymous(path);
    }

    @When("I GET {string} with bearer token {string}")
    public void i_get_with_bearer_token(String path, String token) {
        http.setBearerToken(token);
        http.get(path);
        http.clearBearerToken();
    }

    @When("I GET {string} using the auth cookie")
    public void i_get_using_the_auth_cookie(String path) {
        // ApiSecurityConfig accepts both Bearer header and auth-token cookie.
        // TmsHttpClient sends Bearer header, which is accepted by TmsBearerTokenResolver.
        http.get(path);
    }

    // ── Given — pre-conditions not in ProjectApiSteps ────────────────────────

    @Given("I have logged in and stored the cookie for {string} with password {string}")
    public void i_have_logged_in_and_stored_the_cookie(String username, String password) throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
        http.postAnonymous(CTX + "/auth/login", body);
        assertEquals(200, http.lastStatus(),
                "Pre-condition login failed for '" + username + "'. Body: " + http.lastBody());
        String token = extractField(http.lastBody(), "token");
        assertNotNull(token, "Login did not return a token");
        http.setBearerToken(token);
    }

    // ── Then — login response assertions ─────────────────────────────────────

    @Then("the login response status is {int}")
    public void the_login_response_status_is(int expected) {
        assertEquals(expected, http.lastStatus(),
                "Login status mismatch. Body: " + http.lastBody());
    }

    @Then("the login response contains a {string} field")
    public void the_login_response_contains_a_field(String field) throws Exception {
        JsonNode root = mapper.readTree(http.lastBody());
        assertTrue(root.has(field) && !root.get(field).isNull(),
                "Expected non-null field '" + field + "' in login response: " + http.lastBody());
    }

    @Then("the login response contains field {string} with value {string}")
    public void the_login_response_contains_field_with_value(String field, String expected) throws Exception {
        JsonNode root = mapper.readTree(http.lastBody());
        assertTrue(root.has(field),
                "Expected field '" + field + "' in login response: " + http.lastBody());
        assertEquals(expected, root.get(field).asText(),
                "Field '" + field + "' value mismatch. Body: " + http.lastBody());
    }

    @Then("an httpOnly cookie {string} is set")
    public void an_httponly_cookie_is_set(String cookieName) {
        String setCookie = http.getLastResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie, "Expected Set-Cookie header but none was returned");
        assertTrue(setCookie.contains(cookieName),
                "Expected cookie '" + cookieName + "' in Set-Cookie: " + setCookie);
        assertTrue(setCookie.toLowerCase().contains("httponly"),
                "Expected HttpOnly flag in Set-Cookie: " + setCookie);
    }

    @Then("the token subject claim is a valid UUID")
    public void the_token_subject_claim_is_a_valid_uuid() throws Exception {
        JsonNode root = mapper.readTree(http.lastBody());
        assertTrue(root.has("token"), "No 'token' field in login response. Body: " + http.lastBody());
        String token = root.get("token").asText();

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Expected 3-part JWT but got: " + token);

        String padded = parts[1] + "=".repeat((4 - parts[1].length() % 4) % 4);
        String payloadJson = new String(Base64.getUrlDecoder().decode(padded));
        JsonNode payload = mapper.readTree(payloadJson);

        assertTrue(payload.has("sub"), "JWT payload missing 'sub' claim: " + payloadJson);
        String sub = payload.get("sub").asText();

        try {
            UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            fail("JWT 'sub' claim '" + sub + "' is not a valid UUID. "
                    + "After the OBO auth refactor, sub must be the user's Cyoda UUID.");
        }
    }

    // ── Then — logout assertions ──────────────────────────────────────────────

    @Then("the logout response status is {int}")
    public void the_logout_response_status_is(int expected) {
        assertEquals(expected, http.lastStatus(),
                "Logout status mismatch. Body: " + http.lastBody());
    }

    @Then("the {string} cookie is expired")
    public void the_cookie_is_expired(String cookieName) {
        String setCookie = http.getLastResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie, "Expected Set-Cookie header on logout response");
        assertTrue(setCookie.contains(cookieName),
                "Expected cookie '" + cookieName + "' to be cleared: " + setCookie);
        assertTrue(setCookie.contains("Max-Age=0") || setCookie.contains("max-age=0"),
                "Expected Max-Age=0 to expire cookie: " + setCookie);
    }

    // ── Then — generic response status ───────────────────────────────────────

    @Then("the response status is {int}")
    public void the_response_status_is(int expected) {
        assertEquals(expected, http.lastStatus(),
                "Response status mismatch. Body: " + http.lastBody());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractField(String json, String field) {
        try {
            JsonNode node = mapper.readTree(json).get(field);
            return (node != null && !node.isNull()) ? node.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

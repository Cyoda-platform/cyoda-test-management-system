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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

public class AttachmentApiSteps {

    private static final Logger log = LoggerFactory.getLogger(AttachmentApiSteps.class);
    private static final String CTX = "/api";

    @Autowired private TmsHttpClient http;
    @Autowired private GherkinE2eTest runner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String projectId;
    private String suiteId;
    private String caseId;
    private String lastAttachmentId;

    @Before
    public void resetAttachmentSteps() {
        http.setServerPort(runner.serverPort);
        http.reset();
        projectId = null;
        suiteId = null;
        caseId = null;
        lastAttachmentId = null;
    }

    @After
    public void cleanupAttachments() {
        if (projectId == null) return;
        loginAs("test_admin", "admin123");
        if (lastAttachmentId != null) {
            try { http.delete(CTX + "/projects/" + projectId + "/attachments/" + lastAttachmentId); }
            catch (Exception e) { log.warn("[Cleanup] attachment {}: {}", lastAttachmentId, e.getMessage()); }
        }
        if (caseId != null && suiteId != null) {
            try { http.delete(CTX + "/projects/" + projectId + "/suites/" + suiteId + "/cases/" + caseId); }
            catch (Exception e) { log.warn("[Cleanup] case {}: {}", caseId, e.getMessage()); }
        }
        if (suiteId != null) {
            try { http.delete(CTX + "/projects/" + projectId + "/suites/" + suiteId); }
            catch (Exception e) { log.warn("[Cleanup] suite {}: {}", suiteId, e.getMessage()); }
        }
        try { http.delete(CTX + "/projects/" + projectId); }
        catch (Exception e) { log.warn("[Cleanup] project {}: {}", projectId, e.getMessage()); }
    }

    @Given("I have an attachment testing project with a suite and a case")
    public void i_have_an_attachment_testing_project_with_a_suite_and_a_case() {
        // Create project
        http.post(CTX + "/projects", "{\"name\":\"Attachment E2E Project\",\"description\":\"Attachment E2E\"}");
        assertEquals(201, http.lastStatus(), "Project creation failed: " + http.lastBody());
        projectId = extractField(http.lastBody(), "id");
        assertNotNull(projectId);

        // Create suite
        http.post(CTX + "/projects/" + projectId + "/suites",
                "{\"name\":\"Attachment Suite\",\"projectId\":\"" + projectId + "\"}");
        assertEquals(201, http.lastStatus(), "Suite creation failed: " + http.lastBody());
        suiteId = extractField(http.lastBody(), "id");
        assertNotNull(suiteId);

        // Create case
        http.post(CTX + "/projects/" + projectId + "/suites/" + suiteId + "/cases",
                "{\"title\":\"Attachment Case\",\"description\":\"\",\"priority\":\"MEDIUM\","
                        + "\"suiteId\":\"" + suiteId + "\",\"projectId\":\"" + projectId + "\"}");
        assertEquals(201, http.lastStatus(), "Case creation failed: " + http.lastBody());
        caseId = extractField(http.lastBody(), "id");
        assertNotNull(caseId);
    }

    @Given("I have uploaded a text file attachment to that case")
    public void i_have_uploaded_a_text_file_attachment_to_that_case() {
        i_upload_a_text_file_attachment_to_that_case();
        assertEquals(201, http.lastStatus(), "Prerequisite attachment upload failed: " + http.lastBody());
    }

    @When("I upload a text file attachment to that case")
    public void i_upload_a_text_file_attachment_to_that_case() {
        assertNotNull(caseId, "No test case created yet");
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource("E2E test attachment content".getBytes()) {
            @Override public String getFilename() { return "test-attachment.txt"; }
        };
        parts.add("file", fileResource);
        parts.add("caseId", caseId);
        parts.add("attachmentType", "CASE");
        http.postMultipart(CTX + "/projects/" + projectId + "/attachments", parts);
        String id = extractField(http.lastBody(), "id");
        if (id != null) lastAttachmentId = id;
    }

    @When("I GET all attachments for the project")
    public void i_get_all_attachments_for_the_project() {
        http.get(CTX + "/projects/" + projectId + "/attachments");
    }

    @When("I delete that attachment")
    public void i_delete_that_attachment() {
        assertNotNull(lastAttachmentId, "No attachment uploaded yet");
        http.delete(CTX + "/projects/" + projectId + "/attachments/" + lastAttachmentId);
        lastAttachmentId = null;
    }

    @Then("the attachment response body contains {string}: {string}")
    public void attachment_body_contains(String field, String value) {
        String actual = extractField(http.lastBody(), field);
        assertEquals(value, actual, "Field '" + field + "' mismatch. Body: " + http.lastBody());
    }

    @Then("the attachment response body has a {string} array")
    public void attachment_body_has_array(String field) throws Exception {
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

package e2e.support;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP wrapper used by all TMS Cucumber step-definition classes.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Hold the last {@link ResponseEntity} produced by any call (used for assertions)</li>
 *   <li>Carry an optional Bearer token across steps in the same scenario</li>
 *   <li>Build the absolute URL from the injected random server port</li>
 * </ul>
 *
 * <p><b>Threading note</b>: Cucumber runs one scenario at a time on a single thread,
 * so mutable state here is safe.  The {@link #reset()} method must be called from a
 * {@code @Before} hook between scenarios.
 */
@Component
public class TmsHttpClient {

    private final RestTemplate restTemplate;

    private int serverPort;
    private String bearerToken;
    private ResponseEntity<String> lastResponse;

    public TmsHttpClient() {
        // Plain RestTemplate — does NOT follow redirects or inject cookies automatically.
        // We manage the Bearer token explicitly for full control over auth state.
        this.restTemplate = new RestTemplate();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void setServerPort(int port) {
        this.serverPort = port;
    }

    /** Clear auth state and last response between scenarios. */
    public void reset() {
        bearerToken = null;
        lastResponse = null;
    }

    public void setBearerToken(String token) {
        this.bearerToken = token;
    }

    public void clearBearerToken() {
        this.bearerToken = null;
    }

    // ── HTTP methods ──────────────────────────────────────────────────────

    public ResponseEntity<String> get(String path) {
        return exchange(path, HttpMethod.GET, null);
    }

    public ResponseEntity<String> getAnonymous(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return executeAndCapture(path, HttpMethod.GET, entity);
    }

    public ResponseEntity<String> post(String path, String jsonBody) {
        return exchange(path, HttpMethod.POST, jsonBody);
    }

    public ResponseEntity<String> postAnonymous(String path, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        return executeAndCapture(path, HttpMethod.POST, entity);
    }

    public ResponseEntity<String> put(String path, String jsonBody) {
        return exchange(path, HttpMethod.PUT, jsonBody);
    }

    public ResponseEntity<String> delete(String path) {
        return exchange(path, HttpMethod.DELETE, null);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public ResponseEntity<String> getLastResponse() {
        return lastResponse;
    }

    public int lastStatus() {
        if (lastResponse == null) throw new IllegalStateException("No HTTP call has been made yet");
        return lastResponse.getStatusCode().value();
    }

    public String lastBody() {
        if (lastResponse == null) throw new IllegalStateException("No HTTP call has been made yet");
        return lastResponse.getBody();
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private ResponseEntity<String> exchange(String path, HttpMethod method, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        return executeAndCapture(path, method, entity);
    }

    private ResponseEntity<String> executeAndCapture(String path, HttpMethod method, HttpEntity<String> entity) {
        String url = "http://localhost:" + serverPort + path;
        try {
            lastResponse = restTemplate.exchange(url, method, entity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // RestTemplate throws for 4xx/5xx by default.
            // Capture the response so step assertions can still inspect it.
            lastResponse = ResponseEntity
                    .status(ex.getStatusCode())
                    .headers(ex.getResponseHeaders())
                    .body(ex.getResponseBodyAsString());
        }
        return lastResponse;
    }

    public String buildUrl(String path) {
        return "http://localhost:" + serverPort + path;
    }
}

package com.java_template.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.java_template.common.workflow.CyodaEntity;
import com.java_template.common.workflow.OperationSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cyoda.cloud.api.event.common.ModelSpec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test Run DTO for TMS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRunDTO implements CyodaEntity {

    /**
     * Jackson view used by HTTP controllers.
     * When this view is active (via @JsonView on controller methods), CaseIdsSerializer
     * writes caseIds as a JSON array for the HTTP response.
     * When no view is active (i.e. objectMapper.valueToTree() inside EntityService),
     * it writes caseIds as a quoted JSON string — matching the Cyoda schema type.
     */
    public static class Views {
        public static class Http {}
    }

    public static final String ENTITY_NAME = "TestRun";
    public static final Integer ENTITY_VERSION = 1;
    private static final ModelSpec MODEL_SPEC = new ModelSpec().withName(ENTITY_NAME).withVersion(ENTITY_VERSION);

    private UUID id;

    // projectId is always set from the URL path parameter — never sent in the request body
    private UUID projectId;

    /** Human-readable, stable identifier (e.g. "TR-01"). Generated on creation and never recomputed from list position. */
    @Size(max = 50, message = "Display ID must not exceed 50 characters")
    private String displayId;

    @NotBlank(message = "Test run name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 100, message = "Environment must not exceed 100 characters")
    private String environment;

    @Size(max = 100, message = "Build version must not exceed 100 characters")
    private String buildVersion;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String status;

    /** Pass/fail/skip counters — computed from TestRunCase statuses */
    private int passed;
    private int failed;
    private int skipped;
    private int untested;

    /**
     * IDs of the test cases selected for this run.
     * Stored in Cyoda as a JSON string (1 schema field) to avoid the 150-field-per-model
     * subscription limit that array indexing would cause.
     * HTTP API sends/receives this as a JSON array — the custom ser/deser handles conversion.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonDeserialize(using = CaseIdsDeserializer.class)
    @JsonSerialize(using = CaseIdsSerializer.class)
    private String caseIds;

    /**
     * Flat step-level execution state, stored as JSON string on the run entity to avoid
     * creating per-run TestRunStep entities.
     * Stored as JSON string: "{\"key\": \"status\"}" where status is one of:
     * "UNTESTED" | "PASSED" | "FAILED" | "SKIPPED"
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String stepStatuses;

    private String startedAt;
    private String completedAt;
    private String createdAt;
    private String updatedAt;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }

    // ── Custom Jackson ser/deser ───────────────────────────────────────────────
    // HTTP sends/receives caseIds as a JSON array; Cyoda stores it as a JSON string.

    /** Accepts a JSON array from HTTP requests and converts to a JSON string for Cyoda storage. */
    public static class CaseIdsDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.START_ARRAY) {
                List<String> ids = new ArrayList<>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    ids.add(p.getText());
                }
                if (ids.isEmpty()) return null;
                return "[" + ids.stream()
                        .map(id -> "\"" + id.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                        .collect(Collectors.joining(",")) + "]";
            }
            // Already a string (from Cyoda storage)
            String text = p.getText();
            return (text == null || text.isBlank()) ? null : text;
        }
    }

    /**
     * Outputs the stored JSON string either as a JSON array (HTTP responses) or as a
     * quoted string (Cyoda entity writes).
     *
     * Detection: when Spring MVC serializes a @JsonView(Views.Http.class) response,
     * provider.getActiveView() == Views.Http.class → write raw array.
     * When EntityService calls objectMapper.valueToTree(entity), no view is active →
     * write as quoted string to match the Cyoda schema type.
     *
     * CONTRACT: Cyoda writes rely on EntityService never activating a Jackson view when
     * calling objectMapper.valueToTree(). If common/EntityServiceImpl is ever changed to
     * use a view-aware serialization call, caseIds will be stored as a raw array in Cyoda
     * instead of a quoted string, silently violating the schema.
     * Related: application.yml spring.jackson.mapper.default-view-inclusion=true is
     * required so non-@JsonView fields (id, name, status, …) are included in @JsonView
     * HTTP responses. Removing it breaks ALL TestRun controller endpoints.
     */
    public static class CaseIdsSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null || value.isBlank()) return;
            Class<?> activeView = provider.getActiveView();
            if (activeView != null && Views.Http.class.isAssignableFrom(activeView)) {
                // HTTP response: write as a JSON array so the frontend receives string[]
                gen.writeRawValue(value);
            } else {
                // Cyoda entity write: store as a quoted JSON string (matches schema type)
                gen.writeString(value);
            }
        }
    }
}

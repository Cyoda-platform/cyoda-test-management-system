package com.java_template.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.java_template.common.workflow.CyodaEntity;
import com.java_template.common.workflow.OperationSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cyoda.cloud.api.event.common.ModelSpec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Run DTO for TMS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRunDTO implements CyodaEntity {

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
     * IDs of the test cases selected for this run, captured at creation time.
     * Stored directly on the run to avoid creating separate TestRunCase entities
     * (whose Cyoda entity model may not be registered).
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> caseIds;

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

    private static final Logger log = LoggerFactory.getLogger(TestRunDTO.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert stepStatuses from JSON string to Map.
     * Used when converting from Cyoda entity (stored as JSON string) to application code (uses Map).
     */
    @JsonIgnore
    public Map<String, String> getStepStatusesAsMap() {
        if (stepStatuses == null || stepStatuses.isEmpty() || stepStatuses.equals("{}")) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(stepStatuses, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>(){});
        } catch (Exception e) {
            log.warn("Failed to parse stepStatuses JSON: {}", stepStatuses, e);
            return new HashMap<>();
        }
    }

    /**
     * Convert stepStatuses from Map to JSON string.
     * Used when converting from application code (Map) to Cyoda entity (stored as JSON string).
     */
    @JsonIgnore
    public void setStepStatusesFromMap(Map<String, String> map) {
        try {
            this.stepStatuses = objectMapper.writeValueAsString(map != null ? map : new HashMap<>());
        } catch (Exception e) {
            log.warn("Failed to serialize stepStatuses map", e);
            this.stepStatuses = "{}";
        }
    }

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}


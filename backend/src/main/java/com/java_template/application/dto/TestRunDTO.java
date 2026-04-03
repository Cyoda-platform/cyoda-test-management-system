package com.java_template.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java_template.common.workflow.CyodaEntity;
import com.java_template.common.workflow.OperationSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cyoda.cloud.api.event.common.ModelSpec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test Run DTO for TMS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}


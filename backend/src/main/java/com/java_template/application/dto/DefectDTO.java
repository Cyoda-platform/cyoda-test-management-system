package com.java_template.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java_template.common.workflow.CyodaEntity;
import com.java_template.common.workflow.OperationSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cyoda.cloud.api.event.common.ModelSpec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Defect DTO for TMS
 * Represents a project-level defect / bug record linked to a test run or test case.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefectDTO implements CyodaEntity {

    public static final String ENTITY_NAME = "Defect";
    public static final Integer ENTITY_VERSION = 1;
    private static final ModelSpec MODEL_SPEC = new ModelSpec().withName(ENTITY_NAME).withVersion(ENTITY_VERSION);

    private UUID id;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    /** Severity level: Critical, Major, Minor */
    @NotBlank(message = "Severity is required")
    private String severity;

    /** URL link to external bug tracker (e.g. Jira) */
    private String link;

    /** Status: Open, In Progress, Fixed, Closed */
    private String status;

    /** Reference to the source entity ID (test run, test case, test step) */
    private String source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}

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
import java.util.List;
import java.util.UUID;

/**
 * Report DTO for TMS
 * Represents a QA report generated from one or more test runs for a project.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO implements CyodaEntity {

    public static final String ENTITY_NAME = "Report";
    public static final Integer ENTITY_VERSION = 1;
    private static final ModelSpec MODEL_SPEC = new ModelSpec().withName(ENTITY_NAME).withVersion(ENTITY_VERSION);

    private UUID id;

    // projectId is always set from the URL path parameter — never sent in the request body
    private UUID projectId;

    /** Human-readable, stable identifier (e.g. "REP-01"). Generated on creation and never recomputed from list position. */
    @Size(max = 50, message = "Display ID must not exceed 50 characters")
    private String displayId;

    @NotBlank(message = "Report name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    /** Report type: Summary, Regression, Sprint, Custom */
    @NotBlank(message = "Report type is required")
    private String type;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String createdBy;

    /** Date range filter (optional, informational) */
    private String dateFrom;
    private String dateTo;

    /** IDs of the test runs included in this report */
    private List<String> selectedRuns;

    // Report sections (which sections to include)
    private boolean sectionExecutiveSummary = true;
    private boolean sectionSuiteAnalytics   = true;
    private boolean sectionDefectTable      = true;
    private boolean sectionEnvironmentInfo  = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}

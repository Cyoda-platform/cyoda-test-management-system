package com.java_template.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java_template.common.workflow.CyodaEntity;
import com.java_template.common.workflow.OperationSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cyoda.cloud.api.event.common.ModelSpec;

import java.util.UUID;

/**
 * Test Run Case DTO for TMS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunCaseDTO implements CyodaEntity {

    public static final String ENTITY_NAME = "TestRunCase";
    public static final Integer ENTITY_VERSION = 1;
    private static final ModelSpec MODEL_SPEC = new ModelSpec().withName(ENTITY_NAME).withVersion(ENTITY_VERSION);

    private UUID id;

    // testRunId, testCaseId and projectId are always set from path parameters / service logic
    // — never sent in the request body
    private UUID testRunId;
    private UUID testCaseId;
    private UUID projectId;

    private String status;
    private String bugUrl;

    // Snapshot fields — copied from TestCase at initialize_run time.
    // These are immutable once set and allow historical reporting even after
    // the original TestCase or TestStep is soft-deleted.
    private String title;
    private String description;
    private String preconditions;
    private Priority priority;
    private String displayId;
    private UUID suiteId;

    private String startedAt;
    private String completedAt;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}


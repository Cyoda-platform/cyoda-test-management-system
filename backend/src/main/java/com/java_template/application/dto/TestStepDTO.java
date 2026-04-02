package com.java_template.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.java_template.common.workflow.CyodaEntity;
import com.java_template.common.workflow.OperationSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cyoda.cloud.api.event.common.ModelSpec;

import java.util.UUID;

/**
 * Test Step DTO for TMS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestStepDTO implements CyodaEntity {
    public static final String ENTITY_NAME = "TestStep";
    public static final Integer ENTITY_VERSION = 1;
    private static final ModelSpec MODEL_SPEC = new ModelSpec().withName(ENTITY_NAME).withVersion(ENTITY_VERSION);

    private UUID id;
    private UUID testCaseId;
    private Integer stepNumber;
    @JsonProperty("action")
    private String action;
    private String expectedResult;
    private String status;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}


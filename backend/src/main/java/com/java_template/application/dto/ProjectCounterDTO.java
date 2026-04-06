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
 * Stores a monotonically-increasing counter per project used to assign unique,
 * non-reusable display IDs (TC-1, TC-2, …) to test cases.
 *
 * One record exists per project. The counter is read-then-incremented inside a
 * per-project synchronized block in ProjectCounterService to prevent reuse even
 * under concurrent case creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCounterDTO implements CyodaEntity {

    public static final String ENTITY_NAME = "ProjectCounter";
    public static final Integer ENTITY_VERSION = 1;
    private static final ModelSpec MODEL_SPEC = new ModelSpec().withName(ENTITY_NAME).withVersion(ENTITY_VERSION);

    private UUID id;

    /** The project this counter belongs to. */
    private UUID projectId;

    /** The next ID value to assign. Starts at 1 and only ever increases. */
    private long nextId;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}

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
 * Stores monotonically-increasing counters per project for assigning unique,
 * non-reusable display IDs to test cases (TC-N), test runs (TR-N), and defects (DEF-N).
 *
 * One record exists per project. Each counter is read-then-incremented inside a
 * per-project synchronized block in ProjectCounterService to prevent reuse even
 * under concurrent creation.
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

    /** Next TC-N value to assign (test cases). Starts at 1, only ever increases. */
    private long nextId;

    /** Next TR-N value to assign (test runs). Starts at 1, only ever increases. */
    private long nextRunId;

    /** Next DEF-N value to assign (defects). Starts at 1, only ever increases. */
    private long nextDefectId;

    /** Next REP-N value to assign (reports). Starts at 1, only ever increases. */
    private long nextReportId;

    @Override
    @JsonIgnore
    public OperationSpecification getModelKey() {
        return new OperationSpecification.Entity(MODEL_SPEC, ENTITY_NAME);
    }
}

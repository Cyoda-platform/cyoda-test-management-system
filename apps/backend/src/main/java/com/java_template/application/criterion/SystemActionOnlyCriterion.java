package com.java_template.application.criterion;

import com.java_template.common.serializer.CriterionSerializer;
import com.java_template.common.serializer.EvaluationOutcome;
import com.java_template.common.serializer.SerializerFactory;
import com.java_template.common.workflow.CyodaCriterion;
import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import org.cyoda.cloud.api.event.processing.EntityCriteriaCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityCriteriaCalculationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Criterion that permits transitions triggered by the system (non-manual).
 *
 * <p>This criterion is attached to {@code manual: false} transitions in the
 * TestRunCase workflow (e.g. {@code update_case_status_to_passed/failed/skipped}).
 * Since those transitions are system-initiated — they can only be driven by
 * internal processors or services, never by a direct user API call — the
 * criterion always returns {@code true}.</p>
 *
 * <p>The {@code manual: false} flag in the workflow JSON is the real guard
 * against user-initiated invocations. This criterion exists as an explicit
 * declaration of that intent and to satisfy the Cyoda evaluation framework.</p>
 */
@Component("SystemActionOnly")
public class SystemActionOnlyCriterion implements CyodaCriterion {

    private static final Logger log = LoggerFactory.getLogger(SystemActionOnlyCriterion.class);
    private static final String CRITERION_NAME = SystemActionOnlyCriterion.class.getSimpleName()
            .replace("Criterion", "");

    private final CriterionSerializer serializer;

    public SystemActionOnlyCriterion(SerializerFactory serializerFactory) {
        this.serializer = serializerFactory.getDefaultCriteriaSerializer();
    }

    @Override
    public EntityCriteriaCalculationResponse check(
            CyodaEventContext<EntityCriteriaCalculationRequest> context) {
        EntityCriteriaCalculationRequest request = context.getEvent();
        log.debug("SystemActionOnly: allowing system transition for entity={}", request.getEntityId());
        return serializer.withRequest(request)
                .evaluate(jsonNode -> EvaluationOutcome.success())
                .complete();
    }

    @Override
    public boolean supports(OperationSpecification opsSpec) {
        return "SystemActionOnly".equals(opsSpec.operationName());
    }
}

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
 * Permits transitions that require Admin role.
 * Role enforcement is handled at the HTTP layer (Spring Security);
 * this criterion always returns true to allow the FSM transition to proceed.
 */
@Component
public class RequireAdminRoleCriterion implements CyodaCriterion {

    private static final Logger log = LoggerFactory.getLogger(RequireAdminRoleCriterion.class);
    private static final String CRITERION_NAME = "RequireAdminRole";

    private final CriterionSerializer serializer;

    public RequireAdminRoleCriterion(SerializerFactory serializerFactory) {
        this.serializer = serializerFactory.getDefaultCriteriaSerializer();
    }

    @Override
    public EntityCriteriaCalculationResponse check(CyodaEventContext<EntityCriteriaCalculationRequest> context) {
        EntityCriteriaCalculationRequest request = context.getEvent();
        log.debug("{}: allowing transition for entity={}", CRITERION_NAME, request.getEntityId());
        return serializer.withRequest(request)
                .evaluate(jsonNode -> EvaluationOutcome.success())
                .complete();
    }

    @Override
    public boolean supports(OperationSpecification opsSpec) {
        return CRITERION_NAME.equals(opsSpec.operationName());
    }
}

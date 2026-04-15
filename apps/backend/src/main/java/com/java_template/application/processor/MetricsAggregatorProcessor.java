package com.java_template.application.processor;

import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.CyodaProcessor;
import com.java_template.common.workflow.OperationSpecification;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for aggregating test run metrics.
 *
 * This processor is responsible for calculating and aggregating metrics from a
 * completed test run, such as pass/fail ratios, execution duration, and other
 * analytics. It captures summary statistics for reporting and dashboards.
 *
 * The processor returns the entity unchanged, allowing the workflow to continue
 * with the original entity while metrics are aggregated as a side effect.
 */
@Component("MetricsAggregatorProcessor")
public class MetricsAggregatorProcessor implements CyodaProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MetricsAggregatorProcessor.class);
    private final String processorName = "MetricsAggregatorProcessor";

    @Override
    public EntityProcessorCalculationResponse process(CyodaEventContext<EntityProcessorCalculationRequest> context) {
        EntityProcessorCalculationRequest request = context.getEvent();
        logger.debug("{}: Processing metrics aggregation for request: {}", processorName, request.getId());

        // Stub implementation - returns entity unchanged
        // In future, this would calculate: pass/fail ratios, duration, execution time, etc.
        EntityProcessorCalculationResponse response = new EntityProcessorCalculationResponse();
        response.setId(request.getId());
        response.setSuccess(true);

        logger.debug("{}: Metrics aggregation completed", processorName);
        return response;
    }

    @Override
    public boolean supports(OperationSpecification opSpec) {
        return processorName.equals(opSpec.operationName());
    }

    /**
     * Gets the name of this processor.
     * @return the processor name
     */
    public String getName() {
        return processorName;
    }
}


package org.cyoda.cloud.api.event.processing;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.DataPayload;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.statemachine.TransitionInfo;
import org.cyoda.cloud.api.event.common.statemachine.WorkflowInfo;


/**
 * EntityProcessorCalculationRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "entityId",
    "processorId",
    "processorName",
    "workflow",
    "transition",
    "transactionId",
    "parameters",
    "payload"
})
@Generated("jsonschema2pojo")
public class EntityProcessorCalculationRequest
    extends BaseEvent
{

    /**
     * Request ID.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    @JsonPropertyDescription("Request ID.")
    private String requestId;
    /**
     * Entity ID.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    @JsonPropertyDescription("Entity ID.")
    private String entityId;
    /**
     * Processor ID.
     * (Required)
     * 
     */
    @JsonProperty("processorId")
    @JsonPropertyDescription("Processor ID.")
    private String processorId;
    /**
     * Processor name.
     * (Required)
     * 
     */
    @JsonProperty("processorName")
    @JsonPropertyDescription("Processor name.")
    private String processorName;
    /**
     * WorkflowInfo
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("workflow")
    private WorkflowInfo workflow;
    /**
     * TransitionInfo
     * <p>
     * 
     * 
     */
    @JsonProperty("transition")
    private TransitionInfo transition;
    /**
     * Transaction ID.
     * 
     */
    @JsonProperty("transactionId")
    @JsonPropertyDescription("Transaction ID.")
    private String transactionId;
    /**
     * Configured parameters, if any.
     * 
     */
    @JsonProperty("parameters")
    @JsonPropertyDescription("Configured parameters, if any.")
    private JsonNode parameters;
    /**
     * DataPayload
     * <p>
     * 
     * 
     */
    @JsonProperty("payload")
    private DataPayload payload;

    /**
     * Request ID.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public String getRequestId() {
        return requestId;
    }

    /**
     * Request ID.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public EntityProcessorCalculationRequest withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Entity ID.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    public String getEntityId() {
        return entityId;
    }

    /**
     * Entity ID.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public EntityProcessorCalculationRequest withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    /**
     * Processor ID.
     * (Required)
     * 
     */
    @JsonProperty("processorId")
    public String getProcessorId() {
        return processorId;
    }

    /**
     * Processor ID.
     * (Required)
     * 
     */
    @JsonProperty("processorId")
    public void setProcessorId(String processorId) {
        this.processorId = processorId;
    }

    public EntityProcessorCalculationRequest withProcessorId(String processorId) {
        this.processorId = processorId;
        return this;
    }

    /**
     * Processor name.
     * (Required)
     * 
     */
    @JsonProperty("processorName")
    public String getProcessorName() {
        return processorName;
    }

    /**
     * Processor name.
     * (Required)
     * 
     */
    @JsonProperty("processorName")
    public void setProcessorName(String processorName) {
        this.processorName = processorName;
    }

    public EntityProcessorCalculationRequest withProcessorName(String processorName) {
        this.processorName = processorName;
        return this;
    }

    /**
     * WorkflowInfo
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("workflow")
    public WorkflowInfo getWorkflow() {
        return workflow;
    }

    /**
     * WorkflowInfo
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("workflow")
    public void setWorkflow(WorkflowInfo workflow) {
        this.workflow = workflow;
    }

    public EntityProcessorCalculationRequest withWorkflow(WorkflowInfo workflow) {
        this.workflow = workflow;
        return this;
    }

    /**
     * TransitionInfo
     * <p>
     * 
     * 
     */
    @JsonProperty("transition")
    public TransitionInfo getTransition() {
        return transition;
    }

    /**
     * TransitionInfo
     * <p>
     * 
     * 
     */
    @JsonProperty("transition")
    public void setTransition(TransitionInfo transition) {
        this.transition = transition;
    }

    public EntityProcessorCalculationRequest withTransition(TransitionInfo transition) {
        this.transition = transition;
        return this;
    }

    /**
     * Transaction ID.
     * 
     */
    @JsonProperty("transactionId")
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Transaction ID.
     * 
     */
    @JsonProperty("transactionId")
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public EntityProcessorCalculationRequest withTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * Configured parameters, if any.
     * 
     */
    @JsonProperty("parameters")
    public JsonNode getParameters() {
        return parameters;
    }

    /**
     * Configured parameters, if any.
     * 
     */
    @JsonProperty("parameters")
    public void setParameters(JsonNode parameters) {
        this.parameters = parameters;
    }

    public EntityProcessorCalculationRequest withParameters(JsonNode parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * DataPayload
     * <p>
     * 
     * 
     */
    @JsonProperty("payload")
    public DataPayload getPayload() {
        return payload;
    }

    /**
     * DataPayload
     * <p>
     * 
     * 
     */
    @JsonProperty("payload")
    public void setPayload(DataPayload payload) {
        this.payload = payload;
    }

    public EntityProcessorCalculationRequest withPayload(DataPayload payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public EntityProcessorCalculationRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityProcessorCalculationRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityProcessorCalculationRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityProcessorCalculationRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityProcessorCalculationRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        int baseLength = sb.length();
        String superString = super.toString();
        if (superString!= null) {
            int contentStart = superString.indexOf('[');
            int contentEnd = superString.lastIndexOf(']');
            if ((contentStart >= 0)&&(contentEnd >contentStart)) {
                sb.append(superString, (contentStart + 1), contentEnd);
            } else {
                sb.append(superString);
            }
        }
        if (sb.length()>baseLength) {
            sb.append(',');
        }
        sb.append("requestId");
        sb.append('=');
        sb.append(((this.requestId == null)?"<null>":this.requestId));
        sb.append(',');
        sb.append("entityId");
        sb.append('=');
        sb.append(((this.entityId == null)?"<null>":this.entityId));
        sb.append(',');
        sb.append("processorId");
        sb.append('=');
        sb.append(((this.processorId == null)?"<null>":this.processorId));
        sb.append(',');
        sb.append("processorName");
        sb.append('=');
        sb.append(((this.processorName == null)?"<null>":this.processorName));
        sb.append(',');
        sb.append("workflow");
        sb.append('=');
        sb.append(((this.workflow == null)?"<null>":this.workflow));
        sb.append(',');
        sb.append("transition");
        sb.append('=');
        sb.append(((this.transition == null)?"<null>":this.transition));
        sb.append(',');
        sb.append("transactionId");
        sb.append('=');
        sb.append(((this.transactionId == null)?"<null>":this.transactionId));
        sb.append(',');
        sb.append("parameters");
        sb.append('=');
        sb.append(((this.parameters == null)?"<null>":this.parameters));
        sb.append(',');
        sb.append("payload");
        sb.append('=');
        sb.append(((this.payload == null)?"<null>":this.payload));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.processorId == null)? 0 :this.processorId.hashCode()));
        result = ((result* 31)+((this.workflow == null)? 0 :this.workflow.hashCode()));
        result = ((result* 31)+((this.payload == null)? 0 :this.payload.hashCode()));
        result = ((result* 31)+((this.requestId == null)? 0 :this.requestId.hashCode()));
        result = ((result* 31)+((this.entityId == null)? 0 :this.entityId.hashCode()));
        result = ((result* 31)+((this.processorName == null)? 0 :this.processorName.hashCode()));
        result = ((result* 31)+((this.parameters == null)? 0 :this.parameters.hashCode()));
        result = ((result* 31)+((this.transition == null)? 0 :this.transition.hashCode()));
        result = ((result* 31)+((this.transactionId == null)? 0 :this.transactionId.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityProcessorCalculationRequest) == false) {
            return false;
        }
        EntityProcessorCalculationRequest rhs = ((EntityProcessorCalculationRequest) other);
        return (((((((((super.equals(rhs)&&((this.processorId == rhs.processorId)||((this.processorId!= null)&&this.processorId.equals(rhs.processorId))))&&((this.workflow == rhs.workflow)||((this.workflow!= null)&&this.workflow.equals(rhs.workflow))))&&((this.payload == rhs.payload)||((this.payload!= null)&&this.payload.equals(rhs.payload))))&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.entityId == rhs.entityId)||((this.entityId!= null)&&this.entityId.equals(rhs.entityId))))&&((this.processorName == rhs.processorName)||((this.processorName!= null)&&this.processorName.equals(rhs.processorName))))&&((this.parameters == rhs.parameters)||((this.parameters!= null)&&this.parameters.equals(rhs.parameters))))&&((this.transition == rhs.transition)||((this.transition!= null)&&this.transition.equals(rhs.transition))))&&((this.transactionId == rhs.transactionId)||((this.transactionId!= null)&&this.transactionId.equals(rhs.transactionId))));
    }

}

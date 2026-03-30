
package org.cyoda.cloud.api.event.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.DataPayload;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.statemachine.ProcessorInfo;
import org.cyoda.cloud.api.event.common.statemachine.TransitionInfo;
import org.cyoda.cloud.api.event.common.statemachine.WorkflowInfo;


/**
 * EntityCriteriaCalculationRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "entityId",
    "criteriaId",
    "criteriaName",
    "target",
    "workflow",
    "processor",
    "transition",
    "transactionId",
    "parameters",
    "payload"
})
@Generated("jsonschema2pojo")
public class EntityCriteriaCalculationRequest
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
     * Criteria ID.
     * (Required)
     * 
     */
    @JsonProperty("criteriaId")
    @JsonPropertyDescription("Criteria ID.")
    private String criteriaId;
    /**
     * Criteria name.
     * (Required)
     * 
     */
    @JsonProperty("criteriaName")
    @JsonPropertyDescription("Criteria name.")
    private String criteriaName;
    /**
     * Target to which this condition is attached. NA is reserved for future cases.
     * (Required)
     * 
     */
    @JsonProperty("target")
    @JsonPropertyDescription("Target to which this condition is attached. NA is reserved for future cases.")
    private EntityCriteriaCalculationRequest.Target target;
    /**
     * WorkflowInfo
     * <p>
     * 
     * 
     */
    @JsonProperty("workflow")
    private WorkflowInfo workflow;
    /**
     * ProcessorInfo
     * <p>
     * 
     * 
     */
    @JsonProperty("processor")
    private ProcessorInfo processor;
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

    public EntityCriteriaCalculationRequest withRequestId(String requestId) {
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

    public EntityCriteriaCalculationRequest withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    /**
     * Criteria ID.
     * (Required)
     * 
     */
    @JsonProperty("criteriaId")
    public String getCriteriaId() {
        return criteriaId;
    }

    /**
     * Criteria ID.
     * (Required)
     * 
     */
    @JsonProperty("criteriaId")
    public void setCriteriaId(String criteriaId) {
        this.criteriaId = criteriaId;
    }

    public EntityCriteriaCalculationRequest withCriteriaId(String criteriaId) {
        this.criteriaId = criteriaId;
        return this;
    }

    /**
     * Criteria name.
     * (Required)
     * 
     */
    @JsonProperty("criteriaName")
    public String getCriteriaName() {
        return criteriaName;
    }

    /**
     * Criteria name.
     * (Required)
     * 
     */
    @JsonProperty("criteriaName")
    public void setCriteriaName(String criteriaName) {
        this.criteriaName = criteriaName;
    }

    public EntityCriteriaCalculationRequest withCriteriaName(String criteriaName) {
        this.criteriaName = criteriaName;
        return this;
    }

    /**
     * Target to which this condition is attached. NA is reserved for future cases.
     * (Required)
     * 
     */
    @JsonProperty("target")
    public EntityCriteriaCalculationRequest.Target getTarget() {
        return target;
    }

    /**
     * Target to which this condition is attached. NA is reserved for future cases.
     * (Required)
     * 
     */
    @JsonProperty("target")
    public void setTarget(EntityCriteriaCalculationRequest.Target target) {
        this.target = target;
    }

    public EntityCriteriaCalculationRequest withTarget(EntityCriteriaCalculationRequest.Target target) {
        this.target = target;
        return this;
    }

    /**
     * WorkflowInfo
     * <p>
     * 
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
     * 
     */
    @JsonProperty("workflow")
    public void setWorkflow(WorkflowInfo workflow) {
        this.workflow = workflow;
    }

    public EntityCriteriaCalculationRequest withWorkflow(WorkflowInfo workflow) {
        this.workflow = workflow;
        return this;
    }

    /**
     * ProcessorInfo
     * <p>
     * 
     * 
     */
    @JsonProperty("processor")
    public ProcessorInfo getProcessor() {
        return processor;
    }

    /**
     * ProcessorInfo
     * <p>
     * 
     * 
     */
    @JsonProperty("processor")
    public void setProcessor(ProcessorInfo processor) {
        this.processor = processor;
    }

    public EntityCriteriaCalculationRequest withProcessor(ProcessorInfo processor) {
        this.processor = processor;
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

    public EntityCriteriaCalculationRequest withTransition(TransitionInfo transition) {
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

    public EntityCriteriaCalculationRequest withTransactionId(String transactionId) {
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

    public EntityCriteriaCalculationRequest withParameters(JsonNode parameters) {
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

    public EntityCriteriaCalculationRequest withPayload(DataPayload payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public EntityCriteriaCalculationRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityCriteriaCalculationRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityCriteriaCalculationRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityCriteriaCalculationRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityCriteriaCalculationRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("criteriaId");
        sb.append('=');
        sb.append(((this.criteriaId == null)?"<null>":this.criteriaId));
        sb.append(',');
        sb.append("criteriaName");
        sb.append('=');
        sb.append(((this.criteriaName == null)?"<null>":this.criteriaName));
        sb.append(',');
        sb.append("target");
        sb.append('=');
        sb.append(((this.target == null)?"<null>":this.target));
        sb.append(',');
        sb.append("workflow");
        sb.append('=');
        sb.append(((this.workflow == null)?"<null>":this.workflow));
        sb.append(',');
        sb.append("processor");
        sb.append('=');
        sb.append(((this.processor == null)?"<null>":this.processor));
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
        result = ((result* 31)+((this.workflow == null)? 0 :this.workflow.hashCode()));
        result = ((result* 31)+((this.criteriaId == null)? 0 :this.criteriaId.hashCode()));
        result = ((result* 31)+((this.payload == null)? 0 :this.payload.hashCode()));
        result = ((result* 31)+((this.requestId == null)? 0 :this.requestId.hashCode()));
        result = ((result* 31)+((this.criteriaName == null)? 0 :this.criteriaName.hashCode()));
        result = ((result* 31)+((this.entityId == null)? 0 :this.entityId.hashCode()));
        result = ((result* 31)+((this.processor == null)? 0 :this.processor.hashCode()));
        result = ((result* 31)+((this.parameters == null)? 0 :this.parameters.hashCode()));
        result = ((result* 31)+((this.transition == null)? 0 :this.transition.hashCode()));
        result = ((result* 31)+((this.transactionId == null)? 0 :this.transactionId.hashCode()));
        result = ((result* 31)+((this.target == null)? 0 :this.target.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityCriteriaCalculationRequest) == false) {
            return false;
        }
        EntityCriteriaCalculationRequest rhs = ((EntityCriteriaCalculationRequest) other);
        return (((((((((((super.equals(rhs)&&((this.workflow == rhs.workflow)||((this.workflow!= null)&&this.workflow.equals(rhs.workflow))))&&((this.criteriaId == rhs.criteriaId)||((this.criteriaId!= null)&&this.criteriaId.equals(rhs.criteriaId))))&&((this.payload == rhs.payload)||((this.payload!= null)&&this.payload.equals(rhs.payload))))&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.criteriaName == rhs.criteriaName)||((this.criteriaName!= null)&&this.criteriaName.equals(rhs.criteriaName))))&&((this.entityId == rhs.entityId)||((this.entityId!= null)&&this.entityId.equals(rhs.entityId))))&&((this.processor == rhs.processor)||((this.processor!= null)&&this.processor.equals(rhs.processor))))&&((this.parameters == rhs.parameters)||((this.parameters!= null)&&this.parameters.equals(rhs.parameters))))&&((this.transition == rhs.transition)||((this.transition!= null)&&this.transition.equals(rhs.transition))))&&((this.transactionId == rhs.transactionId)||((this.transactionId!= null)&&this.transactionId.equals(rhs.transactionId))))&&((this.target == rhs.target)||((this.target!= null)&&this.target.equals(rhs.target))));
    }


    /**
     * Target to which this condition is attached. NA is reserved for future cases.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Target {

        NA("NA"),
        WORKFLOW("WORKFLOW"),
        PROCESSOR("PROCESSOR"),
        TRANSITION("TRANSITION");
        private final String value;
        private final static Map<String, EntityCriteriaCalculationRequest.Target> CONSTANTS = new HashMap<String, EntityCriteriaCalculationRequest.Target>();

        static {
            for (EntityCriteriaCalculationRequest.Target c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Target(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static EntityCriteriaCalculationRequest.Target fromValue(String value) {
            EntityCriteriaCalculationRequest.Target constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

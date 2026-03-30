
package org.cyoda.cloud.api.event.common.statemachine.conf;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.condition.QueryCondition;


/**
 * TransitionDefinition
 * <p>
 * Definition of a state transition
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "next",
    "manual",
    "disabled",
    "processors",
    "criterion"
})
@Generated("jsonschema2pojo")
public class TransitionDefinition {

    /**
     * Name of the transition
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of the transition")
    private String name;
    /**
     * Target state code for this transition
     * (Required)
     * 
     */
    @JsonProperty("next")
    @JsonPropertyDescription("Target state code for this transition")
    private String next;
    /**
     * Whether this transition requires manual triggering
     * (Required)
     * 
     */
    @JsonProperty("manual")
    @JsonPropertyDescription("Whether this transition requires manual triggering")
    private Boolean manual;
    /**
     * Flag indicating if the transition is disabled
     * 
     */
    @JsonProperty("disabled")
    @JsonPropertyDescription("Flag indicating if the transition is disabled")
    private Boolean disabled;
    /**
     * List of processors to execute for this transition
     * 
     */
    @JsonProperty("processors")
    @JsonPropertyDescription("List of processors to execute for this transition")
    private List<ProcessorDefinition> processors = new ArrayList<ProcessorDefinition>();
    /**
     * QueryCondition
     * <p>
     * 
     * 
     */
    @JsonProperty("criterion")
    private QueryCondition criterion;

    /**
     * Name of the transition
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of the transition
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public TransitionDefinition withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Target state code for this transition
     * (Required)
     * 
     */
    @JsonProperty("next")
    public String getNext() {
        return next;
    }

    /**
     * Target state code for this transition
     * (Required)
     * 
     */
    @JsonProperty("next")
    public void setNext(String next) {
        this.next = next;
    }

    public TransitionDefinition withNext(String next) {
        this.next = next;
        return this;
    }

    /**
     * Whether this transition requires manual triggering
     * (Required)
     * 
     */
    @JsonProperty("manual")
    public Boolean getManual() {
        return manual;
    }

    /**
     * Whether this transition requires manual triggering
     * (Required)
     * 
     */
    @JsonProperty("manual")
    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public TransitionDefinition withManual(Boolean manual) {
        this.manual = manual;
        return this;
    }

    /**
     * Flag indicating if the transition is disabled
     * 
     */
    @JsonProperty("disabled")
    public Boolean getDisabled() {
        return disabled;
    }

    /**
     * Flag indicating if the transition is disabled
     * 
     */
    @JsonProperty("disabled")
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public TransitionDefinition withDisabled(Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    /**
     * List of processors to execute for this transition
     * 
     */
    @JsonProperty("processors")
    public List<ProcessorDefinition> getProcessors() {
        return processors;
    }

    /**
     * List of processors to execute for this transition
     * 
     */
    @JsonProperty("processors")
    public void setProcessors(List<ProcessorDefinition> processors) {
        this.processors = processors;
    }

    public TransitionDefinition withProcessors(List<ProcessorDefinition> processors) {
        this.processors = processors;
        return this;
    }

    /**
     * QueryCondition
     * <p>
     * 
     * 
     */
    @JsonProperty("criterion")
    public QueryCondition getCriterion() {
        return criterion;
    }

    /**
     * QueryCondition
     * <p>
     * 
     * 
     */
    @JsonProperty("criterion")
    public void setCriterion(QueryCondition criterion) {
        this.criterion = criterion;
    }

    public TransitionDefinition withCriterion(QueryCondition criterion) {
        this.criterion = criterion;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TransitionDefinition.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("next");
        sb.append('=');
        sb.append(((this.next == null)?"<null>":this.next));
        sb.append(',');
        sb.append("manual");
        sb.append('=');
        sb.append(((this.manual == null)?"<null>":this.manual));
        sb.append(',');
        sb.append("disabled");
        sb.append('=');
        sb.append(((this.disabled == null)?"<null>":this.disabled));
        sb.append(',');
        sb.append("processors");
        sb.append('=');
        sb.append(((this.processors == null)?"<null>":this.processors));
        sb.append(',');
        sb.append("criterion");
        sb.append('=');
        sb.append(((this.criterion == null)?"<null>":this.criterion));
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
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.next == null)? 0 :this.next.hashCode()));
        result = ((result* 31)+((this.disabled == null)? 0 :this.disabled.hashCode()));
        result = ((result* 31)+((this.processors == null)? 0 :this.processors.hashCode()));
        result = ((result* 31)+((this.criterion == null)? 0 :this.criterion.hashCode()));
        result = ((result* 31)+((this.manual == null)? 0 :this.manual.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TransitionDefinition) == false) {
            return false;
        }
        TransitionDefinition rhs = ((TransitionDefinition) other);
        return (((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.next == rhs.next)||((this.next!= null)&&this.next.equals(rhs.next))))&&((this.disabled == rhs.disabled)||((this.disabled!= null)&&this.disabled.equals(rhs.disabled))))&&((this.processors == rhs.processors)||((this.processors!= null)&&this.processors.equals(rhs.processors))))&&((this.criterion == rhs.criterion)||((this.criterion!= null)&&this.criterion.equals(rhs.criterion))))&&((this.manual == rhs.manual)||((this.manual!= null)&&this.manual.equals(rhs.manual))));
    }

}

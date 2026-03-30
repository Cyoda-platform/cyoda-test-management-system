
package org.cyoda.cloud.api.event.common.statemachine.conf;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * StateDefinition
 * <p>
 * Definition of a workflow state
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "transitions"
})
@Generated("jsonschema2pojo")
public class StateDefinition {

    /**
     * List of possible transitions from this state
     * 
     */
    @JsonProperty("transitions")
    @JsonPropertyDescription("List of possible transitions from this state")
    private List<TransitionDefinition> transitions = new ArrayList<TransitionDefinition>();

    /**
     * List of possible transitions from this state
     * 
     */
    @JsonProperty("transitions")
    public List<TransitionDefinition> getTransitions() {
        return transitions;
    }

    /**
     * List of possible transitions from this state
     * 
     */
    @JsonProperty("transitions")
    public void setTransitions(List<TransitionDefinition> transitions) {
        this.transitions = transitions;
    }

    public StateDefinition withTransitions(List<TransitionDefinition> transitions) {
        this.transitions = transitions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(StateDefinition.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("transitions");
        sb.append('=');
        sb.append(((this.transitions == null)?"<null>":this.transitions));
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
        result = ((result* 31)+((this.transitions == null)? 0 :this.transitions.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof StateDefinition) == false) {
            return false;
        }
        StateDefinition rhs = ((StateDefinition) other);
        return ((this.transitions == rhs.transitions)||((this.transitions!= null)&&this.transitions.equals(rhs.transitions)));
    }

}

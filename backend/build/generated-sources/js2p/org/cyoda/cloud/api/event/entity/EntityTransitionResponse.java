
package org.cyoda.cloud.api.event.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityTransitionResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "availableTransitions"
})
@Generated("jsonschema2pojo")
public class EntityTransitionResponse
    extends BaseEvent
{

    /**
     * Available transitions from the current state.
     * 
     */
    @JsonProperty("availableTransitions")
    @JsonPropertyDescription("Available transitions from the current state.")
    private List<String> availableTransitions = new ArrayList<String>();

    /**
     * Available transitions from the current state.
     * 
     */
    @JsonProperty("availableTransitions")
    public List<String> getAvailableTransitions() {
        return availableTransitions;
    }

    /**
     * Available transitions from the current state.
     * 
     */
    @JsonProperty("availableTransitions")
    public void setAvailableTransitions(List<String> availableTransitions) {
        this.availableTransitions = availableTransitions;
    }

    public EntityTransitionResponse withAvailableTransitions(List<String> availableTransitions) {
        this.availableTransitions = availableTransitions;
        return this;
    }

    @Override
    public EntityTransitionResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityTransitionResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityTransitionResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityTransitionResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityTransitionResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("availableTransitions");
        sb.append('=');
        sb.append(((this.availableTransitions == null)?"<null>":this.availableTransitions));
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
        result = ((result* 31)+((this.availableTransitions == null)? 0 :this.availableTransitions.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityTransitionResponse) == false) {
            return false;
        }
        EntityTransitionResponse rhs = ((EntityTransitionResponse) other);
        return (super.equals(rhs)&&((this.availableTransitions == rhs.availableTransitions)||((this.availableTransitions!= null)&&this.availableTransitions.equals(rhs.availableTransitions))));
    }

}

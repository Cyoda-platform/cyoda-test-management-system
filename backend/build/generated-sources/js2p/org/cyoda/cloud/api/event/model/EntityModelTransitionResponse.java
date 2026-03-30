
package org.cyoda.cloud.api.event.model;

import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityModelTransitionResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "modelId",
    "state"
})
@Generated("jsonschema2pojo")
public class EntityModelTransitionResponse
    extends BaseEvent
{

    /**
     * ID of the entity model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    @JsonPropertyDescription("ID of the entity model.")
    private UUID modelId;
    /**
     * State of the entity model.
     * (Required)
     * 
     */
    @JsonProperty("state")
    @JsonPropertyDescription("State of the entity model.")
    private String state;

    /**
     * ID of the entity model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    public UUID getModelId() {
        return modelId;
    }

    /**
     * ID of the entity model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    public void setModelId(UUID modelId) {
        this.modelId = modelId;
    }

    public EntityModelTransitionResponse withModelId(UUID modelId) {
        this.modelId = modelId;
        return this;
    }

    /**
     * State of the entity model.
     * (Required)
     * 
     */
    @JsonProperty("state")
    public String getState() {
        return state;
    }

    /**
     * State of the entity model.
     * (Required)
     * 
     */
    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    public EntityModelTransitionResponse withState(String state) {
        this.state = state;
        return this;
    }

    @Override
    public EntityModelTransitionResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelTransitionResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelTransitionResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelTransitionResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelTransitionResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("modelId");
        sb.append('=');
        sb.append(((this.modelId == null)?"<null>":this.modelId));
        sb.append(',');
        sb.append("state");
        sb.append('=');
        sb.append(((this.state == null)?"<null>":this.state));
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
        result = ((result* 31)+((this.modelId == null)? 0 :this.modelId.hashCode()));
        result = ((result* 31)+((this.state == null)? 0 :this.state.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelTransitionResponse) == false) {
            return false;
        }
        EntityModelTransitionResponse rhs = ((EntityModelTransitionResponse) other);
        return ((super.equals(rhs)&&((this.modelId == rhs.modelId)||((this.modelId!= null)&&this.modelId.equals(rhs.modelId))))&&((this.state == rhs.state)||((this.state!= null)&&this.state.equals(rhs.state))));
    }

}

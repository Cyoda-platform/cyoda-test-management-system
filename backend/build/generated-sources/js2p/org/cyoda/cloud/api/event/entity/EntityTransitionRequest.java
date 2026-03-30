
package org.cyoda.cloud.api.event.entity;

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
 * EntityTransitionRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "entityId",
    "transition"
})
@Generated("jsonschema2pojo")
public class EntityTransitionRequest
    extends BaseEvent
{

    /**
     * ID of the entity.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    @JsonPropertyDescription("ID of the entity.")
    private UUID entityId;
    /**
     * Name of the transition to apply.
     * (Required)
     * 
     */
    @JsonProperty("transition")
    @JsonPropertyDescription("Name of the transition to apply.")
    private String transition;

    /**
     * ID of the entity.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    public UUID getEntityId() {
        return entityId;
    }

    /**
     * ID of the entity.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public EntityTransitionRequest withEntityId(UUID entityId) {
        this.entityId = entityId;
        return this;
    }

    /**
     * Name of the transition to apply.
     * (Required)
     * 
     */
    @JsonProperty("transition")
    public String getTransition() {
        return transition;
    }

    /**
     * Name of the transition to apply.
     * (Required)
     * 
     */
    @JsonProperty("transition")
    public void setTransition(String transition) {
        this.transition = transition;
    }

    public EntityTransitionRequest withTransition(String transition) {
        this.transition = transition;
        return this;
    }

    @Override
    public EntityTransitionRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityTransitionRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityTransitionRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityTransitionRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityTransitionRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("entityId");
        sb.append('=');
        sb.append(((this.entityId == null)?"<null>":this.entityId));
        sb.append(',');
        sb.append("transition");
        sb.append('=');
        sb.append(((this.transition == null)?"<null>":this.transition));
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
        result = ((result* 31)+((this.entityId == null)? 0 :this.entityId.hashCode()));
        result = ((result* 31)+((this.transition == null)? 0 :this.transition.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityTransitionRequest) == false) {
            return false;
        }
        EntityTransitionRequest rhs = ((EntityTransitionRequest) other);
        return ((super.equals(rhs)&&((this.entityId == rhs.entityId)||((this.entityId!= null)&&this.entityId.equals(rhs.entityId))))&&((this.transition == rhs.transition)||((this.transition!= null)&&this.transition.equals(rhs.transition))));
    }

}

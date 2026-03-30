
package org.cyoda.cloud.api.event.entity;

import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * EntityUpdatePayload
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "entityId",
    "data",
    "transition"
})
@Generated("jsonschema2pojo")
public class EntityUpdatePayload {

    /**
     * ID of the entity.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    @JsonPropertyDescription("ID of the entity.")
    private UUID entityId;
    /**
     * Entity payload data.
     * (Required)
     * 
     */
    @JsonProperty("data")
    @JsonPropertyDescription("Entity payload data.")
    private JsonNode data;
    /**
     * Transition to use for update.
     * 
     */
    @JsonProperty("transition")
    @JsonPropertyDescription("Transition to use for update.")
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

    public EntityUpdatePayload withEntityId(UUID entityId) {
        this.entityId = entityId;
        return this;
    }

    /**
     * Entity payload data.
     * (Required)
     * 
     */
    @JsonProperty("data")
    public JsonNode getData() {
        return data;
    }

    /**
     * Entity payload data.
     * (Required)
     * 
     */
    @JsonProperty("data")
    public void setData(JsonNode data) {
        this.data = data;
    }

    public EntityUpdatePayload withData(JsonNode data) {
        this.data = data;
        return this;
    }

    /**
     * Transition to use for update.
     * 
     */
    @JsonProperty("transition")
    public String getTransition() {
        return transition;
    }

    /**
     * Transition to use for update.
     * 
     */
    @JsonProperty("transition")
    public void setTransition(String transition) {
        this.transition = transition;
    }

    public EntityUpdatePayload withTransition(String transition) {
        this.transition = transition;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityUpdatePayload.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("entityId");
        sb.append('=');
        sb.append(((this.entityId == null)?"<null>":this.entityId));
        sb.append(',');
        sb.append("data");
        sb.append('=');
        sb.append(((this.data == null)?"<null>":this.data));
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
        result = ((result* 31)+((this.data == null)? 0 :this.data.hashCode()));
        result = ((result* 31)+((this.entityId == null)? 0 :this.entityId.hashCode()));
        result = ((result* 31)+((this.transition == null)? 0 :this.transition.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityUpdatePayload) == false) {
            return false;
        }
        EntityUpdatePayload rhs = ((EntityUpdatePayload) other);
        return ((((this.data == rhs.data)||((this.data!= null)&&this.data.equals(rhs.data)))&&((this.entityId == rhs.entityId)||((this.entityId!= null)&&this.entityId.equals(rhs.entityId))))&&((this.transition == rhs.transition)||((this.transition!= null)&&this.transition.equals(rhs.transition))));
    }

}

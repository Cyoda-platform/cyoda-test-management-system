
package org.cyoda.cloud.api.event.common;

import java.util.Date;
import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * EntityMetadata
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "modelKey",
    "state",
    "creationDate",
    "lastUpdateTime",
    "pointInTime",
    "transitionForLatestSave"
})
@Generated("jsonschema2pojo")
public class EntityMetadata {

    /**
     * ID of the entity.
     * (Required)
     * 
     */
    @JsonProperty("id")
    @JsonPropertyDescription("ID of the entity.")
    private UUID id;
    /**
     * ModelSpec
     * <p>
     * 
     * 
     */
    @JsonProperty("modelKey")
    private ModelSpec modelKey;
    /**
     * The state of the entity.
     * (Required)
     * 
     */
    @JsonProperty("state")
    @JsonPropertyDescription("The state of the entity.")
    private String state;
    /**
     * The creation date of the entity.
     * (Required)
     * 
     */
    @JsonProperty("creationDate")
    @JsonPropertyDescription("The creation date of the entity.")
    private Date creationDate;
    /**
     * The last time the entity was updated. Equals the creation date if the entity has not been updated.
     * (Required)
     * 
     */
    @JsonProperty("lastUpdateTime")
    @JsonPropertyDescription("The last time the entity was updated. Equals the creation date if the entity has not been updated.")
    private Date lastUpdateTime;
    /**
     * Optional value for the as-at point-in-time for which the entity was retrieved.
     * 
     */
    @JsonProperty("pointInTime")
    @JsonPropertyDescription("Optional value for the as-at point-in-time for which the entity was retrieved.")
    private Date pointInTime;
    /**
     * The transition applied of the entity when last saved.
     * 
     */
    @JsonProperty("transitionForLatestSave")
    @JsonPropertyDescription("The transition applied of the entity when last saved.")
    private String transitionForLatestSave;

    /**
     * ID of the entity.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public UUID getId() {
        return id;
    }

    /**
     * ID of the entity.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(UUID id) {
        this.id = id;
    }

    public EntityMetadata withId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * ModelSpec
     * <p>
     * 
     * 
     */
    @JsonProperty("modelKey")
    public ModelSpec getModelKey() {
        return modelKey;
    }

    /**
     * ModelSpec
     * <p>
     * 
     * 
     */
    @JsonProperty("modelKey")
    public void setModelKey(ModelSpec modelKey) {
        this.modelKey = modelKey;
    }

    public EntityMetadata withModelKey(ModelSpec modelKey) {
        this.modelKey = modelKey;
        return this;
    }

    /**
     * The state of the entity.
     * (Required)
     * 
     */
    @JsonProperty("state")
    public String getState() {
        return state;
    }

    /**
     * The state of the entity.
     * (Required)
     * 
     */
    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    public EntityMetadata withState(String state) {
        this.state = state;
        return this;
    }

    /**
     * The creation date of the entity.
     * (Required)
     * 
     */
    @JsonProperty("creationDate")
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * The creation date of the entity.
     * (Required)
     * 
     */
    @JsonProperty("creationDate")
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public EntityMetadata withCreationDate(Date creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * The last time the entity was updated. Equals the creation date if the entity has not been updated.
     * (Required)
     * 
     */
    @JsonProperty("lastUpdateTime")
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * The last time the entity was updated. Equals the creation date if the entity has not been updated.
     * (Required)
     * 
     */
    @JsonProperty("lastUpdateTime")
    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public EntityMetadata withLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
        return this;
    }

    /**
     * Optional value for the as-at point-in-time for which the entity was retrieved.
     * 
     */
    @JsonProperty("pointInTime")
    public Date getPointInTime() {
        return pointInTime;
    }

    /**
     * Optional value for the as-at point-in-time for which the entity was retrieved.
     * 
     */
    @JsonProperty("pointInTime")
    public void setPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
    }

    public EntityMetadata withPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
        return this;
    }

    /**
     * The transition applied of the entity when last saved.
     * 
     */
    @JsonProperty("transitionForLatestSave")
    public String getTransitionForLatestSave() {
        return transitionForLatestSave;
    }

    /**
     * The transition applied of the entity when last saved.
     * 
     */
    @JsonProperty("transitionForLatestSave")
    public void setTransitionForLatestSave(String transitionForLatestSave) {
        this.transitionForLatestSave = transitionForLatestSave;
    }

    public EntityMetadata withTransitionForLatestSave(String transitionForLatestSave) {
        this.transitionForLatestSave = transitionForLatestSave;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityMetadata.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("modelKey");
        sb.append('=');
        sb.append(((this.modelKey == null)?"<null>":this.modelKey));
        sb.append(',');
        sb.append("state");
        sb.append('=');
        sb.append(((this.state == null)?"<null>":this.state));
        sb.append(',');
        sb.append("creationDate");
        sb.append('=');
        sb.append(((this.creationDate == null)?"<null>":this.creationDate));
        sb.append(',');
        sb.append("lastUpdateTime");
        sb.append('=');
        sb.append(((this.lastUpdateTime == null)?"<null>":this.lastUpdateTime));
        sb.append(',');
        sb.append("pointInTime");
        sb.append('=');
        sb.append(((this.pointInTime == null)?"<null>":this.pointInTime));
        sb.append(',');
        sb.append("transitionForLatestSave");
        sb.append('=');
        sb.append(((this.transitionForLatestSave == null)?"<null>":this.transitionForLatestSave));
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
        result = ((result* 31)+((this.pointInTime == null)? 0 :this.pointInTime.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.state == null)? 0 :this.state.hashCode()));
        result = ((result* 31)+((this.creationDate == null)? 0 :this.creationDate.hashCode()));
        result = ((result* 31)+((this.transitionForLatestSave == null)? 0 :this.transitionForLatestSave.hashCode()));
        result = ((result* 31)+((this.modelKey == null)? 0 :this.modelKey.hashCode()));
        result = ((result* 31)+((this.lastUpdateTime == null)? 0 :this.lastUpdateTime.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityMetadata) == false) {
            return false;
        }
        EntityMetadata rhs = ((EntityMetadata) other);
        return ((((((((this.pointInTime == rhs.pointInTime)||((this.pointInTime!= null)&&this.pointInTime.equals(rhs.pointInTime)))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.state == rhs.state)||((this.state!= null)&&this.state.equals(rhs.state))))&&((this.creationDate == rhs.creationDate)||((this.creationDate!= null)&&this.creationDate.equals(rhs.creationDate))))&&((this.transitionForLatestSave == rhs.transitionForLatestSave)||((this.transitionForLatestSave!= null)&&this.transitionForLatestSave.equals(rhs.transitionForLatestSave))))&&((this.modelKey == rhs.modelKey)||((this.modelKey!= null)&&this.modelKey.equals(rhs.modelKey))))&&((this.lastUpdateTime == rhs.lastUpdateTime)||((this.lastUpdateTime!= null)&&this.lastUpdateTime.equals(rhs.lastUpdateTime))));
    }

}

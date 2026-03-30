
package org.cyoda.cloud.api.event.search;

import java.util.Date;
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
 * EntityChangesMetadataGetRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "entityId",
    "pointInTime"
})
@Generated("jsonschema2pojo")
public class EntityChangesMetadataGetRequest
    extends BaseEvent
{

    /**
     * ID of the entity to retrieve change history for.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    @JsonPropertyDescription("ID of the entity to retrieve change history for.")
    private UUID entityId;
    /**
     * Point in time to retrieve the entity changes. If not provided, retrieves all changes up to the current consistency time.
     * 
     */
    @JsonProperty("pointInTime")
    @JsonPropertyDescription("Point in time to retrieve the entity changes. If not provided, retrieves all changes up to the current consistency time.")
    private Date pointInTime;

    /**
     * ID of the entity to retrieve change history for.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    public UUID getEntityId() {
        return entityId;
    }

    /**
     * ID of the entity to retrieve change history for.
     * (Required)
     * 
     */
    @JsonProperty("entityId")
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public EntityChangesMetadataGetRequest withEntityId(UUID entityId) {
        this.entityId = entityId;
        return this;
    }

    /**
     * Point in time to retrieve the entity changes. If not provided, retrieves all changes up to the current consistency time.
     * 
     */
    @JsonProperty("pointInTime")
    public Date getPointInTime() {
        return pointInTime;
    }

    /**
     * Point in time to retrieve the entity changes. If not provided, retrieves all changes up to the current consistency time.
     * 
     */
    @JsonProperty("pointInTime")
    public void setPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
    }

    public EntityChangesMetadataGetRequest withPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
        return this;
    }

    @Override
    public EntityChangesMetadataGetRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityChangesMetadataGetRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityChangesMetadataGetRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityChangesMetadataGetRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityChangesMetadataGetRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("pointInTime");
        sb.append('=');
        sb.append(((this.pointInTime == null)?"<null>":this.pointInTime));
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
        result = ((result* 31)+((this.entityId == null)? 0 :this.entityId.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityChangesMetadataGetRequest) == false) {
            return false;
        }
        EntityChangesMetadataGetRequest rhs = ((EntityChangesMetadataGetRequest) other);
        return ((super.equals(rhs)&&((this.pointInTime == rhs.pointInTime)||((this.pointInTime!= null)&&this.pointInTime.equals(rhs.pointInTime))))&&((this.entityId == rhs.entityId)||((this.entityId!= null)&&this.entityId.equals(rhs.entityId))));
    }

}

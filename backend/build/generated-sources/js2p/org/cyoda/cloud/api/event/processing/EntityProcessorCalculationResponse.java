
package org.cyoda.cloud.api.event.processing;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.DataPayload;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityProcessorCalculationResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "entityId",
    "payload"
})
@Generated("jsonschema2pojo")
public class EntityProcessorCalculationResponse
    extends BaseEvent
{

    /**
     * ID of the original calculation request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    @JsonPropertyDescription("ID of the original calculation request.")
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
     * DataPayload
     * <p>
     * 
     * 
     */
    @JsonProperty("payload")
    private DataPayload payload;

    /**
     * ID of the original calculation request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public String getRequestId() {
        return requestId;
    }

    /**
     * ID of the original calculation request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public EntityProcessorCalculationResponse withRequestId(String requestId) {
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

    public EntityProcessorCalculationResponse withEntityId(String entityId) {
        this.entityId = entityId;
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

    public EntityProcessorCalculationResponse withPayload(DataPayload payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public EntityProcessorCalculationResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityProcessorCalculationResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityProcessorCalculationResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityProcessorCalculationResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityProcessorCalculationResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        result = ((result* 31)+((this.payload == null)? 0 :this.payload.hashCode()));
        result = ((result* 31)+((this.requestId == null)? 0 :this.requestId.hashCode()));
        result = ((result* 31)+((this.entityId == null)? 0 :this.entityId.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityProcessorCalculationResponse) == false) {
            return false;
        }
        EntityProcessorCalculationResponse rhs = ((EntityProcessorCalculationResponse) other);
        return (((super.equals(rhs)&&((this.payload == rhs.payload)||((this.payload!= null)&&this.payload.equals(rhs.payload))))&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.entityId == rhs.entityId)||((this.entityId!= null)&&this.entityId.equals(rhs.entityId))));
    }

}


package org.cyoda.cloud.api.event.search;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.EntityChangeMeta;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityChangesMetadataResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "changeMeta"
})
@Generated("jsonschema2pojo")
public class EntityChangesMetadataResponse
    extends BaseEvent
{

    /**
     * ID of the original request to get entity changes metadata.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    @JsonPropertyDescription("ID of the original request to get entity changes metadata.")
    private String requestId;
    /**
     * EntityChangeMeta
     * <p>
     * Metadata about entity changes including transaction information and change type.
     * (Required)
     * 
     */
    @JsonProperty("changeMeta")
    @JsonPropertyDescription("Metadata about entity changes including transaction information and change type.")
    private EntityChangeMeta changeMeta;

    /**
     * ID of the original request to get entity changes metadata.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public String getRequestId() {
        return requestId;
    }

    /**
     * ID of the original request to get entity changes metadata.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public EntityChangesMetadataResponse withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * EntityChangeMeta
     * <p>
     * Metadata about entity changes including transaction information and change type.
     * (Required)
     * 
     */
    @JsonProperty("changeMeta")
    public EntityChangeMeta getChangeMeta() {
        return changeMeta;
    }

    /**
     * EntityChangeMeta
     * <p>
     * Metadata about entity changes including transaction information and change type.
     * (Required)
     * 
     */
    @JsonProperty("changeMeta")
    public void setChangeMeta(EntityChangeMeta changeMeta) {
        this.changeMeta = changeMeta;
    }

    public EntityChangesMetadataResponse withChangeMeta(EntityChangeMeta changeMeta) {
        this.changeMeta = changeMeta;
        return this;
    }

    @Override
    public EntityChangesMetadataResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityChangesMetadataResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityChangesMetadataResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityChangesMetadataResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityChangesMetadataResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("changeMeta");
        sb.append('=');
        sb.append(((this.changeMeta == null)?"<null>":this.changeMeta));
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
        result = ((result* 31)+((this.requestId == null)? 0 :this.requestId.hashCode()));
        result = ((result* 31)+((this.changeMeta == null)? 0 :this.changeMeta.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityChangesMetadataResponse) == false) {
            return false;
        }
        EntityChangesMetadataResponse rhs = ((EntityChangesMetadataResponse) other);
        return ((super.equals(rhs)&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.changeMeta == rhs.changeMeta)||((this.changeMeta!= null)&&this.changeMeta.equals(rhs.changeMeta))));
    }

}

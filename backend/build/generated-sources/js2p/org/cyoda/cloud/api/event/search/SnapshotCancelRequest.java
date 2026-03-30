
package org.cyoda.cloud.api.event.search;

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
 * SnapshotCancelRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "snapshotId"
})
@Generated("jsonschema2pojo")
public class SnapshotCancelRequest
    extends BaseEvent
{

    /**
     * ID of the snapshot to cancel.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    @JsonPropertyDescription("ID of the snapshot to cancel.")
    private UUID snapshotId;

    /**
     * ID of the snapshot to cancel.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    public UUID getSnapshotId() {
        return snapshotId;
    }

    /**
     * ID of the snapshot to cancel.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public SnapshotCancelRequest withSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
        return this;
    }

    @Override
    public SnapshotCancelRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public SnapshotCancelRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public SnapshotCancelRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public SnapshotCancelRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SnapshotCancelRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("snapshotId");
        sb.append('=');
        sb.append(((this.snapshotId == null)?"<null>":this.snapshotId));
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
        result = ((result* 31)+((this.snapshotId == null)? 0 :this.snapshotId.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SnapshotCancelRequest) == false) {
            return false;
        }
        SnapshotCancelRequest rhs = ((SnapshotCancelRequest) other);
        return (super.equals(rhs)&&((this.snapshotId == rhs.snapshotId)||((this.snapshotId!= null)&&this.snapshotId.equals(rhs.snapshotId))));
    }

}

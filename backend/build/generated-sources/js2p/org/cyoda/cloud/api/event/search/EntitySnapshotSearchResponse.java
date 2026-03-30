
package org.cyoda.cloud.api.event.search;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntitySnapshotSearchResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "status"
})
@Generated("jsonschema2pojo")
public class EntitySnapshotSearchResponse
    extends BaseEvent
{

    /**
     * SearchSnapshotStatus
     * <p>
     * 
     * 
     */
    @JsonProperty("status")
    private SearchSnapshotStatus status;

    /**
     * SearchSnapshotStatus
     * <p>
     * 
     * 
     */
    @JsonProperty("status")
    public SearchSnapshotStatus getStatus() {
        return status;
    }

    /**
     * SearchSnapshotStatus
     * <p>
     * 
     * 
     */
    @JsonProperty("status")
    public void setStatus(SearchSnapshotStatus status) {
        this.status = status;
    }

    public EntitySnapshotSearchResponse withStatus(SearchSnapshotStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public EntitySnapshotSearchResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntitySnapshotSearchResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntitySnapshotSearchResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntitySnapshotSearchResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntitySnapshotSearchResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
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
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntitySnapshotSearchResponse) == false) {
            return false;
        }
        EntitySnapshotSearchResponse rhs = ((EntitySnapshotSearchResponse) other);
        return (super.equals(rhs)&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}

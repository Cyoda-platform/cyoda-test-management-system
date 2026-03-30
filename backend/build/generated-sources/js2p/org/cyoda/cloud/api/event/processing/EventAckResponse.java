
package org.cyoda.cloud.api.event.processing;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EventAckResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "sourceEventId"
})
@Generated("jsonschema2pojo")
public class EventAckResponse
    extends BaseEvent
{

    /**
     * ID of the original event.
     * (Required)
     * 
     */
    @JsonProperty("sourceEventId")
    @JsonPropertyDescription("ID of the original event.")
    private String sourceEventId;

    /**
     * ID of the original event.
     * (Required)
     * 
     */
    @JsonProperty("sourceEventId")
    public String getSourceEventId() {
        return sourceEventId;
    }

    /**
     * ID of the original event.
     * (Required)
     * 
     */
    @JsonProperty("sourceEventId")
    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public EventAckResponse withSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
        return this;
    }

    @Override
    public EventAckResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EventAckResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EventAckResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EventAckResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EventAckResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("sourceEventId");
        sb.append('=');
        sb.append(((this.sourceEventId == null)?"<null>":this.sourceEventId));
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
        result = ((result* 31)+((this.sourceEventId == null)? 0 :this.sourceEventId.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EventAckResponse) == false) {
            return false;
        }
        EventAckResponse rhs = ((EventAckResponse) other);
        return (super.equals(rhs)&&((this.sourceEventId == rhs.sourceEventId)||((this.sourceEventId!= null)&&this.sourceEventId.equals(rhs.sourceEventId))));
    }

}

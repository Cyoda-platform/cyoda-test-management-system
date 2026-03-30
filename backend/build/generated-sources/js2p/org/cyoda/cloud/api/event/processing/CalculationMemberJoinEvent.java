
package org.cyoda.cloud.api.event.processing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * CalculationMemberJoinEvent
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "tags"
})
@Generated("jsonschema2pojo")
public class CalculationMemberJoinEvent
    extends BaseEvent
{

    /**
     * Member tags. Could be used to filter applicability.
     * 
     */
    @JsonProperty("tags")
    @JsonPropertyDescription("Member tags. Could be used to filter applicability.")
    private List<String> tags = new ArrayList<String>();

    /**
     * Member tags. Could be used to filter applicability.
     * 
     */
    @JsonProperty("tags")
    public List<String> getTags() {
        return tags;
    }

    /**
     * Member tags. Could be used to filter applicability.
     * 
     */
    @JsonProperty("tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public CalculationMemberJoinEvent withTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public CalculationMemberJoinEvent withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public CalculationMemberJoinEvent withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public CalculationMemberJoinEvent withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public CalculationMemberJoinEvent withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CalculationMemberJoinEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("tags");
        sb.append('=');
        sb.append(((this.tags == null)?"<null>":this.tags));
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
        result = ((result* 31)+((this.tags == null)? 0 :this.tags.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CalculationMemberJoinEvent) == false) {
            return false;
        }
        CalculationMemberJoinEvent rhs = ((CalculationMemberJoinEvent) other);
        return (super.equals(rhs)&&((this.tags == rhs.tags)||((this.tags!= null)&&this.tags.equals(rhs.tags))));
    }

}

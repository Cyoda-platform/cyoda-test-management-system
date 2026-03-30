
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
 * CalculationMemberGreetEvent
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "memberId",
    "joinedLegalEntityId"
})
@Generated("jsonschema2pojo")
public class CalculationMemberGreetEvent
    extends BaseEvent
{

    /**
     * Assigned member ID.
     * (Required)
     * 
     */
    @JsonProperty("memberId")
    @JsonPropertyDescription("Assigned member ID.")
    private String memberId;
    /**
     * ID of the legal entity under which this member has joined.
     * (Required)
     * 
     */
    @JsonProperty("joinedLegalEntityId")
    @JsonPropertyDescription("ID of the legal entity under which this member has joined.")
    private String joinedLegalEntityId;

    /**
     * Assigned member ID.
     * (Required)
     * 
     */
    @JsonProperty("memberId")
    public String getMemberId() {
        return memberId;
    }

    /**
     * Assigned member ID.
     * (Required)
     * 
     */
    @JsonProperty("memberId")
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public CalculationMemberGreetEvent withMemberId(String memberId) {
        this.memberId = memberId;
        return this;
    }

    /**
     * ID of the legal entity under which this member has joined.
     * (Required)
     * 
     */
    @JsonProperty("joinedLegalEntityId")
    public String getJoinedLegalEntityId() {
        return joinedLegalEntityId;
    }

    /**
     * ID of the legal entity under which this member has joined.
     * (Required)
     * 
     */
    @JsonProperty("joinedLegalEntityId")
    public void setJoinedLegalEntityId(String joinedLegalEntityId) {
        this.joinedLegalEntityId = joinedLegalEntityId;
    }

    public CalculationMemberGreetEvent withJoinedLegalEntityId(String joinedLegalEntityId) {
        this.joinedLegalEntityId = joinedLegalEntityId;
        return this;
    }

    @Override
    public CalculationMemberGreetEvent withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public CalculationMemberGreetEvent withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public CalculationMemberGreetEvent withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public CalculationMemberGreetEvent withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CalculationMemberGreetEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("memberId");
        sb.append('=');
        sb.append(((this.memberId == null)?"<null>":this.memberId));
        sb.append(',');
        sb.append("joinedLegalEntityId");
        sb.append('=');
        sb.append(((this.joinedLegalEntityId == null)?"<null>":this.joinedLegalEntityId));
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
        result = ((result* 31)+((this.joinedLegalEntityId == null)? 0 :this.joinedLegalEntityId.hashCode()));
        result = ((result* 31)+((this.memberId == null)? 0 :this.memberId.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CalculationMemberGreetEvent) == false) {
            return false;
        }
        CalculationMemberGreetEvent rhs = ((CalculationMemberGreetEvent) other);
        return ((super.equals(rhs)&&((this.joinedLegalEntityId == rhs.joinedLegalEntityId)||((this.joinedLegalEntityId!= null)&&this.joinedLegalEntityId.equals(rhs.joinedLegalEntityId))))&&((this.memberId == rhs.memberId)||((this.memberId!= null)&&this.memberId.equals(rhs.memberId))));
    }

}

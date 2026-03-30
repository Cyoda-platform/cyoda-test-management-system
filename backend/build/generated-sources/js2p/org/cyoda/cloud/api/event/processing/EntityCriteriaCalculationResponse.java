
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
 * EntityCriteriaCalculationResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "entityId",
    "matches"
})
@Generated("jsonschema2pojo")
public class EntityCriteriaCalculationResponse
    extends BaseEvent
{

    /**
     * ID of the original criteria calculation request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    @JsonPropertyDescription("ID of the original criteria calculation request.")
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
     * Criteria check result.
     * 
     */
    @JsonProperty("matches")
    @JsonPropertyDescription("Criteria check result.")
    private Boolean matches;

    /**
     * ID of the original criteria calculation request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public String getRequestId() {
        return requestId;
    }

    /**
     * ID of the original criteria calculation request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public EntityCriteriaCalculationResponse withRequestId(String requestId) {
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

    public EntityCriteriaCalculationResponse withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    /**
     * Criteria check result.
     * 
     */
    @JsonProperty("matches")
    public Boolean getMatches() {
        return matches;
    }

    /**
     * Criteria check result.
     * 
     */
    @JsonProperty("matches")
    public void setMatches(Boolean matches) {
        this.matches = matches;
    }

    public EntityCriteriaCalculationResponse withMatches(Boolean matches) {
        this.matches = matches;
        return this;
    }

    @Override
    public EntityCriteriaCalculationResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityCriteriaCalculationResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityCriteriaCalculationResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityCriteriaCalculationResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityCriteriaCalculationResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("matches");
        sb.append('=');
        sb.append(((this.matches == null)?"<null>":this.matches));
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
        result = ((result* 31)+((this.matches == null)? 0 :this.matches.hashCode()));
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
        if ((other instanceof EntityCriteriaCalculationResponse) == false) {
            return false;
        }
        EntityCriteriaCalculationResponse rhs = ((EntityCriteriaCalculationResponse) other);
        return (((super.equals(rhs)&&((this.matches == rhs.matches)||((this.matches!= null)&&this.matches.equals(rhs.matches))))&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.entityId == rhs.entityId)||((this.entityId!= null)&&this.entityId.equals(rhs.entityId))));
    }

}

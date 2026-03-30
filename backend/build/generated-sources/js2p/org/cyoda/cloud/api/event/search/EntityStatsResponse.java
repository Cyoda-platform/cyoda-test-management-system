
package org.cyoda.cloud.api.event.search;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityStatsResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "modelName",
    "modelVersion",
    "count"
})
@Generated("jsonschema2pojo")
public class EntityStatsResponse
    extends BaseEvent
{

    /**
     * ID of the original request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    @JsonPropertyDescription("ID of the original request.")
    private String requestId;
    /**
     * Entity model name.
     * (Required)
     * 
     */
    @JsonProperty("modelName")
    @JsonPropertyDescription("Entity model name.")
    private String modelName;
    /**
     * Entity model version.
     * (Required)
     * 
     */
    @JsonProperty("modelVersion")
    @JsonPropertyDescription("Entity model version.")
    private Integer modelVersion;
    /**
     * Entity count for this model.
     * (Required)
     * 
     */
    @JsonProperty("count")
    @JsonPropertyDescription("Entity count for this model.")
    private Long count;

    /**
     * ID of the original request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public String getRequestId() {
        return requestId;
    }

    /**
     * ID of the original request.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public EntityStatsResponse withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Entity model name.
     * (Required)
     * 
     */
    @JsonProperty("modelName")
    public String getModelName() {
        return modelName;
    }

    /**
     * Entity model name.
     * (Required)
     * 
     */
    @JsonProperty("modelName")
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public EntityStatsResponse withModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    /**
     * Entity model version.
     * (Required)
     * 
     */
    @JsonProperty("modelVersion")
    public Integer getModelVersion() {
        return modelVersion;
    }

    /**
     * Entity model version.
     * (Required)
     * 
     */
    @JsonProperty("modelVersion")
    public void setModelVersion(Integer modelVersion) {
        this.modelVersion = modelVersion;
    }

    public EntityStatsResponse withModelVersion(Integer modelVersion) {
        this.modelVersion = modelVersion;
        return this;
    }

    /**
     * Entity count for this model.
     * (Required)
     * 
     */
    @JsonProperty("count")
    public Long getCount() {
        return count;
    }

    /**
     * Entity count for this model.
     * (Required)
     * 
     */
    @JsonProperty("count")
    public void setCount(Long count) {
        this.count = count;
    }

    public EntityStatsResponse withCount(Long count) {
        this.count = count;
        return this;
    }

    @Override
    public EntityStatsResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityStatsResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityStatsResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityStatsResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityStatsResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("modelName");
        sb.append('=');
        sb.append(((this.modelName == null)?"<null>":this.modelName));
        sb.append(',');
        sb.append("modelVersion");
        sb.append('=');
        sb.append(((this.modelVersion == null)?"<null>":this.modelVersion));
        sb.append(',');
        sb.append("count");
        sb.append('=');
        sb.append(((this.count == null)?"<null>":this.count));
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
        result = ((result* 31)+((this.count == null)? 0 :this.count.hashCode()));
        result = ((result* 31)+((this.modelName == null)? 0 :this.modelName.hashCode()));
        result = ((result* 31)+((this.requestId == null)? 0 :this.requestId.hashCode()));
        result = ((result* 31)+((this.modelVersion == null)? 0 :this.modelVersion.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityStatsResponse) == false) {
            return false;
        }
        EntityStatsResponse rhs = ((EntityStatsResponse) other);
        return ((((super.equals(rhs)&&((this.count == rhs.count)||((this.count!= null)&&this.count.equals(rhs.count))))&&((this.modelName == rhs.modelName)||((this.modelName!= null)&&this.modelName.equals(rhs.modelName))))&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.modelVersion == rhs.modelVersion)||((this.modelVersion!= null)&&this.modelVersion.equals(rhs.modelVersion))));
    }

}

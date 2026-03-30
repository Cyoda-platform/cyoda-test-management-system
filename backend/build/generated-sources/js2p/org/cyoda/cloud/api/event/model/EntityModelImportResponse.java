
package org.cyoda.cloud.api.event.model;

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
 * EntityModelImportResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "modelId"
})
@Generated("jsonschema2pojo")
public class EntityModelImportResponse
    extends BaseEvent
{

    /**
     * ID of the created or updated entity model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    @JsonPropertyDescription("ID of the created or updated entity model.")
    private UUID modelId;

    /**
     * ID of the created or updated entity model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    public UUID getModelId() {
        return modelId;
    }

    /**
     * ID of the created or updated entity model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    public void setModelId(UUID modelId) {
        this.modelId = modelId;
    }

    public EntityModelImportResponse withModelId(UUID modelId) {
        this.modelId = modelId;
        return this;
    }

    @Override
    public EntityModelImportResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelImportResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelImportResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelImportResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelImportResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("modelId");
        sb.append('=');
        sb.append(((this.modelId == null)?"<null>":this.modelId));
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
        result = ((result* 31)+((this.modelId == null)? 0 :this.modelId.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelImportResponse) == false) {
            return false;
        }
        EntityModelImportResponse rhs = ((EntityModelImportResponse) other);
        return (super.equals(rhs)&&((this.modelId == rhs.modelId)||((this.modelId!= null)&&this.modelId.equals(rhs.modelId))));
    }

}

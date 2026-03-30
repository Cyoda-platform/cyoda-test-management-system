
package org.cyoda.cloud.api.event.model;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.ModelInfo;


/**
 * EntityModelGetAllResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "models"
})
@Generated("jsonschema2pojo")
public class EntityModelGetAllResponse
    extends BaseEvent
{

    /**
     * Information about registered models.
     * (Required)
     * 
     */
    @JsonProperty("models")
    @JsonPropertyDescription("Information about registered models.")
    private List<ModelInfo> models = new ArrayList<ModelInfo>();

    /**
     * Information about registered models.
     * (Required)
     * 
     */
    @JsonProperty("models")
    public List<ModelInfo> getModels() {
        return models;
    }

    /**
     * Information about registered models.
     * (Required)
     * 
     */
    @JsonProperty("models")
    public void setModels(List<ModelInfo> models) {
        this.models = models;
    }

    public EntityModelGetAllResponse withModels(List<ModelInfo> models) {
        this.models = models;
        return this;
    }

    @Override
    public EntityModelGetAllResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelGetAllResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelGetAllResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelGetAllResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelGetAllResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("models");
        sb.append('=');
        sb.append(((this.models == null)?"<null>":this.models));
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
        result = ((result* 31)+((this.models == null)? 0 :this.models.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelGetAllResponse) == false) {
            return false;
        }
        EntityModelGetAllResponse rhs = ((EntityModelGetAllResponse) other);
        return (super.equals(rhs)&&((this.models == rhs.models)||((this.models!= null)&&this.models.equals(rhs.models))));
    }

}

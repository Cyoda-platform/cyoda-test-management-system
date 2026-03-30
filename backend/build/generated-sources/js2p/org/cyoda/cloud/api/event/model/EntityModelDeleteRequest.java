
package org.cyoda.cloud.api.event.model;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.ModelSpec;


/**
 * EntityModelDeleteRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "model"
})
@Generated("jsonschema2pojo")
public class EntityModelDeleteRequest
    extends BaseEvent
{

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    private ModelSpec model;

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    public ModelSpec getModel() {
        return model;
    }

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    public void setModel(ModelSpec model) {
        this.model = model;
    }

    public EntityModelDeleteRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    @Override
    public EntityModelDeleteRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelDeleteRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelDeleteRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelDeleteRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelDeleteRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("model");
        sb.append('=');
        sb.append(((this.model == null)?"<null>":this.model));
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
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelDeleteRequest) == false) {
            return false;
        }
        EntityModelDeleteRequest rhs = ((EntityModelDeleteRequest) other);
        return (super.equals(rhs)&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))));
    }

}

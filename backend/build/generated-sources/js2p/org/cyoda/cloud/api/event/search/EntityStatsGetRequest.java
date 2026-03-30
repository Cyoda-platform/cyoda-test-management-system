
package org.cyoda.cloud.api.event.search;

import java.util.Date;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.ModelSpec;


/**
 * EntityStatsGetRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "pointInTime",
    "model"
})
@Generated("jsonschema2pojo")
public class EntityStatsGetRequest
    extends BaseEvent
{

    /**
     * The point-in-time for statistics in ISO 8601 format. Defaults to current consistency time if not provided.
     * 
     */
    @JsonProperty("pointInTime")
    @JsonPropertyDescription("The point-in-time for statistics in ISO 8601 format. Defaults to current consistency time if not provided.")
    private Date pointInTime;
    /**
     * ModelSpec
     * <p>
     * 
     * 
     */
    @JsonProperty("model")
    private ModelSpec model;

    /**
     * The point-in-time for statistics in ISO 8601 format. Defaults to current consistency time if not provided.
     * 
     */
    @JsonProperty("pointInTime")
    public Date getPointInTime() {
        return pointInTime;
    }

    /**
     * The point-in-time for statistics in ISO 8601 format. Defaults to current consistency time if not provided.
     * 
     */
    @JsonProperty("pointInTime")
    public void setPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
    }

    public EntityStatsGetRequest withPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
        return this;
    }

    /**
     * ModelSpec
     * <p>
     * 
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
     * 
     */
    @JsonProperty("model")
    public void setModel(ModelSpec model) {
        this.model = model;
    }

    public EntityStatsGetRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    @Override
    public EntityStatsGetRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityStatsGetRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityStatsGetRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityStatsGetRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityStatsGetRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("pointInTime");
        sb.append('=');
        sb.append(((this.pointInTime == null)?"<null>":this.pointInTime));
        sb.append(',');
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
        result = ((result* 31)+((this.pointInTime == null)? 0 :this.pointInTime.hashCode()));
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityStatsGetRequest) == false) {
            return false;
        }
        EntityStatsGetRequest rhs = ((EntityStatsGetRequest) other);
        return ((super.equals(rhs)&&((this.pointInTime == rhs.pointInTime)||((this.pointInTime!= null)&&this.pointInTime.equals(rhs.pointInTime))))&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))));
    }

}


package org.cyoda.cloud.api.event.search;

import java.util.ArrayList;
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
 * EntityStatsByStateGetRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "pointInTime",
    "model",
    "states"
})
@Generated("jsonschema2pojo")
public class EntityStatsByStateGetRequest
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
     * Optional list of states for which to calculate statistics. If not provided, statistics will be calculated for all current workflow states.
     * 
     */
    @JsonProperty("states")
    @JsonPropertyDescription("Optional list of states for which to calculate statistics. If not provided, statistics will be calculated for all current workflow states.")
    private List<String> states = new ArrayList<String>();

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

    public EntityStatsByStateGetRequest withPointInTime(Date pointInTime) {
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

    public EntityStatsByStateGetRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    /**
     * Optional list of states for which to calculate statistics. If not provided, statistics will be calculated for all current workflow states.
     * 
     */
    @JsonProperty("states")
    public List<String> getStates() {
        return states;
    }

    /**
     * Optional list of states for which to calculate statistics. If not provided, statistics will be calculated for all current workflow states.
     * 
     */
    @JsonProperty("states")
    public void setStates(List<String> states) {
        this.states = states;
    }

    public EntityStatsByStateGetRequest withStates(List<String> states) {
        this.states = states;
        return this;
    }

    @Override
    public EntityStatsByStateGetRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityStatsByStateGetRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityStatsByStateGetRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityStatsByStateGetRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityStatsByStateGetRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("states");
        sb.append('=');
        sb.append(((this.states == null)?"<null>":this.states));
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
        result = ((result* 31)+((this.states == null)? 0 :this.states.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityStatsByStateGetRequest) == false) {
            return false;
        }
        EntityStatsByStateGetRequest rhs = ((EntityStatsByStateGetRequest) other);
        return (((super.equals(rhs)&&((this.pointInTime == rhs.pointInTime)||((this.pointInTime!= null)&&this.pointInTime.equals(rhs.pointInTime))))&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))))&&((this.states == rhs.states)||((this.states!= null)&&this.states.equals(rhs.states))));
    }

}

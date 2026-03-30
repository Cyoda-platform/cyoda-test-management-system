
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
import org.cyoda.cloud.api.event.common.condition.GroupCondition;


/**
 * EntitySearchRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "model",
    "condition",
    "pointInTime",
    "timeoutMillis",
    "limit"
})
@Generated("jsonschema2pojo")
public class EntitySearchRequest
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
     * GroupCondition
     * <p>
     * Condition with a type 'group'
     * (Required)
     * 
     */
    @JsonProperty("condition")
    @JsonPropertyDescription("Condition with a type 'group'")
    private GroupCondition condition;
    /**
     * point in time
     * 
     */
    @JsonProperty("pointInTime")
    @JsonPropertyDescription("point in time")
    private Date pointInTime;
    /**
     * The maximum time to wait in milliseconds for the query to complete.
     * 
     */
    @JsonProperty("timeoutMillis")
    @JsonPropertyDescription("The maximum time to wait in milliseconds for the query to complete.")
    private Integer timeoutMillis;
    /**
     * The maximum number of rows to return.
     * 
     */
    @JsonProperty("limit")
    @JsonPropertyDescription("The maximum number of rows to return.")
    private Integer limit;

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

    public EntitySearchRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    /**
     * GroupCondition
     * <p>
     * Condition with a type 'group'
     * (Required)
     * 
     */
    @JsonProperty("condition")
    public GroupCondition getCondition() {
        return condition;
    }

    /**
     * GroupCondition
     * <p>
     * Condition with a type 'group'
     * (Required)
     * 
     */
    @JsonProperty("condition")
    public void setCondition(GroupCondition condition) {
        this.condition = condition;
    }

    public EntitySearchRequest withCondition(GroupCondition condition) {
        this.condition = condition;
        return this;
    }

    /**
     * point in time
     * 
     */
    @JsonProperty("pointInTime")
    public Date getPointInTime() {
        return pointInTime;
    }

    /**
     * point in time
     * 
     */
    @JsonProperty("pointInTime")
    public void setPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
    }

    public EntitySearchRequest withPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
        return this;
    }

    /**
     * The maximum time to wait in milliseconds for the query to complete.
     * 
     */
    @JsonProperty("timeoutMillis")
    public Integer getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * The maximum time to wait in milliseconds for the query to complete.
     * 
     */
    @JsonProperty("timeoutMillis")
    public void setTimeoutMillis(Integer timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public EntitySearchRequest withTimeoutMillis(Integer timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    /**
     * The maximum number of rows to return.
     * 
     */
    @JsonProperty("limit")
    public Integer getLimit() {
        return limit;
    }

    /**
     * The maximum number of rows to return.
     * 
     */
    @JsonProperty("limit")
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public EntitySearchRequest withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public EntitySearchRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntitySearchRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntitySearchRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntitySearchRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntitySearchRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("condition");
        sb.append('=');
        sb.append(((this.condition == null)?"<null>":this.condition));
        sb.append(',');
        sb.append("pointInTime");
        sb.append('=');
        sb.append(((this.pointInTime == null)?"<null>":this.pointInTime));
        sb.append(',');
        sb.append("timeoutMillis");
        sb.append('=');
        sb.append(((this.timeoutMillis == null)?"<null>":this.timeoutMillis));
        sb.append(',');
        sb.append("limit");
        sb.append('=');
        sb.append(((this.limit == null)?"<null>":this.limit));
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
        result = ((result* 31)+((this.limit == null)? 0 :this.limit.hashCode()));
        result = ((result* 31)+((this.pointInTime == null)? 0 :this.pointInTime.hashCode()));
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+((this.condition == null)? 0 :this.condition.hashCode()));
        result = ((result* 31)+((this.timeoutMillis == null)? 0 :this.timeoutMillis.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntitySearchRequest) == false) {
            return false;
        }
        EntitySearchRequest rhs = ((EntitySearchRequest) other);
        return (((((super.equals(rhs)&&((this.limit == rhs.limit)||((this.limit!= null)&&this.limit.equals(rhs.limit))))&&((this.pointInTime == rhs.pointInTime)||((this.pointInTime!= null)&&this.pointInTime.equals(rhs.pointInTime))))&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))))&&((this.condition == rhs.condition)||((this.condition!= null)&&this.condition.equals(rhs.condition))))&&((this.timeoutMillis == rhs.timeoutMillis)||((this.timeoutMillis!= null)&&this.timeoutMillis.equals(rhs.timeoutMillis))));
    }

}

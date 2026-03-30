
package org.cyoda.cloud.api.event.entity;

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
 * EntityDeleteAllRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "model",
    "pageSize",
    "transactionSize",
    "pointInTime",
    "verbose"
})
@Generated("jsonschema2pojo")
public class EntityDeleteAllRequest
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
     * Page size.
     * 
     */
    @JsonProperty("pageSize")
    @JsonPropertyDescription("Page size.")
    private Integer pageSize = 10;
    /**
     * Transaction size.
     * 
     */
    @JsonProperty("transactionSize")
    @JsonPropertyDescription("Transaction size.")
    private Integer transactionSize = 1000;
    /**
     * point in time, i.e. delete all that existed prior to this point in time
     * 
     */
    @JsonProperty("pointInTime")
    @JsonPropertyDescription("point in time, i.e. delete all that existed prior to this point in time")
    private Date pointInTime;
    /**
     * Include the list of entity ids deleted in the response
     * 
     */
    @JsonProperty("verbose")
    @JsonPropertyDescription("Include the list of entity ids deleted in the response")
    private Boolean verbose = false;

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

    public EntityDeleteAllRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    /**
     * Page size.
     * 
     */
    @JsonProperty("pageSize")
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Page size.
     * 
     */
    @JsonProperty("pageSize")
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public EntityDeleteAllRequest withPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Transaction size.
     * 
     */
    @JsonProperty("transactionSize")
    public Integer getTransactionSize() {
        return transactionSize;
    }

    /**
     * Transaction size.
     * 
     */
    @JsonProperty("transactionSize")
    public void setTransactionSize(Integer transactionSize) {
        this.transactionSize = transactionSize;
    }

    public EntityDeleteAllRequest withTransactionSize(Integer transactionSize) {
        this.transactionSize = transactionSize;
        return this;
    }

    /**
     * point in time, i.e. delete all that existed prior to this point in time
     * 
     */
    @JsonProperty("pointInTime")
    public Date getPointInTime() {
        return pointInTime;
    }

    /**
     * point in time, i.e. delete all that existed prior to this point in time
     * 
     */
    @JsonProperty("pointInTime")
    public void setPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
    }

    public EntityDeleteAllRequest withPointInTime(Date pointInTime) {
        this.pointInTime = pointInTime;
        return this;
    }

    /**
     * Include the list of entity ids deleted in the response
     * 
     */
    @JsonProperty("verbose")
    public Boolean getVerbose() {
        return verbose;
    }

    /**
     * Include the list of entity ids deleted in the response
     * 
     */
    @JsonProperty("verbose")
    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }

    public EntityDeleteAllRequest withVerbose(Boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    @Override
    public EntityDeleteAllRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityDeleteAllRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityDeleteAllRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityDeleteAllRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityDeleteAllRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("pageSize");
        sb.append('=');
        sb.append(((this.pageSize == null)?"<null>":this.pageSize));
        sb.append(',');
        sb.append("transactionSize");
        sb.append('=');
        sb.append(((this.transactionSize == null)?"<null>":this.transactionSize));
        sb.append(',');
        sb.append("pointInTime");
        sb.append('=');
        sb.append(((this.pointInTime == null)?"<null>":this.pointInTime));
        sb.append(',');
        sb.append("verbose");
        sb.append('=');
        sb.append(((this.verbose == null)?"<null>":this.verbose));
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
        result = ((result* 31)+((this.transactionSize == null)? 0 :this.transactionSize.hashCode()));
        result = ((result* 31)+((this.pageSize == null)? 0 :this.pageSize.hashCode()));
        result = ((result* 31)+((this.pointInTime == null)? 0 :this.pointInTime.hashCode()));
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+((this.verbose == null)? 0 :this.verbose.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityDeleteAllRequest) == false) {
            return false;
        }
        EntityDeleteAllRequest rhs = ((EntityDeleteAllRequest) other);
        return (((((super.equals(rhs)&&((this.transactionSize == rhs.transactionSize)||((this.transactionSize!= null)&&this.transactionSize.equals(rhs.transactionSize))))&&((this.pageSize == rhs.pageSize)||((this.pageSize!= null)&&this.pageSize.equals(rhs.pageSize))))&&((this.pointInTime == rhs.pointInTime)||((this.pointInTime!= null)&&this.pointInTime.equals(rhs.pointInTime))))&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))))&&((this.verbose == rhs.verbose)||((this.verbose!= null)&&this.verbose.equals(rhs.verbose))));
    }

}

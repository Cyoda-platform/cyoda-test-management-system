
package org.cyoda.cloud.api.event.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityDeleteAllResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "entityIds",
    "modelId",
    "numDeleted",
    "errorsById"
})
@Generated("jsonschema2pojo")
public class EntityDeleteAllResponse
    extends BaseEvent
{

    /**
     * ID of the original request to get data.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    @JsonPropertyDescription("ID of the original request to get data.")
    private java.lang.String requestId;
    /**
     * IDs of the removed entities.
     * (Required)
     * 
     */
    @JsonProperty("entityIds")
    @JsonPropertyDescription("IDs of the removed entities.")
    private List<UUID> entityIds = new ArrayList<UUID>();
    /**
     * ID of the model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    @JsonPropertyDescription("ID of the model.")
    private UUID modelId;
    /**
     * Number of the deleted entities.
     * (Required)
     * 
     */
    @JsonProperty("numDeleted")
    @JsonPropertyDescription("Number of the deleted entities.")
    private Integer numDeleted;
    /**
     * Collections of errors by ids if any.
     * 
     */
    @JsonProperty("errorsById")
    @JsonPropertyDescription("Collections of errors by ids if any.")
    private Map<String, String> errorsById;

    /**
     * ID of the original request to get data.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public java.lang.String getRequestId() {
        return requestId;
    }

    /**
     * ID of the original request to get data.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public void setRequestId(java.lang.String requestId) {
        this.requestId = requestId;
    }

    public EntityDeleteAllResponse withRequestId(java.lang.String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * IDs of the removed entities.
     * (Required)
     * 
     */
    @JsonProperty("entityIds")
    public List<UUID> getEntityIds() {
        return entityIds;
    }

    /**
     * IDs of the removed entities.
     * (Required)
     * 
     */
    @JsonProperty("entityIds")
    public void setEntityIds(List<UUID> entityIds) {
        this.entityIds = entityIds;
    }

    public EntityDeleteAllResponse withEntityIds(List<UUID> entityIds) {
        this.entityIds = entityIds;
        return this;
    }

    /**
     * ID of the model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    public UUID getModelId() {
        return modelId;
    }

    /**
     * ID of the model.
     * (Required)
     * 
     */
    @JsonProperty("modelId")
    public void setModelId(UUID modelId) {
        this.modelId = modelId;
    }

    public EntityDeleteAllResponse withModelId(UUID modelId) {
        this.modelId = modelId;
        return this;
    }

    /**
     * Number of the deleted entities.
     * (Required)
     * 
     */
    @JsonProperty("numDeleted")
    public Integer getNumDeleted() {
        return numDeleted;
    }

    /**
     * Number of the deleted entities.
     * (Required)
     * 
     */
    @JsonProperty("numDeleted")
    public void setNumDeleted(Integer numDeleted) {
        this.numDeleted = numDeleted;
    }

    public EntityDeleteAllResponse withNumDeleted(Integer numDeleted) {
        this.numDeleted = numDeleted;
        return this;
    }

    /**
     * Collections of errors by ids if any.
     * 
     */
    @JsonProperty("errorsById")
    public Map<String, String> getErrorsById() {
        return errorsById;
    }

    /**
     * Collections of errors by ids if any.
     * 
     */
    @JsonProperty("errorsById")
    public void setErrorsById(Map<String, String> errorsById) {
        this.errorsById = errorsById;
    }

    public EntityDeleteAllResponse withErrorsById(Map<String, String> errorsById) {
        this.errorsById = errorsById;
        return this;
    }

    @Override
    public EntityDeleteAllResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityDeleteAllResponse withId(java.lang.String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityDeleteAllResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityDeleteAllResponse withWarnings(List<java.lang.String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public java.lang.String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityDeleteAllResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        int baseLength = sb.length();
        java.lang.String superString = super.toString();
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
        sb.append("entityIds");
        sb.append('=');
        sb.append(((this.entityIds == null)?"<null>":this.entityIds));
        sb.append(',');
        sb.append("modelId");
        sb.append('=');
        sb.append(((this.modelId == null)?"<null>":this.modelId));
        sb.append(',');
        sb.append("numDeleted");
        sb.append('=');
        sb.append(((this.numDeleted == null)?"<null>":this.numDeleted));
        sb.append(',');
        sb.append("errorsById");
        sb.append('=');
        sb.append(((this.errorsById == null)?"<null>":this.errorsById));
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
        result = ((result* 31)+((this.numDeleted == null)? 0 :this.numDeleted.hashCode()));
        result = ((result* 31)+((this.errorsById == null)? 0 :this.errorsById.hashCode()));
        result = ((result* 31)+((this.modelId == null)? 0 :this.modelId.hashCode()));
        result = ((result* 31)+((this.requestId == null)? 0 :this.requestId.hashCode()));
        result = ((result* 31)+((this.entityIds == null)? 0 :this.entityIds.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityDeleteAllResponse) == false) {
            return false;
        }
        EntityDeleteAllResponse rhs = ((EntityDeleteAllResponse) other);
        return (((((super.equals(rhs)&&((this.numDeleted == rhs.numDeleted)||((this.numDeleted!= null)&&this.numDeleted.equals(rhs.numDeleted))))&&((this.errorsById == rhs.errorsById)||((this.errorsById!= null)&&this.errorsById.equals(rhs.errorsById))))&&((this.modelId == rhs.modelId)||((this.modelId!= null)&&this.modelId.equals(rhs.modelId))))&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.entityIds == rhs.entityIds)||((this.entityIds!= null)&&this.entityIds.equals(rhs.entityIds))));
    }

}

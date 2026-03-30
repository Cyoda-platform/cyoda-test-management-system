
package org.cyoda.cloud.api.event.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * EntityTransactionInfo
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "transactionId",
    "entityIds"
})
@Generated("jsonschema2pojo")
public class EntityTransactionInfo {

    /**
     * ID of the transaction.
     * 
     */
    @JsonProperty("transactionId")
    @JsonPropertyDescription("ID of the transaction.")
    private UUID transactionId;
    /**
     * IDs of entities in this transaction.
     * (Required)
     * 
     */
    @JsonProperty("entityIds")
    @JsonPropertyDescription("IDs of entities in this transaction.")
    private List<UUID> entityIds = new ArrayList<UUID>();

    /**
     * ID of the transaction.
     * 
     */
    @JsonProperty("transactionId")
    public UUID getTransactionId() {
        return transactionId;
    }

    /**
     * ID of the transaction.
     * 
     */
    @JsonProperty("transactionId")
    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public EntityTransactionInfo withTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * IDs of entities in this transaction.
     * (Required)
     * 
     */
    @JsonProperty("entityIds")
    public List<UUID> getEntityIds() {
        return entityIds;
    }

    /**
     * IDs of entities in this transaction.
     * (Required)
     * 
     */
    @JsonProperty("entityIds")
    public void setEntityIds(List<UUID> entityIds) {
        this.entityIds = entityIds;
    }

    public EntityTransactionInfo withEntityIds(List<UUID> entityIds) {
        this.entityIds = entityIds;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityTransactionInfo.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("transactionId");
        sb.append('=');
        sb.append(((this.transactionId == null)?"<null>":this.transactionId));
        sb.append(',');
        sb.append("entityIds");
        sb.append('=');
        sb.append(((this.entityIds == null)?"<null>":this.entityIds));
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
        result = ((result* 31)+((this.transactionId == null)? 0 :this.transactionId.hashCode()));
        result = ((result* 31)+((this.entityIds == null)? 0 :this.entityIds.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityTransactionInfo) == false) {
            return false;
        }
        EntityTransactionInfo rhs = ((EntityTransactionInfo) other);
        return (((this.transactionId == rhs.transactionId)||((this.transactionId!= null)&&this.transactionId.equals(rhs.transactionId)))&&((this.entityIds == rhs.entityIds)||((this.entityIds!= null)&&this.entityIds.equals(rhs.entityIds))));
    }

}


package org.cyoda.cloud.api.event.common;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * EntityChangeMeta
 * <p>
 * Metadata about entity changes including transaction information and change type.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "transactionId",
    "timeOfChange",
    "user",
    "changeType",
    "fieldsChangedCount"
})
@Generated("jsonschema2pojo")
public class EntityChangeMeta {

    /**
     * UUID of the transaction that made this change.
     * 
     */
    @JsonProperty("transactionId")
    @JsonPropertyDescription("UUID of the transaction that made this change.")
    private UUID transactionId;
    /**
     * Timestamp when the change occurred.
     * (Required)
     * 
     */
    @JsonProperty("timeOfChange")
    @JsonPropertyDescription("Timestamp when the change occurred.")
    private Date timeOfChange;
    /**
     * User who made the change.
     * (Required)
     * 
     */
    @JsonProperty("user")
    @JsonPropertyDescription("User who made the change.")
    private String user;
    /**
     * Type of change that was made to the entity.
     * (Required)
     * 
     */
    @JsonProperty("changeType")
    @JsonPropertyDescription("Type of change that was made to the entity.")
    private EntityChangeMeta.ChangeType changeType;
    /**
     * Number of fields changed in the entity for this change.
     * 
     */
    @JsonProperty("fieldsChangedCount")
    @JsonPropertyDescription("Number of fields changed in the entity for this change.")
    private Integer fieldsChangedCount;

    /**
     * UUID of the transaction that made this change.
     * 
     */
    @JsonProperty("transactionId")
    public UUID getTransactionId() {
        return transactionId;
    }

    /**
     * UUID of the transaction that made this change.
     * 
     */
    @JsonProperty("transactionId")
    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public EntityChangeMeta withTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * Timestamp when the change occurred.
     * (Required)
     * 
     */
    @JsonProperty("timeOfChange")
    public Date getTimeOfChange() {
        return timeOfChange;
    }

    /**
     * Timestamp when the change occurred.
     * (Required)
     * 
     */
    @JsonProperty("timeOfChange")
    public void setTimeOfChange(Date timeOfChange) {
        this.timeOfChange = timeOfChange;
    }

    public EntityChangeMeta withTimeOfChange(Date timeOfChange) {
        this.timeOfChange = timeOfChange;
        return this;
    }

    /**
     * User who made the change.
     * (Required)
     * 
     */
    @JsonProperty("user")
    public String getUser() {
        return user;
    }

    /**
     * User who made the change.
     * (Required)
     * 
     */
    @JsonProperty("user")
    public void setUser(String user) {
        this.user = user;
    }

    public EntityChangeMeta withUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * Type of change that was made to the entity.
     * (Required)
     * 
     */
    @JsonProperty("changeType")
    public EntityChangeMeta.ChangeType getChangeType() {
        return changeType;
    }

    /**
     * Type of change that was made to the entity.
     * (Required)
     * 
     */
    @JsonProperty("changeType")
    public void setChangeType(EntityChangeMeta.ChangeType changeType) {
        this.changeType = changeType;
    }

    public EntityChangeMeta withChangeType(EntityChangeMeta.ChangeType changeType) {
        this.changeType = changeType;
        return this;
    }

    /**
     * Number of fields changed in the entity for this change.
     * 
     */
    @JsonProperty("fieldsChangedCount")
    public Integer getFieldsChangedCount() {
        return fieldsChangedCount;
    }

    /**
     * Number of fields changed in the entity for this change.
     * 
     */
    @JsonProperty("fieldsChangedCount")
    public void setFieldsChangedCount(Integer fieldsChangedCount) {
        this.fieldsChangedCount = fieldsChangedCount;
    }

    public EntityChangeMeta withFieldsChangedCount(Integer fieldsChangedCount) {
        this.fieldsChangedCount = fieldsChangedCount;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityChangeMeta.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("transactionId");
        sb.append('=');
        sb.append(((this.transactionId == null)?"<null>":this.transactionId));
        sb.append(',');
        sb.append("timeOfChange");
        sb.append('=');
        sb.append(((this.timeOfChange == null)?"<null>":this.timeOfChange));
        sb.append(',');
        sb.append("user");
        sb.append('=');
        sb.append(((this.user == null)?"<null>":this.user));
        sb.append(',');
        sb.append("changeType");
        sb.append('=');
        sb.append(((this.changeType == null)?"<null>":this.changeType));
        sb.append(',');
        sb.append("fieldsChangedCount");
        sb.append('=');
        sb.append(((this.fieldsChangedCount == null)?"<null>":this.fieldsChangedCount));
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
        result = ((result* 31)+((this.timeOfChange == null)? 0 :this.timeOfChange.hashCode()));
        result = ((result* 31)+((this.user == null)? 0 :this.user.hashCode()));
        result = ((result* 31)+((this.transactionId == null)? 0 :this.transactionId.hashCode()));
        result = ((result* 31)+((this.fieldsChangedCount == null)? 0 :this.fieldsChangedCount.hashCode()));
        result = ((result* 31)+((this.changeType == null)? 0 :this.changeType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityChangeMeta) == false) {
            return false;
        }
        EntityChangeMeta rhs = ((EntityChangeMeta) other);
        return ((((((this.timeOfChange == rhs.timeOfChange)||((this.timeOfChange!= null)&&this.timeOfChange.equals(rhs.timeOfChange)))&&((this.user == rhs.user)||((this.user!= null)&&this.user.equals(rhs.user))))&&((this.transactionId == rhs.transactionId)||((this.transactionId!= null)&&this.transactionId.equals(rhs.transactionId))))&&((this.fieldsChangedCount == rhs.fieldsChangedCount)||((this.fieldsChangedCount!= null)&&this.fieldsChangedCount.equals(rhs.fieldsChangedCount))))&&((this.changeType == rhs.changeType)||((this.changeType!= null)&&this.changeType.equals(rhs.changeType))));
    }


    /**
     * Type of change that was made to the entity.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum ChangeType {

        CREATE("CREATE"),
        UPDATE("UPDATE"),
        DELETE("DELETE");
        private final String value;
        private final static Map<String, EntityChangeMeta.ChangeType> CONSTANTS = new HashMap<String, EntityChangeMeta.ChangeType>();

        static {
            for (EntityChangeMeta.ChangeType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ChangeType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static EntityChangeMeta.ChangeType fromValue(String value) {
            EntityChangeMeta.ChangeType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

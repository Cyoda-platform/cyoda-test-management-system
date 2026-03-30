
package org.cyoda.cloud.api.event.search;

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
 * SearchSnapshotStatus
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "snapshotId",
    "status",
    "expirationDate",
    "entitiesCount"
})
@Generated("jsonschema2pojo")
public class SearchSnapshotStatus {

    /**
     * ID of the snapshot.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    @JsonPropertyDescription("ID of the snapshot.")
    private UUID snapshotId;
    /**
     * Status of the snapshot.
     * (Required)
     * 
     */
    @JsonProperty("status")
    @JsonPropertyDescription("Status of the snapshot.")
    private SearchSnapshotStatus.Status status;
    /**
     * Expiration date of the snapshot.
     * 
     */
    @JsonProperty("expirationDate")
    @JsonPropertyDescription("Expiration date of the snapshot.")
    private Date expirationDate;
    /**
     * Number of entities collected.
     * 
     */
    @JsonProperty("entitiesCount")
    @JsonPropertyDescription("Number of entities collected.")
    private Long entitiesCount;

    /**
     * ID of the snapshot.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    public UUID getSnapshotId() {
        return snapshotId;
    }

    /**
     * ID of the snapshot.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public SearchSnapshotStatus withSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
        return this;
    }

    /**
     * Status of the snapshot.
     * (Required)
     * 
     */
    @JsonProperty("status")
    public SearchSnapshotStatus.Status getStatus() {
        return status;
    }

    /**
     * Status of the snapshot.
     * (Required)
     * 
     */
    @JsonProperty("status")
    public void setStatus(SearchSnapshotStatus.Status status) {
        this.status = status;
    }

    public SearchSnapshotStatus withStatus(SearchSnapshotStatus.Status status) {
        this.status = status;
        return this;
    }

    /**
     * Expiration date of the snapshot.
     * 
     */
    @JsonProperty("expirationDate")
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * Expiration date of the snapshot.
     * 
     */
    @JsonProperty("expirationDate")
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public SearchSnapshotStatus withExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    /**
     * Number of entities collected.
     * 
     */
    @JsonProperty("entitiesCount")
    public Long getEntitiesCount() {
        return entitiesCount;
    }

    /**
     * Number of entities collected.
     * 
     */
    @JsonProperty("entitiesCount")
    public void setEntitiesCount(Long entitiesCount) {
        this.entitiesCount = entitiesCount;
    }

    public SearchSnapshotStatus withEntitiesCount(Long entitiesCount) {
        this.entitiesCount = entitiesCount;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SearchSnapshotStatus.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("snapshotId");
        sb.append('=');
        sb.append(((this.snapshotId == null)?"<null>":this.snapshotId));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
        sb.append(',');
        sb.append("expirationDate");
        sb.append('=');
        sb.append(((this.expirationDate == null)?"<null>":this.expirationDate));
        sb.append(',');
        sb.append("entitiesCount");
        sb.append('=');
        sb.append(((this.entitiesCount == null)?"<null>":this.entitiesCount));
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
        result = ((result* 31)+((this.snapshotId == null)? 0 :this.snapshotId.hashCode()));
        result = ((result* 31)+((this.entitiesCount == null)? 0 :this.entitiesCount.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        result = ((result* 31)+((this.expirationDate == null)? 0 :this.expirationDate.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SearchSnapshotStatus) == false) {
            return false;
        }
        SearchSnapshotStatus rhs = ((SearchSnapshotStatus) other);
        return (((((this.snapshotId == rhs.snapshotId)||((this.snapshotId!= null)&&this.snapshotId.equals(rhs.snapshotId)))&&((this.entitiesCount == rhs.entitiesCount)||((this.entitiesCount!= null)&&this.entitiesCount.equals(rhs.entitiesCount))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))))&&((this.expirationDate == rhs.expirationDate)||((this.expirationDate!= null)&&this.expirationDate.equals(rhs.expirationDate))));
    }


    /**
     * Status of the snapshot.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Status {

        RUNNING("RUNNING"),
        FAILED("FAILED"),
        CANCELLED("CANCELLED"),
        SUCCESSFUL("SUCCESSFUL"),
        NOT_FOUND("NOT_FOUND");
        private final String value;
        private final static Map<String, SearchSnapshotStatus.Status> CONSTANTS = new HashMap<String, SearchSnapshotStatus.Status>();

        static {
            for (SearchSnapshotStatus.Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Status(String value) {
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
        public static SearchSnapshotStatus.Status fromValue(String value) {
            SearchSnapshotStatus.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

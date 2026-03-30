
package org.cyoda.cloud.api.event.search;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * SnapshotGetRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "snapshotId",
    "pageSize",
    "pageNumber",
    "clientPointTime"
})
@Generated("jsonschema2pojo")
public class SnapshotGetRequest
    extends BaseEvent
{

    /**
     * ID of the snapshot to retrieve data.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    @JsonPropertyDescription("ID of the snapshot to retrieve data.")
    private UUID snapshotId;
    /**
     * Page size.
     * 
     */
    @JsonProperty("pageSize")
    @JsonPropertyDescription("Page size.")
    private Integer pageSize = 10;
    /**
     * Page number (from 0).
     * 
     */
    @JsonProperty("pageNumber")
    @JsonPropertyDescription("Page number (from 0).")
    private Integer pageNumber = 0;
    /**
     * Page of time to retrieve the results.
     * 
     */
    @JsonProperty("clientPointTime")
    @JsonPropertyDescription("Page of time to retrieve the results.")
    private Date clientPointTime;

    /**
     * ID of the snapshot to retrieve data.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    public UUID getSnapshotId() {
        return snapshotId;
    }

    /**
     * ID of the snapshot to retrieve data.
     * (Required)
     * 
     */
    @JsonProperty("snapshotId")
    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public SnapshotGetRequest withSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
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

    public SnapshotGetRequest withPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Page number (from 0).
     * 
     */
    @JsonProperty("pageNumber")
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Page number (from 0).
     * 
     */
    @JsonProperty("pageNumber")
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public SnapshotGetRequest withPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    /**
     * Page of time to retrieve the results.
     * 
     */
    @JsonProperty("clientPointTime")
    public Date getClientPointTime() {
        return clientPointTime;
    }

    /**
     * Page of time to retrieve the results.
     * 
     */
    @JsonProperty("clientPointTime")
    public void setClientPointTime(Date clientPointTime) {
        this.clientPointTime = clientPointTime;
    }

    public SnapshotGetRequest withClientPointTime(Date clientPointTime) {
        this.clientPointTime = clientPointTime;
        return this;
    }

    @Override
    public SnapshotGetRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public SnapshotGetRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public SnapshotGetRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public SnapshotGetRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SnapshotGetRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("snapshotId");
        sb.append('=');
        sb.append(((this.snapshotId == null)?"<null>":this.snapshotId));
        sb.append(',');
        sb.append("pageSize");
        sb.append('=');
        sb.append(((this.pageSize == null)?"<null>":this.pageSize));
        sb.append(',');
        sb.append("pageNumber");
        sb.append('=');
        sb.append(((this.pageNumber == null)?"<null>":this.pageNumber));
        sb.append(',');
        sb.append("clientPointTime");
        sb.append('=');
        sb.append(((this.clientPointTime == null)?"<null>":this.clientPointTime));
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
        result = ((result* 31)+((this.pageSize == null)? 0 :this.pageSize.hashCode()));
        result = ((result* 31)+((this.snapshotId == null)? 0 :this.snapshotId.hashCode()));
        result = ((result* 31)+((this.pageNumber == null)? 0 :this.pageNumber.hashCode()));
        result = ((result* 31)+((this.clientPointTime == null)? 0 :this.clientPointTime.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SnapshotGetRequest) == false) {
            return false;
        }
        SnapshotGetRequest rhs = ((SnapshotGetRequest) other);
        return ((((super.equals(rhs)&&((this.pageSize == rhs.pageSize)||((this.pageSize!= null)&&this.pageSize.equals(rhs.pageSize))))&&((this.snapshotId == rhs.snapshotId)||((this.snapshotId!= null)&&this.snapshotId.equals(rhs.snapshotId))))&&((this.pageNumber == rhs.pageNumber)||((this.pageNumber!= null)&&this.pageNumber.equals(rhs.pageNumber))))&&((this.clientPointTime == rhs.clientPointTime)||((this.clientPointTime!= null)&&this.clientPointTime.equals(rhs.clientPointTime))));
    }

}

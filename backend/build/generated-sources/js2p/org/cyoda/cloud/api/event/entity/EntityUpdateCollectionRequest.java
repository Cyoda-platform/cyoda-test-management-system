
package org.cyoda.cloud.api.event.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.DataFormat;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityUpdateCollectionRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "dataFormat",
    "transactionWindow",
    "transactionTimeoutMs",
    "payloads"
})
@Generated("jsonschema2pojo")
public class EntityUpdateCollectionRequest
    extends BaseEvent
{

    /**
     * DataFormat
     * <p>
     * Specifies the format of the input data (e.g., JSON).
     * (Required)
     * 
     */
    @JsonProperty("dataFormat")
    @JsonPropertyDescription("Specifies the format of the input data (e.g., JSON).")
    private DataFormat dataFormat;
    /**
     * The collection will be saved in a single transaction up to a maximum number of entities given by the transactionWindow. Collections exceeding the transactionWindow size will be saved in separate chunked transactions of the transactionWindow size.
     * 
     */
    @JsonProperty("transactionWindow")
    @JsonPropertyDescription("The collection will be saved in a single transaction up to a maximum number of entities given by the transactionWindow. Collections exceeding the transactionWindow size will be saved in separate chunked transactions of the transactionWindow size.")
    private Integer transactionWindow;
    /**
     * Indicates the timeout of transaction for transactional save.
     * 
     */
    @JsonProperty("transactionTimeoutMs")
    @JsonPropertyDescription("Indicates the timeout of transaction for transactional save.")
    private Long transactionTimeoutMs;
    /**
     * Data payloads containing entities to update.
     * (Required)
     * 
     */
    @JsonProperty("payloads")
    @JsonPropertyDescription("Data payloads containing entities to update.")
    private List<EntityUpdatePayload> payloads = new ArrayList<EntityUpdatePayload>();

    /**
     * DataFormat
     * <p>
     * Specifies the format of the input data (e.g., JSON).
     * (Required)
     * 
     */
    @JsonProperty("dataFormat")
    public DataFormat getDataFormat() {
        return dataFormat;
    }

    /**
     * DataFormat
     * <p>
     * Specifies the format of the input data (e.g., JSON).
     * (Required)
     * 
     */
    @JsonProperty("dataFormat")
    public void setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    public EntityUpdateCollectionRequest withDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
        return this;
    }

    /**
     * The collection will be saved in a single transaction up to a maximum number of entities given by the transactionWindow. Collections exceeding the transactionWindow size will be saved in separate chunked transactions of the transactionWindow size.
     * 
     */
    @JsonProperty("transactionWindow")
    public Integer getTransactionWindow() {
        return transactionWindow;
    }

    /**
     * The collection will be saved in a single transaction up to a maximum number of entities given by the transactionWindow. Collections exceeding the transactionWindow size will be saved in separate chunked transactions of the transactionWindow size.
     * 
     */
    @JsonProperty("transactionWindow")
    public void setTransactionWindow(Integer transactionWindow) {
        this.transactionWindow = transactionWindow;
    }

    public EntityUpdateCollectionRequest withTransactionWindow(Integer transactionWindow) {
        this.transactionWindow = transactionWindow;
        return this;
    }

    /**
     * Indicates the timeout of transaction for transactional save.
     * 
     */
    @JsonProperty("transactionTimeoutMs")
    public Long getTransactionTimeoutMs() {
        return transactionTimeoutMs;
    }

    /**
     * Indicates the timeout of transaction for transactional save.
     * 
     */
    @JsonProperty("transactionTimeoutMs")
    public void setTransactionTimeoutMs(Long transactionTimeoutMs) {
        this.transactionTimeoutMs = transactionTimeoutMs;
    }

    public EntityUpdateCollectionRequest withTransactionTimeoutMs(Long transactionTimeoutMs) {
        this.transactionTimeoutMs = transactionTimeoutMs;
        return this;
    }

    /**
     * Data payloads containing entities to update.
     * (Required)
     * 
     */
    @JsonProperty("payloads")
    public List<EntityUpdatePayload> getPayloads() {
        return payloads;
    }

    /**
     * Data payloads containing entities to update.
     * (Required)
     * 
     */
    @JsonProperty("payloads")
    public void setPayloads(List<EntityUpdatePayload> payloads) {
        this.payloads = payloads;
    }

    public EntityUpdateCollectionRequest withPayloads(List<EntityUpdatePayload> payloads) {
        this.payloads = payloads;
        return this;
    }

    @Override
    public EntityUpdateCollectionRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityUpdateCollectionRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityUpdateCollectionRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityUpdateCollectionRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityUpdateCollectionRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("dataFormat");
        sb.append('=');
        sb.append(((this.dataFormat == null)?"<null>":this.dataFormat));
        sb.append(',');
        sb.append("transactionWindow");
        sb.append('=');
        sb.append(((this.transactionWindow == null)?"<null>":this.transactionWindow));
        sb.append(',');
        sb.append("transactionTimeoutMs");
        sb.append('=');
        sb.append(((this.transactionTimeoutMs == null)?"<null>":this.transactionTimeoutMs));
        sb.append(',');
        sb.append("payloads");
        sb.append('=');
        sb.append(((this.payloads == null)?"<null>":this.payloads));
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
        result = ((result* 31)+((this.transactionWindow == null)? 0 :this.transactionWindow.hashCode()));
        result = ((result* 31)+((this.payloads == null)? 0 :this.payloads.hashCode()));
        result = ((result* 31)+((this.dataFormat == null)? 0 :this.dataFormat.hashCode()));
        result = ((result* 31)+((this.transactionTimeoutMs == null)? 0 :this.transactionTimeoutMs.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityUpdateCollectionRequest) == false) {
            return false;
        }
        EntityUpdateCollectionRequest rhs = ((EntityUpdateCollectionRequest) other);
        return ((((super.equals(rhs)&&((this.transactionWindow == rhs.transactionWindow)||((this.transactionWindow!= null)&&this.transactionWindow.equals(rhs.transactionWindow))))&&((this.payloads == rhs.payloads)||((this.payloads!= null)&&this.payloads.equals(rhs.payloads))))&&((this.dataFormat == rhs.dataFormat)||((this.dataFormat!= null)&&this.dataFormat.equals(rhs.dataFormat))))&&((this.transactionTimeoutMs == rhs.transactionTimeoutMs)||((this.transactionTimeoutMs!= null)&&this.transactionTimeoutMs.equals(rhs.transactionTimeoutMs))));
    }

}

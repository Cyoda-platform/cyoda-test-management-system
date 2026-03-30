
package org.cyoda.cloud.api.event.entity;

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
 * EntityCreateRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "payload",
    "dataFormat",
    "transactionTimeoutMs"
})
@Generated("jsonschema2pojo")
public class EntityCreateRequest
    extends BaseEvent
{

    /**
     * EntityCreatePayload
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("payload")
    private EntityCreatePayload payload;
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
     * Indicates the timeout of transaction for transactional save.
     * 
     */
    @JsonProperty("transactionTimeoutMs")
    @JsonPropertyDescription("Indicates the timeout of transaction for transactional save.")
    private Long transactionTimeoutMs;

    /**
     * EntityCreatePayload
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("payload")
    public EntityCreatePayload getPayload() {
        return payload;
    }

    /**
     * EntityCreatePayload
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("payload")
    public void setPayload(EntityCreatePayload payload) {
        this.payload = payload;
    }

    public EntityCreateRequest withPayload(EntityCreatePayload payload) {
        this.payload = payload;
        return this;
    }

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

    public EntityCreateRequest withDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
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

    public EntityCreateRequest withTransactionTimeoutMs(Long transactionTimeoutMs) {
        this.transactionTimeoutMs = transactionTimeoutMs;
        return this;
    }

    @Override
    public EntityCreateRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityCreateRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityCreateRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityCreateRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityCreateRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("payload");
        sb.append('=');
        sb.append(((this.payload == null)?"<null>":this.payload));
        sb.append(',');
        sb.append("dataFormat");
        sb.append('=');
        sb.append(((this.dataFormat == null)?"<null>":this.dataFormat));
        sb.append(',');
        sb.append("transactionTimeoutMs");
        sb.append('=');
        sb.append(((this.transactionTimeoutMs == null)?"<null>":this.transactionTimeoutMs));
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
        result = ((result* 31)+((this.payload == null)? 0 :this.payload.hashCode()));
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
        if ((other instanceof EntityCreateRequest) == false) {
            return false;
        }
        EntityCreateRequest rhs = ((EntityCreateRequest) other);
        return (((super.equals(rhs)&&((this.payload == rhs.payload)||((this.payload!= null)&&this.payload.equals(rhs.payload))))&&((this.dataFormat == rhs.dataFormat)||((this.dataFormat!= null)&&this.dataFormat.equals(rhs.dataFormat))))&&((this.transactionTimeoutMs == rhs.transactionTimeoutMs)||((this.transactionTimeoutMs!= null)&&this.transactionTimeoutMs.equals(rhs.transactionTimeoutMs))));
    }

}

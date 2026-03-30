
package org.cyoda.cloud.api.event.entity;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;


/**
 * EntityTransactionResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestId",
    "transactionInfo"
})
@Generated("jsonschema2pojo")
public class EntityTransactionResponse
    extends BaseEvent
{

    /**
     * ID of the original request to save data.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    @JsonPropertyDescription("ID of the original request to save data.")
    private String requestId;
    /**
     * EntityTransactionInfo
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("transactionInfo")
    private EntityTransactionInfo transactionInfo;

    /**
     * ID of the original request to save data.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public String getRequestId() {
        return requestId;
    }

    /**
     * ID of the original request to save data.
     * (Required)
     * 
     */
    @JsonProperty("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public EntityTransactionResponse withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * EntityTransactionInfo
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("transactionInfo")
    public EntityTransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    /**
     * EntityTransactionInfo
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("transactionInfo")
    public void setTransactionInfo(EntityTransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
    }

    public EntityTransactionResponse withTransactionInfo(EntityTransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
        return this;
    }

    @Override
    public EntityTransactionResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityTransactionResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityTransactionResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityTransactionResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityTransactionResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("requestId");
        sb.append('=');
        sb.append(((this.requestId == null)?"<null>":this.requestId));
        sb.append(',');
        sb.append("transactionInfo");
        sb.append('=');
        sb.append(((this.transactionInfo == null)?"<null>":this.transactionInfo));
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
        result = ((result* 31)+((this.requestId == null)? 0 :this.requestId.hashCode()));
        result = ((result* 31)+((this.transactionInfo == null)? 0 :this.transactionInfo.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityTransactionResponse) == false) {
            return false;
        }
        EntityTransactionResponse rhs = ((EntityTransactionResponse) other);
        return ((super.equals(rhs)&&((this.requestId == rhs.requestId)||((this.requestId!= null)&&this.requestId.equals(rhs.requestId))))&&((this.transactionInfo == rhs.transactionInfo)||((this.transactionInfo!= null)&&this.transactionInfo.equals(rhs.transactionInfo))));
    }

}

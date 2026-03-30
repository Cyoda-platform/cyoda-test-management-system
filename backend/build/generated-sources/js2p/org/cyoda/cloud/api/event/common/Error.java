
package org.cyoda.cloud.api.event.common;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Error details (if present).
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "code",
    "message",
    "retryable"
})
@Generated("jsonschema2pojo")
public class Error {

    /**
     * Error code.
     * (Required)
     * 
     */
    @JsonProperty("code")
    @JsonPropertyDescription("Error code.")
    private String code;
    /**
     * Error message.
     * (Required)
     * 
     */
    @JsonProperty("message")
    @JsonPropertyDescription("Error message.")
    private String message;
    /**
     * Flag indicates whether this error is retryable (for example, whether cyoda should retry the calculation request).
     * 
     */
    @JsonProperty("retryable")
    @JsonPropertyDescription("Flag indicates whether this error is retryable (for example, whether cyoda should retry the calculation request).")
    private Boolean retryable;

    /**
     * Error code.
     * (Required)
     * 
     */
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    /**
     * Error code.
     * (Required)
     * 
     */
    @JsonProperty("code")
    public void setCode(String code) {
        this.code = code;
    }

    public Error withCode(String code) {
        this.code = code;
        return this;
    }

    /**
     * Error message.
     * (Required)
     * 
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * Error message.
     * (Required)
     * 
     */
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    public Error withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Flag indicates whether this error is retryable (for example, whether cyoda should retry the calculation request).
     * 
     */
    @JsonProperty("retryable")
    public Boolean getRetryable() {
        return retryable;
    }

    /**
     * Flag indicates whether this error is retryable (for example, whether cyoda should retry the calculation request).
     * 
     */
    @JsonProperty("retryable")
    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public Error withRetryable(Boolean retryable) {
        this.retryable = retryable;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Error.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("code");
        sb.append('=');
        sb.append(((this.code == null)?"<null>":this.code));
        sb.append(',');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null)?"<null>":this.message));
        sb.append(',');
        sb.append("retryable");
        sb.append('=');
        sb.append(((this.retryable == null)?"<null>":this.retryable));
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
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.retryable == null)? 0 :this.retryable.hashCode()));
        result = ((result* 31)+((this.code == null)? 0 :this.code.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Error) == false) {
            return false;
        }
        Error rhs = ((Error) other);
        return ((((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message)))&&((this.retryable == rhs.retryable)||((this.retryable!= null)&&this.retryable.equals(rhs.retryable))))&&((this.code == rhs.code)||((this.code!= null)&&this.code.equals(rhs.code))));
    }

}

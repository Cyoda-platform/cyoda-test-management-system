
package org.cyoda.cloud.api.event.common;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * BaseEvent
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "success",
    "id",
    "error",
    "warnings"
})
@Generated("jsonschema2pojo")
public class BaseEvent {

    /**
     * Flag indicates whether this message relates to some failure.
     * 
     */
    @JsonProperty("success")
    @JsonPropertyDescription("Flag indicates whether this message relates to some failure.")
    private Boolean success = true;
    /**
     * Event ID.
     * (Required)
     * 
     */
    @JsonProperty("id")
    @JsonPropertyDescription("Event ID.")
    private String id;
    /**
     * Error details (if present).
     * 
     */
    @JsonProperty("error")
    @JsonPropertyDescription("Error details (if present).")
    private Error error;
    /**
     * Warnings (if applicable).
     * 
     */
    @JsonProperty("warnings")
    @JsonPropertyDescription("Warnings (if applicable).")
    private List<String> warnings = new ArrayList<String>();

    /**
     * Flag indicates whether this message relates to some failure.
     * 
     */
    @JsonProperty("success")
    public Boolean getSuccess() {
        return success;
    }

    /**
     * Flag indicates whether this message relates to some failure.
     * 
     */
    @JsonProperty("success")
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public BaseEvent withSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    /**
     * Event ID.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * Event ID.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public BaseEvent withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Error details (if present).
     * 
     */
    @JsonProperty("error")
    public Error getError() {
        return error;
    }

    /**
     * Error details (if present).
     * 
     */
    @JsonProperty("error")
    public void setError(Error error) {
        this.error = error;
    }

    public BaseEvent withError(Error error) {
        this.error = error;
        return this;
    }

    /**
     * Warnings (if applicable).
     * 
     */
    @JsonProperty("warnings")
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Warnings (if applicable).
     * 
     */
    @JsonProperty("warnings")
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public BaseEvent withWarnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BaseEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("success");
        sb.append('=');
        sb.append(((this.success == null)?"<null>":this.success));
        sb.append(',');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("error");
        sb.append('=');
        sb.append(((this.error == null)?"<null>":this.error));
        sb.append(',');
        sb.append("warnings");
        sb.append('=');
        sb.append(((this.warnings == null)?"<null>":this.warnings));
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
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.error == null)? 0 :this.error.hashCode()));
        result = ((result* 31)+((this.success == null)? 0 :this.success.hashCode()));
        result = ((result* 31)+((this.warnings == null)? 0 :this.warnings.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BaseEvent) == false) {
            return false;
        }
        BaseEvent rhs = ((BaseEvent) other);
        return (((((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id)))&&((this.error == rhs.error)||((this.error!= null)&&this.error.equals(rhs.error))))&&((this.success == rhs.success)||((this.success!= null)&&this.success.equals(rhs.success))))&&((this.warnings == rhs.warnings)||((this.warnings!= null)&&this.warnings.equals(rhs.warnings))));
    }

}


package org.cyoda.cloud.api.event.common;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * DataPayload
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "data",
    "meta"
})
@Generated("jsonschema2pojo")
public class DataPayload {

    /**
     * Payload type.
     * (Required)
     * 
     */
    @JsonProperty("type")
    @JsonPropertyDescription("Payload type.")
    private String type;
    /**
     * Payload data.
     * 
     */
    @JsonProperty("data")
    @JsonPropertyDescription("Payload data.")
    private JsonNode data;
    /**
     * Metadata for the payload.
     * 
     */
    @JsonProperty("meta")
    @JsonPropertyDescription("Metadata for the payload.")
    private JsonNode meta;

    /**
     * Payload type.
     * (Required)
     * 
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     * Payload type.
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    public DataPayload withType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Payload data.
     * 
     */
    @JsonProperty("data")
    public JsonNode getData() {
        return data;
    }

    /**
     * Payload data.
     * 
     */
    @JsonProperty("data")
    public void setData(JsonNode data) {
        this.data = data;
    }

    public DataPayload withData(JsonNode data) {
        this.data = data;
        return this;
    }

    /**
     * Metadata for the payload.
     * 
     */
    @JsonProperty("meta")
    public JsonNode getMeta() {
        return meta;
    }

    /**
     * Metadata for the payload.
     * 
     */
    @JsonProperty("meta")
    public void setMeta(JsonNode meta) {
        this.meta = meta;
    }

    public DataPayload withMeta(JsonNode meta) {
        this.meta = meta;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DataPayload.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("data");
        sb.append('=');
        sb.append(((this.data == null)?"<null>":this.data));
        sb.append(',');
        sb.append("meta");
        sb.append('=');
        sb.append(((this.meta == null)?"<null>":this.meta));
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
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.data == null)? 0 :this.data.hashCode()));
        result = ((result* 31)+((this.meta == null)? 0 :this.meta.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DataPayload) == false) {
            return false;
        }
        DataPayload rhs = ((DataPayload) other);
        return ((((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type)))&&((this.data == rhs.data)||((this.data!= null)&&this.data.equals(rhs.data))))&&((this.meta == rhs.meta)||((this.meta!= null)&&this.meta.equals(rhs.meta))));
    }

}

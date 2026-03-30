
package org.cyoda.cloud.api.event.entity;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import org.cyoda.cloud.api.event.common.ModelSpec;


/**
 * EntityCreatePayload
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "model",
    "data"
})
@Generated("jsonschema2pojo")
public class EntityCreatePayload {

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    private ModelSpec model;
    /**
     * Payload data.
     * (Required)
     * 
     */
    @JsonProperty("data")
    @JsonPropertyDescription("Payload data.")
    private JsonNode data;

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    public ModelSpec getModel() {
        return model;
    }

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    public void setModel(ModelSpec model) {
        this.model = model;
    }

    public EntityCreatePayload withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    /**
     * Payload data.
     * (Required)
     * 
     */
    @JsonProperty("data")
    public JsonNode getData() {
        return data;
    }

    /**
     * Payload data.
     * (Required)
     * 
     */
    @JsonProperty("data")
    public void setData(JsonNode data) {
        this.data = data;
    }

    public EntityCreatePayload withData(JsonNode data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityCreatePayload.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("model");
        sb.append('=');
        sb.append(((this.model == null)?"<null>":this.model));
        sb.append(',');
        sb.append("data");
        sb.append('=');
        sb.append(((this.data == null)?"<null>":this.data));
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
        result = ((result* 31)+((this.data == null)? 0 :this.data.hashCode()));
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityCreatePayload) == false) {
            return false;
        }
        EntityCreatePayload rhs = ((EntityCreatePayload) other);
        return (((this.data == rhs.data)||((this.data!= null)&&this.data.equals(rhs.data)))&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))));
    }

}

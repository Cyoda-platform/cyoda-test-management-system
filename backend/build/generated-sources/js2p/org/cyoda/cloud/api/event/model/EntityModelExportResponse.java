
package org.cyoda.cloud.api.event.model;

import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.ModelSpec;


/**
 * EntityModelExportResponse
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "modelId",
    "model",
    "payload"
})
@Generated("jsonschema2pojo")
public class EntityModelExportResponse
    extends BaseEvent
{

    /**
     * ID of the entity model.
     * 
     */
    @JsonProperty("modelId")
    @JsonPropertyDescription("ID of the entity model.")
    private UUID modelId;
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
     * The content format of the exported entity model depends on the selected converter.
     * (Required)
     * 
     */
    @JsonProperty("payload")
    @JsonPropertyDescription("The content format of the exported entity model depends on the selected converter.")
    private JsonNode payload;

    /**
     * ID of the entity model.
     * 
     */
    @JsonProperty("modelId")
    public UUID getModelId() {
        return modelId;
    }

    /**
     * ID of the entity model.
     * 
     */
    @JsonProperty("modelId")
    public void setModelId(UUID modelId) {
        this.modelId = modelId;
    }

    public EntityModelExportResponse withModelId(UUID modelId) {
        this.modelId = modelId;
        return this;
    }

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

    public EntityModelExportResponse withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    /**
     * The content format of the exported entity model depends on the selected converter.
     * (Required)
     * 
     */
    @JsonProperty("payload")
    public JsonNode getPayload() {
        return payload;
    }

    /**
     * The content format of the exported entity model depends on the selected converter.
     * (Required)
     * 
     */
    @JsonProperty("payload")
    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public EntityModelExportResponse withPayload(JsonNode payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public EntityModelExportResponse withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelExportResponse withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelExportResponse withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelExportResponse withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelExportResponse.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("modelId");
        sb.append('=');
        sb.append(((this.modelId == null)?"<null>":this.modelId));
        sb.append(',');
        sb.append("model");
        sb.append('=');
        sb.append(((this.model == null)?"<null>":this.model));
        sb.append(',');
        sb.append("payload");
        sb.append('=');
        sb.append(((this.payload == null)?"<null>":this.payload));
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
        result = ((result* 31)+((this.modelId == null)? 0 :this.modelId.hashCode()));
        result = ((result* 31)+((this.payload == null)? 0 :this.payload.hashCode()));
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelExportResponse) == false) {
            return false;
        }
        EntityModelExportResponse rhs = ((EntityModelExportResponse) other);
        return (((super.equals(rhs)&&((this.modelId == rhs.modelId)||((this.modelId!= null)&&this.modelId.equals(rhs.modelId))))&&((this.payload == rhs.payload)||((this.payload!= null)&&this.payload.equals(rhs.payload))))&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))));
    }

}

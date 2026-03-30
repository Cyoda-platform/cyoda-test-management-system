
package org.cyoda.cloud.api.event.model;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.DataFormat;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.ModelConverterType;
import org.cyoda.cloud.api.event.common.ModelSpec;


/**
 * EntityModelImportRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "model",
    "converter",
    "dataFormat",
    "payload"
})
@Generated("jsonschema2pojo")
public class EntityModelImportRequest
    extends BaseEvent
{

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
     * ModelConverterType
     * <p>
     * Defines the type of converter to use when importing the model (e.g., SAMPLE_DATA to use sample object)
     * (Required)
     * 
     */
    @JsonProperty("converter")
    @JsonPropertyDescription("Defines the type of converter to use when importing the model (e.g., SAMPLE_DATA to use sample object)")
    private ModelConverterType converter;
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
     * The data to be used for importing the model.
     * (Required)
     * 
     */
    @JsonProperty("payload")
    @JsonPropertyDescription("The data to be used for importing the model.")
    private JsonNode payload;

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

    public EntityModelImportRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    /**
     * ModelConverterType
     * <p>
     * Defines the type of converter to use when importing the model (e.g., SAMPLE_DATA to use sample object)
     * (Required)
     * 
     */
    @JsonProperty("converter")
    public ModelConverterType getConverter() {
        return converter;
    }

    /**
     * ModelConverterType
     * <p>
     * Defines the type of converter to use when importing the model (e.g., SAMPLE_DATA to use sample object)
     * (Required)
     * 
     */
    @JsonProperty("converter")
    public void setConverter(ModelConverterType converter) {
        this.converter = converter;
    }

    public EntityModelImportRequest withConverter(ModelConverterType converter) {
        this.converter = converter;
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

    public EntityModelImportRequest withDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
        return this;
    }

    /**
     * The data to be used for importing the model.
     * (Required)
     * 
     */
    @JsonProperty("payload")
    public JsonNode getPayload() {
        return payload;
    }

    /**
     * The data to be used for importing the model.
     * (Required)
     * 
     */
    @JsonProperty("payload")
    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public EntityModelImportRequest withPayload(JsonNode payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public EntityModelImportRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelImportRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelImportRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelImportRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelImportRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("model");
        sb.append('=');
        sb.append(((this.model == null)?"<null>":this.model));
        sb.append(',');
        sb.append("converter");
        sb.append('=');
        sb.append(((this.converter == null)?"<null>":this.converter));
        sb.append(',');
        sb.append("dataFormat");
        sb.append('=');
        sb.append(((this.dataFormat == null)?"<null>":this.dataFormat));
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
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+((this.payload == null)? 0 :this.payload.hashCode()));
        result = ((result* 31)+((this.dataFormat == null)? 0 :this.dataFormat.hashCode()));
        result = ((result* 31)+((this.converter == null)? 0 :this.converter.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelImportRequest) == false) {
            return false;
        }
        EntityModelImportRequest rhs = ((EntityModelImportRequest) other);
        return ((((super.equals(rhs)&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))))&&((this.payload == rhs.payload)||((this.payload!= null)&&this.payload.equals(rhs.payload))))&&((this.dataFormat == rhs.dataFormat)||((this.dataFormat!= null)&&this.dataFormat.equals(rhs.dataFormat))))&&((this.converter == rhs.converter)||((this.converter!= null)&&this.converter.equals(rhs.converter))));
    }

}

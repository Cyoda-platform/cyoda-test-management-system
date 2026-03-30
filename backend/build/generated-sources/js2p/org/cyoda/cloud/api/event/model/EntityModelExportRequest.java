
package org.cyoda.cloud.api.event.model;

import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.ModelConverterType;
import org.cyoda.cloud.api.event.common.ModelSpec;


/**
 * EntityModelExportRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "converter",
    "model"
})
@Generated("jsonschema2pojo")
public class EntityModelExportRequest
    extends BaseEvent
{

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

    public EntityModelExportRequest withConverter(ModelConverterType converter) {
        this.converter = converter;
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

    public EntityModelExportRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    @Override
    public EntityModelExportRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelExportRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelExportRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelExportRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelExportRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("converter");
        sb.append('=');
        sb.append(((this.converter == null)?"<null>":this.converter));
        sb.append(',');
        sb.append("model");
        sb.append('=');
        sb.append(((this.model == null)?"<null>":this.model));
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
        result = ((result* 31)+((this.converter == null)? 0 :this.converter.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelExportRequest) == false) {
            return false;
        }
        EntityModelExportRequest rhs = ((EntityModelExportRequest) other);
        return ((super.equals(rhs)&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))))&&((this.converter == rhs.converter)||((this.converter!= null)&&this.converter.equals(rhs.converter))));
    }

}

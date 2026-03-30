
package org.cyoda.cloud.api.event.common;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * ModelConverterType
 * <p>
 * Defines the type of converter to use when importing the model (e.g., SAMPLE_DATA to use sample object)
 * 
 */
@Generated("jsonschema2pojo")
public enum ModelConverterType {

    SAMPLE_DATA("SAMPLE_DATA"),
    SIMPLE_VIEW("SIMPLE_VIEW");
    private final String value;
    private final static Map<String, ModelConverterType> CONSTANTS = new HashMap<String, ModelConverterType>();

    static {
        for (ModelConverterType c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    ModelConverterType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static ModelConverterType fromValue(String value) {
        ModelConverterType constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}

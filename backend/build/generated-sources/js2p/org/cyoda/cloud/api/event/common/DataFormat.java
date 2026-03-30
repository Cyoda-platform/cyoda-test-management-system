
package org.cyoda.cloud.api.event.common;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * DataFormat
 * <p>
 * Specifies the format of the input data (e.g., JSON).
 * 
 */
@Generated("jsonschema2pojo")
public enum DataFormat {

    JSON("JSON"),
    XML("XML");
    private final String value;
    private final static Map<String, DataFormat> CONSTANTS = new HashMap<String, DataFormat>();

    static {
        for (DataFormat c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    DataFormat(String value) {
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
    public static DataFormat fromValue(String value) {
        DataFormat constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}

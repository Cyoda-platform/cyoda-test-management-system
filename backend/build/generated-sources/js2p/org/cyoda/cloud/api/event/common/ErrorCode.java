
package org.cyoda.cloud.api.event.common;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * ErrorCode
 * <p>
 * 
 * 
 */
@Generated("jsonschema2pojo")
public enum ErrorCode {

    SERVER_ERROR("SERVER_ERROR"),
    CLIENT_ERROR("CLIENT_ERROR");
    private final String value;
    private final static Map<String, ErrorCode> CONSTANTS = new HashMap<String, ErrorCode>();

    static {
        for (ErrorCode c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    ErrorCode(String value) {
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
    public static ErrorCode fromValue(String value) {
        ErrorCode constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}

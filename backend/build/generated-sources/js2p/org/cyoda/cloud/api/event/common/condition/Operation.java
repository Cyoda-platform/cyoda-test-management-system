
package org.cyoda.cloud.api.event.common.condition;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Operation
 * <p>
 * Specifies operation to use within condition.
 * 
 */
@Generated("jsonschema2pojo")
public enum Operation {

    EQUALS("EQUALS"),
    NOT_EQUAL("NOT_EQUAL"),
    IEQUALS("IEQUALS"),
    INOT_EQUAL("INOT_EQUAL"),
    IS_NULL("IS_NULL"),
    NOT_NULL("NOT_NULL"),
    GREATER_THAN("GREATER_THAN"),
    GREATER_OR_EQUAL("GREATER_OR_EQUAL"),
    LESS_THAN("LESS_THAN"),
    LESS_OR_EQUAL("LESS_OR_EQUAL"),
    CONTAINS("CONTAINS"),
    NOT_CONTAINS("NOT_CONTAINS"),
    STARTS_WITH("STARTS_WITH"),
    NOT_STARTS_WITH("NOT_STARTS_WITH"),
    ENDS_WITH("ENDS_WITH"),
    NOT_ENDS_WITH("NOT_ENDS_WITH"),
    ICONTAINS("ICONTAINS"),
    ISTARTS_WITH("ISTARTS_WITH"),
    IENDS_WITH("IENDS_WITH"),
    INOT_CONTAINS("INOT_CONTAINS"),
    INOT_STARTS_WITH("INOT_STARTS_WITH"),
    INOT_ENDS_WITH("INOT_ENDS_WITH"),
    MATCHES_PATTERN("MATCHES_PATTERN"),
    LIKE("LIKE"),
    BETWEEN("BETWEEN"),
    BETWEEN_INCLUSIVE("BETWEEN_INCLUSIVE"),
    IS_UNCHANGED("IS_UNCHANGED"),
    IS_CHANGED("IS_CHANGED");
    private final String value;
    private final static Map<String, Operation> CONSTANTS = new HashMap<String, Operation>();

    static {
        for (Operation c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    Operation(String value) {
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
    public static Operation fromValue(String value) {
        Operation constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}

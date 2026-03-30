
package org.cyoda.cloud.api.event.common.condition;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * LifecycleCondition
 * <p>
 * Condition with a type 'lifecycle'
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "field",
    "operation",
    "value"
})
@Generated("jsonschema2pojo")
public class LifecycleCondition
    extends QueryCondition
{

    @JsonProperty("type")
    private String type = "lifecycle";
    /**
     * The lifecycle field
     * (Required)
     * 
     */
    @JsonProperty("field")
    @JsonPropertyDescription("The lifecycle field")
    private String field;
    /**
     * Operation
     * <p>
     * Specifies operation to use within condition.
     * (Required)
     * 
     */
    @JsonProperty("operation")
    @JsonPropertyDescription("Specifies operation to use within condition.")
    private Operation operation;
    /**
     * Value to use within condition.
     * (Required)
     * 
     */
    @JsonProperty("value")
    @JsonPropertyDescription("Value to use within condition.")
    private JsonNode value;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    public LifecycleCondition withType(String type) {
        this.type = type;
        return this;
    }

    /**
     * The lifecycle field
     * (Required)
     * 
     */
    @JsonProperty("field")
    public String getField() {
        return field;
    }

    /**
     * The lifecycle field
     * (Required)
     * 
     */
    @JsonProperty("field")
    public void setField(String field) {
        this.field = field;
    }

    public LifecycleCondition withField(String field) {
        this.field = field;
        return this;
    }

    /**
     * Operation
     * <p>
     * Specifies operation to use within condition.
     * (Required)
     * 
     */
    @JsonProperty("operation")
    public Operation getOperation() {
        return operation;
    }

    /**
     * Operation
     * <p>
     * Specifies operation to use within condition.
     * (Required)
     * 
     */
    @JsonProperty("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public LifecycleCondition withOperation(Operation operation) {
        this.operation = operation;
        return this;
    }

    /**
     * Value to use within condition.
     * (Required)
     * 
     */
    @JsonProperty("value")
    public JsonNode getValue() {
        return value;
    }

    /**
     * Value to use within condition.
     * (Required)
     * 
     */
    @JsonProperty("value")
    public void setValue(JsonNode value) {
        this.value = value;
    }

    public LifecycleCondition withValue(JsonNode value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(LifecycleCondition.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("field");
        sb.append('=');
        sb.append(((this.field == null)?"<null>":this.field));
        sb.append(',');
        sb.append("operation");
        sb.append('=');
        sb.append(((this.operation == null)?"<null>":this.operation));
        sb.append(',');
        sb.append("value");
        sb.append('=');
        sb.append(((this.value == null)?"<null>":this.value));
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
        result = ((result* 31)+((this.field == null)? 0 :this.field.hashCode()));
        result = ((result* 31)+((this.operation == null)? 0 :this.operation.hashCode()));
        result = ((result* 31)+((this.value == null)? 0 :this.value.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof LifecycleCondition) == false) {
            return false;
        }
        LifecycleCondition rhs = ((LifecycleCondition) other);
        return ((((super.equals(rhs)&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.field == rhs.field)||((this.field!= null)&&this.field.equals(rhs.field))))&&((this.operation == rhs.operation)||((this.operation!= null)&&this.operation.equals(rhs.operation))))&&((this.value == rhs.value)||((this.value!= null)&&this.value.equals(rhs.value))));
    }

}

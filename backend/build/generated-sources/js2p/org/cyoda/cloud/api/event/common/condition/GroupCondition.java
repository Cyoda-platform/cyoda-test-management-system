
package org.cyoda.cloud.api.event.common.condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * GroupCondition
 * <p>
 * Condition with a type 'group'
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "conditions",
    "operator"
})
@Generated("jsonschema2pojo")
public class GroupCondition
    extends QueryCondition
{

    @JsonProperty("type")
    private String type = "group";
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("conditions")
    private List<QueryCondition> conditions = new ArrayList<QueryCondition>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("operator")
    private GroupCondition.Operator operator;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    public GroupCondition withType(String type) {
        this.type = type;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("conditions")
    public List<QueryCondition> getConditions() {
        return conditions;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("conditions")
    public void setConditions(List<QueryCondition> conditions) {
        this.conditions = conditions;
    }

    public GroupCondition withConditions(List<QueryCondition> conditions) {
        this.conditions = conditions;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("operator")
    public GroupCondition.Operator getOperator() {
        return operator;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("operator")
    public void setOperator(GroupCondition.Operator operator) {
        this.operator = operator;
    }

    public GroupCondition withOperator(GroupCondition.Operator operator) {
        this.operator = operator;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GroupCondition.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("conditions");
        sb.append('=');
        sb.append(((this.conditions == null)?"<null>":this.conditions));
        sb.append(',');
        sb.append("operator");
        sb.append('=');
        sb.append(((this.operator == null)?"<null>":this.operator));
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
        result = ((result* 31)+((this.conditions == null)? 0 :this.conditions.hashCode()));
        result = ((result* 31)+((this.operator == null)? 0 :this.operator.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof GroupCondition) == false) {
            return false;
        }
        GroupCondition rhs = ((GroupCondition) other);
        return (((super.equals(rhs)&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.conditions == rhs.conditions)||((this.conditions!= null)&&this.conditions.equals(rhs.conditions))))&&((this.operator == rhs.operator)||((this.operator!= null)&&this.operator.equals(rhs.operator))));
    }

    @Generated("jsonschema2pojo")
    public enum Operator {

        AND("AND"),
        OR("OR"),
        NOT("NOT");
        private final String value;
        private final static Map<String, GroupCondition.Operator> CONSTANTS = new HashMap<String, GroupCondition.Operator>();

        static {
            for (GroupCondition.Operator c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Operator(String value) {
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
        public static GroupCondition.Operator fromValue(String value) {
            GroupCondition.Operator constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

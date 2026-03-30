
package org.cyoda.cloud.api.event.common.statemachine.conf;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import org.cyoda.cloud.api.event.common.ExternalizedFunctionConfig;


/**
 * ExternalizedProcessorDefinition
 * <p>
 * Definition of a externalized processor for state transition logic
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "executionMode",
    "config"
})
@Generated("jsonschema2pojo")
public class ExternalizedProcessorDefinition
    extends ProcessorDefinition
{

    /**
     * Execution mode of the processor
     * 
     */
    @JsonProperty("executionMode")
    @JsonPropertyDescription("Execution mode of the processor")
    private ExternalizedProcessorDefinition.ExecutionMode executionMode = ExternalizedProcessorDefinition.ExecutionMode.fromValue("ASYNC_NEW_TX");
    /**
     * ExternalizedFunctionConfig
     * <p>
     * Common configuration parameters for externalized functions and processors
     * 
     */
    @JsonProperty("config")
    @JsonPropertyDescription("Common configuration parameters for externalized functions and processors")
    private ExternalizedFunctionConfig config;

    /**
     * Execution mode of the processor
     * 
     */
    @JsonProperty("executionMode")
    public ExternalizedProcessorDefinition.ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /**
     * Execution mode of the processor
     * 
     */
    @JsonProperty("executionMode")
    public void setExecutionMode(ExternalizedProcessorDefinition.ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public ExternalizedProcessorDefinition withExecutionMode(ExternalizedProcessorDefinition.ExecutionMode executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    /**
     * ExternalizedFunctionConfig
     * <p>
     * Common configuration parameters for externalized functions and processors
     * 
     */
    @JsonProperty("config")
    public ExternalizedFunctionConfig getConfig() {
        return config;
    }

    /**
     * ExternalizedFunctionConfig
     * <p>
     * Common configuration parameters for externalized functions and processors
     * 
     */
    @JsonProperty("config")
    public void setConfig(ExternalizedFunctionConfig config) {
        this.config = config;
    }

    public ExternalizedProcessorDefinition withConfig(ExternalizedFunctionConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ExternalizedProcessorDefinition.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("executionMode");
        sb.append('=');
        sb.append(((this.executionMode == null)?"<null>":this.executionMode));
        sb.append(',');
        sb.append("config");
        sb.append('=');
        sb.append(((this.config == null)?"<null>":this.config));
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
        result = ((result* 31)+((this.executionMode == null)? 0 :this.executionMode.hashCode()));
        result = ((result* 31)+((this.config == null)? 0 :this.config.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ExternalizedProcessorDefinition) == false) {
            return false;
        }
        ExternalizedProcessorDefinition rhs = ((ExternalizedProcessorDefinition) other);
        return ((super.equals(rhs)&&((this.executionMode == rhs.executionMode)||((this.executionMode!= null)&&this.executionMode.equals(rhs.executionMode))))&&((this.config == rhs.config)||((this.config!= null)&&this.config.equals(rhs.config))));
    }


    /**
     * Execution mode of the processor
     * 
     */
    @Generated("jsonschema2pojo")
    public enum ExecutionMode {

        SYNC("SYNC"),
        ASYNC_SAME_TX("ASYNC_SAME_TX"),
        ASYNC_NEW_TX("ASYNC_NEW_TX");
        private final String value;
        private final static Map<String, ExternalizedProcessorDefinition.ExecutionMode> CONSTANTS = new HashMap<String, ExternalizedProcessorDefinition.ExecutionMode>();

        static {
            for (ExternalizedProcessorDefinition.ExecutionMode c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ExecutionMode(String value) {
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
        public static ExternalizedProcessorDefinition.ExecutionMode fromValue(String value) {
            ExternalizedProcessorDefinition.ExecutionMode constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

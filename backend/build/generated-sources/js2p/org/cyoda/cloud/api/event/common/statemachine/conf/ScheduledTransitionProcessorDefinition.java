
package org.cyoda.cloud.api.event.common.statemachine.conf;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.ScheduledTransitionConfig;


/**
 * ScheduledTransitionProcessorDefinition
 * <p>
 * Definition of a scheduled processor for state transition logic
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "config"
})
@Generated("jsonschema2pojo")
public class ScheduledTransitionProcessorDefinition
    extends ProcessorDefinition
{

    @JsonProperty("type")
    private String type = "scheduled";
    /**
     * ScheduledTransitionConfig
     * <p>
     * Configuration parameters for scheduled transition
     * (Required)
     * 
     */
    @JsonProperty("config")
    @JsonPropertyDescription("Configuration parameters for scheduled transition")
    private ScheduledTransitionConfig config;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    public ScheduledTransitionProcessorDefinition withType(String type) {
        this.type = type;
        return this;
    }

    /**
     * ScheduledTransitionConfig
     * <p>
     * Configuration parameters for scheduled transition
     * (Required)
     * 
     */
    @JsonProperty("config")
    public ScheduledTransitionConfig getConfig() {
        return config;
    }

    /**
     * ScheduledTransitionConfig
     * <p>
     * Configuration parameters for scheduled transition
     * (Required)
     * 
     */
    @JsonProperty("config")
    public void setConfig(ScheduledTransitionConfig config) {
        this.config = config;
    }

    public ScheduledTransitionProcessorDefinition withConfig(ScheduledTransitionConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ScheduledTransitionProcessorDefinition.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.config == null)? 0 :this.config.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ScheduledTransitionProcessorDefinition) == false) {
            return false;
        }
        ScheduledTransitionProcessorDefinition rhs = ((ScheduledTransitionProcessorDefinition) other);
        return ((super.equals(rhs)&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.config == rhs.config)||((this.config!= null)&&this.config.equals(rhs.config))));
    }

}

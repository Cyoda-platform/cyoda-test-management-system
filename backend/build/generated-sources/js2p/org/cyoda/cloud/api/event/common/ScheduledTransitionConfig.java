
package org.cyoda.cloud.api.event.common;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * ScheduledTransitionConfig
 * <p>
 * Configuration parameters for scheduled transition
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "delayMs",
    "timeoutMs",
    "transition"
})
@Generated("jsonschema2pojo")
public class ScheduledTransitionConfig {

    /**
     * Delay in milliseconds before executing the transition
     * (Required)
     * 
     */
    @JsonProperty("delayMs")
    @JsonPropertyDescription("Delay in milliseconds before executing the transition")
    private Long delayMs;
    /**
     * Timeout in milliseconds for executing the transition task, after which it will be expired
     * 
     */
    @JsonProperty("timeoutMs")
    @JsonPropertyDescription("Timeout in milliseconds for executing the transition task, after which it will be expired")
    private Long timeoutMs;
    /**
     * The transition to execute after waiting delayMs
     * (Required)
     * 
     */
    @JsonProperty("transition")
    @JsonPropertyDescription("The transition to execute after waiting delayMs")
    private String transition;

    /**
     * Delay in milliseconds before executing the transition
     * (Required)
     * 
     */
    @JsonProperty("delayMs")
    public Long getDelayMs() {
        return delayMs;
    }

    /**
     * Delay in milliseconds before executing the transition
     * (Required)
     * 
     */
    @JsonProperty("delayMs")
    public void setDelayMs(Long delayMs) {
        this.delayMs = delayMs;
    }

    public ScheduledTransitionConfig withDelayMs(Long delayMs) {
        this.delayMs = delayMs;
        return this;
    }

    /**
     * Timeout in milliseconds for executing the transition task, after which it will be expired
     * 
     */
    @JsonProperty("timeoutMs")
    public Long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Timeout in milliseconds for executing the transition task, after which it will be expired
     * 
     */
    @JsonProperty("timeoutMs")
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public ScheduledTransitionConfig withTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /**
     * The transition to execute after waiting delayMs
     * (Required)
     * 
     */
    @JsonProperty("transition")
    public String getTransition() {
        return transition;
    }

    /**
     * The transition to execute after waiting delayMs
     * (Required)
     * 
     */
    @JsonProperty("transition")
    public void setTransition(String transition) {
        this.transition = transition;
    }

    public ScheduledTransitionConfig withTransition(String transition) {
        this.transition = transition;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ScheduledTransitionConfig.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("delayMs");
        sb.append('=');
        sb.append(((this.delayMs == null)?"<null>":this.delayMs));
        sb.append(',');
        sb.append("timeoutMs");
        sb.append('=');
        sb.append(((this.timeoutMs == null)?"<null>":this.timeoutMs));
        sb.append(',');
        sb.append("transition");
        sb.append('=');
        sb.append(((this.transition == null)?"<null>":this.transition));
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
        result = ((result* 31)+((this.delayMs == null)? 0 :this.delayMs.hashCode()));
        result = ((result* 31)+((this.timeoutMs == null)? 0 :this.timeoutMs.hashCode()));
        result = ((result* 31)+((this.transition == null)? 0 :this.transition.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ScheduledTransitionConfig) == false) {
            return false;
        }
        ScheduledTransitionConfig rhs = ((ScheduledTransitionConfig) other);
        return ((((this.delayMs == rhs.delayMs)||((this.delayMs!= null)&&this.delayMs.equals(rhs.delayMs)))&&((this.timeoutMs == rhs.timeoutMs)||((this.timeoutMs!= null)&&this.timeoutMs.equals(rhs.timeoutMs))))&&((this.transition == rhs.transition)||((this.transition!= null)&&this.transition.equals(rhs.transition))));
    }

}

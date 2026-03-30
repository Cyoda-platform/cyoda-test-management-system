
package org.cyoda.cloud.api.event.common;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * ExternalizedFunctionConfig
 * <p>
 * Common configuration parameters for externalized functions and processors
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "attachEntity",
    "calculationNodesTags",
    "responseTimeoutMs",
    "retryPolicy",
    "context"
})
@Generated("jsonschema2pojo")
public class ExternalizedFunctionConfig {

    /**
     * Whether to attach entity data to the function call
     * 
     */
    @JsonProperty("attachEntity")
    @JsonPropertyDescription("Whether to attach entity data to the function call")
    private Boolean attachEntity;
    /**
     * Comma-separated list of calculation node tags
     * 
     */
    @JsonProperty("calculationNodesTags")
    @JsonPropertyDescription("Comma-separated list of calculation node tags")
    private String calculationNodesTags;
    /**
     * Response timeout in milliseconds
     * 
     */
    @JsonProperty("responseTimeoutMs")
    @JsonPropertyDescription("Response timeout in milliseconds")
    private Long responseTimeoutMs;
    /**
     * Retry policy for the function
     * 
     */
    @JsonProperty("retryPolicy")
    @JsonPropertyDescription("Retry policy for the function")
    private String retryPolicy;
    /**
     * Additional context for the function
     * 
     */
    @JsonProperty("context")
    @JsonPropertyDescription("Additional context for the function")
    private String context;

    /**
     * Whether to attach entity data to the function call
     * 
     */
    @JsonProperty("attachEntity")
    public Boolean getAttachEntity() {
        return attachEntity;
    }

    /**
     * Whether to attach entity data to the function call
     * 
     */
    @JsonProperty("attachEntity")
    public void setAttachEntity(Boolean attachEntity) {
        this.attachEntity = attachEntity;
    }

    public ExternalizedFunctionConfig withAttachEntity(Boolean attachEntity) {
        this.attachEntity = attachEntity;
        return this;
    }

    /**
     * Comma-separated list of calculation node tags
     * 
     */
    @JsonProperty("calculationNodesTags")
    public String getCalculationNodesTags() {
        return calculationNodesTags;
    }

    /**
     * Comma-separated list of calculation node tags
     * 
     */
    @JsonProperty("calculationNodesTags")
    public void setCalculationNodesTags(String calculationNodesTags) {
        this.calculationNodesTags = calculationNodesTags;
    }

    public ExternalizedFunctionConfig withCalculationNodesTags(String calculationNodesTags) {
        this.calculationNodesTags = calculationNodesTags;
        return this;
    }

    /**
     * Response timeout in milliseconds
     * 
     */
    @JsonProperty("responseTimeoutMs")
    public Long getResponseTimeoutMs() {
        return responseTimeoutMs;
    }

    /**
     * Response timeout in milliseconds
     * 
     */
    @JsonProperty("responseTimeoutMs")
    public void setResponseTimeoutMs(Long responseTimeoutMs) {
        this.responseTimeoutMs = responseTimeoutMs;
    }

    public ExternalizedFunctionConfig withResponseTimeoutMs(Long responseTimeoutMs) {
        this.responseTimeoutMs = responseTimeoutMs;
        return this;
    }

    /**
     * Retry policy for the function
     * 
     */
    @JsonProperty("retryPolicy")
    public String getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * Retry policy for the function
     * 
     */
    @JsonProperty("retryPolicy")
    public void setRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public ExternalizedFunctionConfig withRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * Additional context for the function
     * 
     */
    @JsonProperty("context")
    public String getContext() {
        return context;
    }

    /**
     * Additional context for the function
     * 
     */
    @JsonProperty("context")
    public void setContext(String context) {
        this.context = context;
    }

    public ExternalizedFunctionConfig withContext(String context) {
        this.context = context;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ExternalizedFunctionConfig.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("attachEntity");
        sb.append('=');
        sb.append(((this.attachEntity == null)?"<null>":this.attachEntity));
        sb.append(',');
        sb.append("calculationNodesTags");
        sb.append('=');
        sb.append(((this.calculationNodesTags == null)?"<null>":this.calculationNodesTags));
        sb.append(',');
        sb.append("responseTimeoutMs");
        sb.append('=');
        sb.append(((this.responseTimeoutMs == null)?"<null>":this.responseTimeoutMs));
        sb.append(',');
        sb.append("retryPolicy");
        sb.append('=');
        sb.append(((this.retryPolicy == null)?"<null>":this.retryPolicy));
        sb.append(',');
        sb.append("context");
        sb.append('=');
        sb.append(((this.context == null)?"<null>":this.context));
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
        result = ((result* 31)+((this.context == null)? 0 :this.context.hashCode()));
        result = ((result* 31)+((this.calculationNodesTags == null)? 0 :this.calculationNodesTags.hashCode()));
        result = ((result* 31)+((this.retryPolicy == null)? 0 :this.retryPolicy.hashCode()));
        result = ((result* 31)+((this.attachEntity == null)? 0 :this.attachEntity.hashCode()));
        result = ((result* 31)+((this.responseTimeoutMs == null)? 0 :this.responseTimeoutMs.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ExternalizedFunctionConfig) == false) {
            return false;
        }
        ExternalizedFunctionConfig rhs = ((ExternalizedFunctionConfig) other);
        return ((((((this.context == rhs.context)||((this.context!= null)&&this.context.equals(rhs.context)))&&((this.calculationNodesTags == rhs.calculationNodesTags)||((this.calculationNodesTags!= null)&&this.calculationNodesTags.equals(rhs.calculationNodesTags))))&&((this.retryPolicy == rhs.retryPolicy)||((this.retryPolicy!= null)&&this.retryPolicy.equals(rhs.retryPolicy))))&&((this.attachEntity == rhs.attachEntity)||((this.attachEntity!= null)&&this.attachEntity.equals(rhs.attachEntity))))&&((this.responseTimeoutMs == rhs.responseTimeoutMs)||((this.responseTimeoutMs!= null)&&this.responseTimeoutMs.equals(rhs.responseTimeoutMs))));
    }

}

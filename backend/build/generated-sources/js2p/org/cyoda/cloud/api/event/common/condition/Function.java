
package org.cyoda.cloud.api.event.common.condition;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.ExternalizedFunctionConfig;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "config",
    "criterion"
})
@Generated("jsonschema2pojo")
public class Function {

    /**
     * Name of the function that returns boolean
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of the function that returns boolean")
    private String name;
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
     * QueryCondition
     * <p>
     * 
     * 
     */
    @JsonProperty("criterion")
    private QueryCondition criterion;

    /**
     * Name of the function that returns boolean
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of the function that returns boolean
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public Function withName(String name) {
        this.name = name;
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

    public Function withConfig(ExternalizedFunctionConfig config) {
        this.config = config;
        return this;
    }

    /**
     * QueryCondition
     * <p>
     * 
     * 
     */
    @JsonProperty("criterion")
    public QueryCondition getCriterion() {
        return criterion;
    }

    /**
     * QueryCondition
     * <p>
     * 
     * 
     */
    @JsonProperty("criterion")
    public void setCriterion(QueryCondition criterion) {
        this.criterion = criterion;
    }

    public Function withCriterion(QueryCondition criterion) {
        this.criterion = criterion;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Function.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("config");
        sb.append('=');
        sb.append(((this.config == null)?"<null>":this.config));
        sb.append(',');
        sb.append("criterion");
        sb.append('=');
        sb.append(((this.criterion == null)?"<null>":this.criterion));
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
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.config == null)? 0 :this.config.hashCode()));
        result = ((result* 31)+((this.criterion == null)? 0 :this.criterion.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Function) == false) {
            return false;
        }
        Function rhs = ((Function) other);
        return ((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.config == rhs.config)||((this.config!= null)&&this.config.equals(rhs.config))))&&((this.criterion == rhs.criterion)||((this.criterion!= null)&&this.criterion.equals(rhs.criterion))));
    }

}

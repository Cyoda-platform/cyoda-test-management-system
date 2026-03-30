
package org.cyoda.cloud.api.event.common.statemachine.conf;

import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cyoda.cloud.api.event.common.condition.QueryCondition;


/**
 * WorkflowConfiguration
 * <p>
 * Workflow configuration schema version 1.0. Supports workflow-level and transition-level criterion using QueryCondition types: 'group', 'simple', and 'function'. Function conditions enable externalized integration for external processing and validation logic.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "version",
    "name",
    "desc",
    "initialState",
    "active",
    "criterion",
    "states"
})
@Generated("jsonschema2pojo")
public class WorkflowConfiguration {

    /**
     * Version of the workflow configuration schema
     * (Required)
     * 
     */
    @JsonProperty("version")
    @JsonPropertyDescription("Version of the workflow configuration schema")
    private String version = "1.0";
    /**
     * Name of the workflow
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of the workflow")
    private String name;
    /**
     * Description of the workflow
     * 
     */
    @JsonProperty("desc")
    @JsonPropertyDescription("Description of the workflow")
    private String desc;
    /**
     * Initial state for entities in this workflow
     * (Required)
     * 
     */
    @JsonProperty("initialState")
    @JsonPropertyDescription("Initial state for entities in this workflow")
    private String initialState;
    /**
     * Flag indicating if the workflow is active
     * 
     */
    @JsonProperty("active")
    @JsonPropertyDescription("Flag indicating if the workflow is active")
    private Boolean active;
    /**
     * QueryCondition
     * <p>
     * 
     * 
     */
    @JsonProperty("criterion")
    private QueryCondition criterion;
    /**
     * Map of state codes to state definitions. State names must start with a letter and contain only letters, numbers, underscores, and hyphens.
     * (Required)
     * 
     */
    @JsonProperty("states")
    @JsonPropertyDescription("Map of state codes to state definitions. State names must start with a letter and contain only letters, numbers, underscores, and hyphens.")
    private Map<String, StateDefinition> states;

    /**
     * Version of the workflow configuration schema
     * (Required)
     * 
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     * Version of the workflow configuration schema
     * (Required)
     * 
     */
    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    public WorkflowConfiguration withVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * Name of the workflow
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of the workflow
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public WorkflowConfiguration withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Description of the workflow
     * 
     */
    @JsonProperty("desc")
    public String getDesc() {
        return desc;
    }

    /**
     * Description of the workflow
     * 
     */
    @JsonProperty("desc")
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public WorkflowConfiguration withDesc(String desc) {
        this.desc = desc;
        return this;
    }

    /**
     * Initial state for entities in this workflow
     * (Required)
     * 
     */
    @JsonProperty("initialState")
    public String getInitialState() {
        return initialState;
    }

    /**
     * Initial state for entities in this workflow
     * (Required)
     * 
     */
    @JsonProperty("initialState")
    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }

    public WorkflowConfiguration withInitialState(String initialState) {
        this.initialState = initialState;
        return this;
    }

    /**
     * Flag indicating if the workflow is active
     * 
     */
    @JsonProperty("active")
    public Boolean getActive() {
        return active;
    }

    /**
     * Flag indicating if the workflow is active
     * 
     */
    @JsonProperty("active")
    public void setActive(Boolean active) {
        this.active = active;
    }

    public WorkflowConfiguration withActive(Boolean active) {
        this.active = active;
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

    public WorkflowConfiguration withCriterion(QueryCondition criterion) {
        this.criterion = criterion;
        return this;
    }

    /**
     * Map of state codes to state definitions. State names must start with a letter and contain only letters, numbers, underscores, and hyphens.
     * (Required)
     * 
     */
    @JsonProperty("states")
    public Map<String, StateDefinition> getStates() {
        return states;
    }

    /**
     * Map of state codes to state definitions. State names must start with a letter and contain only letters, numbers, underscores, and hyphens.
     * (Required)
     * 
     */
    @JsonProperty("states")
    public void setStates(Map<String, StateDefinition> states) {
        this.states = states;
    }

    public WorkflowConfiguration withStates(Map<String, StateDefinition> states) {
        this.states = states;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(WorkflowConfiguration.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("version");
        sb.append('=');
        sb.append(((this.version == null)?"<null>":this.version));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("desc");
        sb.append('=');
        sb.append(((this.desc == null)?"<null>":this.desc));
        sb.append(',');
        sb.append("initialState");
        sb.append('=');
        sb.append(((this.initialState == null)?"<null>":this.initialState));
        sb.append(',');
        sb.append("active");
        sb.append('=');
        sb.append(((this.active == null)?"<null>":this.active));
        sb.append(',');
        sb.append("criterion");
        sb.append('=');
        sb.append(((this.criterion == null)?"<null>":this.criterion));
        sb.append(',');
        sb.append("states");
        sb.append('=');
        sb.append(((this.states == null)?"<null>":this.states));
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
        result = ((result* 31)+((this.initialState == null)? 0 :this.initialState.hashCode()));
        result = ((result* 31)+((this.criterion == null)? 0 :this.criterion.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.active == null)? 0 :this.active.hashCode()));
        result = ((result* 31)+((this.version == null)? 0 :this.version.hashCode()));
        result = ((result* 31)+((this.desc == null)? 0 :this.desc.hashCode()));
        result = ((result* 31)+((this.states == null)? 0 :this.states.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof WorkflowConfiguration) == false) {
            return false;
        }
        WorkflowConfiguration rhs = ((WorkflowConfiguration) other);
        return ((((((((this.initialState == rhs.initialState)||((this.initialState!= null)&&this.initialState.equals(rhs.initialState)))&&((this.criterion == rhs.criterion)||((this.criterion!= null)&&this.criterion.equals(rhs.criterion))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.active == rhs.active)||((this.active!= null)&&this.active.equals(rhs.active))))&&((this.version == rhs.version)||((this.version!= null)&&this.version.equals(rhs.version))))&&((this.desc == rhs.desc)||((this.desc!= null)&&this.desc.equals(rhs.desc))))&&((this.states == rhs.states)||((this.states!= null)&&this.states.equals(rhs.states))));
    }

}

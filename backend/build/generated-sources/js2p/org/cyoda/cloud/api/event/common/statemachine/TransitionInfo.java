
package org.cyoda.cloud.api.event.common.statemachine;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * TransitionInfo
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "stateFrom",
    "stateTo"
})
@Generated("jsonschema2pojo")
public class TransitionInfo {

    /**
     * Transition ID.
     * (Required)
     * 
     */
    @JsonProperty("id")
    @JsonPropertyDescription("Transition ID.")
    private String id;
    /**
     * Transition name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Transition name.")
    private String name;
    /**
     * Source state.
     * (Required)
     * 
     */
    @JsonProperty("stateFrom")
    @JsonPropertyDescription("Source state.")
    private String stateFrom;
    /**
     * Target state.
     * (Required)
     * 
     */
    @JsonProperty("stateTo")
    @JsonPropertyDescription("Target state.")
    private String stateTo;

    /**
     * Transition ID.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * Transition ID.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public TransitionInfo withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Transition name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Transition name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public TransitionInfo withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Source state.
     * (Required)
     * 
     */
    @JsonProperty("stateFrom")
    public String getStateFrom() {
        return stateFrom;
    }

    /**
     * Source state.
     * (Required)
     * 
     */
    @JsonProperty("stateFrom")
    public void setStateFrom(String stateFrom) {
        this.stateFrom = stateFrom;
    }

    public TransitionInfo withStateFrom(String stateFrom) {
        this.stateFrom = stateFrom;
        return this;
    }

    /**
     * Target state.
     * (Required)
     * 
     */
    @JsonProperty("stateTo")
    public String getStateTo() {
        return stateTo;
    }

    /**
     * Target state.
     * (Required)
     * 
     */
    @JsonProperty("stateTo")
    public void setStateTo(String stateTo) {
        this.stateTo = stateTo;
    }

    public TransitionInfo withStateTo(String stateTo) {
        this.stateTo = stateTo;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TransitionInfo.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("stateFrom");
        sb.append('=');
        sb.append(((this.stateFrom == null)?"<null>":this.stateFrom));
        sb.append(',');
        sb.append("stateTo");
        sb.append('=');
        sb.append(((this.stateTo == null)?"<null>":this.stateTo));
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
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.stateFrom == null)? 0 :this.stateFrom.hashCode()));
        result = ((result* 31)+((this.stateTo == null)? 0 :this.stateTo.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TransitionInfo) == false) {
            return false;
        }
        TransitionInfo rhs = ((TransitionInfo) other);
        return (((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.stateFrom == rhs.stateFrom)||((this.stateFrom!= null)&&this.stateFrom.equals(rhs.stateFrom))))&&((this.stateTo == rhs.stateTo)||((this.stateTo!= null)&&this.stateTo.equals(rhs.stateTo))));
    }

}


package org.cyoda.cloud.api.event.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import org.cyoda.cloud.api.event.common.BaseEvent;
import org.cyoda.cloud.api.event.common.Error;
import org.cyoda.cloud.api.event.common.ModelSpec;


/**
 * EntityModelTransitionRequest
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "model",
    "transition"
})
@Generated("jsonschema2pojo")
public class EntityModelTransitionRequest
    extends BaseEvent
{

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    private ModelSpec model;
    /**
     * Specifies the transition to perform.
     * (Required)
     * 
     */
    @JsonProperty("transition")
    @JsonPropertyDescription("Specifies the transition to perform.")
    private EntityModelTransitionRequest.Transition transition;

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    public ModelSpec getModel() {
        return model;
    }

    /**
     * ModelSpec
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("model")
    public void setModel(ModelSpec model) {
        this.model = model;
    }

    public EntityModelTransitionRequest withModel(ModelSpec model) {
        this.model = model;
        return this;
    }

    /**
     * Specifies the transition to perform.
     * (Required)
     * 
     */
    @JsonProperty("transition")
    public EntityModelTransitionRequest.Transition getTransition() {
        return transition;
    }

    /**
     * Specifies the transition to perform.
     * (Required)
     * 
     */
    @JsonProperty("transition")
    public void setTransition(EntityModelTransitionRequest.Transition transition) {
        this.transition = transition;
    }

    public EntityModelTransitionRequest withTransition(EntityModelTransitionRequest.Transition transition) {
        this.transition = transition;
        return this;
    }

    @Override
    public EntityModelTransitionRequest withSuccess(Boolean success) {
        super.withSuccess(success);
        return this;
    }

    @Override
    public EntityModelTransitionRequest withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public EntityModelTransitionRequest withError(Error error) {
        super.withError(error);
        return this;
    }

    @Override
    public EntityModelTransitionRequest withWarnings(List<String> warnings) {
        super.withWarnings(warnings);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EntityModelTransitionRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("model");
        sb.append('=');
        sb.append(((this.model == null)?"<null>":this.model));
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
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+((this.transition == null)? 0 :this.transition.hashCode()));
        result = ((result* 31)+ super.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EntityModelTransitionRequest) == false) {
            return false;
        }
        EntityModelTransitionRequest rhs = ((EntityModelTransitionRequest) other);
        return ((super.equals(rhs)&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))))&&((this.transition == rhs.transition)||((this.transition!= null)&&this.transition.equals(rhs.transition))));
    }


    /**
     * Specifies the transition to perform.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Transition {

        LOCK("LOCK"),
        UNLOCK("UNLOCK");
        private final String value;
        private final static Map<String, EntityModelTransitionRequest.Transition> CONSTANTS = new HashMap<String, EntityModelTransitionRequest.Transition>();

        static {
            for (EntityModelTransitionRequest.Transition c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Transition(String value) {
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
        public static EntityModelTransitionRequest.Transition fromValue(String value) {
            EntityModelTransitionRequest.Transition constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

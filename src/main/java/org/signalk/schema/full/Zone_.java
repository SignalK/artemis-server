
package org.signalk.schema.full;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "upper",
    "state",
    "message",
    "lower"
})
public class Zone_ {

    @JsonProperty("upper")
    public Integer upper;
    @JsonProperty("state")
    public String state;
    @JsonProperty("message")
    public String message;
    @JsonProperty("lower")
    public Integer lower;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Zone_ withUpper(Integer upper) {
        this.upper = upper;
        return this;
    }

    public Zone_ withState(String state) {
        this.state = state;
        return this;
    }

    public Zone_ withMessage(String message) {
        this.message = message;
        return this;
    }

    public Zone_ withLower(Integer lower) {
        this.lower = lower;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Zone_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("upper", upper).append("state", state).append("message", message).append("lower", lower).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(state).append(additionalProperties).append(message).append(upper).append(lower).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Zone_) == false) {
            return false;
        }
        Zone_ rhs = ((Zone_) other);
        return new EqualsBuilder().append(state, rhs.state).append(additionalProperties, rhs.additionalProperties).append(message, rhs.message).append(upper, rhs.upper).append(lower, rhs.lower).isEquals();
    }

}

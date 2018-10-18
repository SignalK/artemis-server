
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
    "lower",
    "upper",
    "state",
    "message"
})
public class Zone__ {

    @JsonProperty("lower")
    public Double lower;
    @JsonProperty("upper")
    public Double upper;
    @JsonProperty("state")
    public String state;
    @JsonProperty("message")
    public String message;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Zone__ withLower(Double lower) {
        this.lower = lower;
        return this;
    }

    public Zone__ withUpper(Double upper) {
        this.upper = upper;
        return this;
    }

    public Zone__ withState(String state) {
        this.state = state;
        return this;
    }

    public Zone__ withMessage(String message) {
        this.message = message;
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

    public Zone__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("lower", lower).append("upper", upper).append("state", state).append("message", message).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(state).append(additionalProperties).append(message).append(lower).append(upper).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Zone__) == false) {
            return false;
        }
        Zone__ rhs = ((Zone__) other);
        return new EqualsBuilder().append(state, rhs.state).append(additionalProperties, rhs.additionalProperties).append(message, rhs.message).append(lower, rhs.lower).append(upper, rhs.upper).isEquals();
    }

}

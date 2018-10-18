
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
    "state"
})
public class Zone {

    @JsonProperty("lower")
    public Integer lower;
    @JsonProperty("upper")
    public Integer upper;
    @JsonProperty("state")
    public String state;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Zone withLower(Integer lower) {
        this.lower = lower;
        return this;
    }

    public Zone withUpper(Integer upper) {
        this.upper = upper;
        return this;
    }

    public Zone withState(String state) {
        this.state = state;
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

    public Zone withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("lower", lower).append("upper", upper).append("state", state).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(state).append(additionalProperties).append(lower).append(upper).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Zone) == false) {
            return false;
        }
        Zone rhs = ((Zone) other);
        return new EqualsBuilder().append(state, rhs.state).append(additionalProperties, rhs.additionalProperties).append(lower, rhs.lower).append(upper, rhs.upper).isEquals();
    }

}

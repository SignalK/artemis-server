
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
    "type"
})
public class DisplayScale {

    @JsonProperty("lower")
    public Integer lower;
    @JsonProperty("upper")
    public Integer upper;
    @JsonProperty("type")
    public String type;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DisplayScale withLower(Integer lower) {
        this.lower = lower;
        return this;
    }

    public DisplayScale withUpper(Integer upper) {
        this.upper = upper;
        return this;
    }

    public DisplayScale withType(String type) {
        this.type = type;
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

    public DisplayScale withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("lower", lower).append("upper", upper).append("type", type).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(type).append(lower).append(upper).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DisplayScale) == false) {
            return false;
        }
        DisplayScale rhs = ((DisplayScale) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(type, rhs.type).append(lower, rhs.lower).append(upper, rhs.upper).isEquals();
    }

}


package org.signalk.schema;

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
    "value",
    "timestamp",
    "$source"
})
public class Position {

    @JsonProperty("value")
    public Value value;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("$source")
    public String $source;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Position withValue(Value value) {
        this.value = value;
        return this;
    }

    public Position withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Position with$source(String $source) {
        this.$source = $source;
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

    public Position withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("value", value).append("timestamp", timestamp).append("$source", $source).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(value).append(timestamp).append($source).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Position) == false) {
            return false;
        }
        Position rhs = ((Position) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(value, rhs.value).append(timestamp, rhs.timestamp).append($source, rhs.$source).isEquals();
    }

}

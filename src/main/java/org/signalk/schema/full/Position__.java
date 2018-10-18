
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
    "timestamp",
    "value",
    "$source",
    "_attr"
})
public class Position__ {

    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("value")
    public Value__ value;
    @JsonProperty("$source")
    public String $source;
    @JsonProperty("_attr")
    public Attr attr;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Position__ withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Position__ withValue(Value__ value) {
        this.value = value;
        return this;
    }

    public Position__ with$source(String $source) {
        this.$source = $source;
        return this;
    }

    public Position__ withAttr(Attr attr) {
        this.attr = attr;
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

    public Position__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("timestamp", timestamp).append("value", value).append("$source", $source).append("attr", attr).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(attr).append(value).append(timestamp).append($source).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Position__) == false) {
            return false;
        }
        Position__ rhs = ((Position__) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(attr, rhs.attr).append(value, rhs.value).append(timestamp, rhs.timestamp).append($source, rhs.$source).isEquals();
    }

}

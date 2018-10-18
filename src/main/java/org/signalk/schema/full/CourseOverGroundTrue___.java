
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
    "value",
    "$source",
    "timestamp",
    "values"
})
public class CourseOverGroundTrue___ {

    @JsonProperty("value")
    public Double value;
    @JsonProperty("$source")
    public String $source;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("values")
    public Values values;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public CourseOverGroundTrue___ withValue(Double value) {
        this.value = value;
        return this;
    }

    public CourseOverGroundTrue___ with$source(String $source) {
        this.$source = $source;
        return this;
    }

    public CourseOverGroundTrue___ withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public CourseOverGroundTrue___ withValues(Values values) {
        this.values = values;
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

    public CourseOverGroundTrue___ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("value", value).append("$source", $source).append("timestamp", timestamp).append("values", values).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(value).append($source).append(timestamp).append(values).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CourseOverGroundTrue___) == false) {
            return false;
        }
        CourseOverGroundTrue___ rhs = ((CourseOverGroundTrue___) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(value, rhs.value).append($source, rhs.$source).append(timestamp, rhs.timestamp).append(values, rhs.values).isEquals();
    }

}

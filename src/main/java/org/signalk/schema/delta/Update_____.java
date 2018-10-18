
package org.signalk.schema.delta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    "source",
    "timestamp",
    "values"
})
public class Update_____ {

    @JsonProperty("source")
    public Source___ source;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("values")
    public List<Value_______> values = new ArrayList<Value_______>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Update_____ withSource(Source___ source) {
        this.source = source;
        return this;
    }

    public Update_____ withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Update_____ withValues(List<Value_______> values) {
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

    public Update_____ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("source", source).append("timestamp", timestamp).append("values", values).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(source).append(additionalProperties).append(timestamp).append(values).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Update_____) == false) {
            return false;
        }
        Update_____ rhs = ((Update_____) other);
        return new EqualsBuilder().append(source, rhs.source).append(additionalProperties, rhs.additionalProperties).append(timestamp, rhs.timestamp).append(values, rhs.values).isEquals();
    }

}

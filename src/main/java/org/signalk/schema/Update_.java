
package org.signalk.schema;

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
    "values",
    "timestamp",
    "$source"
})
public class Update_ {

    @JsonProperty("values")
    public List<Value__> values = new ArrayList<Value__>();
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("$source")
    public String $source;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Update_ withValues(List<Value__> values) {
        this.values = values;
        return this;
    }

    public Update_ withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Update_ with$source(String $source) {
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

    public Update_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("values", values).append("timestamp", timestamp).append("$source", $source).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(values).append(timestamp).append($source).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Update_) == false) {
            return false;
        }
        Update_ rhs = ((Update_) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(values, rhs.values).append(timestamp, rhs.timestamp).append($source, rhs.$source).isEquals();
    }

}

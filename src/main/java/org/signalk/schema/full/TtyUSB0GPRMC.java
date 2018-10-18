
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
    "timestamp"
})
public class TtyUSB0GPRMC {

    @JsonProperty("value")
    public Double value;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public TtyUSB0GPRMC withValue(Double value) {
        this.value = value;
        return this;
    }

    public TtyUSB0GPRMC withTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    public TtyUSB0GPRMC withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("value", value).append("timestamp", timestamp).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(value).append(timestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TtyUSB0GPRMC) == false) {
            return false;
        }
        TtyUSB0GPRMC rhs = ((TtyUSB0GPRMC) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(value, rhs.value).append(timestamp, rhs.timestamp).isEquals();
    }

}

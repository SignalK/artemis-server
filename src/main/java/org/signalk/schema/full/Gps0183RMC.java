
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
    "src",
    "bus"
})
public class Gps0183RMC {

    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("src")
    public String src;
    @JsonProperty("bus")
    public String bus;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Gps0183RMC withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Gps0183RMC withSrc(String src) {
        this.src = src;
        return this;
    }

    public Gps0183RMC withBus(String bus) {
        this.bus = bus;
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

    public Gps0183RMC withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("timestamp", timestamp).append("src", src).append("bus", bus).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(bus).append(additionalProperties).append(src).append(timestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Gps0183RMC) == false) {
            return false;
        }
        Gps0183RMC rhs = ((Gps0183RMC) other);
        return new EqualsBuilder().append(bus, rhs.bus).append(additionalProperties, rhs.additionalProperties).append(src, rhs.src).append(timestamp, rhs.timestamp).isEquals();
    }

}

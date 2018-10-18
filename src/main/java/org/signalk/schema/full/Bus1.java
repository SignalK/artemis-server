
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
    "name",
    "location",
    "dateInstalled",
    "phase"
})
public class Bus1 {

    @JsonProperty("name")
    public String name;
    @JsonProperty("location")
    public String location;
    @JsonProperty("dateInstalled")
    public String dateInstalled;
    @JsonProperty("phase")
    public Phase phase;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Bus1 withName(String name) {
        this.name = name;
        return this;
    }

    public Bus1 withLocation(String location) {
        this.location = location;
        return this;
    }

    public Bus1 withDateInstalled(String dateInstalled) {
        this.dateInstalled = dateInstalled;
        return this;
    }

    public Bus1 withPhase(Phase phase) {
        this.phase = phase;
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

    public Bus1 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("location", location).append("dateInstalled", dateInstalled).append("phase", phase).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(phase).append(location).append(dateInstalled).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Bus1) == false) {
            return false;
        }
        Bus1 rhs = ((Bus1) other);
        return new EqualsBuilder().append(name, rhs.name).append(phase, rhs.phase).append(location, rhs.location).append(dateInstalled, rhs.dateInstalled).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}


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
    "longitude",
    "latitude",
    "altitude"
})
public class Value__ {

    @JsonProperty("longitude")
    public Double longitude;
    @JsonProperty("latitude")
    public Double latitude;
    @JsonProperty("altitude")
    public Integer altitude;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Value__ withLongitude(Double longitude) {
        this.longitude = longitude;
        return this;
    }

    public Value__ withLatitude(Double latitude) {
        this.latitude = latitude;
        return this;
    }

    public Value__ withAltitude(Integer altitude) {
        this.altitude = altitude;
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

    public Value__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("longitude", longitude).append("latitude", latitude).append("altitude", altitude).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(altitude).append(additionalProperties).append(longitude).append(latitude).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Value__) == false) {
            return false;
        }
        Value__ rhs = ((Value__) other);
        return new EqualsBuilder().append(altitude, rhs.altitude).append(additionalProperties, rhs.additionalProperties).append(longitude, rhs.longitude).append(latitude, rhs.latitude).isEquals();
    }

}

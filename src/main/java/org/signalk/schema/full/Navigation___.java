
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
    "speedOverGround",
    "position",
    "headingMagnetic"
})
public class Navigation___ {

    @JsonProperty("speedOverGround")
    public SpeedOverGround speedOverGround;
    @JsonProperty("position")
    public Position___ position;
    @JsonProperty("headingMagnetic")
    public HeadingMagnetic headingMagnetic;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Navigation___ withSpeedOverGround(SpeedOverGround speedOverGround) {
        this.speedOverGround = speedOverGround;
        return this;
    }

    public Navigation___ withPosition(Position___ position) {
        this.position = position;
        return this;
    }

    public Navigation___ withHeadingMagnetic(HeadingMagnetic headingMagnetic) {
        this.headingMagnetic = headingMagnetic;
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

    public Navigation___ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("speedOverGround", speedOverGround).append("position", position).append("headingMagnetic", headingMagnetic).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(speedOverGround).append(position).append(additionalProperties).append(headingMagnetic).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Navigation___) == false) {
            return false;
        }
        Navigation___ rhs = ((Navigation___) other);
        return new EqualsBuilder().append(speedOverGround, rhs.speedOverGround).append(position, rhs.position).append(additionalProperties, rhs.additionalProperties).append(headingMagnetic, rhs.headingMagnetic).isEquals();
    }

}

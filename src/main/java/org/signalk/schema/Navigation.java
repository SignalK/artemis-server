
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
    "courseOverGroundTrue",
    "speedOverGround",
    "position"
})
public class Navigation {

    @JsonProperty("courseOverGroundTrue")
    public CourseOverGroundTrue courseOverGroundTrue;
    @JsonProperty("speedOverGround")
    public SpeedOverGround speedOverGround;
    @JsonProperty("position")
    public Position position;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Navigation withCourseOverGroundTrue(CourseOverGroundTrue courseOverGroundTrue) {
        this.courseOverGroundTrue = courseOverGroundTrue;
        return this;
    }

    public Navigation withSpeedOverGround(SpeedOverGround speedOverGround) {
        this.speedOverGround = speedOverGround;
        return this;
    }

    public Navigation withPosition(Position position) {
        this.position = position;
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

    public Navigation withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("courseOverGroundTrue", courseOverGroundTrue).append("speedOverGround", speedOverGround).append("position", position).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(courseOverGroundTrue).append(speedOverGround).append(position).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Navigation) == false) {
            return false;
        }
        Navigation rhs = ((Navigation) other);
        return new EqualsBuilder().append(courseOverGroundTrue, rhs.courseOverGroundTrue).append(speedOverGround, rhs.speedOverGround).append(position, rhs.position).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

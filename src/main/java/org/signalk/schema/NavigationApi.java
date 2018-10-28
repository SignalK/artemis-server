
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
public class NavigationApi {

    @JsonProperty("courseOverGroundTrue")
    public CourseOverGroundTrue_ courseOverGroundTrue;
    @JsonProperty("speedOverGround")
    public SpeedOverGround_ speedOverGround;
    @JsonProperty("position")
    public Position_ position;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public NavigationApi withCourseOverGroundTrue(CourseOverGroundTrue_ courseOverGroundTrue) {
        this.courseOverGroundTrue = courseOverGroundTrue;
        return this;
    }

    public NavigationApi withSpeedOverGround(SpeedOverGround_ speedOverGround) {
        this.speedOverGround = speedOverGround;
        return this;
    }

    public NavigationApi withPosition(Position_ position) {
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

    public NavigationApi withAdditionalProperty(String name, Object value) {
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
        if ((other instanceof NavigationApi) == false) {
            return false;
        }
        NavigationApi rhs = ((NavigationApi) other);
        return new EqualsBuilder().append(courseOverGroundTrue, rhs.courseOverGroundTrue).append(speedOverGround, rhs.speedOverGround).append(position, rhs.position).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

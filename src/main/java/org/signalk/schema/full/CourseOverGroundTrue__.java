
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
    "meta",
    "timestamp",
    "$source",
    "_attr",
    "value"
})
public class CourseOverGroundTrue__ {

    @JsonProperty("meta")
    public Meta meta;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("$source")
    public String $source;
    @JsonProperty("_attr")
    public Attr_ attr;
    @JsonProperty("value")
    public Double value;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public CourseOverGroundTrue__ withMeta(Meta meta) {
        this.meta = meta;
        return this;
    }

    public CourseOverGroundTrue__ withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public CourseOverGroundTrue__ with$source(String $source) {
        this.$source = $source;
        return this;
    }

    public CourseOverGroundTrue__ withAttr(Attr_ attr) {
        this.attr = attr;
        return this;
    }

    public CourseOverGroundTrue__ withValue(Double value) {
        this.value = value;
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

    public CourseOverGroundTrue__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("meta", meta).append("timestamp", timestamp).append("$source", $source).append("attr", attr).append("value", value).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(meta).append(additionalProperties).append(attr).append(value).append(timestamp).append($source).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CourseOverGroundTrue__) == false) {
            return false;
        }
        CourseOverGroundTrue__ rhs = ((CourseOverGroundTrue__) other);
        return new EqualsBuilder().append(meta, rhs.meta).append(additionalProperties, rhs.additionalProperties).append(attr, rhs.attr).append(value, rhs.value).append(timestamp, rhs.timestamp).append($source, rhs.$source).isEquals();
    }

}

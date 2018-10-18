
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
    "millis",
    "timestamp",
    "source",
    "timezoneOffset"
})
public class Time {

    @JsonProperty("millis")
    public Integer millis;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("source")
    public Source source;
    @JsonProperty("timezoneOffset")
    public Integer timezoneOffset;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Time withMillis(Integer millis) {
        this.millis = millis;
        return this;
    }

    public Time withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Time withSource(Source source) {
        this.source = source;
        return this;
    }

    public Time withTimezoneOffset(Integer timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
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

    public Time withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("millis", millis).append("timestamp", timestamp).append("source", source).append("timezoneOffset", timezoneOffset).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(source).append(additionalProperties).append(millis).append(timezoneOffset).append(timestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Time) == false) {
            return false;
        }
        Time rhs = ((Time) other);
        return new EqualsBuilder().append(source, rhs.source).append(additionalProperties, rhs.additionalProperties).append(millis, rhs.millis).append(timezoneOffset, rhs.timezoneOffset).append(timestamp, rhs.timestamp).isEquals();
    }

}

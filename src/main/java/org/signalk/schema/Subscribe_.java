
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
    "path",
    "period",
    "format",
    "policy",
    "minPeriod"
})
public class Subscribe_ {

    @JsonProperty("path")
    public String path;
    @JsonProperty("period")
    public Integer period;
    @JsonProperty("format")
    public String format;
    @JsonProperty("policy")
    public String policy;
    @JsonProperty("minPeriod")
    public Integer minPeriod;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Subscribe_ withPath(String path) {
        this.path = path;
        return this;
    }

    public Subscribe_ withPeriod(Integer period) {
        this.period = period;
        return this;
    }

    public Subscribe_ withFormat(String format) {
        this.format = format;
        return this;
    }

    public Subscribe_ withPolicy(String policy) {
        this.policy = policy;
        return this;
    }

    public Subscribe_ withMinPeriod(Integer minPeriod) {
        this.minPeriod = minPeriod;
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

    public Subscribe_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("path", path).append("period", period).append("format", format).append("policy", policy).append("minPeriod", minPeriod).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(path).append(period).append(minPeriod).append(format).append(additionalProperties).append(policy).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Subscribe_) == false) {
            return false;
        }
        Subscribe_ rhs = ((Subscribe_) other);
        return new EqualsBuilder().append(path, rhs.path).append(period, rhs.period).append(minPeriod, rhs.minPeriod).append(format, rhs.format).append(additionalProperties, rhs.additionalProperties).append(policy, rhs.policy).isEquals();
    }

}

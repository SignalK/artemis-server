
package org.signalk.schema.subscribe;

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
    "policy"
})
public class Subscribe__ {

    @JsonProperty("path")
    public String path;
    @JsonProperty("period")
    public Integer period;
    @JsonProperty("policy")
    public String policy;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Subscribe__ withPath(String path) {
        this.path = path;
        return this;
    }

    public Subscribe__ withPeriod(Integer period) {
        this.period = period;
        return this;
    }

    public Subscribe__ withPolicy(String policy) {
        this.policy = policy;
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

    public Subscribe__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("path", path).append("period", period).append("policy", policy).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(path).append(period).append(additionalProperties).append(policy).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Subscribe__) == false) {
            return false;
        }
        Subscribe__ rhs = ((Subscribe__) other);
        return new EqualsBuilder().append(path, rhs.path).append(period, rhs.period).append(additionalProperties, rhs.additionalProperties).append(policy, rhs.policy).isEquals();
    }

}

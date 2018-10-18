
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
    "minPeriod",
    "policy"
})
public class Subscribe___ {

    @JsonProperty("path")
    public String path;
    @JsonProperty("minPeriod")
    public Integer minPeriod;
    @JsonProperty("policy")
    public String policy;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Subscribe___ withPath(String path) {
        this.path = path;
        return this;
    }

    public Subscribe___ withMinPeriod(Integer minPeriod) {
        this.minPeriod = minPeriod;
        return this;
    }

    public Subscribe___ withPolicy(String policy) {
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

    public Subscribe___ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("path", path).append("minPeriod", minPeriod).append("policy", policy).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(path).append(additionalProperties).append(minPeriod).append(policy).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Subscribe___) == false) {
            return false;
        }
        Subscribe___ rhs = ((Subscribe___) other);
        return new EqualsBuilder().append(path, rhs.path).append(additionalProperties, rhs.additionalProperties).append(minPeriod, rhs.minPeriod).append(policy, rhs.policy).isEquals();
    }

}

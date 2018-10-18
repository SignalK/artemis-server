
package org.signalk.schema.discovery;

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
    "v1",
    "v3"
})
public class Endpoints_ {

    @JsonProperty("v1")
    public V1_ v1;
    @JsonProperty("v3")
    public V3_ v3;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Endpoints_ withV1(V1_ v1) {
        this.v1 = v1;
        return this;
    }

    public Endpoints_ withV3(V3_ v3) {
        this.v3 = v3;
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

    public Endpoints_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("v1", v1).append("v3", v3).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(v1).append(additionalProperties).append(v3).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Endpoints_) == false) {
            return false;
        }
        Endpoints_ rhs = ((Endpoints_) other);
        return new EqualsBuilder().append(v1, rhs.v1).append(additionalProperties, rhs.additionalProperties).append(v3, rhs.v3).isEquals();
    }

}

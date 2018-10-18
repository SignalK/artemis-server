
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
    "ttyUSB0",
    "ikommunicate"
})
public class Sources__ {

    @JsonProperty("ttyUSB0")
    public TtyUSB0_ ttyUSB0;
    @JsonProperty("ikommunicate")
    public Ikommunicate ikommunicate;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Sources__ withTtyUSB0(TtyUSB0_ ttyUSB0) {
        this.ttyUSB0 = ttyUSB0;
        return this;
    }

    public Sources__ withIkommunicate(Ikommunicate ikommunicate) {
        this.ikommunicate = ikommunicate;
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

    public Sources__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("ttyUSB0", ttyUSB0).append("ikommunicate", ikommunicate).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(ikommunicate).append(ttyUSB0).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Sources__) == false) {
            return false;
        }
        Sources__ rhs = ((Sources__) other);
        return new EqualsBuilder().append(ikommunicate, rhs.ikommunicate).append(ttyUSB0, rhs.ttyUSB0).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

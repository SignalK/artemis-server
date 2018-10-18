
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
    "urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d"
})
public class Vessels_____ {

    @JsonProperty("urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d")
    public UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_ urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Vessels_____ withUrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d(UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_ urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d) {
        this.urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d = urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d;
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

    public Vessels_____ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d", urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Vessels_____) == false) {
            return false;
        }
        Vessels_____ rhs = ((Vessels_____) other);
        return new EqualsBuilder().append(urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d, rhs.urnMrnSignalkUuidC0d793344e254245889254e8ccc8021d).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

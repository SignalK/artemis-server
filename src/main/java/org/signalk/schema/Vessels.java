
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
    "urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270"
})
public class Vessels {

    @JsonProperty("urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270")
    public UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270 urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Vessels withUrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270(UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270 urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270) {
        this.urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270 = urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270;
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

    public Vessels withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270", urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Vessels) == false) {
            return false;
        }
        Vessels rhs = ((Vessels) other);
        return new EqualsBuilder().append(urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270, rhs.urnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

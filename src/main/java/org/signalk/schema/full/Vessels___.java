
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
    "urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c"
})
public class Vessels___ {

    @JsonProperty("urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c")
    public UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Vessels___ withUrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c(UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c) {
        this.urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c = urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c;
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

    public Vessels___ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c", urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Vessels___) == false) {
            return false;
        }
        Vessels___ rhs = ((Vessels___) other);
        return new EqualsBuilder().append(urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c, rhs.urnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

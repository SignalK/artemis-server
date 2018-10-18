
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
    "urn:mrn:signalk:uuid:b7590868-1d62-47d9-989c-32321b349fb9"
})
public class Vessels___________ {

    @JsonProperty("urn:mrn:signalk:uuid:b7590868-1d62-47d9-989c-32321b349fb9")
    public UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__ urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Vessels___________ withUrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9(UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__ urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9) {
        this.urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9 = urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9;
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

    public Vessels___________ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9", urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Vessels___________) == false) {
            return false;
        }
        Vessels___________ rhs = ((Vessels___________) other);
        return new EqualsBuilder().append(urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9, rhs.urnMrnSignalkUuidB75908681d6247d9989c32321b349fb9).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

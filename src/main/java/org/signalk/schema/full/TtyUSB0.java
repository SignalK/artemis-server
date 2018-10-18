
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
    "label",
    "type",
    "GP",
    "II"
})
public class TtyUSB0 {

    @JsonProperty("label")
    public String label;
    @JsonProperty("type")
    public String type;
    @JsonProperty("GP")
    public GP gP;
    @JsonProperty("II")
    public II iI;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public TtyUSB0 withLabel(String label) {
        this.label = label;
        return this;
    }

    public TtyUSB0 withType(String type) {
        this.type = type;
        return this;
    }

    public TtyUSB0 withGP(GP gP) {
        this.gP = gP;
        return this;
    }

    public TtyUSB0 withII(II iI) {
        this.iI = iI;
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

    public TtyUSB0 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("label", label).append("type", type).append("gP", gP).append("iI", iI).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(iI).append(gP).append(label).append(additionalProperties).append(type).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TtyUSB0) == false) {
            return false;
        }
        TtyUSB0 rhs = ((TtyUSB0) other);
        return new EqualsBuilder().append(iI, rhs.iI).append(gP, rhs.gP).append(label, rhs.label).append(additionalProperties, rhs.additionalProperties).append(type, rhs.type).isEquals();
    }

}

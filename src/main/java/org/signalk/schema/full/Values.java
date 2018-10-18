
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
    "ttyUSB0.GP.RMC",
    "n2k.ikommunicate.128267"
})
public class Values {

    @JsonProperty("ttyUSB0.GP.RMC")
    public TtyUSB0GPRMC ttyUSB0GPRMC;
    @JsonProperty("n2k.ikommunicate.128267")
    public N2kIkommunicate128267 n2kIkommunicate128267;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Values withTtyUSB0GPRMC(TtyUSB0GPRMC ttyUSB0GPRMC) {
        this.ttyUSB0GPRMC = ttyUSB0GPRMC;
        return this;
    }

    public Values withN2kIkommunicate128267(N2kIkommunicate128267 n2kIkommunicate128267) {
        this.n2kIkommunicate128267 = n2kIkommunicate128267;
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

    public Values withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("ttyUSB0GPRMC", ttyUSB0GPRMC).append("n2kIkommunicate128267", n2kIkommunicate128267).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(ttyUSB0GPRMC).append(n2kIkommunicate128267).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Values) == false) {
            return false;
        }
        Values rhs = ((Values) other);
        return new EqualsBuilder().append(ttyUSB0GPRMC, rhs.ttyUSB0GPRMC).append(n2kIkommunicate128267, rhs.n2kIkommunicate128267).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

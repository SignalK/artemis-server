
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
    "mmsi",
    "navigation"
})
public class UrnMrnImoMmsi366982330 {

    @JsonProperty("mmsi")
    public String mmsi;
    @JsonProperty("navigation")
    public Navigation navigation;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnImoMmsi366982330 withMmsi(String mmsi) {
        this.mmsi = mmsi;
        return this;
    }

    public UrnMrnImoMmsi366982330 withNavigation(Navigation navigation) {
        this.navigation = navigation;
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

    public UrnMrnImoMmsi366982330 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("mmsi", mmsi).append("navigation", navigation).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(navigation).append(additionalProperties).append(mmsi).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnImoMmsi366982330) == false) {
            return false;
        }
        UrnMrnImoMmsi366982330 rhs = ((UrnMrnImoMmsi366982330) other);
        return new EqualsBuilder().append(navigation, rhs.navigation).append(additionalProperties, rhs.additionalProperties).append(mmsi, rhs.mmsi).isEquals();
    }

}

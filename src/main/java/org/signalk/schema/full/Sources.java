
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
    "gps_0183_RMC",
    "_attr"
})
public class Sources {

    @JsonProperty("gps_0183_RMC")
    public Gps0183RMC gps0183RMC;
    @JsonProperty("_attr")
    public Attr__ attr;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Sources withGps0183RMC(Gps0183RMC gps0183RMC) {
        this.gps0183RMC = gps0183RMC;
        return this;
    }

    public Sources withAttr(Attr__ attr) {
        this.attr = attr;
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

    public Sources withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("gps0183RMC", gps0183RMC).append("attr", attr).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(gps0183RMC).append(additionalProperties).append(attr).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Sources) == false) {
            return false;
        }
        Sources rhs = ((Sources) other);
        return new EqualsBuilder().append(gps0183RMC, rhs.gps0183RMC).append(additionalProperties, rhs.additionalProperties).append(attr, rhs.attr).isEquals();
    }

}

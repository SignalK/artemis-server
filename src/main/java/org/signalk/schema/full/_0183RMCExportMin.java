
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
    "self",
    "vessels",
    "version"
})
public class _0183RMCExportMin {

    @JsonProperty("self")
    public String self;
    @JsonProperty("vessels")
    public Vessels vessels;
    @JsonProperty("version")
    public String version;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public _0183RMCExportMin withSelf(String self) {
        this.self = self;
        return this;
    }

    public _0183RMCExportMin withVessels(Vessels vessels) {
        this.vessels = vessels;
        return this;
    }

    public _0183RMCExportMin withVersion(String version) {
        this.version = version;
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

    public _0183RMCExportMin withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("self", self).append("vessels", vessels).append("version", version).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(self).append(additionalProperties).append(version).append(vessels).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof _0183RMCExportMin) == false) {
            return false;
        }
        _0183RMCExportMin rhs = ((_0183RMCExportMin) other);
        return new EqualsBuilder().append(self, rhs.self).append(additionalProperties, rhs.additionalProperties).append(version, rhs.version).append(vessels, rhs.vessels).isEquals();
    }

}

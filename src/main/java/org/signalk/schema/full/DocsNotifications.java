
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
    "version",
    "self",
    "vessels"
})
public class DocsNotifications {

    @JsonProperty("version")
    public String version;
    @JsonProperty("self")
    public String self;
    @JsonProperty("vessels")
    public Vessels______ vessels;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DocsNotifications withVersion(String version) {
        this.version = version;
        return this;
    }

    public DocsNotifications withSelf(String self) {
        this.self = self;
        return this;
    }

    public DocsNotifications withVessels(Vessels______ vessels) {
        this.vessels = vessels;
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

    public DocsNotifications withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("version", version).append("self", self).append("vessels", vessels).append("additionalProperties", additionalProperties).toString();
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
        if ((other instanceof DocsNotifications) == false) {
            return false;
        }
        DocsNotifications rhs = ((DocsNotifications) other);
        return new EqualsBuilder().append(self, rhs.self).append(additionalProperties, rhs.additionalProperties).append(version, rhs.version).append(vessels, rhs.vessels).isEquals();
    }

}

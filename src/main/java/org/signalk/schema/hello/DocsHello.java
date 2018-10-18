
package org.signalk.schema.hello;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    "name",
    "version",
    "timestamp",
    "self",
    "roles"
})
public class DocsHello {

    @JsonProperty("name")
    public String name;
    @JsonProperty("version")
    public String version;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("self")
    public String self;
    @JsonProperty("roles")
    public List<String> roles = new ArrayList<String>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DocsHello withName(String name) {
        this.name = name;
        return this;
    }

    public DocsHello withVersion(String version) {
        this.version = version;
        return this;
    }

    public DocsHello withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public DocsHello withSelf(String self) {
        this.self = self;
        return this;
    }

    public DocsHello withRoles(List<String> roles) {
        this.roles = roles;
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

    public DocsHello withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("version", version).append("timestamp", timestamp).append("self", self).append("roles", roles).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(roles).append(name).append(self).append(additionalProperties).append(version).append(timestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DocsHello) == false) {
            return false;
        }
        DocsHello rhs = ((DocsHello) other);
        return new EqualsBuilder().append(roles, rhs.roles).append(name, rhs.name).append(self, rhs.self).append(additionalProperties, rhs.additionalProperties).append(version, rhs.version).append(timestamp, rhs.timestamp).isEquals();
    }

}

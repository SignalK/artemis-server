
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
    "navigation",
    "name",
    "uuid"
})
public class UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c {

    @JsonProperty("navigation")
    public Navigation___ navigation;
    @JsonProperty("name")
    public String name;
    @JsonProperty("uuid")
    public String uuid;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c withNavigation(Navigation___ navigation) {
        this.navigation = navigation;
        return this;
    }

    public UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c withName(String name) {
        this.name = name;
        return this;
    }

    public UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c withUuid(String uuid) {
        this.uuid = uuid;
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

    public UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("navigation", navigation).append("name", name).append("uuid", uuid).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(navigation).append(additionalProperties).append(uuid).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c) == false) {
            return false;
        }
        UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c rhs = ((UrnMrnSignalkUuid705f5f1aEfaf44aa9cb8A0fd6305567c) other);
        return new EqualsBuilder().append(name, rhs.name).append(navigation, rhs.navigation).append(additionalProperties, rhs.additionalProperties).append(uuid, rhs.uuid).isEquals();
    }

}

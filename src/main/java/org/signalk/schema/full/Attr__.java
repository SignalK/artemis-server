
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
    "_mode",
    "_owner",
    "_group"
})
public class Attr__ {

    @JsonProperty("_mode")
    public Integer mode;
    @JsonProperty("_owner")
    public String owner;
    @JsonProperty("_group")
    public String group;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Attr__ withMode(Integer mode) {
        this.mode = mode;
        return this;
    }

    public Attr__ withOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public Attr__ withGroup(String group) {
        this.group = group;
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

    public Attr__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("mode", mode).append("owner", owner).append("group", group).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(mode).append(owner).append(additionalProperties).append(group).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Attr__) == false) {
            return false;
        }
        Attr__ rhs = ((Attr__) other);
        return new EqualsBuilder().append(mode, rhs.mode).append(owner, rhs.owner).append(additionalProperties, rhs.additionalProperties).append(group, rhs.group).isEquals();
    }

}


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
    "uuid",
    "navigation"
})
public class UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_ {

    @JsonProperty("uuid")
    public String uuid;
    @JsonProperty("navigation")
    public Navigation____ navigation;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_ withUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_ withNavigation(Navigation____ navigation) {
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

    public UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("uuid", uuid).append("navigation", navigation).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(navigation).append(additionalProperties).append(uuid).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_) == false) {
            return false;
        }
        UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_ rhs = ((UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d_) other);
        return new EqualsBuilder().append(navigation, rhs.navigation).append(additionalProperties, rhs.additionalProperties).append(uuid, rhs.uuid).isEquals();
    }

}

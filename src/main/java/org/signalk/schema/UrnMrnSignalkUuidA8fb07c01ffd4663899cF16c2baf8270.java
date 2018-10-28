
package org.signalk.schema;

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
    "navigation"
})
public class UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270 {

    @JsonProperty("navigation")
    public Navigation navigation;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270 withNavigation(Navigation navigation) {
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

    public UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("navigation", navigation).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(navigation).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270) == false) {
            return false;
        }
        UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270 rhs = ((UrnMrnSignalkUuidA8fb07c01ffd4663899cF16c2baf8270) other);
        return new EqualsBuilder().append(navigation, rhs.navigation).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

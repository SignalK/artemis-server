
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
    "propulsion"
})
public class UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d {

    @JsonProperty("uuid")
    public String uuid;
    @JsonProperty("propulsion")
    public Propulsion propulsion;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d withUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d withPropulsion(Propulsion propulsion) {
        this.propulsion = propulsion;
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

    public UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("uuid", uuid).append("propulsion", propulsion).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(propulsion).append(additionalProperties).append(uuid).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d) == false) {
            return false;
        }
        UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d rhs = ((UrnMrnSignalkUuidC0d793344e254245889254e8ccc8021d) other);
        return new EqualsBuilder().append(propulsion, rhs.propulsion).append(additionalProperties, rhs.additionalProperties).append(uuid, rhs.uuid).isEquals();
    }

}

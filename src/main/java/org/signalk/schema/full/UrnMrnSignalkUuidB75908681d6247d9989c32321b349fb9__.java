
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
    "environment"
})
public class UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__ {

    @JsonProperty("uuid")
    public String uuid;
    @JsonProperty("environment")
    public Environment__ environment;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__ withUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__ withEnvironment(Environment__ environment) {
        this.environment = environment;
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

    public UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("uuid", uuid).append("environment", environment).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(environment).append(additionalProperties).append(uuid).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__) == false) {
            return false;
        }
        UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__ rhs = ((UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9__) other);
        return new EqualsBuilder().append(environment, rhs.environment).append(additionalProperties, rhs.additionalProperties).append(uuid, rhs.uuid).isEquals();
    }

}

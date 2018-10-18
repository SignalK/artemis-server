
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
    "notifications",
    "uuid"
})
public class UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9 {

    @JsonProperty("notifications")
    public Notifications__ notifications;
    @JsonProperty("uuid")
    public String uuid;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9 withNotifications(Notifications__ notifications) {
        this.notifications = notifications;
        return this;
    }

    public UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9 withUuid(String uuid) {
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

    public UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("notifications", notifications).append("uuid", uuid).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(uuid).append(notifications).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9) == false) {
            return false;
        }
        UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9 rhs = ((UrnMrnSignalkUuidB75908681d6247d9989c32321b349fb9) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(uuid, rhs.uuid).append(notifications, rhs.notifications).isEquals();
    }

}

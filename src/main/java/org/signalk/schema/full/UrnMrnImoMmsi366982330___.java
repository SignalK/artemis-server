
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
    "mmsi",
    "notifications"
})
public class UrnMrnImoMmsi366982330___ {

    @JsonProperty("mmsi")
    public String mmsi;
    @JsonProperty("notifications")
    public Notifications_ notifications;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public UrnMrnImoMmsi366982330___ withMmsi(String mmsi) {
        this.mmsi = mmsi;
        return this;
    }

    public UrnMrnImoMmsi366982330___ withNotifications(Notifications_ notifications) {
        this.notifications = notifications;
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

    public UrnMrnImoMmsi366982330___ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("mmsi", mmsi).append("notifications", notifications).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(mmsi).append(notifications).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof UrnMrnImoMmsi366982330___) == false) {
            return false;
        }
        UrnMrnImoMmsi366982330___ rhs = ((UrnMrnImoMmsi366982330___) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(mmsi, rhs.mmsi).append(notifications, rhs.notifications).isEquals();
    }

}

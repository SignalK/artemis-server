
package org.signalk.schema.full;

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
    "displayName",
    "longName",
    "shortName",
    "description",
    "units",
    "timeout",
    "displayScale",
    "alertMethod",
    "warnMethod",
    "alarmMethod",
    "emergencyMethod",
    "zones"
})
public class Meta_ {

    @JsonProperty("displayName")
    public String displayName;
    @JsonProperty("longName")
    public String longName;
    @JsonProperty("shortName")
    public String shortName;
    @JsonProperty("description")
    public String description;
    @JsonProperty("units")
    public String units;
    @JsonProperty("timeout")
    public Integer timeout;
    @JsonProperty("displayScale")
    public DisplayScale displayScale;
    @JsonProperty("alertMethod")
    public List<String> alertMethod = new ArrayList<String>();
    @JsonProperty("warnMethod")
    public List<String> warnMethod = new ArrayList<String>();
    @JsonProperty("alarmMethod")
    public List<String> alarmMethod = new ArrayList<String>();
    @JsonProperty("emergencyMethod")
    public List<String> emergencyMethod = new ArrayList<String>();
    @JsonProperty("zones")
    public List<Zone_> zones = new ArrayList<Zone_>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Meta_ withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public Meta_ withLongName(String longName) {
        this.longName = longName;
        return this;
    }

    public Meta_ withShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public Meta_ withDescription(String description) {
        this.description = description;
        return this;
    }

    public Meta_ withUnits(String units) {
        this.units = units;
        return this;
    }

    public Meta_ withTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    public Meta_ withDisplayScale(DisplayScale displayScale) {
        this.displayScale = displayScale;
        return this;
    }

    public Meta_ withAlertMethod(List<String> alertMethod) {
        this.alertMethod = alertMethod;
        return this;
    }

    public Meta_ withWarnMethod(List<String> warnMethod) {
        this.warnMethod = warnMethod;
        return this;
    }

    public Meta_ withAlarmMethod(List<String> alarmMethod) {
        this.alarmMethod = alarmMethod;
        return this;
    }

    public Meta_ withEmergencyMethod(List<String> emergencyMethod) {
        this.emergencyMethod = emergencyMethod;
        return this;
    }

    public Meta_ withZones(List<Zone_> zones) {
        this.zones = zones;
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

    public Meta_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("displayName", displayName).append("longName", longName).append("shortName", shortName).append("description", description).append("units", units).append("timeout", timeout).append("displayScale", displayScale).append("alertMethod", alertMethod).append("warnMethod", warnMethod).append("alarmMethod", alarmMethod).append("emergencyMethod", emergencyMethod).append("zones", zones).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(displayName).append(warnMethod).append(description).append(units).append(zones).append(timeout).append(emergencyMethod).append(alertMethod).append(alarmMethod).append(additionalProperties).append(shortName).append(displayScale).append(longName).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Meta_) == false) {
            return false;
        }
        Meta_ rhs = ((Meta_) other);
        return new EqualsBuilder().append(displayName, rhs.displayName).append(warnMethod, rhs.warnMethod).append(description, rhs.description).append(units, rhs.units).append(zones, rhs.zones).append(timeout, rhs.timeout).append(emergencyMethod, rhs.emergencyMethod).append(alertMethod, rhs.alertMethod).append(alarmMethod, rhs.alarmMethod).append(additionalProperties, rhs.additionalProperties).append(shortName, rhs.shortName).append(displayScale, rhs.displayScale).append(longName, rhs.longName).isEquals();
    }

}

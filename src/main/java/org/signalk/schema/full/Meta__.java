
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
    "description",
    "displayName",
    "longName",
    "shortName",
    "units",
    "warnMethod",
    "alarmMethod",
    "zones"
})
public class Meta__ {

    @JsonProperty("description")
    public String description;
    @JsonProperty("displayName")
    public String displayName;
    @JsonProperty("longName")
    public String longName;
    @JsonProperty("shortName")
    public String shortName;
    @JsonProperty("units")
    public String units;
    @JsonProperty("warnMethod")
    public List<String> warnMethod = new ArrayList<String>();
    @JsonProperty("alarmMethod")
    public List<String> alarmMethod = new ArrayList<String>();
    @JsonProperty("zones")
    public List<Zone__> zones = new ArrayList<Zone__>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Meta__ withDescription(String description) {
        this.description = description;
        return this;
    }

    public Meta__ withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public Meta__ withLongName(String longName) {
        this.longName = longName;
        return this;
    }

    public Meta__ withShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public Meta__ withUnits(String units) {
        this.units = units;
        return this;
    }

    public Meta__ withWarnMethod(List<String> warnMethod) {
        this.warnMethod = warnMethod;
        return this;
    }

    public Meta__ withAlarmMethod(List<String> alarmMethod) {
        this.alarmMethod = alarmMethod;
        return this;
    }

    public Meta__ withZones(List<Zone__> zones) {
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

    public Meta__ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("description", description).append("displayName", displayName).append("longName", longName).append("shortName", shortName).append("units", units).append("warnMethod", warnMethod).append("alarmMethod", alarmMethod).append("zones", zones).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(displayName).append(warnMethod).append(description).append(units).append(alarmMethod).append(additionalProperties).append(shortName).append(zones).append(longName).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Meta__) == false) {
            return false;
        }
        Meta__ rhs = ((Meta__) other);
        return new EqualsBuilder().append(displayName, rhs.displayName).append(warnMethod, rhs.warnMethod).append(description, rhs.description).append(units, rhs.units).append(alarmMethod, rhs.alarmMethod).append(additionalProperties, rhs.additionalProperties).append(shortName, rhs.shortName).append(zones, rhs.zones).append(longName, rhs.longName).isEquals();
    }

}

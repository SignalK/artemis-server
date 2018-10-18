
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
    "units",
    "zones",
    "shortName",
    "alarmMethod",
    "warnMethod",
    "displayName"
})
public class Meta {

    @JsonProperty("description")
    public String description;
    @JsonProperty("units")
    public String units;
    @JsonProperty("zones")
    public List<Zone> zones = new ArrayList<Zone>();
    @JsonProperty("shortName")
    public String shortName;
    @JsonProperty("alarmMethod")
    public List<String> alarmMethod = new ArrayList<String>();
    @JsonProperty("warnMethod")
    public List<String> warnMethod = new ArrayList<String>();
    @JsonProperty("displayName")
    public String displayName;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Meta withDescription(String description) {
        this.description = description;
        return this;
    }

    public Meta withUnits(String units) {
        this.units = units;
        return this;
    }

    public Meta withZones(List<Zone> zones) {
        this.zones = zones;
        return this;
    }

    public Meta withShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public Meta withAlarmMethod(List<String> alarmMethod) {
        this.alarmMethod = alarmMethod;
        return this;
    }

    public Meta withWarnMethod(List<String> warnMethod) {
        this.warnMethod = warnMethod;
        return this;
    }

    public Meta withDisplayName(String displayName) {
        this.displayName = displayName;
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

    public Meta withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("description", description).append("units", units).append("zones", zones).append("shortName", shortName).append("alarmMethod", alarmMethod).append("warnMethod", warnMethod).append("displayName", displayName).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(displayName).append(warnMethod).append(description).append(units).append(alarmMethod).append(additionalProperties).append(zones).append(shortName).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Meta) == false) {
            return false;
        }
        Meta rhs = ((Meta) other);
        return new EqualsBuilder().append(displayName, rhs.displayName).append(warnMethod, rhs.warnMethod).append(description, rhs.description).append(units, rhs.units).append(alarmMethod, rhs.alarmMethod).append(additionalProperties, rhs.additionalProperties).append(zones, rhs.zones).append(shortName, rhs.shortName).isEquals();
    }

}

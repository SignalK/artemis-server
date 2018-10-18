
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
    "lineLineVoltage",
    "lineNeutralVoltage",
    "frequency"
})
public class C {

    @JsonProperty("lineLineVoltage")
    public LineLineVoltage__ lineLineVoltage;
    @JsonProperty("lineNeutralVoltage")
    public LineNeutralVoltage__ lineNeutralVoltage;
    @JsonProperty("frequency")
    public Frequency__ frequency;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public C withLineLineVoltage(LineLineVoltage__ lineLineVoltage) {
        this.lineLineVoltage = lineLineVoltage;
        return this;
    }

    public C withLineNeutralVoltage(LineNeutralVoltage__ lineNeutralVoltage) {
        this.lineNeutralVoltage = lineNeutralVoltage;
        return this;
    }

    public C withFrequency(Frequency__ frequency) {
        this.frequency = frequency;
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

    public C withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("lineLineVoltage", lineLineVoltage).append("lineNeutralVoltage", lineNeutralVoltage).append("frequency", frequency).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(lineNeutralVoltage).append(lineLineVoltage).append(frequency).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof C) == false) {
            return false;
        }
        C rhs = ((C) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(lineNeutralVoltage, rhs.lineNeutralVoltage).append(lineLineVoltage, rhs.lineLineVoltage).append(frequency, rhs.frequency).isEquals();
    }

}

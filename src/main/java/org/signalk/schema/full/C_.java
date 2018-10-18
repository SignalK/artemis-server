
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
    "frequency",
    "current",
    "reactivePower",
    "powerFactor",
    "powerFactorLagging",
    "realPower",
    "apparentPower"
})
public class C_ {

    @JsonProperty("lineLineVoltage")
    public LineLineVoltage_____ lineLineVoltage;
    @JsonProperty("lineNeutralVoltage")
    public LineNeutralVoltage_____ lineNeutralVoltage;
    @JsonProperty("frequency")
    public Frequency_____ frequency;
    @JsonProperty("current")
    public Current__ current;
    @JsonProperty("reactivePower")
    public ReactivePower__ reactivePower;
    @JsonProperty("powerFactor")
    public PowerFactor__ powerFactor;
    @JsonProperty("powerFactorLagging")
    public String powerFactorLagging;
    @JsonProperty("realPower")
    public RealPower__ realPower;
    @JsonProperty("apparentPower")
    public ApparentPower__ apparentPower;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public C_ withLineLineVoltage(LineLineVoltage_____ lineLineVoltage) {
        this.lineLineVoltage = lineLineVoltage;
        return this;
    }

    public C_ withLineNeutralVoltage(LineNeutralVoltage_____ lineNeutralVoltage) {
        this.lineNeutralVoltage = lineNeutralVoltage;
        return this;
    }

    public C_ withFrequency(Frequency_____ frequency) {
        this.frequency = frequency;
        return this;
    }

    public C_ withCurrent(Current__ current) {
        this.current = current;
        return this;
    }

    public C_ withReactivePower(ReactivePower__ reactivePower) {
        this.reactivePower = reactivePower;
        return this;
    }

    public C_ withPowerFactor(PowerFactor__ powerFactor) {
        this.powerFactor = powerFactor;
        return this;
    }

    public C_ withPowerFactorLagging(String powerFactorLagging) {
        this.powerFactorLagging = powerFactorLagging;
        return this;
    }

    public C_ withRealPower(RealPower__ realPower) {
        this.realPower = realPower;
        return this;
    }

    public C_ withApparentPower(ApparentPower__ apparentPower) {
        this.apparentPower = apparentPower;
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

    public C_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("lineLineVoltage", lineLineVoltage).append("lineNeutralVoltage", lineNeutralVoltage).append("frequency", frequency).append("current", current).append("reactivePower", reactivePower).append("powerFactor", powerFactor).append("powerFactorLagging", powerFactorLagging).append("realPower", realPower).append("apparentPower", apparentPower).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(current).append(reactivePower).append(realPower).append(apparentPower).append(additionalProperties).append(lineNeutralVoltage).append(lineLineVoltage).append(frequency).append(powerFactor).append(powerFactorLagging).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof C_) == false) {
            return false;
        }
        C_ rhs = ((C_) other);
        return new EqualsBuilder().append(current, rhs.current).append(reactivePower, rhs.reactivePower).append(realPower, rhs.realPower).append(apparentPower, rhs.apparentPower).append(additionalProperties, rhs.additionalProperties).append(lineNeutralVoltage, rhs.lineNeutralVoltage).append(lineLineVoltage, rhs.lineLineVoltage).append(frequency, rhs.frequency).append(powerFactor, rhs.powerFactor).append(powerFactorLagging, rhs.powerFactorLagging).isEquals();
    }

}

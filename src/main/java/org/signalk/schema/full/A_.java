
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
public class A_ {

    @JsonProperty("lineLineVoltage")
    public LineLineVoltage___ lineLineVoltage;
    @JsonProperty("lineNeutralVoltage")
    public LineNeutralVoltage___ lineNeutralVoltage;
    @JsonProperty("frequency")
    public Frequency___ frequency;
    @JsonProperty("current")
    public Current current;
    @JsonProperty("reactivePower")
    public ReactivePower reactivePower;
    @JsonProperty("powerFactor")
    public PowerFactor powerFactor;
    @JsonProperty("powerFactorLagging")
    public String powerFactorLagging;
    @JsonProperty("realPower")
    public RealPower realPower;
    @JsonProperty("apparentPower")
    public ApparentPower apparentPower;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public A_ withLineLineVoltage(LineLineVoltage___ lineLineVoltage) {
        this.lineLineVoltage = lineLineVoltage;
        return this;
    }

    public A_ withLineNeutralVoltage(LineNeutralVoltage___ lineNeutralVoltage) {
        this.lineNeutralVoltage = lineNeutralVoltage;
        return this;
    }

    public A_ withFrequency(Frequency___ frequency) {
        this.frequency = frequency;
        return this;
    }

    public A_ withCurrent(Current current) {
        this.current = current;
        return this;
    }

    public A_ withReactivePower(ReactivePower reactivePower) {
        this.reactivePower = reactivePower;
        return this;
    }

    public A_ withPowerFactor(PowerFactor powerFactor) {
        this.powerFactor = powerFactor;
        return this;
    }

    public A_ withPowerFactorLagging(String powerFactorLagging) {
        this.powerFactorLagging = powerFactorLagging;
        return this;
    }

    public A_ withRealPower(RealPower realPower) {
        this.realPower = realPower;
        return this;
    }

    public A_ withApparentPower(ApparentPower apparentPower) {
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

    public A_ withAdditionalProperty(String name, Object value) {
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
        if ((other instanceof A_) == false) {
            return false;
        }
        A_ rhs = ((A_) other);
        return new EqualsBuilder().append(current, rhs.current).append(reactivePower, rhs.reactivePower).append(realPower, rhs.realPower).append(apparentPower, rhs.apparentPower).append(additionalProperties, rhs.additionalProperties).append(lineNeutralVoltage, rhs.lineNeutralVoltage).append(lineLineVoltage, rhs.lineLineVoltage).append(frequency, rhs.frequency).append(powerFactor, rhs.powerFactor).append(powerFactorLagging, rhs.powerFactorLagging).isEquals();
    }

}

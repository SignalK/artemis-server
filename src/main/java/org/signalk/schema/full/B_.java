
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
public class B_ {

    @JsonProperty("lineLineVoltage")
    public LineLineVoltage____ lineLineVoltage;
    @JsonProperty("lineNeutralVoltage")
    public LineNeutralVoltage____ lineNeutralVoltage;
    @JsonProperty("frequency")
    public Frequency____ frequency;
    @JsonProperty("current")
    public Current_ current;
    @JsonProperty("reactivePower")
    public ReactivePower_ reactivePower;
    @JsonProperty("powerFactor")
    public PowerFactor_ powerFactor;
    @JsonProperty("powerFactorLagging")
    public String powerFactorLagging;
    @JsonProperty("realPower")
    public RealPower_ realPower;
    @JsonProperty("apparentPower")
    public ApparentPower_ apparentPower;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public B_ withLineLineVoltage(LineLineVoltage____ lineLineVoltage) {
        this.lineLineVoltage = lineLineVoltage;
        return this;
    }

    public B_ withLineNeutralVoltage(LineNeutralVoltage____ lineNeutralVoltage) {
        this.lineNeutralVoltage = lineNeutralVoltage;
        return this;
    }

    public B_ withFrequency(Frequency____ frequency) {
        this.frequency = frequency;
        return this;
    }

    public B_ withCurrent(Current_ current) {
        this.current = current;
        return this;
    }

    public B_ withReactivePower(ReactivePower_ reactivePower) {
        this.reactivePower = reactivePower;
        return this;
    }

    public B_ withPowerFactor(PowerFactor_ powerFactor) {
        this.powerFactor = powerFactor;
        return this;
    }

    public B_ withPowerFactorLagging(String powerFactorLagging) {
        this.powerFactorLagging = powerFactorLagging;
        return this;
    }

    public B_ withRealPower(RealPower_ realPower) {
        this.realPower = realPower;
        return this;
    }

    public B_ withApparentPower(ApparentPower_ apparentPower) {
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

    public B_ withAdditionalProperty(String name, Object value) {
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
        if ((other instanceof B_) == false) {
            return false;
        }
        B_ rhs = ((B_) other);
        return new EqualsBuilder().append(current, rhs.current).append(reactivePower, rhs.reactivePower).append(realPower, rhs.realPower).append(apparentPower, rhs.apparentPower).append(additionalProperties, rhs.additionalProperties).append(lineNeutralVoltage, rhs.lineNeutralVoltage).append(lineLineVoltage, rhs.lineLineVoltage).append(frequency, rhs.frequency).append(powerFactor, rhs.powerFactor).append(powerFactorLagging, rhs.powerFactorLagging).isEquals();
    }

}

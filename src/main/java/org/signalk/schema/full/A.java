
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
public class A {

    @JsonProperty("lineLineVoltage")
    public LineLineVoltage lineLineVoltage;
    @JsonProperty("lineNeutralVoltage")
    public LineNeutralVoltage lineNeutralVoltage;
    @JsonProperty("frequency")
    public Frequency frequency;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public A withLineLineVoltage(LineLineVoltage lineLineVoltage) {
        this.lineLineVoltage = lineLineVoltage;
        return this;
    }

    public A withLineNeutralVoltage(LineNeutralVoltage lineNeutralVoltage) {
        this.lineNeutralVoltage = lineNeutralVoltage;
        return this;
    }

    public A withFrequency(Frequency frequency) {
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

    public A withAdditionalProperty(String name, Object value) {
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
        if ((other instanceof A) == false) {
            return false;
        }
        A rhs = ((A) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(lineNeutralVoltage, rhs.lineNeutralVoltage).append(lineLineVoltage, rhs.lineLineVoltage).append(frequency, rhs.frequency).isEquals();
    }

}

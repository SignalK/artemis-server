
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
    "value",
    "$source",
    "sentence",
    "timestamp"
})
public class SpeedOverGround {

    @JsonProperty("value")
    public Double value;
    @JsonProperty("$source")
    public String $source;
    @JsonProperty("sentence")
    public String sentence;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public SpeedOverGround withValue(Double value) {
        this.value = value;
        return this;
    }

    public SpeedOverGround with$source(String $source) {
        this.$source = $source;
        return this;
    }

    public SpeedOverGround withSentence(String sentence) {
        this.sentence = sentence;
        return this;
    }

    public SpeedOverGround withTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    public SpeedOverGround withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("value", value).append("$source", $source).append("sentence", sentence).append("timestamp", timestamp).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(sentence).append(additionalProperties).append(value).append($source).append(timestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SpeedOverGround) == false) {
            return false;
        }
        SpeedOverGround rhs = ((SpeedOverGround) other);
        return new EqualsBuilder().append(sentence, rhs.sentence).append(additionalProperties, rhs.additionalProperties).append(value, rhs.value).append($source, rhs.$source).append(timestamp, rhs.timestamp).isEquals();
    }

}

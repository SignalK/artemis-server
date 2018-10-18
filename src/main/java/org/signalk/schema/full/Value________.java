
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
    "method",
    "state",
    "message"
})
public class Value________ {

    @JsonProperty("method")
    public List<String> method = new ArrayList<String>();
    @JsonProperty("state")
    public String state;
    @JsonProperty("message")
    public String message;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Value________ withMethod(List<String> method) {
        this.method = method;
        return this;
    }

    public Value________ withState(String state) {
        this.state = state;
        return this;
    }

    public Value________ withMessage(String message) {
        this.message = message;
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

    public Value________ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("method", method).append("state", state).append("message", message).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(state).append(additionalProperties).append(method).append(message).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Value________) == false) {
            return false;
        }
        Value________ rhs = ((Value________) other);
        return new EqualsBuilder().append(state, rhs.state).append(additionalProperties, rhs.additionalProperties).append(method, rhs.method).append(message, rhs.message).isEquals();
    }

}


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
    "A",
    "B",
    "C"
})
public class Phase_ {

    @JsonProperty("A")
    public A_ a;
    @JsonProperty("B")
    public B_ b;
    @JsonProperty("C")
    public C_ c;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Phase_ withA(A_ a) {
        this.a = a;
        return this;
    }

    public Phase_ withB(B_ b) {
        this.b = b;
        return this;
    }

    public Phase_ withC(C_ c) {
        this.c = c;
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

    public Phase_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("a", a).append("b", b).append("c", c).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(a).append(b).append(c).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Phase_) == false) {
            return false;
        }
        Phase_ rhs = ((Phase_) other);
        return new EqualsBuilder().append(a, rhs.a).append(b, rhs.b).append(c, rhs.c).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

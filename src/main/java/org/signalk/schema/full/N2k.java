
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
    "src",
    "pgns"
})
public class N2k {

    @JsonProperty("src")
    public String src;
    @JsonProperty("pgns")
    public Pgns pgns;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public N2k withSrc(String src) {
        this.src = src;
        return this;
    }

    public N2k withPgns(Pgns pgns) {
        this.pgns = pgns;
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

    public N2k withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("src", src).append("pgns", pgns).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(pgns).append(additionalProperties).append(src).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof N2k) == false) {
            return false;
        }
        N2k rhs = ((N2k) other);
        return new EqualsBuilder().append(pgns, rhs.pgns).append(additionalProperties, rhs.additionalProperties).append(src, rhs.src).isEquals();
    }

}

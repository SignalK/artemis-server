
package org.signalk.schema.delta;

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
    "label",
    "type",
    "src",
    "pgn"
})
public class Source_ {

    @JsonProperty("label")
    public String label;
    @JsonProperty("type")
    public String type;
    @JsonProperty("src")
    public String src;
    @JsonProperty("pgn")
    public Integer pgn;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Source_ withLabel(String label) {
        this.label = label;
        return this;
    }

    public Source_ withType(String type) {
        this.type = type;
        return this;
    }

    public Source_ withSrc(String src) {
        this.src = src;
        return this;
    }

    public Source_ withPgn(Integer pgn) {
        this.pgn = pgn;
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

    public Source_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("label", label).append("type", type).append("src", src).append("pgn", pgn).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(label).append(additionalProperties).append(type).append(src).append(pgn).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Source_) == false) {
            return false;
        }
        Source_ rhs = ((Source_) other);
        return new EqualsBuilder().append(label, rhs.label).append(additionalProperties, rhs.additionalProperties).append(type, rhs.type).append(src, rhs.src).append(pgn, rhs.pgn).isEquals();
    }

}

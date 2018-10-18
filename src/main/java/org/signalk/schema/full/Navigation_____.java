
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
    "gnss",
    "anchor"
})
public class Navigation_____ {

    @JsonProperty("gnss")
    public Gnss gnss;
    @JsonProperty("anchor")
    public Anchor anchor;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Navigation_____ withGnss(Gnss gnss) {
        this.gnss = gnss;
        return this;
    }

    public Navigation_____ withAnchor(Anchor anchor) {
        this.anchor = anchor;
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

    public Navigation_____ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("gnss", gnss).append("anchor", anchor).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(gnss).append(anchor).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Navigation_____) == false) {
            return false;
        }
        Navigation_____ rhs = ((Navigation_____) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(gnss, rhs.gnss).append(anchor, rhs.anchor).isEquals();
    }

}

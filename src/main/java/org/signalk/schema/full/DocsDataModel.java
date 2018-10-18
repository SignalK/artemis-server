
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
    "version",
    "self",
    "vessels",
    "sources"
})
public class DocsDataModel {

    @JsonProperty("version")
    public String version;
    @JsonProperty("self")
    public String self;
    @JsonProperty("vessels")
    public Vessels___ vessels;
    @JsonProperty("sources")
    public Sources_ sources;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DocsDataModel withVersion(String version) {
        this.version = version;
        return this;
    }

    public DocsDataModel withSelf(String self) {
        this.self = self;
        return this;
    }

    public DocsDataModel withVessels(Vessels___ vessels) {
        this.vessels = vessels;
        return this;
    }

    public DocsDataModel withSources(Sources_ sources) {
        this.sources = sources;
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

    public DocsDataModel withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("version", version).append("self", self).append("vessels", vessels).append("sources", sources).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(self).append(additionalProperties).append(sources).append(version).append(vessels).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DocsDataModel) == false) {
            return false;
        }
        DocsDataModel rhs = ((DocsDataModel) other);
        return new EqualsBuilder().append(self, rhs.self).append(additionalProperties, rhs.additionalProperties).append(sources, rhs.sources).append(version, rhs.version).append(vessels, rhs.vessels).isEquals();
    }

}

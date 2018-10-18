
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
    "self",
    "version",
    "vessels",
    "sources"
})
public class DocsDataModelMultipleValues {

    @JsonProperty("self")
    public String self;
    @JsonProperty("version")
    public String version;
    @JsonProperty("vessels")
    public Vessels_____ vessels;
    @JsonProperty("sources")
    public Sources__ sources;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DocsDataModelMultipleValues withSelf(String self) {
        this.self = self;
        return this;
    }

    public DocsDataModelMultipleValues withVersion(String version) {
        this.version = version;
        return this;
    }

    public DocsDataModelMultipleValues withVessels(Vessels_____ vessels) {
        this.vessels = vessels;
        return this;
    }

    public DocsDataModelMultipleValues withSources(Sources__ sources) {
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

    public DocsDataModelMultipleValues withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("self", self).append("version", version).append("vessels", vessels).append("sources", sources).append("additionalProperties", additionalProperties).toString();
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
        if ((other instanceof DocsDataModelMultipleValues) == false) {
            return false;
        }
        DocsDataModelMultipleValues rhs = ((DocsDataModelMultipleValues) other);
        return new EqualsBuilder().append(self, rhs.self).append(additionalProperties, rhs.additionalProperties).append(sources, rhs.sources).append(version, rhs.version).append(vessels, rhs.vessels).isEquals();
    }

}


package org.signalk.schema.hello;

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
    "name",
    "version",
    "startTime",
    "playbackRate",
    "self",
    "roles"
})
public class DocsHelloPlayback {

    @JsonProperty("name")
    public String name;
    @JsonProperty("version")
    public String version;
    @JsonProperty("startTime")
    public String startTime;
    @JsonProperty("playbackRate")
    public Integer playbackRate;
    @JsonProperty("self")
    public String self;
    @JsonProperty("roles")
    public List<String> roles = new ArrayList<String>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DocsHelloPlayback withName(String name) {
        this.name = name;
        return this;
    }

    public DocsHelloPlayback withVersion(String version) {
        this.version = version;
        return this;
    }

    public DocsHelloPlayback withStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public DocsHelloPlayback withPlaybackRate(Integer playbackRate) {
        this.playbackRate = playbackRate;
        return this;
    }

    public DocsHelloPlayback withSelf(String self) {
        this.self = self;
        return this;
    }

    public DocsHelloPlayback withRoles(List<String> roles) {
        this.roles = roles;
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

    public DocsHelloPlayback withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("version", version).append("startTime", startTime).append("playbackRate", playbackRate).append("self", self).append("roles", roles).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(playbackRate).append(roles).append(name).append(self).append(startTime).append(additionalProperties).append(version).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DocsHelloPlayback) == false) {
            return false;
        }
        DocsHelloPlayback rhs = ((DocsHelloPlayback) other);
        return new EqualsBuilder().append(playbackRate, rhs.playbackRate).append(roles, rhs.roles).append(name, rhs.name).append(self, rhs.self).append(startTime, rhs.startTime).append(additionalProperties, rhs.additionalProperties).append(version, rhs.version).isEquals();
    }

}

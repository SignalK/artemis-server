
package org.signalk.schema;

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
    "server",
    "endpoints"
})
public class Endpoints {

    @JsonProperty("server")
    public Server server;
    @JsonProperty("endpoints")
    public Endpoints_ endpoints;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Endpoints withServer(Server server) {
        this.server = server;
        return this;
    }

    public Endpoints withEndpoints(Endpoints_ endpoints) {
        this.endpoints = endpoints;
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

    public Endpoints withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("server", server).append("endpoints", endpoints).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(server).append(endpoints).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Endpoints) == false) {
            return false;
        }
        Endpoints rhs = ((Endpoints) other);
        return new EqualsBuilder().append(server, rhs.server).append(endpoints, rhs.endpoints).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

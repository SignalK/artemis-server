
package org.signalk.schema.discovery;

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
    "endpoints",
    "server"
})
public class DocsRestApi {

    @JsonProperty("endpoints")
    public Endpoints_ endpoints;
    @JsonProperty("server")
    public Server_ server;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DocsRestApi withEndpoints(Endpoints_ endpoints) {
        this.endpoints = endpoints;
        return this;
    }

    public DocsRestApi withServer(Server_ server) {
        this.server = server;
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

    public DocsRestApi withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("endpoints", endpoints).append("server", server).append("additionalProperties", additionalProperties).toString();
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
        if ((other instanceof DocsRestApi) == false) {
            return false;
        }
        DocsRestApi rhs = ((DocsRestApi) other);
        return new EqualsBuilder().append(server, rhs.server).append(endpoints, rhs.endpoints).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}

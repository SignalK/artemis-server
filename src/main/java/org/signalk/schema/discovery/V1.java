
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
    "version",
    "signalk-http",
    "signalk-ws"
})
public class V1 {

    @JsonProperty("version")
    public String version;
    @JsonProperty("signalk-http")
    public String signalkHttp;
    @JsonProperty("signalk-ws")
    public String signalkWs;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public V1 withVersion(String version) {
        this.version = version;
        return this;
    }

    public V1 withSignalkHttp(String signalkHttp) {
        this.signalkHttp = signalkHttp;
        return this;
    }

    public V1 withSignalkWs(String signalkWs) {
        this.signalkWs = signalkWs;
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

    public V1 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("version", version).append("signalkHttp", signalkHttp).append("signalkWs", signalkWs).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(signalkHttp).append(additionalProperties).append(version).append(signalkWs).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof V1) == false) {
            return false;
        }
        V1 rhs = ((V1) other);
        return new EqualsBuilder().append(signalkHttp, rhs.signalkHttp).append(additionalProperties, rhs.additionalProperties).append(version, rhs.version).append(signalkWs, rhs.signalkWs).isEquals();
    }

}

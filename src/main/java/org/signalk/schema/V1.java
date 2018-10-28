
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
    "signalk-tcp",
    "signalk-udp",
    "nmea-tcp",
    "nmea-udp",
    "signalk-http",
    "mqtt",
    "signalk-ws",
    "stomp",
    "version"
})
public class V1 {

    @JsonProperty("signalk-tcp")
    public String signalkTcp;
    @JsonProperty("signalk-udp")
    public String signalkUdp;
    @JsonProperty("nmea-tcp")
    public String nmeaTcp;
    @JsonProperty("nmea-udp")
    public String nmeaUdp;
    @JsonProperty("signalk-http")
    public String signalkHttp;
    @JsonProperty("mqtt")
    public String mqtt;
    @JsonProperty("signalk-ws")
    public String signalkWs;
    @JsonProperty("stomp")
    public String stomp;
    @JsonProperty("version")
    public String version;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public V1 withSignalkTcp(String signalkTcp) {
        this.signalkTcp = signalkTcp;
        return this;
    }

    public V1 withSignalkUdp(String signalkUdp) {
        this.signalkUdp = signalkUdp;
        return this;
    }

    public V1 withNmeaTcp(String nmeaTcp) {
        this.nmeaTcp = nmeaTcp;
        return this;
    }

    public V1 withNmeaUdp(String nmeaUdp) {
        this.nmeaUdp = nmeaUdp;
        return this;
    }

    public V1 withSignalkHttp(String signalkHttp) {
        this.signalkHttp = signalkHttp;
        return this;
    }

    public V1 withMqtt(String mqtt) {
        this.mqtt = mqtt;
        return this;
    }

    public V1 withSignalkWs(String signalkWs) {
        this.signalkWs = signalkWs;
        return this;
    }

    public V1 withStomp(String stomp) {
        this.stomp = stomp;
        return this;
    }

    public V1 withVersion(String version) {
        this.version = version;
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
        return new ToStringBuilder(this).append("signalkTcp", signalkTcp).append("signalkUdp", signalkUdp).append("nmeaTcp", nmeaTcp).append("nmeaUdp", nmeaUdp).append("signalkHttp", signalkHttp).append("mqtt", mqtt).append("signalkWs", signalkWs).append("stomp", stomp).append("version", version).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(signalkTcp).append(signalkUdp).append(signalkWs).append(mqtt).append(signalkHttp).append(stomp).append(additionalProperties).append(nmeaTcp).append(nmeaUdp).append(version).toHashCode();
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
        return new EqualsBuilder().append(signalkTcp, rhs.signalkTcp).append(signalkUdp, rhs.signalkUdp).append(signalkWs, rhs.signalkWs).append(mqtt, rhs.mqtt).append(signalkHttp, rhs.signalkHttp).append(stomp, rhs.stomp).append(additionalProperties, rhs.additionalProperties).append(nmeaTcp, rhs.nmeaTcp).append(nmeaUdp, rhs.nmeaUdp).append(version, rhs.version).isEquals();
    }

}

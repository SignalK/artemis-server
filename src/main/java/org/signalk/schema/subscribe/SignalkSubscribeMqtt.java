
package org.signalk.schema.subscribe;

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
    "context",
    "websocket.connectionkey",
    "reply-to",
    "subscribe"
})
public class SignalkSubscribeMqtt {

    @JsonProperty("context")
    public String context;
    @JsonProperty("websocket.connectionkey")
    public String websocketConnectionkey;
    @JsonProperty("reply-to")
    public String replyTo;
    @JsonProperty("subscribe")
    public List<Subscribe____> subscribe = new ArrayList<Subscribe____>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public SignalkSubscribeMqtt withContext(String context) {
        this.context = context;
        return this;
    }

    public SignalkSubscribeMqtt withWebsocketConnectionkey(String websocketConnectionkey) {
        this.websocketConnectionkey = websocketConnectionkey;
        return this;
    }

    public SignalkSubscribeMqtt withReplyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    public SignalkSubscribeMqtt withSubscribe(List<Subscribe____> subscribe) {
        this.subscribe = subscribe;
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

    public SignalkSubscribeMqtt withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("context", context).append("websocketConnectionkey", websocketConnectionkey).append("replyTo", replyTo).append("subscribe", subscribe).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(context).append(websocketConnectionkey).append(replyTo).append(additionalProperties).append(subscribe).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SignalkSubscribeMqtt) == false) {
            return false;
        }
        SignalkSubscribeMqtt rhs = ((SignalkSubscribeMqtt) other);
        return new EqualsBuilder().append(context, rhs.context).append(websocketConnectionkey, rhs.websocketConnectionkey).append(replyTo, rhs.replyTo).append(additionalProperties, rhs.additionalProperties).append(subscribe, rhs.subscribe).isEquals();
    }

}

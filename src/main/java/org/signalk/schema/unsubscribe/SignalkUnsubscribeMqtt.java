
package org.signalk.schema.unsubscribe;

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
    "unsubscribe"
})
public class SignalkUnsubscribeMqtt {

    @JsonProperty("context")
    public String context;
    @JsonProperty("websocket.connectionkey")
    public String websocketConnectionkey;
    @JsonProperty("unsubscribe")
    public List<Unsubscribe_> unsubscribe = new ArrayList<Unsubscribe_>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public SignalkUnsubscribeMqtt withContext(String context) {
        this.context = context;
        return this;
    }

    public SignalkUnsubscribeMqtt withWebsocketConnectionkey(String websocketConnectionkey) {
        this.websocketConnectionkey = websocketConnectionkey;
        return this;
    }

    public SignalkUnsubscribeMqtt withUnsubscribe(List<Unsubscribe_> unsubscribe) {
        this.unsubscribe = unsubscribe;
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

    public SignalkUnsubscribeMqtt withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("context", context).append("websocketConnectionkey", websocketConnectionkey).append("unsubscribe", unsubscribe).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(context).append(websocketConnectionkey).append(additionalProperties).append(unsubscribe).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SignalkUnsubscribeMqtt) == false) {
            return false;
        }
        SignalkUnsubscribeMqtt rhs = ((SignalkUnsubscribeMqtt) other);
        return new EqualsBuilder().append(context, rhs.context).append(websocketConnectionkey, rhs.websocketConnectionkey).append(additionalProperties, rhs.additionalProperties).append(unsubscribe, rhs.unsubscribe).isEquals();
    }

}

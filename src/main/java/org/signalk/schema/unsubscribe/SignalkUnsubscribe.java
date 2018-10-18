
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
    "unsubscribe"
})
public class SignalkUnsubscribe {

    @JsonProperty("context")
    public String context;
    @JsonProperty("unsubscribe")
    public List<Unsubscribe__> unsubscribe = new ArrayList<Unsubscribe__>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public SignalkUnsubscribe withContext(String context) {
        this.context = context;
        return this;
    }

    public SignalkUnsubscribe withUnsubscribe(List<Unsubscribe__> unsubscribe) {
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

    public SignalkUnsubscribe withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("context", context).append("unsubscribe", unsubscribe).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(context).append(additionalProperties).append(unsubscribe).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SignalkUnsubscribe) == false) {
            return false;
        }
        SignalkUnsubscribe rhs = ((SignalkUnsubscribe) other);
        return new EqualsBuilder().append(context, rhs.context).append(additionalProperties, rhs.additionalProperties).append(unsubscribe, rhs.unsubscribe).isEquals();
    }

}


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
    "value",
    "timestamp",
    "$source",
    "_attr",
    "meta"
})
public class BelowKeel_ {

    @JsonProperty("value")
    public Double value;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("$source")
    public String $source;
    @JsonProperty("_attr")
    public Attr___ attr;
    @JsonProperty("meta")
    public Meta__ meta;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public BelowKeel_ withValue(Double value) {
        this.value = value;
        return this;
    }

    public BelowKeel_ withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public BelowKeel_ with$source(String $source) {
        this.$source = $source;
        return this;
    }

    public BelowKeel_ withAttr(Attr___ attr) {
        this.attr = attr;
        return this;
    }

    public BelowKeel_ withMeta(Meta__ meta) {
        this.meta = meta;
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

    public BelowKeel_ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("value", value).append("timestamp", timestamp).append("$source", $source).append("attr", attr).append("meta", meta).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(meta).append(additionalProperties).append(attr).append(value).append(timestamp).append($source).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BelowKeel_) == false) {
            return false;
        }
        BelowKeel_ rhs = ((BelowKeel_) other);
        return new EqualsBuilder().append(meta, rhs.meta).append(additionalProperties, rhs.additionalProperties).append(attr, rhs.attr).append(value, rhs.value).append(timestamp, rhs.timestamp).append($source, rhs.$source).isEquals();
    }

}

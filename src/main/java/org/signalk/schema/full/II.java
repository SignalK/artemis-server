
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
    "talker",
    "sentences"
})
public class II {

    @JsonProperty("talker")
    public String talker;
    @JsonProperty("sentences")
    public Sentences_ sentences;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public II withTalker(String talker) {
        this.talker = talker;
        return this;
    }

    public II withSentences(Sentences_ sentences) {
        this.sentences = sentences;
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

    public II withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("talker", talker).append("sentences", sentences).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(talker).append(additionalProperties).append(sentences).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof II) == false) {
            return false;
        }
        II rhs = ((II) other);
        return new EqualsBuilder().append(talker, rhs.talker).append(additionalProperties, rhs.additionalProperties).append(sentences, rhs.sentences).isEquals();
    }

}

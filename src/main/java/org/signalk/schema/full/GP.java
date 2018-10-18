
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
public class GP {

    @JsonProperty("talker")
    public String talker;
    @JsonProperty("sentences")
    public Sentences sentences;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public GP withTalker(String talker) {
        this.talker = talker;
        return this;
    }

    public GP withSentences(Sentences sentences) {
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

    public GP withAdditionalProperty(String name, Object value) {
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
        if ((other instanceof GP) == false) {
            return false;
        }
        GP rhs = ((GP) other);
        return new EqualsBuilder().append(talker, rhs.talker).append(additionalProperties, rhs.additionalProperties).append(sentences, rhs.sentences).isEquals();
    }

}

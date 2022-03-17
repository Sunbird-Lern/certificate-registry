package org.sunbird.utilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ESResponseMapper {
    public List<Map<String,Object>>content;
    public int count;

    @JsonCreator
    public ESResponseMapper(
            @JsonProperty("hits") List<Map<String,Object>>content,
            @JsonProperty("total") int count) {
        this.content = content;
        this.count = count;
    }
    
    public ESResponseMapper() {}

    public List<Map<String, Object>> getContent() {
        return content;
    }

    public int getCount() {
        return count;
    }
    
    public void setContent(List<Map<String, Object>> content) {
        this.content = content;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
}
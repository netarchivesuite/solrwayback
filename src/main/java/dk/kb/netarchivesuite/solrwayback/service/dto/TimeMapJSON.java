package dk.kb.netarchivesuite.solrwayback.service.dto;

import java.util.List;
import java.util.Map;

public class TimeMapJSON {
    private String original_uri;
    private String timegate_uri;
    private Map<String, String> timemap_uri;
    private Map<String, Map<String,String>> mementos;

    public String getOriginal_uri() {
        return original_uri;
    }

    public void setOriginal_uri(String original_uri) {
        this.original_uri = original_uri;
    }

    public String getTimegate_uri() {
        return timegate_uri;
    }

    public void setTimegate_uri(String timegate_uri) {
        this.timegate_uri = timegate_uri;
    }

    public Map<String, String> getTimemap_uri() {
        return timemap_uri;
    }

    public void setTimemap_uri(Map<String, String> timemap_uri) {
        this.timemap_uri = timemap_uri;
    }

    public Map<String, Map<String, String>> getMementos() {
        return mementos;
    }

    public void setMementos(Map<String,Map<String, String>> mementos) {
        this.mementos = mementos;
    }
}

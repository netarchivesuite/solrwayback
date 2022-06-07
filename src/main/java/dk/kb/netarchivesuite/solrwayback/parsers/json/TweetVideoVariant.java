package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TweetVideoVariant {
    @JsonProperty("url")
    private String url;

    public TweetVideoVariant() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

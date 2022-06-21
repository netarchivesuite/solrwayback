package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.tuple.Pair;

public class TweetURL implements TweetEntity {
    private Pair<Integer, Integer> indices;

    private String expandedUrl;

    private String displayUrl;


    public TweetURL() {
    }

    @JsonProperty("indices")
    private void unpackIndices(int[] indices) {
        this.indices = Pair.of(indices[0], indices[1]);
    }

    public Pair<Integer, Integer> getIndices() {
        return indices;
    }

    @Override
    public void setIndices(Pair<Integer, Integer> newIndices) {
        this.indices = newIndices;
    }

    public String getExpandedUrl() {
        return expandedUrl;
    }

    public void setExpandedUrl(String expandedUrl) {
        this.expandedUrl = expandedUrl;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }

    public void setDisplayUrl(String displayUrl) {
        this.displayUrl = displayUrl;
    }
}

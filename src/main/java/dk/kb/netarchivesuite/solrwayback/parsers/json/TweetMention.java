package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.tuple.Pair;

public class TweetMention implements TweetEntity {
    @JsonProperty("id_str")
    private String id;

    private Pair<Integer, Integer> indices;

    private String screenName;


    public TweetMention() {
    }

    public TweetMention(String screenName) {
        this.screenName = screenName;
    }

    @JsonProperty("indices")
    private void unpackIndices(int[] indices) {
        this.indices = Pair.of(indices[0], indices[1]);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Pair<Integer, Integer> getIndices() {
        return indices;
    }

    @Override
    public void setIndices(Pair<Integer, Integer> newIndices) {
        this.indices = newIndices;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
}

package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.tuple.Pair;

public class TweetHashtag {
    private Pair<Integer, Integer> indices;

    private String text;


    public TweetHashtag() {
    }

    @JsonProperty("indices")
    private void unpackIndices(int[] indices) {
        this.indices = Pair.of(indices[0], indices[1]);
    }

    public Pair<Integer, Integer> getIndices() {
        return indices;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

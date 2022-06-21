package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public class TweetExtendedContent {
    // AFAIK display_text_range will always exist when extended_tweet does
    private Pair<Integer, Integer> displayTextRange;

    // See Tweet class for explanation of 'entities' vs. 'extended_entities'
    private TweetEntities entities;

    private List<TweetMedia> media;

    private String fullText;

    @JsonProperty("extended_entities")
    private void unpackMedia(Map<String, List<TweetMedia>> extendedEntitiesObj) {
        this.media = extendedEntitiesObj.get("media");
    }

    public TweetExtendedContent() {
    }

    @JsonProperty("display_text_range")
    private void unpackDisplayTextRange(int[] displayTextRange) {
        this.displayTextRange = Pair.of(displayTextRange[0], displayTextRange[1]);
    }

    public Pair<Integer, Integer> getDisplayTextRange() {
        return displayTextRange;
    }

    public void setDisplayTextRange(Pair<Integer, Integer> displayTextRange) {
        this.displayTextRange = displayTextRange;
    }

    public TweetEntities getEntities() {
        return entities;
    }

    public void setEntities(TweetEntities entities) {
        this.entities = entities;
    }

    public List<TweetMedia> getMedia() {
        return media;
    }

    public void setMedia(List<TweetMedia> media) {
        this.media = media;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }
}

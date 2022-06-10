package dk.kb.netarchivesuite.solrwayback.parsers.json;

import java.util.List;

/**
 * Object that contains a tweet's video variants - i.e. the objects holding the actual links to the video formats.
 * Annoyingly, this useless intermediary object had to be made in order to parse the child objects without doing it in
 * an ugly and hacky way...
 */
public class TweetVideoInfo {
    private List<TweetVideoVariant> variants;

    public TweetVideoInfo() {
    }

    public List<TweetVideoVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<TweetVideoVariant> variants) {
        this.variants = variants;
    }
}

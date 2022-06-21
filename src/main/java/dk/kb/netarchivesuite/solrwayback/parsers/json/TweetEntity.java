package dk.kb.netarchivesuite.solrwayback.parsers.json;

import org.apache.commons.lang3.tuple.Pair;

public interface TweetEntity {
    Pair<Integer, Integer> getIndices();
    void setIndices(Pair<Integer, Integer> newIndices);
}

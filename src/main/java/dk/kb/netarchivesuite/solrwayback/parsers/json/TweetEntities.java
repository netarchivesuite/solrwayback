package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Even though 'entities' also includes 'media'-objects this is parsed outside this pojo, as media will always
 * be contained inside 'extended_entities' that is on the same level as 'entities'.
 */
public class TweetEntities {
    @JsonProperty("user_mentions")
    private List<TweetMention> mentions;

    private List<TweetURL> urls;

    private List<TweetHashtag> hashtags;


    public TweetEntities() {
    }

    public List<TweetMention> getMentions() {
        return mentions;
    }

    public void setMentions(List<TweetMention> mentions) {
        this.mentions = mentions;
    }

    public List<TweetURL> getUrls() {
        return urls;
    }

    public void setUrls(List<TweetURL> urls) {
        this.urls = urls;
    }

    public List<TweetHashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<TweetHashtag> hashtags) {
        this.hashtags = hashtags;
    }
}

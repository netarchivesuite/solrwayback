package dk.kb.netarchivesuite.solrwayback.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TweetEntities {
    @JsonProperty("user_mentions")
    private List<TweetMention> mentions;

    private List<TweetUrl> urls;

    private List<TweetHashtag> hashtags;


    public TweetEntities() {
    }

    public List<TweetMention> getMentions() {
        return mentions;
    }

    public void setMentions(List<TweetMention> mentions) {
        this.mentions = mentions;
    }

    public List<TweetUrl> getUrls() {
        return urls;
    }

    public void setUrls(List<TweetUrl> urls) {
        this.urls = urls;
    }

    public List<TweetHashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<TweetHashtag> hashtags) {
        this.hashtags = hashtags;
    }
}

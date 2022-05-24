package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Tweet {
    private String quotePermalink;

    @JsonProperty("id_str")
    private String id;

    @JsonProperty("retweeted_status")
    private Tweet retweetedTweet;

    private String text;

    @JsonProperty("is_quote_status")
    private boolean hasQuote;

    @JsonProperty("quoted_status")
    private Tweet quotedTweet;

    private TweetUser user;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE MMM dd kk:mm:ss Z yyyy") // "Thu Nov 04 23:37:36 +0000 2021"
    @JsonProperty("created_at")
    private Date creationDate;

    @JsonProperty("in_reply_to_status_id_str")
    private String inReplyToTweetId;

    private String inReplyToScreenName;

    // Default to size of standard tweet if no display_text_range is found
    private Pair<Integer, Integer> displayTextRange = Pair.of(0, 140);

    // Tweet sometimes also has extended_entities at same level, but that will only contain media and not hashtags etc.
    // See #unpackMedia().
    private TweetEntities entities;

    private List<TweetMedia> media;

    @JsonProperty("extended_tweet")
    private TweetExtendedContent extendedContent;


    @JsonProperty("quoted_status_permalink")
    private void unpackQuotePermalink(Map<String, String> quotedStatusPermalinkObj) {
        quotePermalink = quotedStatusPermalinkObj.get("expanded");
    }

    @JsonProperty("display_text_range")
    private void unpackDisplayTextRange(int[] displayTextRange) {
        this.displayTextRange = Pair.of(displayTextRange[0], displayTextRange[1]);
    }

    @JsonProperty("extended_entities")
    private void unpackMedia(Map<String, List<TweetMedia>> extendedEntitiesObj) {
        this.media = extendedEntitiesObj.get("media");
    }

    public Tweet() {
    }

    public String getQuotePermalink() {
        return quotePermalink;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Tweet getRetweetedTweet() {
        return retweetedTweet;
    }

    public void setRetweetedTweet(Tweet retweetedTweet) {
        this.retweetedTweet = retweetedTweet;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean hasQuote() {
        return hasQuote;
    }

    public void setHasQuoteStatus(boolean isQuoteStatus) {
        this.hasQuote = isQuoteStatus;
    }

    public TweetUser getUser() {
        return user;
    }

    public void setUser(TweetUser user) {
        this.user = user;
    }

    public Tweet getQuotedTweet() {
        return quotedTweet;
    }

    public void setQuotedTweet(Tweet quotedTweet) {
        this.quotedTweet = quotedTweet;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getInReplyToTweetId() {
        return inReplyToTweetId;
    }

    public void setInReplyToTweetId(String inReplyToTweetId) {
        this.inReplyToTweetId = inReplyToTweetId;
    }

    public String getInReplyToScreenName() {
        return inReplyToScreenName;
    }

    public void setInReplyToScreenName(String inReplyToScreenName) {
        this.inReplyToScreenName = inReplyToScreenName;
    }

    public Pair<Integer, Integer> getDisplayTextRange() {
        return displayTextRange;
    }

    public TweetEntities getEntities() {
        return entities;
    }

    public void setEntities(TweetEntities entities) {
        this.entities = entities;
    }

    public TweetExtendedContent getExtendedContent() {
        return extendedContent;
    }

    public void setExtendedContent(TweetExtendedContent extendedContent) {
        this.extendedContent = extendedContent;
    }
}

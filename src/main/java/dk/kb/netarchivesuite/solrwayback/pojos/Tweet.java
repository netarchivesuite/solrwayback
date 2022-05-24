package dk.kb.netarchivesuite.solrwayback.pojos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class Tweet { // TODO move to meaningful package
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

    private TwitterUser user;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE MMM dd kk:mm:ss Z yyyy") // "Thu Nov 04 23:37:36 +0000 2021"
    @JsonProperty("created_at")
    private Date creationDate;

    @JsonProperty("in_reply_to_status_id_str")
    private String inReplyToTweetId;

    private String inReplyToScreenName;

    private int minTextRange;

    @JsonProperty("extended_entities")
    @JsonAlias("entities")
    private TweetEntities entities;


    public Tweet() {
    }

    @JsonProperty("quoted_status_permalink")
    private void unpackQuotePermalink(Map<String, String> quotedStatusPermalinkObj) {
        quotePermalink = quotedStatusPermalinkObj.get("expanded");
    }

    @JsonProperty("display_text_range")
    private void unpackMinTextRange(ArrayList<Integer> displayTextRange) {
        // NB: defaults to 0 if display_text_range does not exist - this is also what it should be
        minTextRange = displayTextRange.get(0);
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

    public TwitterUser getUser() {
        return user;
    }

    public void setUser(TwitterUser user) {
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

    public int getMinTextRange() {
        return minTextRange;
    }

    public TweetEntities getEntities() {
        return entities;
    }

    public void setEntities(TweetEntities entities) {
        this.entities = entities;
    }
}

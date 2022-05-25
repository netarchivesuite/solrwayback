package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.parsers.json.Tweet;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetUser;

// TODO figure out the precise purpose of this and rename
public class TwitterHelper {
    private final Tweet tweet;
    private final String crawlDate;

    public TwitterHelper(Tweet tweet, String crawlDate) {
        this.tweet = tweet;
        this.crawlDate = crawlDate;
    }

    public Tweet getMainContentTweet() {
        return tweet.isRetweet() ? tweet.getRetweetedTweet() : tweet;
    }

    public TweetUser getMainContentAuthor() {
        return getMainContentTweet().getUser();
    }
}

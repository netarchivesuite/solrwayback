package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.parsers.json.Tweet;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetEntities;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetEntity;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetMedia;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetMention;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetUser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     *
     * @param tweet
     * @return List of reply-mentions
     */
    public List<TweetMention> getReplyMentions(Tweet tweet) {
        List<TweetMention> allMentions = getContentEntities(tweet).getMentions();
        int minDisplayRange = getDisplayTextRangeMin(tweet); // tweet.getDisplayTextRange().getLeft();
        String screenNameBeingRepliedTo = tweet.getInReplyToScreenName();

        List<TweetMention> replyMentions = allMentions.stream()
                .filter(mention -> mention.getIndices().getLeft() < minDisplayRange)
                .collect(Collectors.toList());
        List<String> replyScreenNames = replyMentions.stream()
                .map(TweetMention::getScreenName)
                .collect(Collectors.toList());

        // In case of self-reply the author's screen name will not be contained in the reply mentions, so add it for clarity
        if (!replyScreenNames.contains(screenNameBeingRepliedTo)) {
            TweetMention selfMention = new TweetMention(screenNameBeingRepliedTo);
            replyMentions.add(0, selfMention);
        }
        return replyMentions;
    }

    public List<TweetMention> getContentMentions(Tweet tweet) {
        List<TweetMention> allMentions = getContentEntities(tweet).getMentions();
        int minDisplayRange = getDisplayTextRangeMin(tweet);
        return allMentions.stream()
                .filter(mention -> mention.getIndices().getLeft() >= minDisplayRange)
                .collect(Collectors.toList());
    }

    public List<String> getTweetImageURLStrings(Tweet tweet) {
        List<TweetMedia> images;
        if (tweet.getExtendedContent() != null) {
            images = tweet.getExtendedContent().getMedia();
        } else {
            images = tweet.getMedia();
        }
        // Unlike the other entities in json 'media' won't exist if no media is contained - so need to check for null
        return Optional.ofNullable(images).orElseGet(Collections::emptyList).stream()
                .map(TweetMedia::getMediaUrl)
                .collect(Collectors.toList());
    }

    public String getContentText(Tweet tweet) {
        String contentText;
        if (tweet.getExtendedContent() != null) {
            contentText = tweet.getExtendedContent().getFullText();
        } else {
            contentText = tweet.getText();
        }
        return contentText;
    }

    public int getDisplayTextRangeMin(Tweet tweet) {
        int displayTextRangeMin;
        if (tweet.getExtendedContent() != null) {
            displayTextRangeMin = tweet.getExtendedContent().getDisplayTextRange().getLeft();
        } else {
            displayTextRangeMin = tweet.getDisplayTextRange().getLeft();
        }
        return displayTextRangeMin;
    }

    public TweetEntities getContentEntities(Tweet tweet) {
        TweetEntities contentEntities;
        if (tweet.getExtendedContent() != null) {
            contentEntities = tweet.getExtendedContent().getEntities();
        } else {
            contentEntities = tweet.getEntities();
        }
        return contentEntities;
    }
}

package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.parsers.json.Tweet;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetEntities;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetMedia;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetMention;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetUser;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TwitterParsingUtils {
    private TwitterParsingUtils() {
    }

    /**
     * Gets the Tweet that contains the main content of the 'tweet' as a whole - i.e. if given tweet is just a retweet
     * we want its retweeted tweet that contains the actual content. So if no retweet, the passed arg is just returned.
     * @param tweet Tweet to get main content tweet from
     * @return Tweet containing actual content - might just return argument
     */
    public static Tweet getMainContentTweet(Tweet tweet) {
        return tweet.isRetweet() ? tweet.getRetweetedTweet() : tweet;
    }

    /**
     * Gets the author/user of the main content of a tweet. See {@link #getMainContentTweet}.
     * @param tweet Tweet to get author/user from
     * @return User that authored the main content tweet
     */
    public static TweetUser getMainContentAuthor(Tweet tweet) {
        return getMainContentTweet(tweet).getUser();
    }

    /**
     * Grabs the mentions from a parsed tweet and filters for the mentions that are part of the reply line.
     *
     * As a self-reply mention (i.e. user @example replies to @example) are not part of the parsed JSON
     * (but are part of the text), this method has the side effect of adding this 'mention'
     * if the tweet is self-replying.
     * @param tweet Tweet to get reply-mentions for
     * @return List of reply-mentions
     */
    public static List<TweetMention> getReplyMentions(Tweet tweet) {
        List<TweetMention> allMentions = getContentEntities(tweet).getMentions();
        int minDisplayRange = getDisplayTextRangeMin(tweet);
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

    /**
     * Grabs the mentions from a parsed tweet and filters for the mentions that are actually part of the tweet text
     * i.e. non-reply-mentions.
     * @param tweet Tweet to get mentions for
     * @return List of mentions that are contained in the tweets text
     */
    public static List<TweetMention> getContentMentions(Tweet tweet) {
        List<TweetMention> allMentions = getContentEntities(tweet).getMentions();
        int minDisplayRange = getDisplayTextRangeMin(tweet);
        return allMentions.stream()
                .filter(mention -> mention.getIndices().getLeft() >= minDisplayRange)
                .collect(Collectors.toList());
    }

    /**
     * Grabs the image urls from the tweet if any exist.
     *
     * The tweet-JSON can either contain media on its first level or under the 'extended_tweet' tag, and this method
     * accounts for that unlike just using {@link Tweet#getMedia()}.
     * An 'extended_entities'-tag containing media can actually also exist, but this is accounted for when parsing
     * (see e.g. {@link Tweet#unpackMedia(Map)}).
     * @param tweet The tweet to get image urls for
     * @return List of image urls in tweet or empty list if tweet contains none
     */
    public static List<String> getTweetImageURLStrings(Tweet tweet) {
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

    /**
     * Gets the text from the given tweet.
     *
     * The tweet-JSON can either contain text on its first level in the 'text'-tag or under 'extended_tweet'
     * as the 'full_text'-tag, which this method accounts for unlike just using {@link Tweet#getText()}.
     * @param tweet Tweet to get the text from
     * @return The text contained in given tweet
     */
    public static String getContentText(Tweet tweet) {
        String contentText;
        if (tweet.getExtendedContent() != null) {
            contentText = tweet.getExtendedContent().getFullText();
        } else {
            contentText = tweet.getText();
        }
        return contentText;
    }

    /**
     * Gets the start-index from the display_text_range of a tweet, which indicates where the tweet's
     * actual body text starts in the parsed tweet text.
     *
     * The tweet-JSON can either contain 'display_text_range' on its first level or under the 'extended_tweet'-tag,
     * which this method accounts for unlike just using {@link Tweet#getDisplayTextRange()}.
     * @param tweet Tweet to get the lower bound from
     * @return Index indicating start of tweet body text
     */
    public static int getDisplayTextRangeMin(Tweet tweet) {
        Pair<Integer, Integer> displayTextRange;
        if (tweet.getExtendedContent() != null) {
            displayTextRange = tweet.getExtendedContent().getDisplayTextRange();
        } else {
            displayTextRange = tweet.getDisplayTextRange();
        }
        return displayTextRange.getLeft();
    }

    /**
     * Gets the entities belonging to the given tweet.
     *
     * The tweet-JSON can either contain 'entities' on its first level or under the 'extended_tweet'-tag,
     * which this method accounts for unlike just using {@link Tweet#getDisplayTextRange()}.
     * @param tweet Tweet containing entities
     * @return Entity object containing parsed mentions, hashtags and URLs
     */
    public static TweetEntities getContentEntities(Tweet tweet) {
        TweetEntities contentEntities;
        if (tweet.getExtendedContent() != null) {
            contentEntities = tweet.getExtendedContent().getEntities();
        } else {
            contentEntities = tweet.getEntities();
        }
        return contentEntities;
    }
}

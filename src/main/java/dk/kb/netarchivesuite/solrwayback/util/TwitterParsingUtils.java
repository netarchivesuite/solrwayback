package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.parsers.json.Tweet;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetEntities;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetEntity;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetMedia;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetMention;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetUser;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetVideoVariant;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static List<TweetMention> getTextContentMentions(Tweet tweet) {
        List<TweetMention> allMentions = getContentEntities(tweet).getMentions();
        int minDisplayRange = getDisplayTextRangeMin(tweet);
        return allMentions.stream()
                .filter(mention -> mention.getIndices().getLeft() >= minDisplayRange)
                .collect(Collectors.toList());
    }

    /**
     * TODO probably merge with getTweetVideoURLStrings, so content order is preserved
     * Grabs the image urls from the tweet media if any exist.
     * @param tweet The tweet to get image urls for
     * @return List of image urls in tweet or empty list if tweet contains none
     */
    public static List<String> getTweetImageURLStrings(Tweet tweet) {
        List<TweetMedia> media = getTweetMedia(tweet);
        return media.stream()
                .filter(mediaObj -> mediaObj.getType().equals("photo"))
                .map(TweetMedia::getMediaUrl)
                .collect(Collectors.toList());
    }

    /**
     * Grabs the video urls with best bitrate from the tweet media if any exist.
     * @param tweet The tweet to get video urls for
     * @return List of video urls in tweet or empty list if tweet contains none
     */
    public static List<String> getTweetVideoURLStrings(Tweet tweet) {
        List<TweetMedia> media = getTweetMedia(tweet);
        return media.stream()
                .filter(mediaObj -> mediaObj.getType().equals("video")) // Get only video media
                .map(TweetMedia::getVideoInfo) // Get the video info object
                // Get video variant with best bitrate
                .map(videoInfo -> Collections.max(
                        videoInfo.getVariants(), Comparator.comparing(TweetVideoVariant::getBitrate)))
                .map(TweetVideoVariant::getUrl)// Get the url
                .collect(Collectors.toList());
    }

    /**
     * Get the tweet media.
     *
     * The tweet-JSON can either contain media on its first level or under the 'extended_tweet' tag, and this method
     * accounts for that unlike just using {@link Tweet#getMedia()}.
     * An 'extended_entities'-tag containing media can actually also exist, but this is accounted for when parsing
     * (see e.g. {@link Tweet#unpackMedia(Map)}).
     * @param tweet Tweet to get the media from.
     * @return List of tweet media. If no media exists return an empty list
     */
    private static List<TweetMedia> getTweetMedia(Tweet tweet) {
        List<TweetMedia> media;
        if (tweet.getExtendedContent() != null) {
            media = tweet.getExtendedContent().getMedia();
        } else {
            media = tweet.getMedia();
        }
        // Unlike the other entities in json 'media' won't exist if no media is contained - so need to check for null
        return (media != null) ? media : Collections.emptyList();
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

    /**
     * Gets the entities for the given tweet and sorts them by their indices in descending order for easy insertion
     * backwards through the tweet text.
     * @param tweet Tweet to grab the entities from.
     * @return The sorted entities in descending order.
     */
    public static List<? extends TweetEntity> getSortedEntitiesForInsertion(Tweet tweet) {
        TweetEntities entities = getContentEntities(tweet);
        // Merge hashtags, mentions and urls
        List<? extends TweetEntity> hashtags = entities.getHashtags();
        // Can't just get mentions straight up since they also contain reply mentions
        List<? extends TweetEntity> mentions = getTextContentMentions(tweet);
        List<? extends TweetEntity> test = entities.getUrls();

        List<? extends TweetEntity> allEntities = Stream.of(hashtags, mentions, test)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(entity -> entity.getIndices().getLeft()))
                .collect(Collectors.toList());

        Collections.reverse(allEntities); // Reverse to insert entities in text from end to start
        return allEntities;
    }
}

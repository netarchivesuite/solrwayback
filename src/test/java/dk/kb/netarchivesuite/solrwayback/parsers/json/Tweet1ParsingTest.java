package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import static org.junit.Assert.*;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Tweet1ParsingTest {
    private InputStream is = Tweet1ParsingTest.class.getClassLoader().getResourceAsStream("example_twitter/twitter1.json");
    private Tweet tweet;

    @Before
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        tweet = mapper.readValue(is, Tweet.class);
    }

    @Test
    public void testTweetContentParsing() {
        assertEquals("1238344291094847489", tweet.getId());
        assertNull(tweet.getQuotePermalink()); // Tweet does not have a reply
        assertNotNull(tweet.getUser());
        assertEquals("RT @Egense: Test full text with some encoding. This is an extended tweet within" +
                " a retweet, so it should cut off at the 140 char mark:\u00e5c m\u00f8 \u2026", tweet.getText());
        assertEquals(0, tweet.getFavoriteCount());
        assertEquals(0, tweet.getRetweetCount());
        assertEquals(0, tweet.getQuoteCount());
        assertEquals(0, tweet.getReplyCount());
        assertNotNull(tweet.getRetweetedTweet()); // Tweet is a retweet
        assertNull(tweet.getQuotedTweet()); // No quoted tweet
        assertEquals("Fri Mar 13 07:01:00 CET 2020", tweet.getCreationDate().toString());
        assertEquals(Pair.of(0, 140), tweet.getDisplayTextRange());
        assertNotNull(tweet.getEntities());
        assertNull(tweet.getExtendedContent());
        assertNull(tweet.getInReplyToScreenName());
        assertNull(tweet.getMedia());
    }

    @Test
    public void testEntitiesParsing() {
        TweetEntities entities = tweet.getEntities();
        assertNotNull(entities);

        // Tweet only contains mentions
        List<TweetMention> mentions = entities.getMentions();
        assertNotNull(mentions);
        assertEquals(1, mentions.size());

        TweetMention mention = mentions.get(0);
        assertEquals(Pair.of(3, 10), mention.getIndices());
        assertEquals("22695562", mention.getId());
        assertEquals("Egense", mention.getScreenName());

        assertTrue(entities.getUrls().isEmpty());
        assertTrue(entities.getHashtags().isEmpty());
    }

    @Test
    public void testTweetUserParsing() {
        TweetUser user = tweet.getUser();
        assertNotNull(user);
        assertEquals("Thomas2", user.getName());
        assertEquals("thomas2", user.getScreenName());
        assertEquals("2337958629", user.getId());
        assertEquals("Mathematician and beer drinker.", user.getDescription());
        assertEquals(740, user.getFollowersCount());
        assertEquals(635, user.getFriendsCount());
        assertEquals("http://pbs.twimg.com/profile_images/1234/H0waTByu_normal.jpg", user.getProfileImageUrl());
    }

    @Test
    public void testRetweetedContentParsing() {
        Tweet retweetedTweet = tweet.getRetweetedTweet();
        assertNull(retweetedTweet.getRetweetedTweet()); // Retweeted tweet can't contain another retweeted tweet
        assertNotNull(retweetedTweet.getExtendedContent());
        assertEquals("Test full text with some encoding. This is an extended tweet within a retweet," +
                " so it should cut off at the 140 char\u2026 https://t.co/ABCDEFGHIJ", retweetedTweet.getText());
        assertEquals(220, retweetedTweet.getFavoriteCount());
        assertEquals(19, retweetedTweet.getRetweetCount());
        assertEquals(1, retweetedTweet.getQuoteCount());
        assertEquals(11, retweetedTweet.getReplyCount());
        assertNull(retweetedTweet.getQuotedTweet());
        assertNull(retweetedTweet.getInReplyToScreenName());
        assertNotNull(retweetedTweet.getEntities());
        assertNull(retweetedTweet.getQuotePermalink());
        assertEquals("Thu Mar 12 23:35:33 CET 2020", retweetedTweet.getCreationDate().toString());
        assertEquals("1234", retweetedTweet.getId());
        assertEquals(Pair.of(0, 140), retweetedTweet.getDisplayTextRange());
        assertNull(retweetedTweet.getInReplyToTweetId());
    }

    @Test
    public void testRetweetedUserParsing() {
        TweetUser retweetedUser = tweet.getRetweetedTweet().getUser();
        assertNotNull(retweetedUser);
        assertEquals("Thomas Egense", retweetedUser.getName());
        assertEquals("Egense", retweetedUser.getScreenName());
        assertEquals("22695562", retweetedUser.getId());
        assertEquals("Description text. Tweeting about #math", retweetedUser.getDescription());
        assertEquals(42081, retweetedUser.getFollowersCount());
        assertEquals(3089, retweetedUser.getFriendsCount());
        assertEquals("http://pbs.twimg.com/profile_images/12345/-SNh6awI_normal.jpg", retweetedUser.getProfileImageUrl());
    }

    @Test
    public void testRetweetedContentExtendedContentParsing() {
        TweetExtendedContent extendedContent = tweet.getRetweetedTweet().getExtendedContent();
        assertEquals("Test full text with some encoding. This is an extended tweet within a retweet," +
                " so it should cut off at the 140 char mark:\u00e5c m\u00f8 . Also has tag+link #math" +
                " https://t.co/ABCDEFGHIJ https://t.co/1MAGEUR7Y0", extendedContent.getFullText());
        assertEquals(Pair.of(0, 176), extendedContent.getDisplayTextRange());

        TweetEntities entities = extendedContent.getEntities();
        assertNotNull(entities);

        List<TweetHashtag> hashtags = entities.getHashtags();
        assertEquals(1, hashtags.size());
        TweetHashtag hashtag = hashtags.get(0);
        assertEquals(Pair.of(147, 152), hashtag.getIndices());
        assertEquals("math", hashtag.getText());

        List<TweetURL> urls = entities.getUrls();
        assertEquals(1, urls.size());
        TweetURL url = urls.get(0);
        assertEquals(Pair.of(153, 176), url.getIndices());
        assertEquals("https://example.com/example/123", url.getExpandedUrl());
        assertEquals("https://example.com/example\u2026", url.getDisplayUrl());

        assertTrue(entities.getMentions().isEmpty());

        assertEquals(1, extendedContent.getMedia().size());
        assertEquals("https://twitter.com/ThomasEgense/status/1234/photo/1", extendedContent.getMedia().get(0).getExpandedUrl());
    }
}

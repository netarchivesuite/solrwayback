package dk.kb.netarchivesuite.solrwayback.parsers.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// TODO refactor to be more like Tweet1ParsingTest
public class TwitterParsingTest {
	private static ObjectMapper mapper;
	private static Tweet tweet;

	@BeforeClass
	public static void jacksonSetup() {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	// TODO non-extended_tweet stuff is not tested
	@Test
	public void testNonRetweet() throws Exception {
		String jsonContent = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter2.json")));
		tweet = mapper.readValue(jsonContent, Tweet.class);

		assertFalse(tweet.isRetweet());
		assertFalse(tweet.hasQuote());
		assertEquals("Test with links https://t.co/ABC123DEFG filler text for no reason but to fill\n" +
				"There is even one link in this tweet https://t.co/W1ldUr7w0W. The text goes even further beyond what" +
				" is thought possible! What is this math? https://t.co/rABCDEFGHI #math  https://t.co/ABCDEFGHIJ",
				tweet.getExtendedContent().getFullText());
		assertEquals("Fri Mar 13 00:03:52 CET 2020", tweet.getCreationDate().toString());
		assertEquals(0, tweet.getQuoteCount());
		assertEquals(1, tweet.getFavoriteCount());
		assertEquals(2, tweet.getReplyCount());
		assertEquals(3, tweet.getRetweetCount());

		TweetUser user = tweet.getUser();
		assertEquals("2600310521", user.getId());
		assertEquals("Thomas Egense", user.getName());
		assertEquals("ThomasEgense", user.getScreenName());
		assertEquals("Here is another description", user.getDescription());
		assertEquals("http://pbs.twimg.com/profile_images/1234/vHux2YUz_normal.jpeg", user.getProfileImageUrl());
		assertEquals(1227, user.getFollowersCount());
		assertEquals(1121, user.getFriendsCount());
		assertFalse(user.isVerified());

		TweetEntities entities = tweet.getExtendedContent().getEntities();
		List<TweetHashtag> hashtags = entities.getHashtags();
		assertEquals(1, hashtags.size());
		assertEquals("math", hashtags.get(0).getText());
		assertEquals(hashtags.get(0).getIndices(), Pair.of(243,248));

		assertEquals(0, entities.getMentions().size());

		List<TweetURL> urls = entities.getUrls();
		assertEquals(3, urls.size());
		assertEquals("twitter.com/i/web/status/1\u2026", urls.get(0).getDisplayUrl());
		assertEquals("https://twitter.com/i/web/status/1234", urls.get(0).getExpandedUrl());
		assertEquals(urls.get(0).getIndices(), Pair.of(16,39));
		assertEquals("twitter.com/i/web/status/1\u2026", urls.get(1).getDisplayUrl());
		assertEquals("https://twitter.com/i/web/status/1234", urls.get(1).getExpandedUrl());
		assertEquals(urls.get(1).getIndices(), Pair.of(115,138));
		assertEquals("thomas-egense.dk/math/", urls.get(2).getDisplayUrl());
		assertEquals("http://thomas-egense.dk/math/", urls.get(2).getExpandedUrl());
		assertEquals(urls.get(2).getIndices(), Pair.of(219,242));

		List<TweetMedia> media = tweet.getExtendedContent().getMedia();
		assertEquals(1, media.size());
		assertEquals("http://pbs.twimg.com/media/ABCDE.jpg", media.get(0).getMediaUrl());
	}

	// TODO probably move to new class
	@Test
	public void testRetweet() throws Exception {
		String jsonContent = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter1.json")));
		tweet = mapper.readValue(jsonContent, Tweet.class);

		assertEquals("Fri Mar 13 07:01:00 CET 2020", tweet.getCreationDate().toString());

		TweetUser user = tweet.getUser();
		assertEquals("2337958629", user.getId());
		assertEquals("Thomas2", user.getName());
		assertEquals("thomas2", user.getScreenName());
		assertEquals("Mathematician and beer drinker.", user.getDescription());
		assertEquals("http://pbs.twimg.com/profile_images/1234/H0waTByu_normal.jpg", user.getProfileImageUrl());
		assertEquals(740, user.getFollowersCount());
		assertEquals(635, user.getFriendsCount());
		assertFalse(user.isVerified());

		assertTrue(tweet.isRetweet());
		Tweet retweetedTweet = tweet.getRetweetedTweet();
		assertEquals("Thu Mar 12 23:35:33 CET 2020", retweetedTweet.getCreationDate().toString());
		assertEquals("Test full text with some encoding. This is an extended tweet within a retweet, so it" +
				" should cut off at the 140 char mark:åc mø . Also has tag+link #math https://t.co/ABCDEFGHIJ" +
				" https://t.co/1MAGEUR7Y0", retweetedTweet.getExtendedContent().getFullText());
		assertEquals(1, retweetedTweet.getQuoteCount());
		assertEquals(220, retweetedTweet.getFavoriteCount());
		assertEquals(11, retweetedTweet.getReplyCount());
		assertEquals(19, retweetedTweet.getRetweetCount());

		TweetUser retweetedUser = retweetedTweet.getUser();
		assertEquals("22695562", retweetedUser.getId());
		assertEquals("Thomas Egense", retweetedUser.getName());
		assertEquals("Egense", retweetedUser.getScreenName());
		assertEquals("Description text. Tweeting about #math", retweetedUser.getDescription());
		assertEquals("http://pbs.twimg.com/profile_images/12345/-SNh6awI_normal.jpg", retweetedUser.getProfileImageUrl());
		assertEquals(42081, retweetedUser.getFollowersCount());
		assertEquals(3089, retweetedUser.getFriendsCount());
		assertTrue(retweetedUser.isVerified());

		TweetEntities entities = retweetedTweet.getExtendedContent().getEntities();
		List<TweetHashtag> hashtags = entities.getHashtags();
		assertEquals(1, hashtags.size());
		assertEquals("math", hashtags.get(0).getText());
		assertEquals(Pair.of(147, 152), hashtags.get(0).getIndices());

		assertEquals(0, entities.getMentions().size());

		List<TweetURL> urls = entities.getUrls();
		assertEquals(1, urls.size());
		assertEquals("https://example.com/example\u2026", urls.get(0).getDisplayUrl());
		assertEquals("https://example.com/example/123", urls.get(0).getExpandedUrl());
		assertEquals(Pair.of(153, 176), urls.get(0).getIndices());
	}

	// TODO probably move to new class
	@Test
	public void testQuoteTweet() throws IOException {
		String jsonContent = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter3.json")));
		tweet = mapper.readValue(jsonContent, Tweet.class);

		assertEquals("Fri Jul 16 16:03:03 CEST 2021", tweet.getCreationDate().toString());

		assertTrue(tweet.isRetweet());
		assertEquals("Fri Jul 16 13:15:06 CEST 2021", tweet.getRetweetedTweet().getCreationDate().toString());

		assertTrue(tweet.hasQuote());
		Tweet quotedTweet = tweet.getRetweetedTweet().getQuotedTweet(); // Should just as well be able to use quoted tweet directly I believe
		assertEquals("Original tweet text that is being quoted goes here. The person who quoted this does not" +
				" agree with its statement. Such is Twitter. \nRead me https://t.co/GhIjklMnOP\n#HASHtg" +
				" #AnotherTagHere #SomeHashtag https://t.co/a1bc2D3EfG", quotedTweet.getExtendedContent().getFullText());
		assertEquals("Fri Jul 16 09:45:12 CEST 2021", quotedTweet.getCreationDate().toString());

		TweetUser quotedUser = quotedTweet.getUser();
		assertEquals("22222222", quotedUser.getId());
		assertEquals("CoolUser", quotedUser.getName());
		assertEquals("cool_user", quotedUser.getScreenName());
		assertEquals("This person is very interested in politics and boring stuff. Very interesting." +
				" Have a link to their page no one cares about: https://www.example.com", quotedUser.getDescription());
		assertEquals("http://pbs.twimg.com/profile_images/0101010101010101010/s-kiJ9xn_normal.png", quotedUser.getProfileImageUrl());
		assertEquals(5723923, quotedUser.getFollowersCount());
		assertEquals(248, quotedUser.getFriendsCount());
		assertTrue(quotedUser.isVerified());

		TweetEntities entities = quotedTweet.getExtendedContent().getEntities();
		assertEquals(0, entities.getMentions().size());

		List<TweetHashtag> hashtags = entities.getHashtags();
		assertEquals(3, hashtags.size());
		assertEquals(Pair.of(164, 171), hashtags.get(0).getIndices());
		assertEquals("HASHtg", hashtags.get(0).getText());
		assertEquals(Pair.of(172, 187), hashtags.get(1).getIndices());
		assertEquals("AnotherTagHere", hashtags.get(1).getText());
		assertEquals(Pair.of(188, 200), hashtags.get(2).getIndices());
		assertEquals("SomeHashtag", hashtags.get(2).getText());

		List<TweetURL> urls = entities.getUrls();
		assertEquals(1, urls.size());
		assertEquals(Pair.of(140, 163), urls.get(0).getIndices());
		assertEquals("abcde.dk/fghijk", urls.get(0).getDisplayUrl());
		assertEquals("https://abcde.dk/fghijk", urls.get(0).getExpandedUrl());

		List<TweetMedia> media = quotedTweet.getExtendedContent().getMedia();
		assertEquals(1, media.size());
		assertEquals("http://pbs.twimg.com/media/M1EdIauRlHErEO-.jpg", media.get(0).getMediaUrl());
	}
}

package dk.kb.netarchivesuite.solrwayback.parsers;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TwitterParserTest {

	@Test
	public void testNotRetweet() throws Exception {
		String content = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter2.json")));
		TwitterParser2 tweet = new TwitterParser2(content);

		assertFalse(tweet.isRetweet());
		assertFalse(tweet.hasQuote());
		assertEquals("Test with links https://t.co/ABC123DEFG filler text for no reason but to fill\n" +
				"There is even one link in this tweet https://t.co/W1ldUr7w0W. The text goes even further beyond what" +
				" is thought possible! What is this math? https://t.co/rABCDEFGHI #math  https://t.co/ABCDEFGHIJ",
				tweet.getText());

		assertEquals("Fri Mar 13 00:03:52 CET 2020", tweet.getCreatedDate().toString());
		assertEquals(2600310521L, tweet.getUserID());
		assertEquals("Thomas Egense", tweet.getUserName());
		assertEquals("ThomasEgense", tweet.getUserScreenName());
		assertEquals("Here is another description", tweet.getUserDescription());
		assertEquals("http://pbs.twimg.com/profile_images/1234/vHux2YUz_normal.jpeg", tweet.getUserProfileImage());
		assertEquals(1227, tweet.getUserFollowersCount());
		assertEquals(1121, tweet.getUserFriendsCount());
		assertFalse(tweet.isUserVerified());

		assertEquals(0, tweet.getQuoteCount());
		assertEquals(1, tweet.getLikeCount());
		assertEquals(2, tweet.getReplyCount());
		assertEquals(3, tweet.getRetweetCount());

		assertEquals(1, tweet.getHashtags().size());
		assertTrue(tweet.getHashtags().containsValue("#math"));
		assertEquals(0, tweet.getMentions().size());
		assertEquals(3, tweet.getURLs().size());
		Pair<Integer, Integer> firstURL = Pair.of(16, 39);
		assertTrue(tweet.getURLs().containsKey(firstURL));
		assertEquals("https://twitter.com/i/web/status/1234|twitter.com/i/web/status/1\u2026", tweet.getURLs().get(firstURL));
		Pair<Integer, Integer> secondURL = Pair.of(115, 138);
		assertTrue(tweet.getURLs().containsKey(secondURL));
		assertEquals("https://twitter.com/i/web/status/1234|twitter.com/i/web/status/1\u2026", tweet.getURLs().get(secondURL));
		Pair<Integer, Integer> thirdURL = Pair.of(219, 242);
		assertTrue(tweet.getURLs().containsKey(thirdURL));
		assertEquals("http://thomas-egense.dk/math/|thomas-egense.dk/math/", tweet.getURLs().get(thirdURL));

		assertEquals(1, tweet.getImageURLStrings().size());
		assertEquals("http://pbs.twimg.com/media/ABCDE.jpg", tweet.getImageURLStrings().iterator().next());
	}

	@Test
	public void testIsRetweet() throws Exception {
		String content = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter1.json")));
		TwitterParser2 tweet = new TwitterParser2(content);

		assertTrue(tweet.isRetweet());
		assertEquals("Test full text with some encoding. This is an extended tweet within a retweet, so it" +
				" should cut off at the 140 char mark:åc mø . Also has tag+link #math https://t.co/ABCDEFGHIJ" +
				" https://t.co/1MAGEUR7Y0", tweet.getText());
		// Test retweet time and user
		assertEquals("Fri Mar 13 07:01:00 CET 2020", tweet.getCreatedDate().toString());
		assertEquals(2337958629L, tweet.getUserID());
		assertEquals("Thomas2", tweet.getUserName());
		assertEquals("thomas2", tweet.getUserScreenName());
		assertEquals("Mathematician and beer drinker.", tweet.getUserDescription());
		assertEquals("http://pbs.twimg.com/profile_images/1234/H0waTByu_normal.jpg", tweet.getUserProfileImage());
		assertEquals(740, tweet.getUserFollowersCount());
		assertEquals(635, tweet.getUserFriendsCount());
		assertFalse(tweet.isUserVerified());

		// Test original tweet date and retweeted user
		assertEquals("Thu Mar 12 23:35:33 CET 2020", tweet.getRetweetCreatedDate().toString());
		assertEquals(22695562L, tweet.getRetweetUserID());
		assertEquals("Thomas Egense", tweet.getRetweetUserName());
		assertEquals("Egense", tweet.getRetweetUserScreenName());
		assertEquals("Description text. Tweeting about #math", tweet.getRetweetUserDescription());
		assertEquals("http://pbs.twimg.com/profile_images/12345/-SNh6awI_normal.jpg", tweet.getRetweetUserProfileImage());
		assertEquals(42081, tweet.getRetweetUserFollowersCount());
		assertEquals(3089, tweet.getRetweetUserFriendsCount());
		//assertTrue(tweet.isRetweetUserVerified());

		assertEquals(1, tweet.getQuoteCount());
		assertEquals(220, tweet.getLikeCount());
		assertEquals(11, tweet.getReplyCount());
		assertEquals(19, tweet.getRetweetCount());

		assertEquals(1, tweet.getHashtags().size());
		Pair<Integer, Integer> mathHashtagIndices = Pair.of(147, 152);
		assertTrue(tweet.getHashtags().containsKey(mathHashtagIndices));
		assertEquals("#math", tweet.getHashtags().get(mathHashtagIndices));
		assertEquals(0, tweet.getMentions().size());
		assertEquals(1, tweet.getURLs().size());
		Pair<Integer, Integer> urlIndices = Pair.of(153, 176);
		assertTrue(tweet.getURLs().containsKey(urlIndices));
		assertEquals("https://example.com/example/123|https://example.com/example\u2026", tweet.getURLs().get(urlIndices));
	}

	@Test
	public void testQuoteTweet() throws IOException {
		String content = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter3.json")));
		TwitterParser2 tweet = new TwitterParser2(content);

		assertTrue(tweet.isRetweet());
		assertTrue(tweet.hasQuote());
		assertEquals("Original tweet text that is being quoted goes here. The person who quoted this does not" +
				" agree with its statement. Such is Twitter. \nRead me https://t.co/GhIjklMnOP\n#HASHtg" +
				" #AnotherTagHere #SomeHashtag https://t.co/a1bc2D3EfG", tweet.getQuoteText());
		assertEquals("Fri Jul 16 16:03:03 CEST 2021", tweet.getCreatedDate().toString());
		assertEquals("Fri Jul 16 13:15:06 CEST 2021", tweet.getRetweetCreatedDate().toString());
		assertEquals("Fri Jul 16 09:45:12 CEST 2021", tweet.getQuoteCreatedDate().toString());
		assertEquals(22222222, tweet.getQuoteUserID());
		assertEquals("CoolUser", tweet.getQuoteUserName());
		assertEquals("cool_user", tweet.getQuoteUserScreenName());
		assertEquals("This person is very interested in politics and boring stuff. Very interesting." +
				" Have a link to their page no one cares about: https://www.example.com", tweet.getQuoteUserDescription());
		assertEquals("http://pbs.twimg.com/profile_images/0101010101010101010/s-kiJ9xn_normal.png", tweet.getQuoteUserProfileImage());
		assertEquals(5723923, tweet.getQuoteUserFollowersCount());
		assertEquals(248, tweet.getQuoteUserFriendsCount());

		assertEquals(0, tweet.getQuoteMentions().size());

		assertEquals(3, tweet.getQuoteHashtags().size());
		Pair<Integer, Integer> firstTag = Pair.of(164, 171);
		assertTrue(tweet.getQuoteHashtags().containsKey(firstTag));
		assertEquals("#HASHtg", tweet.getQuoteHashtags().get(firstTag));
		Pair<Integer, Integer> secondTag = Pair.of(172, 187);
		assertTrue(tweet.getQuoteHashtags().containsKey(secondTag));
		assertEquals("#AnotherTagHere", tweet.getQuoteHashtags().get(secondTag));
		Pair<Integer, Integer> thirdTag = Pair.of(188, 200);
		assertTrue(tweet.getQuoteHashtags().containsKey(thirdTag));
		assertEquals("#SomeHashtag", tweet.getQuoteHashtags().get(thirdTag));
		//assertTrue(tweet.isQuoteUserVerified());

		assertEquals(1, tweet.getQuoteURLs().size());
		Pair<Integer, Integer> url = Pair.of(140, 163);
		assertTrue(tweet.getQuoteURLs().containsKey(url));
		assertEquals("https://abcde.dk/fghijk|abcde.dk/fghijk", tweet.getQuoteURLs().get(url));

		assertEquals(1, tweet.getQuoteImageURLStrings().size());
		assertEquals("http://pbs.twimg.com/media/M1EdIauRlHErEO-.jpg", tweet.getQuoteImageURLStrings().iterator().next());
	}
}

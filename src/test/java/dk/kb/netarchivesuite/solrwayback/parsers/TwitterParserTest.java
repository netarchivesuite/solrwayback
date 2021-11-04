package dk.kb.netarchivesuite.solrwayback.parsers;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.junit.Test;

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
		assertEquals("Test text with link https://t.co/ABC123 filler text for no reason but to fill\n" +
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
		assertEquals(2, tweet.getURLs().size());
		assertTrue(tweet.getURLs().containsValue("https://twitter.com/i/web/status/1234|twitter.com/i/web/status/1\u2026"));
		assertTrue(tweet.getURLs().containsValue("http://thomas-egense.dk/math/|thomas-egense.dk/math/"));
		assertEquals(1, tweet.getImageURLStrings().size());
		assertEquals("http://pbs.twimg.com/media/ABCDE.jpg", tweet.getImageURLStrings().iterator().next());
	}

	@Test
	public void testIsRetweet() throws Exception {
		String content = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter1.json")));
		TwitterParser2 tweet = new TwitterParser2(content);

		//TODO (encoding test) assertEquals("RT @Test: Test text with some encoding:å ø …  ",tweet.getText());
		assertEquals("Thomas2",tweet.getUserName());
		assertEquals("2337958629",tweet.getUserID());
		assertEquals(220,tweet.getLikeCount());
		assertEquals(11,tweet.getReplyCount());
		assertEquals(19,tweet.getRetweetCount());
		assertTrue(tweet.isRetweet());
		assertEquals("Fri Mar 13 07:01:00 CET 2020",tweet.getCreatedDate().toString());    

		assertEquals(1,tweet.getHashtags().size());
		assertTrue(tweet.getHashtags().containsValue("math"));
		System.out.println(tweet.getHashtags());
		//System.out.println(tweet.getImageUrlsList());        
	}

	//TODO when proper hashtag replacement by offset has been implemented
	/*
	  public static String replaceHashTags(String text, HashSet<String> tags) {
    	  String searchUrl = "http://localhost/";
    	  String otherSearchParams=" AND type%3A\"Twitter Tweet\"&start=0&filter=&imgsearch=false&imggeosearch=false&grouping=false"; //TODO frontend fix so all other params not needed	  
    	  for (String tag : tags) {
            String link = searchUrl+"?query=keywords%3A"+tag+otherSearchParams;	
    	    String replaceText = " <span><a href='"+link+"'>#"+tag+"</a></span> ";     
            if (text.endsWith(tag)) {
              text=text.replaceAll(" #"+tag, replaceText); //Replace if last in text. (no trailing white-space).
            }
            else {
    	      text=text.replaceAll(" #"+tag+ " ", replaceText); //This will not find the tag if it is last. Need space not to replace within tags. Etc. #covid #covid19 
            }	  
    	  }
    	  return text;	  	  
      }
	 */

}

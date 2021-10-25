package dk.kb.netarchivesuite.solrwayback.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;


import org.junit.Test;


public class TwitterParserTest {




	@Test
	public void testNotRetweet() throws Exception {    

		String content = new String(Files.readAllBytes(Paths.get("/home/teg/workspace/solrwayback/src/test/resources/example_twitter/twitter2.json")));


		TwitterParser2 tweet = new TwitterParser2(content);

		assertEquals("Test full text with tag and link: #math https://t.co/ABCDE",tweet.getText());	    

		assertEquals("Thomas Egense",tweet.getUserName());
		assertEquals("ThomasEgense",tweet.getUserScreenName());
		assertEquals("2600310521",tweet.getUserId());
		
		assertEquals(1,tweet.getLikeCount());
		assertEquals(2,tweet.getReplyCount());
		assertEquals(3,tweet.getRetweetCount());
		assertEquals(false,tweet.isRetweet());

		assertEquals("Fri Mar 13 00:03:52 CET 2020",tweet.getCreatedDate().toString());    

		assertEquals(1,tweet.getHashTags().size());
		assertTrue(tweet.getHashTags().contains("math"));

		assertEquals(1,tweet.getImageUrlsList().size());
		assertEquals("http://pbs.twimg.com/media/ABCDE.jpg",tweet.getImageUrlsList().iterator().next());	    	   	    
	}

	@Test
	public void testIsRetweet() throws Exception {    

		String content = new String(Files.readAllBytes(Paths.get("/home/teg/workspace/solrwayback/src/test/resources/example_twitter/twitter1.json")));
		TwitterParser2 tweet = new TwitterParser2(content);

		//TODO (encoding test) assertEquals("RT @Test: Test text with some encoding:å ø …  ",tweet.getText());
		assertEquals("Thomas2",tweet.getUserName());
		assertEquals("2337958629",tweet.getUserId());
		assertEquals(220,tweet.getLikeCount());
		assertEquals(11,tweet.getReplyCount());
		assertEquals(19,tweet.getRetweetCount());
		assertEquals(true,tweet.isRetweet());
		assertEquals("Fri Mar 13 07:01:00 CET 2020",tweet.getCreatedDate().toString());    

		assertEquals(1,tweet.getHashTags().size());
		assertTrue(tweet.getHashTags().contains("math"));
		System.out.println(tweet.getHashTags());
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

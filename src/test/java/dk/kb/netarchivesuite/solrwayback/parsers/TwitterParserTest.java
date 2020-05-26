package dk.kb.netarchivesuite.solrwayback.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class TwitterParserTest {
  
	
/*
	  @Test
	     public void test2() throws Exception {    
	     
	    String content = new String(Files.readAllBytes(Paths.get("/home/teg/workspace/solrwayback/src/test/resources/example_twitter/delete.json")));
	   
	    TwitterParser2 p = new TwitterParser2(content);
	    System.out.println("author:"+p.getAuthor());
	    System.out.println("text:"+p.getText());
	    System.out.println("images"+p.getImageUrlsList());
	    
	  }
  */
	    
	    
	
	  @Test
	     public void testNotRetweet() throws Exception {    
	     
	    String content = new String(Files.readAllBytes(Paths.get("/home/teg/workspace/solrwayback/src/test/resources/example_twitter/twitter2.json")));
	    
	    
	    TwitterParser2 tweet = new TwitterParser2(content);

	    assertEquals("Test full text with tag and link: #math https://t.co/ABCDE",tweet.getText());	    
	    assertEquals("Thomas Egense",tweet.getAuthor());
	    assertEquals(0,tweet.getNumberOfLikes());
	    assertEquals(0,tweet.getNumberOfReplies());
	    assertEquals(0,tweet.getNumberOfRetweets());
	    assertEquals(false,tweet.isRetweet());
	    
	    assertEquals("Fri Mar 13 00:03:52 CET 2020",tweet.getCreatedDate().toString());    
	    System.out.println(tweet.getHashTags()); //TODO
	    assertEquals(1,tweet.getImageUrlsList().size());
	    assertEquals("http://pbs.twimg.com/media/ABCDE.jpg",tweet.getImageUrlsList().iterator().next());	    	   	    
	  }
	
	  @Test
     public void testIsRetweet() throws Exception {    
     /*
     * This is just test code to load the json. When used only a single json document will be parsed at a time.
     * 
     */
    String content = new String(Files.readAllBytes(Paths.get("/home/teg/workspace/solrwayback/src/test/resources/example_twitter/twitter1.json")));
    
    
    TwitterParser2 tweet = new TwitterParser2(content);

    assertEquals("RT @Test: Test text with some encoding:å ø …  ",tweet.getText());
    assertEquals("Thomas2",tweet.getAuthor());
    assertEquals(0,tweet.getNumberOfLikes());
    assertEquals(0,tweet.getNumberOfReplies());
    assertEquals(0,tweet.getNumberOfRetweets());
    assertEquals(true,tweet.isRetweet());
    assertEquals("Fri Mar 13 07:01:00 CET 2020",tweet.getCreatedDate().toString());    
    System.out.println(tweet.getHashTags());
    //System.out.println(tweet.getImageUrlsList());        
  }

}

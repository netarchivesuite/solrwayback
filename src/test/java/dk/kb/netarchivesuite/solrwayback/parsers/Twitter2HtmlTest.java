package dk.kb.netarchivesuite.solrwayback.parsers;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class Twitter2HtmlTest {
	  @Test
	     public void testReplaceTags() throws Exception {    	     		  
	     
		  //First load and parse a tweet
		 String content = new String(Files.readAllBytes(Paths.get("/home/teg/workspace/solrwayback/src/test/resources/example_twitter/twitter2.json")));
	     TwitterParser2 p = new TwitterParser2(content);   	     	     
	     
	     

	     //Test before text. Text has hashtag #math
	     String before = p.getText();	     
	     String expectedBefore="Test full text with tag and link: #math https://t.co/ABCDE";
	     assertEquals(expectedBefore,before);
	     
	     //Test replace hashtags with links
	     String solrwaybackBaseUrl="http://solrwayback/";	     
	     String replacedText= Twitter2Html.replaceHashTags(solrwaybackBaseUrl, p.getText(), p.getHashTags());
	     String expectedAfter ="Test full text with tag and link: <span><a href='http://solrwayback/?query=keywords%3Amath AND type%3A\"Twitter Tweet\"&start=0&filter=&imgsearch=false&imggeosearch=false&grouping=false'>#math</a></span> https://t.co/ABCDE";
	     assertEquals(expectedAfter, replacedText);
	      
	  }

	  
	
}

package dk.kb.netarchivesuite.solrwayback.parsers;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TwitterParserTest {
  
  
  public static void main(String[] args) throws Exception{
    
    /*
     * This is just test code to load the json. When used only a single json document will be parsed at a time.
     * 
     */
    String content = new String(Files.readAllBytes(Paths.get("/home/teg/workspace/twitter/twitter6.txt")));
    
    TwitterParser tweet = new TwitterParser(content);
    System.out.println(tweet.getText());
    System.out.println(tweet.getHashTagsList());
    System.out.println(tweet.getImageUrlsList());
    System.out.println(tweet.getAuthor());
    System.out.println(tweet.getCreateDate());
    System.out.println("likes:"+tweet.getNumberOfLikes());
    System.out.println("retweets:"+tweet.getNumberOfRetweets());
      
    
    
  }

}

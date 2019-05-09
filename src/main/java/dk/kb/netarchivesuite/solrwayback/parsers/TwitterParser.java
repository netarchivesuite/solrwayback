package dk.kb.netarchivesuite.solrwayback.parsers;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitterParser {

  private static final Logger log = LoggerFactory.getLogger(TwitterParser.class);
  
  private String author;
  private Date createDate;
  private ArrayList<String> imageUrlsList;
  private ArrayList<String> hashTagsList;
  private String text;
  private String userImage;
  private int numberOfLikes = 0 ;//favorites
  private int numberOfRetweets = 0;
  private int numberOfReplies = 0; // mentions 
  private boolean retweet = false;
  
  public TwitterParser(String json) throws Exception{
    log.info("parsing twitter json");
    
    imageUrlsList = new  ArrayList<String>();
    hashTagsList = new  ArrayList<String>();

    JSONObject full = new JSONObject(json);                
    
    
    JSONObject extended =null;
    
    if (full.has("extended_entities")) {
      extended = full.getJSONObject("extended_entities");
      log.info("has extended entities");
    }
    
    
    this.text="";

    if (full.has("full_text") ){
      //System.out.println("fulltext case");
      this.text = full.getString("full_text");
    }else if(extended != null && extended.has("full_text")){
      this.text=extended.getString("full_text");      
    }        
    else{
      //System.out.println("text case");
      this.text =full.getString("text"); //legacy 
    }

    //Text can be overruled again under retweet

    //Likes
    if (full.has("favorite_count")){
      numberOfLikes = full.getInt("favorite_count");
    }

    //retweets
    if (full.has("retweet_count")){
      numberOfRetweets = full.getInt("retweet_count");
    }
    
   
    
    //TODO rewrite to JSONUtil
    JSONObject entities; // Getting the entities require many special cases. Sometimes they are double, need to read into specification

    if (full.has("retweeted_status")) {
     this.retweet=true;
      log.info("has retweeted_status");
      JSONObject retweet = full.getJSONObject("retweeted_status");
      this.text= retweet.getString("text");
      if (retweet.has("extended_entities")){
        entities = retweet.getJSONObject("extended_entities");
        log.info("retweet extended entities");
      }
      else{
        log.info("retweet normal entities");
        entities = retweet.getJSONObject("entities");
      }           
    }else if(extended != null && extended.has("entities")){
      entities = extended.getJSONObject("entities");      
      log.info("extended entities");
    }
    else if (full.has("extended_tweet")){     
      entities = full.getJSONObject("extended_tweet").getJSONObject("entities");            
      log.info("normal from extended tweet");
    }    
    else if (full.has("extended_entities")){     
      entities = full.getJSONObject("extended_entities");            
      log.info("extended entities (from full)");
    }
    else if (full.has("entities")){     
      entities = full.getJSONObject("entities");            
      log.info("entities (from full)");
    }
    else{
      throw new Exception("could not find entities on twitter JSON");
    }

    
    //media(images), not always there.
    if (entities.has("media")){
      JSONArray media = entities.getJSONArray("media");   
      for (int i = 0;i<media.length();i++){  //images
        JSONObject medie= media.getJSONObject(i);

        String type =  medie.getString("type");
        if ("photo".equals(type)){
          String imageUrl =  medie.getString("media_url");
          imageUrlsList.add(imageUrl);                 
        }
      }               
    }

    //Not sure always to take hash tags here
    JSONArray hashTags = full.getJSONObject("entities").getJSONArray("hashtags");

    for (int i = 0;i<hashTags.length();i++){ //keywords
      String tag =  ((JSONObject) hashTags.get(i)).getString("text");
      hashTagsList.add(tag);      
    }

    JSONObject user= full.getJSONObject("user"); 
    this.author = user.getString("name");    
    this.userImage = user.getString("profile_image_url");


    //Format Fri Mar 02 10:26:13 +0000 2018
    String created_at_str = full.getString("created_at");

    DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss Z yyyy", Locale.ENGLISH);
    Date created_at =  df.parse(created_at_str);        
    this.createDate=created_at;

    //TODO
    //set language/content_type ?            

  }

  public String getAuthor() {
    return author;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public ArrayList<String> getImageUrlsList() {
    return imageUrlsList;
  }

  public ArrayList<String> getHashTagsList() {
    return hashTagsList;
  }

  public String getText() {
    return text;
  }

  public int getNumberOfLikes() {
    return numberOfLikes;
  }

  public void setNumberOfLikes(int numberOfLikes) {
    this.numberOfLikes = numberOfLikes;
  }

  public int getNumberOfReplies() {
    return numberOfReplies;
  }

  public void setNumberOfReplies(int numberOfReplies) {
    this.numberOfReplies = numberOfReplies;
  }

  public int getNumberOfRetweets() {
    return numberOfRetweets;
  }

  public String getUserImage() {
    return userImage;
  }

  public boolean isRetweet() {
    return retweet;
  }


}


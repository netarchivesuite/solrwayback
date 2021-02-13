package dk.kb.netarchivesuite.solrwayback.parsers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;

public class TwitterParser2 {

	private static final Logger log = LoggerFactory.getLogger(TwitterParser2.class);

	private JSONObject twitterJson;

	private String author;
	private String screenName;
	private String text;
	private boolean retweet = false;
	private Date createdDate;
	private String profileImage;
	private String mentions;
	private String userDescription;
	private boolean verified;
    private String userBackGroundImage;  //TODO both http and https version
	
	private HashSet<String> imageUrlsList;
	private HashSet<String> hashTags;

	private int numberOfLikes = 0;// favorites
	private int numberOfRetweets = 0;
	private int numberOfReplies = 0; // mentions ?
	private int numberOfFollows = 0;
	private int numberOfFriends = 0;

	public TwitterParser2(String twitterJsonString) {
		this.twitterJson = new JSONObject(twitterJsonString);

		this.author = JsonUtils.getValue(twitterJson, "user.name"); // always exists
		this.screenName = JsonUtils.getValue(twitterJson, "user.screen_name");
		this.profileImage = JsonUtils.getValue(twitterJson, "user.profile_image_url"); // always exists
		this.userDescription = JsonUtils.getValue(twitterJson, "user.description"); // always exists

		this.numberOfFollows = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.followers_count"));
		this.numberOfLikes = Integer.parseInt(JsonUtils.getValue(twitterJson, "favorite_count"));
		this.numberOfRetweets = Integer.parseInt(JsonUtils.getValue(twitterJson, "retweet_count"));
		this.numberOfFriends = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.friends_count"));
		this.verified = Boolean.parseBoolean(JsonUtils.getValue(twitterJson, "user.verified"));
		

		HashSet<String> hashTagsvalues = new HashSet<String>();
		JsonUtils.addAllValues(twitterJson, hashTagsvalues, "extended_tweet.entities.hashtags[].text");
		JsonUtils.addAllValues(twitterJson, hashTagsvalues, "entities.hashtags[].text");
		hashTags = hashTagsvalues;
		
		HashSet<String> media_urls = new HashSet<String>();
		//TODO also HTTPs version?
		JsonUtils.addAllValues(twitterJson,  media_urls , "retweeted_status.extended_tweet.entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson,  media_urls , "extended_tweet.entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson,  media_urls , "entities.media[].media_url");
		imageUrlsList=media_urls;
						
		this.text=JsonUtils.getValueIfExistsByPriority(twitterJson, "extended_tweet.full_text", "text");
		
		
		// getValueIfExistsByPriority(twitterJson,"entities.user_mentions[].screen_name",
		// "extended_tweet.entities.user_mentions[].screen_name");

		if (twitterJson.has("retweeted_status")) {
			this.retweet = true;
		}

		String createdAtStr = JsonUtils.getValue(twitterJson, "created_at");
		this.createdDate = parseTwitterDate(createdAtStr);
	}


	public String getAuthor() {
		return author;
	}


	public String getScreenName() {
		return screenName;
	}


	public String getText() {
		return text;
	}


	public boolean isRetweet() {
		return retweet;
	}


	public Date getCreatedDate() {
		return createdDate;
	}


	public String getProfileImage() {
		return profileImage;
	}


	public String getMentions() {
		return mentions;
	}


	public String getUserDescription() {
		return userDescription;
	}


	public boolean isVerified() {
		return verified;
	}


	public String getUserBackGroundImage() {
		return userBackGroundImage;
	}


	public HashSet<String> getImageUrlsList() {
		return imageUrlsList;
	}


	public HashSet<String> getHashTags() {
		return hashTags;
	}


	public int getNumberOfLikes() {
		return numberOfLikes;
	}


	public int getNumberOfRetweets() {
		return numberOfRetweets;
	}


	public int getNumberOfReplies() {
		return numberOfReplies;
	}


	public int getNumberOfFollows() {
		return numberOfFollows;
	}


	public int getNumberOfFriends() {
		return numberOfFriends;
	}


	private Date parseTwitterDate(String dateStr) {
		try {
			DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss Z yyyy", Locale.ENGLISH);
			Date date = df.parse(dateStr);
			return date;
		} catch (Exception e) {
			log.error("failed to parse twitter date:" + dateStr);
			return null;
		}

	}

}

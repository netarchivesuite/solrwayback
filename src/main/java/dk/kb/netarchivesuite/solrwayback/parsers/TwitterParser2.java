package dk.kb.netarchivesuite.solrwayback.parsers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;

public class TwitterParser2 {

	private static final Logger log = LoggerFactory.getLogger(TwitterParser2.class);

	private final JSONObject twitterJson;

	private String author;
	private String screenName;
	private final String originalAuthor;
	private final String text;
	private boolean retweet = false;
	private final Date createdDate;
	private String profileImage;
	private String userDescription;
	private boolean verified;
    private String userBackGroundImage;  //TODO both http and https version
	
	private HashSet<String> imageUrlsList = new HashSet<>();
	private HashSet<String> hashTags = new HashSet<>();
	private HashSet<String> mentions = new HashSet<>();

	private final int numberOfLikes; // "favorites"
	private final int numberOfRetweets;
	private final int numberOfReplies;
	private int numberOfFollowers;
	private int numberOfFriends;
	private final int numberOfQuotes;

	public TwitterParser2(String twitterJsonString) {
		this.twitterJson = new JSONObject(twitterJsonString);

		if (twitterJson.has("retweeted_status")) {
			this.retweet = true;
		}

		parseUserInfo();

		String parsePrefix = retweet ? "retweeted_status." : "";
		this.originalAuthor = JsonUtils.getValue(twitterJson, parsePrefix + "user.screen_name");

		this.text = JsonUtils.getValueIfExistsByPriority(twitterJson, parsePrefix + "extended_tweet.full_text", parsePrefix + "text");
		this.numberOfLikes = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "favorite_count"));
		this.numberOfRetweets = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "retweet_count"));
		this.numberOfQuotes = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "quote_count"));
		this.numberOfReplies = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "reply_count")); // TODO when to use this?

		//TODO also HTTPs version?
		JsonUtils.addAllValues(twitterJson, hashTags, parsePrefix + "extended_tweet.entities.hashtags[].text");
		// JsonUtils.addAllValues(twitterJson, hashTags, "entities.hashtags[].text"); Necessary ?
		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "extended_tweet.extended_entities.media[].media_url");
		// TODO RBKR Not sure if simple 'entities' is needed but pretty sure I encountered a tweet's json without 'extended_entities'
		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "extended_tweet.entities.media[].media_url");
		//JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson, mentions, parsePrefix + "extended_tweet.entities.user_mentions[].screen_name");

		// TODO add support for quotes?
		// Seems if tweet is retweet the quote will appear both in the upper 'quoted_status' and 'retweeted_status' while
		// standard tweet only has 'quoted_status.

		String createdAtStr = JsonUtils.getValue(twitterJson, "created_at");
		this.createdDate = parseTwitterDate(createdAtStr);
	}

	private void parseUserInfo() {
		this.author = JsonUtils.getValue(twitterJson, "user.name"); // always exists
		this.screenName = JsonUtils.getValue(twitterJson, "user.screen_name");
		this.profileImage = JsonUtils.getValue(twitterJson, "user.profile_image_url"); // always exists
		this.userDescription = JsonUtils.getValue(twitterJson, "user.description"); // always exists
		this.numberOfFollowers = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.followers_count"));
		this.numberOfFriends = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.friends_count"));
		this.verified = Boolean.parseBoolean(JsonUtils.getValue(twitterJson, "user.verified"));
	}


	public String getAuthor() {
		return author;
	}


	public String getScreenName() {
		return screenName;
	}


	public String getOriginalAuthor() {
		return originalAuthor;
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


	public HashSet<String> getMentions() {
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


	public int getNumberOfFollowers() {
		return numberOfFollowers;
	}


	public int getNumberOfFriends() {
		return numberOfFriends;
	}

	public int getNumberOfQuotes() {
		return numberOfQuotes;
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

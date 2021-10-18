package dk.kb.netarchivesuite.solrwayback.parsers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;

public class TwitterParser2 {

	private static final Logger log = LoggerFactory.getLogger(TwitterParser2.class);

	private final JSONObject twitterJson;
	private final String RETWEET_PREFIX = "retweeted_status.";

	private String userName;
	private String userScreenName;
	private String retweetUserScreenName;
	private String retweetUserName;
	private final String text;
	private boolean retweet;
	private boolean hasQuote;
	private final Date createdDate;
	private Date retweetCreatedDate;
	private String userProfileImage;
	private String retweetUserProfileImage;
	private String userDescription;
	private String retweetUserDescription;
	private boolean userVerified;
	private String userBackGroundImage;  //TODO both http and https version
	private final int likeCount; // "favorites"
	private final int retweetCount;
	private final int replyCount;
	private int userFollowersCount;
	private int userFriendsCount;
	private int retweetUserFollowersCount;
	private int retweetUserFriendsCount;
	private final int quoteCount;

	private Set<String> imageUrlsList = new HashSet<>();
	private Set<String> hashTags = new HashSet<>();
	private Set<String> mentions = new HashSet<>();

	private String quoteText;
	private String quoteUserName;
	private String quoteUserScreenName;
	private Date quoteCreatedDate;
	private String quoteUserProfileImage;
	private String quoteUserDescription;
	private int quoteUserFollowersCount;
	private int quoteUserFriendsCount;
	private boolean quoteUserVerified;
	private Set<String> quoteImageUrlStrings = new HashSet<>();

	public TwitterParser2(String twitterJsonString) {
		this.twitterJson = new JSONObject(twitterJsonString);
		this.retweet = twitterJson.has("retweeted_status");
		this.hasQuote = twitterJson.getBoolean("is_quote_status");

		String parsePrefix = retweet ? RETWEET_PREFIX : "";
		if (isRetweet()) { // TODO do something smarter than this please.
			String retweetCreatedAtStr = JsonUtils.getValue(twitterJson, "created_at");
			this.retweetCreatedDate = parseTwitterDate(retweetCreatedAtStr);
			this.retweetUserName = JsonUtils.getValue(twitterJson, parsePrefix + "user.name");
			this.retweetUserScreenName = JsonUtils.getValue(twitterJson, parsePrefix + "user.screen_name");
			this.retweetUserProfileImage = JsonUtils.getValue(twitterJson, parsePrefix + "user.profile_image_url");
			this.retweetUserDescription = JsonUtils.getValue(twitterJson, parsePrefix + "user.description", "No description.");
			this.retweetUserFollowersCount = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "user.followers_count"));
			this.retweetUserFriendsCount = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "user.friends_count"));
		}
		if (hasQuote()) {
			parseQuote();
		}
		parseUserInfo();

		// Longer tweets contain the 'extended_tweet' keyword while short tweets do without it
		this.text = JsonUtils.getValueIfExistsByPriority(twitterJson, parsePrefix + "extended_tweet.full_text", parsePrefix + "text");
		this.likeCount = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "favorite_count"));
		this.retweetCount = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "retweet_count"));
		this.quoteCount = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "quote_count"));
		this.replyCount = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "reply_count")); // TODO when to use this?

		//TODO also HTTPs version?
		// TODO RBKR - pretty sure retweets will always have 'extended_tweet', so probably doesn't make sense to add prefix to short standard tweets
		JsonUtils.addAllValues(twitterJson, hashTags, parsePrefix + "entities.hashtags[].text");
		JsonUtils.addAllValues(twitterJson, hashTags, parsePrefix + "extended_tweet.entities.hashtags[].text");
		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "extended_tweet.entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "extended_tweet.extended_entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson, mentions, parsePrefix + "entities.user_mentions[].screen_name");
		JsonUtils.addAllValues(twitterJson, mentions, parsePrefix + "extended_tweet.entities.user_mentions[].screen_name");

		// Seems if tweet is retweet the quote will appear both in the upper 'quoted_status' and 'retweeted_status' while
		// standard tweet only has 'quoted_status.

		String createdAtStr = JsonUtils.getValue(twitterJson, parsePrefix + "created_at");
		this.createdDate = parseTwitterDate(createdAtStr);
	}

	private void parseUserInfo() {
		// All these values always exist
		this.userName = JsonUtils.getValue(twitterJson, "user.name");
		this.userScreenName = JsonUtils.getValue(twitterJson, "user.screen_name");
		this.userProfileImage = JsonUtils.getValue(twitterJson, "user.profile_image_url");
		this.userDescription = JsonUtils.getValue(twitterJson, "user.description",
				"No description."); // Might be null, so want a default
		this.userFollowersCount = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.followers_count"));
		this.userFriendsCount = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.friends_count"));
		this.userVerified = Boolean.parseBoolean(JsonUtils.getValue(twitterJson, "user.verified"));
	}

	private void parseQuote() {
		JSONObject quoteJson = twitterJson.getJSONObject("quoted_status");
		parseQuoteUserInfo(quoteJson);
		String createdAtStr = quoteJson.getString("created_at");
		this.quoteCreatedDate = parseTwitterDate(createdAtStr);
		this.quoteText = JsonUtils.getValueIfExistsByPriority(quoteJson, "extended_tweet.full_text", "text");
		JsonUtils.addAllValues(quoteJson, quoteImageUrlStrings, "extended_entities.media[].media_url");
		JsonUtils.addAllValues(quoteJson, quoteImageUrlStrings, "extended_tweet.entities.media[].media_url");
	}

	private void parseQuoteUserInfo(JSONObject quoteJson) {
		JSONObject userJson = quoteJson.getJSONObject("user");
		this.quoteUserName = userJson.getString("name");
		this.quoteUserScreenName = userJson.getString("screen_name");
		this.quoteUserProfileImage = userJson.getString("profile_image_url");
		this.quoteUserDescription =
				userJson.get("description") == null ? "No description." : userJson.getString("description");
		this.quoteUserFollowersCount = userJson.getInt("followers_count");
		this.quoteUserFriendsCount = userJson.getInt("friends_count");
		this.quoteUserVerified = userJson.getBoolean("verified");
	}


	public String getUserName() {
		return userName;
	}


	public String getUserScreenName() {
		return userScreenName;
	}


	public String getRetweetUserName() {
		return retweetUserName;
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


	public String getUserProfileImage() {
		return userProfileImage;
	}


	public Set<String> getMentions() {
		return mentions;
	}


	public String getUserDescription() {
		return userDescription;
	}


	public boolean isUserVerified() {
		return userVerified;
	}


	public String getUserBackGroundImage() {
		return userBackGroundImage;
	}


	public Set<String> getImageUrlsList() {
		return imageUrlsList;
	}


	public Set<String> getHashTags() {
		return hashTags;
	}


	public int getLikeCount() {
		return likeCount;
	}


	public int getRetweetCount() {
		return retweetCount;
	}


	public int getReplyCount() {
		return replyCount;
	}


	public int getUserFollowersCount() {
		return userFollowersCount;
	}


	public int getUserFriendsCount() {
		return userFriendsCount;
	}

	public int getQuoteCount() {
		return quoteCount;
	}


	private Date parseTwitterDate(String dateStr) {
		try {
			DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss Z yyyy", Locale.ENGLISH);
			return df.parse(dateStr);
		} catch (Exception e) {
			log.error("failed to parse twitter date:" + dateStr);
			return null;
		}

	}

	public boolean hasQuote() {
		return hasQuote;
	}

	public String getQuoteText() {
		return quoteText;
	}

	public String getQuoteUserName() {
		return quoteUserName;
	}

	public String getQuoteUserScreenName() {
		return quoteUserScreenName;
	}

	public Date getQuoteCreatedDate() {
		return quoteCreatedDate;
	}

	public String getQuoteUserProfileImage() {
		return quoteUserProfileImage;
	}

	public String getQuoteUserDescription() {
		return quoteUserDescription;
	}

	public int getQuoteUserFollowersCount() {
		return quoteUserFollowersCount;
	}

	public int getQuoteUserFriendsCount() {
		return quoteUserFriendsCount;
	}

	public boolean isQuoteUserVerified() {
		return quoteUserVerified;
	}

	public Set<String> getQuoteImageUrlStrings() {
		return quoteImageUrlStrings;
	}

	public Date getRetweetCreatedDate() {
		return retweetCreatedDate;
	}

	public String getRetweetUserScreenName() {
		return retweetUserScreenName;
	}

	public String getRetweetUserProfileImage() {
		return retweetUserProfileImage;
	}

	public String getRetweetUserDescription() {
		return retweetUserDescription;
	}

	public int getRetweetUserFollowersCount() {
		return retweetUserFollowersCount;
	}

	public int getRetweetUserFriendsCount() {
		return retweetUserFriendsCount;
	}
}

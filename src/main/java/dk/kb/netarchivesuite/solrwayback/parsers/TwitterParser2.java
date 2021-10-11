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

	private String author;
	private String screenName;
	private String originalScreenName;
	private String originalAuthor;
	private final String text;
	private boolean retweet;
	private boolean hasQuote;
	private final Date createdDate;
	private Date retweetCreatedDate;
	private String profileImage;
	private String originalProfileImage;
	private String userDescription;
	private boolean verified;
	private String userBackGroundImage;  //TODO both http and https version
	private final int numberOfLikes; // "favorites"
	private final int numberOfRetweets;
	private final int numberOfReplies;
	private int numberOfFollowers;
	private int numberOfFriends;
	private final int numberOfQuotes;

	private Set<String> imageUrlsList = new HashSet<>();
	private Set<String> hashTags = new HashSet<>();
	private Set<String> mentions = new HashSet<>();

	private String quoteText;
	private String quoteUserName;
	private String quoteUserScreenName;
	private Date quoteCreatedDate;
	private String quoteUserProfileImage;
	private String quoteUserDescription;
	private int quoteUserFollowCount;
	private int quoteUserFriendCount;
	private boolean quoteUserVerified;
	private Set<String> quoteImageUrlStrings = new HashSet<>();

	public TwitterParser2(String twitterJsonString) {
		this.twitterJson = new JSONObject(twitterJsonString);
		this.retweet = twitterJson.has("retweeted_status");
		this.hasQuote = twitterJson.getBoolean("is_quote_status");

		String parsePrefix = retweet ? "retweeted_status." : "";
		if (isRetweet()) { // TODO do something smarter than this please.
			String retweetCreatedAtStr = JsonUtils.getValue(twitterJson, "created_at");
			this.retweetCreatedDate = parseTwitterDate(retweetCreatedAtStr);
			this.originalAuthor = JsonUtils.getValue(twitterJson, parsePrefix + "user.name");
			this.originalScreenName = JsonUtils.getValue(twitterJson, parsePrefix + "user.screen_name");
			this.originalProfileImage = JsonUtils.getValue(twitterJson, parsePrefix + "user.profile_image_url");
		}
		if (hasQuote()) {
			parseQuote();
		}
		parseUserInfo();

		// Usually, longer tweets contain the 'extended_tweet' keyword while short tweets do without it
		this.text = JsonUtils.getValueIfExistsByPriority(twitterJson, parsePrefix + "extended_tweet.full_text", parsePrefix + "text");
		this.numberOfLikes = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "favorite_count"));
		this.numberOfRetweets = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "retweet_count"));
		this.numberOfQuotes = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "quote_count"));
		this.numberOfReplies = Integer.parseInt(JsonUtils.getValue(twitterJson, parsePrefix + "reply_count")); // TODO when to use this?

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
		this.author = JsonUtils.getValue(twitterJson, "user.name"); // always exists
		this.screenName = JsonUtils.getValue(twitterJson, "user.screen_name");
		this.profileImage = JsonUtils.getValue(twitterJson, "user.profile_image_url"); // always exists
		this.userDescription = JsonUtils.getValue(twitterJson, "user.description"); // always exists
		this.numberOfFollowers = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.followers_count"));
		this.numberOfFriends = Integer.parseInt(JsonUtils.getValue(twitterJson, "user.friends_count"));
		this.verified = Boolean.parseBoolean(JsonUtils.getValue(twitterJson, "user.verified"));
	}

	private void parseQuote() {
		JSONObject quoteJson = twitterJson.getJSONObject("quoted_status");
		parseQuoteUserInfo(quoteJson);
		String createdAtStr = quoteJson.getString("created_at");
		this.quoteCreatedDate = parseTwitterDate(createdAtStr);
		this.quoteText = JsonUtils.getValue(quoteJson, "text");
		JsonUtils.addAllValues(quoteJson, quoteImageUrlStrings, "extended_entities.media[].media_url");
		JsonUtils.addAllValues(quoteJson, quoteImageUrlStrings, "extended_tweet.entities.media[].media_url");
	}

	private void parseQuoteUserInfo(JSONObject quoteJson) {
		JSONObject userJson = quoteJson.getJSONObject("user");
		this.quoteUserName = userJson.getString("name");
		this.quoteUserScreenName = userJson.getString("screen_name");
		this.quoteUserProfileImage = userJson.getString("profile_image_url");
		this.quoteUserDescription = userJson.getString("description");
		this.quoteUserFollowCount = userJson.getInt("followers_count");
		this.quoteUserFriendCount = userJson.getInt("friends_count");
		this.quoteUserVerified = userJson.getBoolean("verified");
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


	public Set<String> getMentions() {
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


	public Set<String> getImageUrlsList() {
		return imageUrlsList;
	}


	public Set<String> getHashTags() {
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

	public int getQuoteUserFollowCount() {
		return quoteUserFollowCount;
	}

	public int getQuoteUserFriendCount() {
		return quoteUserFriendCount;
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

	public String getOriginalScreenName() {
		return originalScreenName;
	}

	public String getOriginalProfileImage() {
		return originalProfileImage;
	}
}

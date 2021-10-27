package dk.kb.netarchivesuite.solrwayback.parsers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
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
	private String userId;
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
	private Map<Pair<Integer, Integer>, String> hashtags;
	private Map<Pair<Integer, Integer>, String> mentions;

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
			String retweetCreatedAtStr = JsonUtils.getValue(twitterJson, parsePrefix + "created_at");
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
		Set<String> rawHashtagIndices = new LinkedHashSet<>();
		JsonUtils.addAllValues(twitterJson, rawHashtagIndices, parsePrefix + "entities.hashtags[].indices");
		JsonUtils.addAllValues(twitterJson, rawHashtagIndices, parsePrefix + "extended_tweet.entities.hashtags[].indices");
		List<Pair<Integer, Integer>> hashtagIndices = rawHashtagIndices.stream()
				.map(rawIndicesString -> rawIndicesString.substring(1, rawIndicesString.length() - 1)) // Cut off surrounding brackets
				.map(indicesString -> indicesString.split(","))
				.map(indexPairString -> Pair.of(Integer.parseInt(indexPairString[0]), Integer.parseInt(indexPairString[1])))
				.map(indexPair -> Pair.of(text.offsetByCodePoints(0, indexPair.getLeft()), text.offsetByCodePoints(0, indexPair.getRight())))
				.collect(Collectors.toList());

		Set<String> hashtagsText = new LinkedHashSet<>();
		JsonUtils.addAllValues(twitterJson, hashtagsText, parsePrefix + "entities.hashtags[].text");
		JsonUtils.addAllValues(twitterJson, hashtagsText, parsePrefix + "extended_tweet.entities.hashtags[].text");
		List<String> hashtagsTextList = hashtagsText.stream()
				.map(hashtag -> "#" + hashtag).collect(Collectors.toList());
		hashtags = IntStream.range(0, hashtagsTextList.size()).boxed()
				.collect(Collectors.toMap(hashtagIndices::get, hashtagsTextList::get,
						(h1, h2) -> { throw new IllegalStateException(); }, LinkedHashMap::new));

		Set<String> rawMentionIndices = new LinkedHashSet<>();
		JsonUtils.addAllValues(twitterJson, rawMentionIndices, parsePrefix + "entities.user_mentions[].indices");
		JsonUtils.addAllValues(twitterJson, rawMentionIndices, parsePrefix + "extended_tweet.entities.user_mentions[].indices");
		List<Pair<Integer, Integer>> mentionIndices = rawMentionIndices.stream()
				.map(rawIndicesString -> rawIndicesString.substring(1, rawIndicesString.length() - 1)) // Cut off surrounding brackets
				.map(indicesString -> indicesString.split(","))
				.map(indexPairString -> Pair.of(Integer.parseInt(indexPairString[0]), Integer.parseInt(indexPairString[1])))
				.map(indexPair -> Pair.of(text.offsetByCodePoints(0, indexPair.getLeft()), text.offsetByCodePoints(0, indexPair.getRight())))
				.collect(Collectors.toList());

		Set<String> mentionScreenNames = new LinkedHashSet<>();
		JsonUtils.addAllValues(twitterJson, mentionScreenNames, parsePrefix + "entities.user_mentions[].screen_name");
		JsonUtils.addAllValues(twitterJson, mentionScreenNames, parsePrefix + "extended_tweet.entities.user_mentions[].screen_name");
		List<String> mentionScreenNamesList = mentionScreenNames.stream()
				.map(mention -> "@" + mention).collect(Collectors.toList());
		mentions = IntStream.range(0, mentionScreenNamesList.size()).boxed()
				.collect(Collectors.toMap(mentionIndices::get, mentionScreenNamesList::get,
						(h1, h2) -> { throw new IllegalStateException(); }, LinkedHashMap::new));

		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "extended_tweet.entities.media[].media_url");
		JsonUtils.addAllValues(twitterJson, imageUrlsList, parsePrefix + "extended_tweet.extended_entities.media[].media_url");

		// Seems if tweet is retweet the quote will appear both in the upper 'quoted_status' and 'retweeted_status' while
		// standard tweet only has 'quoted_status.

		String createdAtStr = JsonUtils.getValue(twitterJson, "created_at");
		this.createdDate = parseTwitterDate(createdAtStr);
	}

	private void parseUserInfo() {
		// All these values always exist
		this.userName = JsonUtils.getValue(twitterJson, "user.name");		
		this.userId = JsonUtils.getValue(twitterJson, "user.id");
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


	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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


	public Map<Pair<Integer, Integer>, String> getMentions() {
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


	public Map<Pair<Integer, Integer>, String> getHashtags() {
		return hashtags;
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

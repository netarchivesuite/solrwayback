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

	private final JSONObject parentJSON;

	private String userName;
	private String userScreenName;
	private String retweetUserScreenName;
	private String retweetUserName;
	private String userId;
	private String text;
	private boolean isRetweet;
	private boolean hasQuote;
	private final Date createdDate;
	private Date retweetCreatedDate;
	private String userProfileImage;
	private String retweetUserProfileImage;
	private String userDescription;
	private String retweetUserDescription;
	private boolean userVerified;
	private String userBackGroundImage;  //TODO both http and https version
	private int likeCount; // "favorites"
	private int retweetCount;
	private int replyCount;
	private int userFollowersCount;
	private int userFriendsCount;
	private int retweetUserFollowersCount;
	private int retweetUserFriendsCount;
	private int quoteCount;

	private Set<String> imageUrlStrings = new HashSet<>();
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
	private Map<Pair<Integer, Integer>, String> quoteHashtags;
	private Map<Pair<Integer, Integer>, String> quoteMentions;

	public TwitterParser2(String twitterJsonString) {
		this.parentJSON = new JSONObject(twitterJsonString);
		this.isRetweet = parentJSON.has("retweeted_status");
		this.hasQuote = parentJSON.getBoolean("is_quote_status");

		this.createdDate = parseTwitterDate(parentJSON);
		parseUserInfo();
		if (isRetweet()) {
			parseRetweetedTweet();
		} else {
			parseTweetContent(parentJSON);
		}
		if (hasQuote()) {
			parseQuote();
		}
	}

	private void parseTweetContent(JSONObject json) {
		// Longer tweets contain the 'extended_tweet' keyword while short tweets do without it
		this.text = JsonUtils.getValueIfExistsByPriority(json, "extended_tweet.full_text", "text");
		this.likeCount = Integer.parseInt(JsonUtils.getValue(json, "favorite_count"));
		this.retweetCount = Integer.parseInt(JsonUtils.getValue(json, "retweet_count"));
		this.quoteCount = Integer.parseInt(JsonUtils.getValue(json, "quote_count"));
		this.replyCount = Integer.parseInt(JsonUtils.getValue(json, "reply_count")); // TODO when to use this?
		this.hashtags = parseHashtags(json);
		this.mentions = parseMentions(json);
		parseImages(json, imageUrlStrings);
	}

	private void parseRetweetedTweet() {
		JSONObject retweetedTweetJSON = parentJSON.getJSONObject("retweeted_status");
		this.retweetCreatedDate = parseTwitterDate(retweetedTweetJSON);
		parseRetweetedUser(retweetedTweetJSON.getJSONObject("user"));
		parseTweetContent(retweetedTweetJSON);
	}

	private void parseRetweetedUser(JSONObject retweetedUserJson) {
		this.retweetUserName = retweetedUserJson.getString("name");
		this.retweetUserScreenName = retweetedUserJson.getString("screen_name");
		this.retweetUserProfileImage = retweetedUserJson.getString("profile_image_url");
		this.retweetUserDescription = retweetedUserJson.get("description") == null ? "No description." : retweetedUserJson.getString("description");
		this.retweetUserFollowersCount = retweetedUserJson.getInt("followers_count");
		this.retweetUserFriendsCount = retweetedUserJson.getInt("friends_count");
	}

	private void parseQuote() {
		// Twitter API has quote-JSON under both "quoted_status" and "retweeted_status.quoted_status" if retweet,
		// so no need to use retweet prefix.
		JSONObject quoteTweetJSON = parentJSON.getJSONObject("quoted_status");
		parseQuoteUserInfo(quoteTweetJSON.getJSONObject("user"));
		this.quoteCreatedDate = parseTwitterDate(quoteTweetJSON);
		this.quoteText = JsonUtils.getValueIfExistsByPriority(quoteTweetJSON, "extended_tweet.full_text", "text");
		this.quoteHashtags = parseHashtags(quoteTweetJSON);
		this.quoteMentions = parseMentions(quoteTweetJSON);
		parseImages(quoteTweetJSON, quoteImageUrlStrings);
	}

	private void parseQuoteUserInfo(JSONObject userJson) {
		this.quoteUserName = userJson.getString("name");
		this.quoteUserScreenName = userJson.getString("screen_name");
		this.quoteUserProfileImage = userJson.getString("profile_image_url");
		this.quoteUserDescription =
				userJson.get("description") == null ? "No description." : userJson.getString("description");
		this.quoteUserFollowersCount = userJson.getInt("followers_count");
		this.quoteUserFriendsCount = userJson.getInt("friends_count");
		this.quoteUserVerified = userJson.getBoolean("verified");
	}

	private Map<Pair<Integer, Integer>, String> parseHashtags(JSONObject json) {
		List<Pair<Integer, Integer>> hashtagIndices = parseHashtagIndices(json);
		List<String> hashtagsStrings = parseHashtagStrings(json);
		return mergeListsIntoMap(hashtagIndices, hashtagsStrings);
	}

	private List<Pair<Integer, Integer>> parseHashtagIndices(JSONObject json) {
		Set<String> rawHashtagIndices = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, rawHashtagIndices, "entities.hashtags[].indices");
		JsonUtils.addAllValues(json, rawHashtagIndices, "extended_tweet.entities.hashtags[].indices");
		return makeIndicesWithOffset(rawHashtagIndices, getText());
	}

	private List<String> parseHashtagStrings(JSONObject json) {
		Set<String> hashtagsText = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, hashtagsText, "entities.hashtags[].text");
		JsonUtils.addAllValues(json, hashtagsText, "extended_tweet.entities.hashtags[].text");
		return hashtagsText.stream()
				.map(hashtag -> "#" + hashtag).collect(Collectors.toList());
	}

	private Map<Pair<Integer, Integer>, String> parseMentions(JSONObject json) {
		List<Pair<Integer, Integer>> mentionIndices = parseMentionIndices(json);
		List<String> mentionStrings = parseMentionStrings(json);
		return mergeListsIntoMap(mentionIndices, mentionStrings);
	}

	private LinkedHashMap<Pair<Integer, Integer>, String> mergeListsIntoMap(List<Pair<Integer, Integer>> mentionIndices, List<String> mentionScreenNamesList) {
		return IntStream.range(0, mentionScreenNamesList.size()).boxed()
				.collect(Collectors.toMap(mentionIndices::get, mentionScreenNamesList::get,
						(h1, h2) -> {
							throw new IllegalStateException(); // If keys are same - shouldn't happen since no tag should share indices
						}, LinkedHashMap::new));
	}

	private List<Pair<Integer, Integer>> parseMentionIndices(JSONObject json) {
		Set<String> rawMentionIndices = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, rawMentionIndices, "entities.user_mentions[].indices");
		JsonUtils.addAllValues(json, rawMentionIndices, "extended_tweet.entities.user_mentions[].indices");
		return makeIndicesWithOffset(rawMentionIndices, getText());
	}

	private List<String> parseMentionStrings(JSONObject json) {
		Set<String> mentionScreenNames = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, mentionScreenNames, "entities.user_mentions[].screen_name");
		JsonUtils.addAllValues(json, mentionScreenNames, "extended_tweet.entities.user_mentions[].screen_name");
		return mentionScreenNames.stream()
				.map(mention -> "@" + mention).collect(Collectors.toList());
	}

	private void parseImages(JSONObject json, Set<String> imageSet) {
		JsonUtils.addAllValues(json, imageSet, "entities.media[].media_url");
		JsonUtils.addAllValues(json, imageSet, "extended_tweet.entities.media[].media_url");
		JsonUtils.addAllValues(json, imageSet, "extended_tweet.extended_entities.media[].media_url");
	}

	private List<Pair<Integer, Integer>> makeIndicesWithOffset(Set<String> rawHashtagIndices, String text) {
		return rawHashtagIndices.stream()
				.map(rawIndicesString -> rawIndicesString.substring(1, rawIndicesString.length() - 1)) // Cut off surrounding brackets
				.map(indicesString -> indicesString.split(","))
				.map(indexPairString -> Pair.of(Integer.parseInt(indexPairString[0]), Integer.parseInt(indexPairString[1])))
				.map(indexPair -> Pair.of(text.offsetByCodePoints(0, indexPair.getLeft()), text.offsetByCodePoints(0, indexPair.getRight())))
				.collect(Collectors.toList());
	}

	private void parseUserInfo() {
		// All these values always exist
		this.userName = JsonUtils.getValue(parentJSON, "user.name");
		this.userId = JsonUtils.getValue(parentJSON, "user.id");
		this.userScreenName = JsonUtils.getValue(parentJSON, "user.screen_name");
		this.userProfileImage = JsonUtils.getValue(parentJSON, "user.profile_image_url");
		this.userDescription = JsonUtils.getValue(parentJSON, "user.description",
				"No description."); // Might be null, so want a default
		this.userFollowersCount = Integer.parseInt(JsonUtils.getValue(parentJSON, "user.followers_count"));
		this.userFriendsCount = Integer.parseInt(JsonUtils.getValue(parentJSON, "user.friends_count"));
		this.userVerified = Boolean.parseBoolean(JsonUtils.getValue(parentJSON, "user.verified"));
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
		return isRetweet;
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


	public Set<String> getImageUrlStrings() {
		return imageUrlStrings;
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


	private Date parseTwitterDate(JSONObject json) {
		String dateStr = json.getString("created_at");
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

	public Map<Pair<Integer, Integer>, String> getQuoteHashtags() {
		return quoteHashtags;
	}

	public Map<Pair<Integer, Integer>, String> getQuoteMentions() {
		return quoteMentions;
	}
}

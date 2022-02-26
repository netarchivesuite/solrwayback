package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class for parsing twitter tweet-json and returning meaningful output from this.
 * TODO in need of overhaul to extrapolate responsibilities into separate classes
 */
public class TwitterParser2 {

	private static final Logger log = LoggerFactory.getLogger(TwitterParser2.class);
	private final JSONObject parentJSON;

	private final String tweetID;
	private final boolean isRetweet;
	private final boolean hasQuote;
	private final Date createdDate;
	private long userID;
	private String userName;
	private String userScreenName;
	private String userProfileImage;
	private String userDescription;
	private int userFollowersCount;
	private int userFriendsCount;
	private boolean userVerified;
	private int tweetMinDisplayTextRange;
	private String text;
	private int likeCount; // "favorites"
	private int retweetCount;
	private int replyCount;
	private int quoteCount;
	private String retweetedTweetID;
	private String replyToStatusID;
	private String replyToScreenName;
	private List<String> replyMentions;
	private Date retweetCreatedDate;
	private long retweetUserID;
	private String retweetUserScreenName;
	private String retweetUserName;
	private String retweetUserProfileImage;
	private String retweetUserDescription;
	private int retweetUserFollowersCount;
	private int retweetUserFriendsCount;
	private boolean retweetUserVerified;

	private Map<Pair<Integer, Integer>, String> hashtags;
	private Map<Pair<Integer, Integer>, String> mentions;
	private Map<Pair<Integer, Integer>, String> urls;
	private final List<String> imageURLStrings = new ArrayList<>();

	private String quotePermaLink;
	private Date quoteCreatedDate;
	private String quoteTweetID;
	private long quoteUserID;
	private String quoteUserName;
	private String quoteUserScreenName;
	private String quoteReplyToStatusID;
	private String quoteReplyToScreenName;
	private List<String> quoteReplyMentions;
	private int quoteMinDisplayTextRange;
	private String quoteText;
	private String quoteUserProfileImage;
	private String quoteUserDescription;
	private int quoteUserFollowersCount;
	private int quoteUserFriendsCount;
	private boolean quoteUserVerified;

	private Map<Pair<Integer, Integer>, String> quoteHashtags;
	private Map<Pair<Integer, Integer>, String> quoteMentions;
	private Map<Pair<Integer, Integer>, String> quoteURLs;
	private final List<String> quoteImageURLStrings = new ArrayList<>();

	public TwitterParser2(String twitterJsonString) {
		this.parentJSON = new JSONObject(twitterJsonString);
		this.tweetID = parentJSON.getString("id_str");
		this.isRetweet = parentJSON.has("retweeted_status");
		this.hasQuote = parentJSON.getBoolean("is_quote_status");

		this.createdDate = parseTwitterDate(parentJSON);
		parseMainUserInfo(parentJSON.getJSONObject("user"));

		if (hasQuote()) {
			this.quotePermaLink = parentJSON.getJSONObject("quoted_status_permalink").getString("expanded");
			parseQuote();
		}
		if (isRetweet()) {
			parseRetweetedTweet();
		} else {
			parseMainTweetContent(parentJSON);
		}
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

	private void parseMainUserInfo(JSONObject mainUserJSON) {
		// All these values always exist
		this.userID = mainUserJSON.getLong("id");
		this.userName = mainUserJSON.getString("name");
		this.userScreenName = mainUserJSON.getString("screen_name");
		this.userProfileImage = mainUserJSON.getString("profile_image_url");
		this.userDescription = mainUserJSON.optString("description", "No description."); // Might be null, so want a default
		this.userFollowersCount = mainUserJSON.getInt("followers_count");
		this.userFriendsCount = mainUserJSON.getInt("friends_count");
		this.userVerified = mainUserJSON.getBoolean("verified");
	}

	private void parseQuote() {
		// Twitter API has quote-JSON under both "quoted_status" and "retweeted_status.quoted_status" if retweet,
		// so no need to use retweet prefix.
		JSONObject quoteTweetJSON = parentJSON.getJSONObject("quoted_status");
		parseQuoteUserInfo(quoteTweetJSON.getJSONObject("user"));
		this.quoteCreatedDate = parseTwitterDate(quoteTweetJSON);
		this.quoteTweetID = quoteTweetJSON.getString("id_str");
		this.quoteReplyToStatusID = quoteTweetJSON.optString("in_reply_to_status_id_str");
		this.quoteReplyToScreenName = quoteTweetJSON.optString("in_reply_to_screen_name");
		this.quoteText = JsonUtils.getValueIfExistsByPriority(quoteTweetJSON, "extended_tweet.full_text", "text");

		JSONObject entityParentJSON = quoteTweetJSON.has("extended_tweet") ? quoteTweetJSON.getJSONObject("extended_tweet") : quoteTweetJSON;
		this.quoteMinDisplayTextRange = parseMinDisplayTextRange(entityParentJSON);
		JSONObject entityJSON = entityParentJSON.getJSONObject("entities");
		if (!quoteReplyToStatusID.isEmpty()) {
			this.quoteReplyMentions = parseReplyMentions(entityJSON, quoteMinDisplayTextRange, quoteReplyToScreenName, quoteText);
		}
		this.quoteMentions = parseMentions(entityJSON, quoteText, quoteMinDisplayTextRange);
		this.quoteHashtags = parseHashtags(entityJSON, quoteText);
		this.quoteURLs = parseURLs(entityJSON, quoteText);
		parseImages(entityParentJSON, quoteImageURLStrings);
	}

	private void parseQuoteUserInfo(JSONObject userJSON) {
		this.quoteUserID = userJSON.getLong("id");
		this.quoteUserName = userJSON.getString("name");
		this.quoteUserScreenName = userJSON.getString("screen_name");
		this.quoteUserProfileImage = userJSON.getString("profile_image_url");
		this.quoteUserDescription = userJSON.optString("description", "No description.");
		this.quoteUserFollowersCount = userJSON.getInt("followers_count");
		this.quoteUserFriendsCount = userJSON.getInt("friends_count");
		this.quoteUserVerified = userJSON.getBoolean("verified");
	}

	private Map<Pair<Integer, Integer>, String> parseHashtags(JSONObject json, String tweetText) {
		List<Pair<Integer, Integer>> hashtagIndices = parseHashtagIndices(json, tweetText);
		List<String> hashtagsStrings = parseHashtagStrings(json);
		return mergeListsIntoMap(hashtagIndices, hashtagsStrings);
	}

	private List<Pair<Integer, Integer>> parseHashtagIndices(JSONObject json, String tweetText) {
		List<String> rawHashtagIndices = new ArrayList<>();
		JsonUtils.addAllValues(json, rawHashtagIndices, "hashtags[].indices");
		return makeIndicesWithOffset(rawHashtagIndices, tweetText);
	}

	private List<Pair<Integer, Integer>> makeIndicesWithOffset(List<String> rawHashtagIndices, String tweetText) {
		return rawHashtagIndices.stream()
				.map(rawIndicesString -> rawIndicesString.substring(1, rawIndicesString.length() - 1)) // Cut off surrounding brackets
				.map(indicesString -> indicesString.split(","))
				.map(indexPairString -> Pair.of(Integer.parseInt(indexPairString[0]), Integer.parseInt(indexPairString[1])))
				//.peek(System.out::println)
				.map(indexPair -> Pair.of(tweetText.offsetByCodePoints(0, indexPair.getLeft()), tweetText.offsetByCodePoints(0, indexPair.getRight())))
				.collect(Collectors.toList());
	}

	private List<String> parseHashtagStrings(JSONObject json) {
		List<String> hashtagsText = new ArrayList<>();
		JsonUtils.addAllValues(json, hashtagsText, "hashtags[].text");
		return hashtagsText.stream()
				.map(hashtag -> "#" + hashtag).collect(Collectors.toList());
	}

	private Map<Pair<Integer, Integer>, String> mergeListsIntoMap(List<Pair<Integer, Integer>> indices, List<String> tags) {
		return IntStream.range(0, tags.size()).boxed()
				.collect(Collectors.toMap(indices::get, tags::get));
	}

	private Map<Pair<Integer, Integer>, String> parseMentions(JSONObject json, String tweetText, int minDisplayTextRange) {
		List<Pair<Integer, Integer>> mentionIndices = parseMentionIndices(json, tweetText);
		List<String> mentionStrings = parseMentionStrings(json);
		Map<Pair<Integer, Integer>, String> mentionsMap = mergeListsIntoMap(mentionIndices, mentionStrings);
		return filterOutEntitiesOutsideRange(mentionsMap, minDisplayTextRange);
	}

	private List<Pair<Integer, Integer>> parseMentionIndices(JSONObject json, String tweetText) {
		List<String> rawMentionIndices = new ArrayList<>();
		JsonUtils.addAllValues(json, rawMentionIndices, "user_mentions[].indices");
		return makeIndicesWithOffset(rawMentionIndices, tweetText);
	}

	private List<String> parseMentionStrings(JSONObject json) {
		List<String> mentionScreenNames = new ArrayList<>();
		JsonUtils.addAllValues(json, mentionScreenNames, "user_mentions[].screen_name");
		return mentionScreenNames.stream()
				.map(mention -> "@" + mention).collect(Collectors.toList());
	}

	private Map<Pair<Integer, Integer>, String> filterOutEntitiesOutsideRange(Map<Pair<Integer, Integer>, String> entityMap, int minDisplayRange) {
		return entityMap.entrySet().stream()
				.filter(map -> minDisplayRange <= map.getKey().getLeft())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private List<String> parseReplyMentions(JSONObject json, int minDisplayTextRange, String replyToScreenName, String tweetText) {
		List<Pair<Integer, Integer>> mentionIndices = parseMentionIndices(json, tweetText);
		List<String> mentionStrings = parseMentionStrings(json);
		Map<Pair<Integer, Integer>, String> mentionsMap = mergeListsIntoMap(mentionIndices, mentionStrings);
		List<String> replyMentions = makeListOfMentionsOutsideRange(mentionsMap, minDisplayTextRange);
		// The person that is directly replied to should be first - pretty sure this condition is only true when self-replying - TODO find out
		if (!replyMentions.contains("@" + replyToScreenName)) {
			replyMentions.add(0, "@" + replyToScreenName);
		}
		return replyMentions;
	}

	private List<String> makeListOfMentionsOutsideRange(Map<Pair<Integer, Integer>, String> entityMap, int minDisplayRange) {
		return entityMap.entrySet().stream()
				.sorted(Comparator.comparing(map -> map.getKey().getLeft())) // Want to keep ordering of reply mentions
				.filter(map -> map.getKey().getLeft() < minDisplayRange)
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	private Map<Pair<Integer, Integer>, String> parseURLs(JSONObject json, String tweetText) {
		List<Pair<Integer, Integer>> urlIndices = parseURLIndices(json, tweetText);
		List<String> urlStrings = parseURLStrings(json);
		List<String> displayURLStrings = parseDisplayURLStrings(json);
		List<String> combinedURLs = new ArrayList<>();
		for (int i = 0; i < urlStrings.size(); i++) {
			String url = urlStrings.get(i);
			if (url.equals(quotePermaLink)) { // Not interested in quote url, so just add empty string - only relevant for main tweet
				combinedURLs.add("");
			} else {
				combinedURLs.add(url + "|" + displayURLStrings.get(i)); // "expanded_url|display_url" for easy split
			}
		}
		return mergeListsIntoMap(urlIndices, combinedURLs);
	}

	private List<Pair<Integer, Integer>> parseURLIndices(JSONObject json, String tweetText) {
		List<String> rawURLIndices = new ArrayList<>();
		JsonUtils.addAllValues(json, rawURLIndices, "urls[].indices");
		return makeIndicesWithOffset(rawURLIndices, tweetText);
	}

	private List<String> parseURLStrings(JSONObject json) {
		List<String> urls = new ArrayList<>();
		JsonUtils.addAllValues(json, urls, "urls[].expanded_url");
		return urls;
	}

	private List<String> parseDisplayURLStrings(JSONObject json) {
		List<String> displayURLs = new ArrayList<>();
		JsonUtils.addAllValues(json, displayURLs, "urls[].display_url");
		return new ArrayList<>(displayURLs);
	}

	private void parseImages(JSONObject json, List<String> images) {
		if (json.has("extended_entities")) {
			json = json.getJSONObject("extended_entities"); // extended_entities always contains all images
		} else {
			json = json.getJSONObject("entities");
		}
		JsonUtils.addAllValues(json, images, "media[].media_url");
	}

	private void parseRetweetedTweet() {
		JSONObject retweetedTweetJSON = parentJSON.getJSONObject("retweeted_status");
		this.retweetedTweetID = retweetedTweetJSON.getString("id_str");
		this.retweetCreatedDate = parseTwitterDate(retweetedTweetJSON);
		parseRetweetedUser(retweetedTweetJSON.getJSONObject("user"));
		parseMainTweetContent(retweetedTweetJSON);
	}

	private void parseRetweetedUser(JSONObject retweetedUserJSON) {
		this.retweetUserID = retweetedUserJSON.getLong("id");
		this.retweetUserName = retweetedUserJSON.getString("name");
		this.retweetUserScreenName = retweetedUserJSON.getString("screen_name");
		this.retweetUserProfileImage = retweetedUserJSON.getString("profile_image_url");
		this.retweetUserDescription = retweetedUserJSON.optString("description", "No description.");
		this.retweetUserFollowersCount = retweetedUserJSON.getInt("followers_count");
		this.retweetUserFriendsCount = retweetedUserJSON.getInt("friends_count");
		this.retweetUserVerified = retweetedUserJSON.getBoolean("verified");
	}

	private void parseMainTweetContent(JSONObject json) {
		// Longer tweets contain the 'extended_tweet' keyword while short tweets do without it.
		// However, 'extended_tweet.text' does not exist and is instead found under 'full_text'
		this.likeCount = json.getInt("favorite_count");
		this.retweetCount = json.getInt("retweet_count");
		this.quoteCount = json.getInt( "quote_count");
		this.replyCount = json.getInt( "reply_count");
		this.replyToStatusID = json.optString("in_reply_to_status_id_str"); // Empty str if not a reply
		this.replyToScreenName = json.optString("in_reply_to_screen_name");
		this.text = JsonUtils.getValueIfExistsByPriority(json, "extended_tweet.full_text", "text");

		JSONObject entityParentJSON = json.has("extended_tweet") ? json.getJSONObject("extended_tweet") : json;
		JSONObject entityJSON = entityParentJSON.getJSONObject("entities");
		this.tweetMinDisplayTextRange = parseMinDisplayTextRange(entityParentJSON);
		if (!replyToStatusID.isEmpty()) {
			this.replyMentions = parseReplyMentions(entityJSON, tweetMinDisplayTextRange, replyToScreenName, text);
		}
		this.hashtags = parseHashtags(entityJSON, text);
		this.mentions = parseMentions(entityJSON, text, tweetMinDisplayTextRange);
		this.urls = parseURLs(entityJSON, text);
		parseImages(entityParentJSON, imageURLStrings);
	}

	private int parseMinDisplayTextRange(JSONObject displayTextParentJSON) {
		JSONArray displayTextRangeArr = displayTextParentJSON.optJSONArray("display_text_range");
		// If display_text_range is not defined just default to 0
		int rawDisplayRangeMin = 0;
		if (displayTextRangeArr != null) {
			rawDisplayRangeMin = displayTextRangeArr.getInt(0);
		}
		return rawDisplayRangeMin;
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


	public long getUserID() {
		return userID;
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


	public List<String> getImageURLStrings() {
		return imageURLStrings;
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

	public List<String> getQuoteImageURLStrings() {
		return quoteImageURLStrings;
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

	public List<String> getQuoteReplyMentions() {
		return quoteReplyMentions;
	}

	public long getRetweetUserID() {
		return retweetUserID;
	}

	public long getQuoteUserID() {
		return quoteUserID;
	}

	public Map<Pair<Integer, Integer>, String> getURLs() {
		return urls;
	}

	public Map<Pair<Integer, Integer>, String> getQuoteURLs() {
		return quoteURLs;
	}

	public boolean isRetweetUserVerified() {
		return retweetUserVerified;
	}

	public String getTweetID() {
		return tweetID;
	}

	public String getRetweetedTweetID() {
		return retweetedTweetID;
	}

	public String getReplyToStatusID() {
		return replyToStatusID;
	}

	public String getQuoteReplyToStatusID() {
		return quoteReplyToStatusID;
	}

	public List<String> getReplyMentions() {
		return replyMentions;
	}

	public int getTweetMinDisplayTextRange() {
		return tweetMinDisplayTextRange;
	}

	public int getQuoteMinDisplayTextRange() {
		return quoteMinDisplayTextRange;
	}

	public String getQuoteTweetID() {
		return quoteTweetID;
	}
}

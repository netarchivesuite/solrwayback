package dk.kb.netarchivesuite.solrwayback.parsers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

	private final boolean isRetweet;
	private final boolean hasQuote;
	private final Date createdDate;
	private long userID;
	private String userName;
	private String userScreenName;
	private String userProfileImage;
	private String userBackGroundImage;  //TODO both http and https version
	private String userDescription;
	private int userFollowersCount;
	private int userFriendsCount;
	private boolean userVerified;
	private String text;
	private int likeCount; // "favorites"
	private int retweetCount;
	private int replyCount;
	private int quoteCount;
	private Date retweetCreatedDate;
	private long retweetUserID;
	private String retweetUserScreenName;
	private String retweetUserName;
	private String retweetUserProfileImage;
	private String retweetUserDescription;
	private int retweetUserFollowersCount;
	private int retweetUserFriendsCount;

	private Map<Pair<Integer, Integer>, String> hashtags;
	private Map<Pair<Integer, Integer>, String> mentions;
	private Map<Pair<Integer, Integer>, String> urls;
	private final Set<String> imageURLStrings = new HashSet<>();

	private String quotePermaLink;
	private Date quoteCreatedDate;
	private long quoteUserID;
	private String quoteUserName;
	private String quoteUserScreenName;
	private String quoteText;
	private String quoteUserProfileImage;
	private String quoteUserDescription;
	private int quoteUserFollowersCount;
	private int quoteUserFriendsCount;
	private boolean quoteUserVerified;

	private Map<Pair<Integer, Integer>, String> quoteHashtags;
	private Map<Pair<Integer, Integer>, String> quoteMentions;
	private Map<Pair<Integer, Integer>, String> quoteURLs;
	private final Set<String> quoteImageURLStrings = new HashSet<>();

	public TwitterParser2(String twitterJsonString) {
		this.parentJSON = new JSONObject(twitterJsonString);
		this.isRetweet = parentJSON.has("retweeted_status");
		this.hasQuote = parentJSON.getBoolean("is_quote_status");

		this.createdDate = parseTwitterDate(parentJSON);
		parseMainUserInfo(parentJSON.getJSONObject("user"));

		if (hasQuote()) {
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
		this.userDescription = mainUserJSON.isNull("description") ? "No description."
				: mainUserJSON.getString("description"); // Might be null, so want a default
		this.userFollowersCount = mainUserJSON.getInt("followers_count");
		this.userFriendsCount = mainUserJSON.getInt("friends_count");
		this.userVerified = mainUserJSON.getBoolean("verified");
	}

	private void parseQuote() {
		// Twitter API has quote-JSON under both "quoted_status" and "retweeted_status.quoted_status" if retweet,
		// so no need to use retweet prefix.
		this.quotePermaLink = parentJSON.getJSONObject("quoted_status_permalink").getString("expanded");

		JSONObject quoteTweetJSON = parentJSON.getJSONObject("quoted_status");
		parseQuoteUserInfo(quoteTweetJSON.getJSONObject("user"));
		this.quoteCreatedDate = parseTwitterDate(quoteTweetJSON);
		this.quoteText = JsonUtils.getValueIfExistsByPriority(quoteTweetJSON, "extended_tweet.full_text", "text");

		JSONObject entityParentJSON = quoteTweetJSON.has("extended_tweet") ? quoteTweetJSON.getJSONObject("extended_tweet") : quoteTweetJSON;
		JSONObject entityJSON = entityParentJSON.getJSONObject("entities");
		this.quoteHashtags = parseHashtags(entityJSON, quoteText);
		this.quoteMentions = parseMentions(entityJSON, quoteText);
		this.quoteURLs = parseURLs(entityJSON, quoteText);
		parseImages(entityParentJSON, quoteImageURLStrings); // need to parse both 'entities' and 'extended_entities'
	}

	private void parseQuoteUserInfo(JSONObject userJSON) {
		this.quoteUserID = userJSON.getLong("id");
		this.quoteUserName = userJSON.getString("name");
		this.quoteUserScreenName = userJSON.getString("screen_name");
		this.quoteUserProfileImage = userJSON.getString("profile_image_url");
		this.quoteUserDescription = userJSON.isNull("description") ? "No description."
				: userJSON.getString("description");
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
		Set<String> rawHashtagIndices = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, rawHashtagIndices, "hashtags[].indices");
		return makeIndicesWithOffset(rawHashtagIndices, tweetText);
	}

	private List<Pair<Integer, Integer>> makeIndicesWithOffset(Set<String> rawHashtagIndices, String tweetText) {
		return rawHashtagIndices.stream()
				.map(rawIndicesString -> rawIndicesString.substring(1, rawIndicesString.length() - 1)) // Cut off surrounding brackets
				.map(indicesString -> indicesString.split(","))
				.map(indexPairString -> Pair.of(Integer.parseInt(indexPairString[0]), Integer.parseInt(indexPairString[1])))
				//.peek(System.out::println)
				.map(indexPair -> Pair.of(tweetText.offsetByCodePoints(0, indexPair.getLeft()), tweetText.offsetByCodePoints(0, indexPair.getRight())))
				.collect(Collectors.toList());
	}

	private List<String> parseHashtagStrings(JSONObject json) {
		Set<String> hashtagsText = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, hashtagsText, "hashtags[].text");
		return hashtagsText.stream()
				.map(hashtag -> "#" + hashtag).collect(Collectors.toList());
	}

	private LinkedHashMap<Pair<Integer, Integer>, String> mergeListsIntoMap(List<Pair<Integer, Integer>> indices, List<String> tags) {
		return IntStream.range(0, tags.size()).boxed()
				.collect(Collectors.toMap(indices::get, tags::get,
						(h1, h2) -> {
							throw new IllegalStateException(); // If keys are same - shouldn't happen since no tag should share indices
						}, LinkedHashMap::new));
	}

	private Map<Pair<Integer, Integer>, String> parseMentions(JSONObject json, String tweetText) {
		List<Pair<Integer, Integer>> mentionIndices = parseMentionIndices(json, tweetText);
		List<String> mentionStrings = parseMentionStrings(json);
		return mergeListsIntoMap(mentionIndices, mentionStrings);
	}

	private List<Pair<Integer, Integer>> parseMentionIndices(JSONObject json, String tweetText) {
		Set<String> rawMentionIndices = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, rawMentionIndices, "user_mentions[].indices");
		return makeIndicesWithOffset(rawMentionIndices, tweetText);
	}

	private List<String> parseMentionStrings(JSONObject json) {
		Set<String> mentionScreenNames = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, mentionScreenNames, "user_mentions[].screen_name");
		return mentionScreenNames.stream()
				.map(mention -> "@" + mention).collect(Collectors.toList());
	}

	private Map<Pair<Integer, Integer>, String> parseURLs(JSONObject json, String tweetText) {
		List<Pair<Integer, Integer>> urlIndices = parseURLIndices(json, tweetText);
		List<String> urlStrings = parseURLStrings(json);
		List<String> displayURLStrings = parseDisplayURLStrings(json);
		List<String> combinedURLs = new ArrayList<>();
		for (int i = 0; i < urlStrings.size(); i++) {
			String url = urlStrings.get(i);
			if (url.equals(quotePermaLink)) { // Not interested in quote url, so just add empty element
				combinedURLs.add("");
			} else {
				combinedURLs.add(url + "|" + displayURLStrings.get(i)); // "expanded_url|display_url" for easy split
			}
		}
		return mergeListsIntoMap(urlIndices, combinedURLs);
	}

	private List<Pair<Integer, Integer>> parseURLIndices(JSONObject json, String tweetText) {
		Set<String> rawURLIndices = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, rawURLIndices, "urls[].indices");
		return makeIndicesWithOffset(rawURLIndices, tweetText);
	}

	private List<String> parseURLStrings(JSONObject json) {
		Set<String> urls = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, urls, "urls[].expanded_url");
		return new ArrayList<>(urls);
	}

	private List<String> parseDisplayURLStrings(JSONObject json) {
		Set<String> displayURLs = new LinkedHashSet<>();
		JsonUtils.addAllValues(json, displayURLs, "urls[].display_url");
		return new ArrayList<>(displayURLs);
	}

	private void parseImages(JSONObject json, Set<String> imageSet) {
		JsonUtils.addAllValues(json, imageSet, "entities.media[].media_url");
		JsonUtils.addAllValues(json, imageSet, "extended_entities.media[].media_url");
	}

	private void parseRetweetedTweet() {
		JSONObject retweetedTweetJSON = parentJSON.getJSONObject("retweeted_status");
		this.retweetCreatedDate = parseTwitterDate(retweetedTweetJSON);
		parseRetweetedUser(retweetedTweetJSON.getJSONObject("user"));
		parseMainTweetContent(retweetedTweetJSON);
	}

	private void parseRetweetedUser(JSONObject retweetedUserJSON) {
		this.retweetUserID = retweetedUserJSON.getLong("id");
		this.retweetUserName = retweetedUserJSON.getString("name");
		this.retweetUserScreenName = retweetedUserJSON.getString("screen_name");
		this.retweetUserProfileImage = retweetedUserJSON.getString("profile_image_url");
		this.retweetUserDescription = retweetedUserJSON.isNull("description") ? "No description."
				: retweetedUserJSON.getString("description");
		this.retweetUserFollowersCount = retweetedUserJSON.getInt("followers_count");
		this.retweetUserFriendsCount = retweetedUserJSON.getInt("friends_count");
	}

	private void parseMainTweetContent(JSONObject json) {
		// Longer tweets contain the 'extended_tweet' keyword while short tweets do without it.
		// However, 'extended_tweet.text' does not exist and is instead found under 'full_text'
		this.text = JsonUtils.getValueIfExistsByPriority(json, "extended_tweet.full_text", "text");
		this.likeCount = json.getInt("favorite_count");
		this.retweetCount = json.getInt("retweet_count");
		this.quoteCount = json.getInt( "quote_count");
		this.replyCount = json.getInt( "reply_count");

		JSONObject entityParentJSON = json.has("extended_tweet") ? json.getJSONObject("extended_tweet") : json;
		JSONObject entityJSON = entityParentJSON.getJSONObject("entities");
		this.hashtags = parseHashtags(entityJSON, text);
		this.mentions = parseMentions(entityJSON, text);
		this.urls = parseURLs(entityJSON, text);
		parseImages(entityParentJSON, imageURLStrings); // need to parse both 'entities' and 'extended_entities'
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


	public String getUserBackGroundImage() {
		return userBackGroundImage;
	}


	public Set<String> getImageURLStrings() {
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

	public Set<String> getQuoteImageURLStrings() {
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
}

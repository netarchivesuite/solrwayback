package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.SolrQueryUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for making html from twitter tweet-json.
 * TODO in need of overhaul to extrapolate responsibilities into separate classes
 */
public class Twitter2Html {
    private static final Logger log = LoggerFactory.getLogger(Twitter2Html.class);

    /**
     * The main method for this class.
     * Builds all of the html for a tweet by giving the JSON to a parser and inserting the parsed tweet data
     * in html tags that resemble the original tweet structure.
     * @param jsonString JSON representing the tweet.
     * @param crawlDate Date of the crawl.
     * @return The html making up the tweet
     */
    public static String twitter2Html(String jsonString, String crawlDate) throws IOException {
        Date date;
        long userID;
        String userName;
        String userScreenName;
        String userDescription;
        int userFriendsCount;
        int userFollowersCount;
        boolean userIsVerified;
        TwitterParser2 parser = new TwitterParser2(jsonString);
        String tweetID = parser.getTweetID();

        String cssFromFile = IOUtils.toString(
                TwitterParser2.class.getClassLoader().getResourceAsStream("twitter_playback_style.css"),
                StandardCharsets.UTF_8);
        String reactionsCss = getIconsCSS();
        String css = cssFromFile + reactionsCss;

        // Get user profile image
        String tweeterProfileImage = parser.isRetweet() ? parser.getRetweetUserProfileImage() : parser.getUserProfileImage();
        List<String> tweeterProfileImageList = Collections.singletonList(tweeterProfileImage);
        List<ImageUrl> tweeterProfileImageUrl = new ArrayList<>();
        try {
            tweeterProfileImageUrl = getImageUrlsFromSolr(tweeterProfileImageList, crawlDate);
        } catch (Exception e) {
            log.warn("Failed getting profile image from solr for main tweet '{}'..", tweetID, e);
        }

        // Get and format tweet text
        String mainTextHtml = formatTweetText(parser.getText(), parser.getTweetMinDisplayTextRange(),
                parser.getHashtags(), parser.getMentions(), parser.getURLs());

        // Get tweet images
        List<String> tweetImages = parser.getImageURLStrings();
        List<ImageUrl> tweetImageUrls = new ArrayList<>();
        try {
            tweetImageUrls = getImageUrlsFromSolr(tweetImages, crawlDate);
        } catch (Exception e) {
            log.warn("Failed getting images from solr for main tweet '{}..", tweetID, e);
        }

        if (parser.isRetweet()) {
            date = parser.getRetweetCreatedDate();
            userID = parser.getRetweetUserID();
            userName = parser.getRetweetUserName();
            userScreenName = parser.getRetweetUserScreenName();
            userDescription = parser.getRetweetUserDescription();
            userFriendsCount = parser.getRetweetUserFriendsCount();
            userFollowersCount = parser.getRetweetUserFollowersCount();
            userIsVerified = parser.isRetweetUserVerified();
        } else {
            date = parser.getCreatedDate();
            userID = parser.getUserID();
            userName = parser.getUserName();
            userScreenName = parser.getUserScreenName();
            userDescription = parser.getUserDescription();
            userFriendsCount = parser.getUserFriendsCount();
            userFollowersCount = parser.getUserFollowersCount();
            userIsVerified = parser.isUserVerified();
        }

        String html =
                "<!DOCTYPE html>"+
                "<html>"+
                "<head>"+
                  "<meta http-equiv='Content-Type' content='text/html;charset=UTF-8'>"+
                  "<meta name='viewport' content='width=device-width, initial-scale=1'>"+
                  "<title>"+getHeadTitle(parser)+"</title>"+
                  "<style>"+css+"</style>"+
                "</head>"+
                "<body>"+
                  "<div id='wrapper'>"+
                    "<div class='tweet'>"+
                      (parser.isRetweet() ? getRetweetHeader(parser, crawlDate) : "")+
                      "<div class='author'>"+
                        "<div class='user-wrapper'>"+
                          "<a href='"+ SolrQueryUtils.createTwitterSearchURL("tw_user_id:" + userID) +"'>"+
                            "<div class='avatar'>"+
                              imageUrlToHtml(tweeterProfileImageUrl)+
                            "</div>"+
                            "<div class='user-handles'>"+
                              "<h2>"+ userName +"</h2>"+
                              (userIsVerified ? "<span class='user-verified'></span>" : "") + // TODO: should probably be img
                              "<h4>@"+ userScreenName +"</h4>"+
                            "</div>"+
                          "</a>"+
                          makeUserCard(tweeterProfileImageUrl, userName, userScreenName, userDescription,
                                  userFriendsCount, userFollowersCount, userIsVerified)+
                        "</div>"+
                      "</div>"+
                      "<div class='item date'>"+
                        "<div>"+ date +"</div>"+
                      "</div>"+
                      (parser.getReplyToStatusID().isEmpty() ? "" : getReplyLine(parser.getReplyMentions(), parser.getReplyToStatusID()))+
                      // Few edge cases contain no main text - e.g. if tweet is a reply containing only a quote
                      (mainTextHtml.isEmpty() ? "" : "<div class='item text'>" + mainTextHtml+ "</div>")+
                      (tweetImageUrls.isEmpty() ? "" : "<div class='media'>"+ imageUrlToHtml(tweetImageUrls) +"</div>")+
                      (parser.hasQuote() ? getQuoteHtml(parser, crawlDate) : "")+
                      "<div class='item bottom-container'>"+
                        "<div class='reactions'>"+
                          "<span class='icon replies'></span>"+ // TODO: should probably be img
                          "<span class='number'>"+parser.getReplyCount()+"</span>"+
                          "<span class='icon retweets'></span>"+
                          "<span class='number'>"+parser.getRetweetCount()+"</span>"+
                          "<span class='icon quotes'></span>"+
                          "<span class='number'>"+parser.getQuoteCount()+"</span>"+
                          "<span class='icon likes'></span>"+
                          "<span class='number'>"+parser.getLikeCount()+"</span>"+
                        "</div>"+
                        "<span class='found-replies-text'>"+ foundRepliesToTweet(tweetID) +"</span>"+
                      "</div>"+
                    "</div>"+
                  "</div>"+
                "</body>"+
                "</html>";
        return html;
    }

    /**
     * Creates the CSS for the tweet reaction icons.
     * Because of the dynamic url path to the image location this is handled here instead of in the static css-file.
     * @return String containing css for the tweet reaction icons
     */
    private static String getIconsCSS() {
        String reactionIconsImageUrl = PropertiesLoader.WAYBACK_BASEURL + "images/twitter_sprite.png";
        return ".reactions span.replies {" +
                "background: transparent url(" + reactionIconsImageUrl + ") no-repeat -145px -50px;}" + // Missing correct icon?
                ".reactions span.retweets {" +
                "background: transparent url(" + reactionIconsImageUrl + ") no-repeat -180px -50px;}" +
                ".reactions span.likes {" +
                "background: transparent url(" + reactionIconsImageUrl + ") no-repeat -145px -130px;}" +
                ".reactions span.quotes {" +
                "background: transparent url(" + reactionIconsImageUrl + ") no-repeat -105px -50px;}" + // Missing correct icon
                "span.user-verified {" +
                "background: transparent url(" + reactionIconsImageUrl + ") no-repeat -67px -130px;}"; // TODO: using temp icon atm
    }

    /**
     * Searches Solr for the provided images and returns the found ImageUrls, if any, in a list.
     * @param imageUrls The image urls to search for in Solr.
     * @param crawlDate The date of the crawl to use in the search.
     * @return List of the found ImageUrls
     * @throws Exception If there is an issue with/while communicating with Solr.
     */
    private static List<ImageUrl> getImageUrlsFromSolr(List<String> imageUrls, String crawlDate) throws Exception {
        String imagesSolrQuery = SolrQueryUtils.createQueryStringForUrls(imageUrls);
        ArrayList<ArcEntryDescriptor> imageEntries = NetarchiveSolrClient.getInstance()
                .findImagesForTimestamp(imagesSolrQuery, crawlDate);
        return Facade.arcEntrys2Images(imageEntries);
    }

    /**
     * Formats the tweet text by inserting html links in place of hashtags, mentions and urls.
     * Also removes reply mentions (these will always be in the start of the text outside 'display_text_range')
     * and removes trailing image urls (if tweet contains images the text will always contain an url linking to images).
     * @param text Parsed text to format
     * @param minDisplayTextRange Minimum of the parsed 'display_text_range' used for cutting off reply mentions
     * @param entities Maps of indices and the corresponding hashtags/mentions/urls
     * @return The formatted text string to show in playback.
     */
    @SafeVarargs
    public static String formatTweetText(String text, int minDisplayTextRange, Map<Pair<Integer, Integer>, String>... entities) {
        StringBuilder textBuilder = new StringBuilder(text);
        formatEntitiesWithIndices(textBuilder, entities);

        String htmlText = newline2Br(textBuilder.toString());
        textBuilder = new StringBuilder(htmlText);
        textBuilder.replace(0, minDisplayTextRange, ""); // Remove reply mentions if any
        text = textBuilder.toString();
        text = text.replaceFirst("https:\\/\\/t\\.co\\/[a-zA-Z0-9]{10}$", ""); // Remove trailing image-URL if any
        return text;
    }

    /**
     * Replaces the entities (hashtags/mentions/urls) in the tweet text by using the direct indices of the entities.
     * This is done backwards through the text, end-to-start, to avoid having to keep track of an index offset while
     * replacing the entities.
     * @param textBuilder StringBuilder containing the text to replace the tags in.
     * @param entities Array of maps containing pairs of indices and the hashtags/mentions/urls placed on these indices.
     */
    private static void formatEntitiesWithIndices(StringBuilder textBuilder, Map<Pair<Integer, Integer>, String>[] entities) {
        // Merge hashtags, mentions and urls
        Map<Pair<Integer, Integer>, String> allEntities = new LinkedHashMap<>();
        for (Map<Pair<Integer, Integer>, String> entityType : entities) {
            allEntities.putAll(entityType);
        }

        List<Pair<Integer, Integer>> entityIndices = new ArrayList<>(allEntities.keySet());
        entityIndices.sort(Comparator.comparing(Pair::getLeft)); // Sort entities to ensure replacement is done in order
        Collections.reverse(entityIndices); // Reverse to insert entities in text from end to start
        try {
            for (Pair<Integer, Integer> entityIndexPair : entityIndices) {
                String entityTag = allEntities.get(entityIndexPair);
                int startIndex = entityIndexPair.getLeft();
                int endIndex = entityIndexPair.getRight();
                String entityHTML;
                if (!entityTag.isEmpty() && (entityTag.charAt(0) == '#' || entityTag.charAt(0) == '@')) {
                    entityHTML = makeTagHTML(entityTag);
                } else {
                    entityHTML = makeURLHtml(entityTag);
                }
                log.debug("Inserting '{}' at indices {},{}", entityHTML, startIndex, endIndex);
                textBuilder.replace(startIndex, endIndex, entityHTML);
            }
        } catch (Exception e) { // Shouldn't happen
            log.warn("Failed replacing raw tags with solr search links", e);
        }
    }

    /**
     * Creates the html for a hashtag or mention in the tweet.
     * This html will contain a link to search SolrWayback for the hashtag or tweets by this user or other mentions of them.
     * @param entityTag The tag (hashtag/mention) to make html for.
     * @return A string containing the html for the tag
     */
    private static String makeTagHTML(String entityTag) {
        String tagWithoutPrefixSymbol = entityTag.substring(1);
        String searchString;
        if (entityTag.charAt(0) == '#') {
            searchString = "keywords%3A" + tagWithoutPrefixSymbol;
        } else { // charAt(0) == '@'
            searchString = "(author:" + tagWithoutPrefixSymbol + " OR tw_user_mentions:"
                    + tagWithoutPrefixSymbol.toLowerCase() + ")";
        }
        String searchUrl = SolrQueryUtils.createTwitterSearchURL(searchString);
        return "<span><a href='" + searchUrl + "'>" + entityTag + "</a></span>";
    }

    /**
     * Makes the html for a given url. Is currently a bit hacky, so expects urls to be of the form
     * 'expanded_url|display_url'. Also if the given url is an empty string no html is generated. This is because of
     * not wanting to show urls linking to quotes although these urls will show in text if tweet contains quote.
     * @param entityTag Entity which should be an url of the form 'expanded_url|display_url'
     * @return html for url
     */
    private static String makeURLHtml(String entityTag) {
        String entityHTML;
        if (entityTag.isEmpty()) { // Should atm. only happen when encountering quote URL
            entityHTML = "";
        } else {
            String[] urls = entityTag.split("\\|");
            String url = urls[0];
            String displayURL = urls[1];
            entityHTML = "<span><a href='" + url + "'>" + displayURL + "</a></span>";
        }
        return entityHTML;
    }

    /**
     * Converts '\n' to the corresponding '<br>' tags in html
     * @param text Text to convert.
     * @return New string with newline characters replaced by <br>
     */
    private static String newline2Br(String text) {
        if (text == null){
            return "";
        }
        return text.replace("\n","<br>");
    }

    /**
     * Generates the content for the html title tag. Depends on if retweet or standard tweet.
     * @param parser The parser.
     * @return String to put inside <title/> tag.
     */
    private static String getHeadTitle(TwitterParser2 parser) {
        String titlePrefix = parser.isRetweet() ? "Retweet by: " : "Tweet by: ";
        return titlePrefix + parser.getUserName() + " (userID: " + parser.getUserID() + ")";
    }

    /**
     * Generates the html for the retweet header above a tweet stating the retweeter and their info etc.
     * @param parser The parser.
     * @param crawlDate The date of the crawl to search for profile image in Solr.
     * @return html making up the retweet header
     */
    private static String getRetweetHeader(TwitterParser2 parser, String crawlDate) {
        List<ImageUrl> profileImageUrl = new ArrayList<>();
        try {
            List<String> retweetProfileImage = Collections.singletonList(parser.getUserProfileImage());
            profileImageUrl = getImageUrlsFromSolr(retweetProfileImage, crawlDate);
        } catch (Exception e) {
            log.warn("Failed getting profile image of retweeter with username '{}'", parser.getUserName());
        }
        String html =
                "<div class='retweet-author'>" +
                    "<div class='retweet-text-wrap'>"+
                        "<div class='user-wrapper'>" +
                            "<a href='" + SolrQueryUtils.createTwitterSearchURL("tw_user_id:" + parser.getUserID()) + "'>" +
                                "<h3>" + parser.getUserName() + " Retweeted</h3>" +
                            "</a>" +
                            makeUserCard(profileImageUrl, parser.getUserName(),
                                    parser.getUserScreenName(), parser.getUserDescription(),
                                    parser.getUserFriendsCount(), parser.getUserFollowersCount(),
                                    parser.isUserVerified()) +
                        "</div>" +
                        "<div class='date'>&middot " + parser.getCreatedDate() + "</div>" +
                    "</div>" +
                    makeButtonLinkingToTweet(parser.getRetweetedTweetID(), "View original tweet") +
                "</div>";
        return html;
    }

    /**
     * Searches Solr for replies to the tweet given by the tweet ID and returns a string stating if any replies
     * were found. If any replies were found, it will also contain a link to search for the replies in SolrWayback.
     * @param tweetID ID of a tweet
     * @return String stating if any replies were found.
     */
    private static String foundRepliesToTweet(String tweetID) {
        String foundRepliesLine = "";
        try {
            SearchResult searchResult = Facade.search("tw_reply_to_tweet_id:" + tweetID, null);
            long resultCount = searchResult.getNumberOfResults();
            String searchLink = SolrQueryUtils.createTwitterSearchURL("tw_reply_to_tweet_id:" + tweetID);
            if (resultCount > 0) {
                log.info("Found replies to tweet id {}.. {}", tweetID, resultCount);
                foundRepliesLine = "Found <a href='" + searchLink + "'>" + resultCount + "</a> replies"; // TODO for some reason shows 0 right now??
            } else {
                log.info("Didn't find any replies to tweet {}", tweetID);
                foundRepliesLine = "No replies found to tweet";
            }
        } catch (Exception e) {
            log.warn("Error while trying to find replies for tweet " + tweetID);
        }
        return foundRepliesLine;
    }

    /**
     * Converts a list of image URLs to a string of html img tags.
     * @param images Images to make html for
     * @return String of concatenated img tags
     */
    public static String imageUrlToHtml(List<ImageUrl> images){
        StringBuilder b = new StringBuilder();
        for (ImageUrl image : images){
            b.append("<img src='")
                    .append(image.getDownloadUrl())
                    .append("'/>\n");
        }
        return b.toString();
    }

    /**
     * Method for creating the html for a user card - i.e. the little box with user info shown on hover over a user
     * @param profileImageUrl List containing the profile image url. Should only contain one element.
     * @param userName Username for user.
     * @param userHandle User handle/screen name.
     * @param description User description found in their profile.
     * @param followingCount User following count.
     * @param followersCount User followers count.
     * @param verified Boolean specifying if user is verified.
     * @return html for user card
     */
    private static String makeUserCard(List<ImageUrl> profileImageUrl, String userName, String userHandle,
                                       String description, int followingCount, int followersCount, boolean verified) {
        return "<div class='user-card'>" +
                    "<div class='author'>" +
                        "<div class='avatar'>" + imageUrlToHtml(profileImageUrl) + "</div>" +
                        "<div class='user-handles'>" +
                            "<h2>" + userName + "</h2>" +
                            (verified ? "<span class='user-verified'></span>" : "") +
                            "<h4>@" + userHandle +"</h4>" +
                        "</div>" +
                    "</div>" +
                    "<span class='item user-desc'>" + description + "</span>" +
                    "<div class='follow-info'>" +
                        "<div class='following'>" +
                            "<span class='follow-num'>" + followingCount + "</span>" +
                            "<span> Following</span>" +
                        "</div>" +
                        "<div class='followers'>" +
                            "<span class='follow-num'>" + followersCount + "</span>" +
                            "<span> Followers</span>" +
                        "</div>" +
                    "</div>" +
                "</div>";
    }

    /**
     * Creates the html specific for a reply tweet. This consists of reply mentions (people being replied to) with
     * links to SolrWayback searches for the individual users and a button that links to the tweet being replied to.
     * @param replyMentions List of people being replied to.
     * @param replyToTweetID ID for the tweet being replied to.
     * @return Html for the reply line in a reply tweet.
     */
    private static String getReplyLine(List<String> replyMentions, String replyToTweetID) {
        // TODO collapse on more than 3 mentions
        List<String> replyMentionsHTML = new ArrayList<>();
        for (String replyMention : replyMentions) {
            replyMentionsHTML.add(makeTagHTML(replyMention));
        }

        Iterator<String> iterator = replyMentionsHTML.iterator();
        String firstMentionHTML = iterator.next();
        StringBuilder replyTagsHTML = new StringBuilder(firstMentionHTML);
        while (iterator.hasNext()) {
            String mention = iterator.next();
            if (!iterator.hasNext()) {
                replyTagsHTML.append(" and ").append(mention);
            } else {
                replyTagsHTML.append(" ").append(mention);
            }
        }

        String replyHTML =  "<div class='reply-line'>" +
                              "<div class='reply-line-text'>" +
                                "Replying to " +
                                "<div class='reply-tags'>" +
                                  replyTagsHTML +
                                "</div>" +
                              "</div>" +
                                makeButtonLinkingToTweet(replyToTweetID, "View tweet replied to") +
                            "</div>";
        return replyHTML;
    }

    /**
     * Creates the html for a button linking to a SolrWayback search for a tweet.
     * Empty string is returned if exception is thrown.
     * @param tweetID ID of the tweet to search for.
     * @param buttonText Explanatory text to put on button.
     * @return Html for a button linking to search for a tweet or empty string
     */
    private static String makeButtonLinkingToTweet(String tweetID, String buttonText) {
        String buttonHTML = "";
        try {
            SearchResult searchResult = Facade.search("tw_tweet_id:" + tweetID, null);
            List<IndexDoc> results = searchResult.getResults();
            if (results.isEmpty()) {
                return buttonHTML;
            }

            IndexDoc tweet = results.get(0);
            String filePath = tweet.getSource_file_path();
            long fileOffset = tweet.getOffset();
            buttonHTML = "<div class='button-wrap'>" +
                            "<a href='" + getPlaybackUrl(filePath, fileOffset) + "'>" +
                                "<button type='button'>" + buttonText + "</button>" +
                            "</a>" +
                        "</div>";
        } catch (Exception e) {
            log.warn("Failed solr search for tweet '{}'", tweetID, e);
        }
        return buttonHTML;
    }

    /**
     * Builds the tweet playback url from the given source file path and its offset. TODO consider moving to utils class
     * @param filePath File path to warc file containing the content to show.
     * @param fileOffset Offset to look at in the file.
     * @return The full wayback url for playback.
     */
    private static String getPlaybackUrl(String filePath, long fileOffset) {
        return PropertiesLoader.WAYBACK_BASEURL +
                "services/viewForward?source_file_path=" + filePath + "&offset=" + fileOffset;
    }

    /**
     * Creates the html for a quote as part of a tweet.
     * @param parser The parser.
     * @param crawlDate The date of the crawl used for searching solr for images in the quote.
     * @return All the html for a quote as a string
     */
    private static String getQuoteHtml(TwitterParser2 parser, String crawlDate) {
        List<ImageUrl> quoteProfileImageUrl = new ArrayList<>();
        List<ImageUrl> quoteImageUrls = new ArrayList<>();
        try {
            List<String> quoteProfileImage = Collections.singletonList(parser.getQuoteUserProfileImage());
            quoteProfileImageUrl = getImageUrlsFromSolr(quoteProfileImage, crawlDate);
            List<String> quoteImages = parser.getQuoteImageURLStrings();
            quoteImageUrls = getImageUrlsFromSolr(quoteImages, crawlDate);
        } catch (Exception e) {
            log.warn("Failed getting images for quote in tweet by '{}'", parser.getUserName(), e);
        }

        String quoteHtml =
                "<div class='quote'>" +
                    "<div class='author-container'>" +
                        "<div class='author'>" +
                            "<div class='user-wrapper'>" +
                                "<a href='" + SolrQueryUtils.createTwitterSearchURL("tw_user_id:" + parser.getQuoteUserID()) + "'>" +
                                    "<div class='avatar'>" + imageUrlToHtml(quoteProfileImageUrl) + "</div>" +
                                    "<div class='user-handles'>" +
                                        "<h2>" + parser.getQuoteUserName() + "</h2>" +
                                        (parser.isQuoteUserVerified() ? "<span class='user-verified'></span>" : "") +
                                        "<h4>@" + parser.getQuoteUserScreenName() + "</h4>" +
                                    "</div>" +
                                "</a>" +
                                makeUserCard(quoteProfileImageUrl, parser.getQuoteUserName(),
                                        parser.getQuoteUserScreenName(), parser.getQuoteUserDescription(),
                                        parser.getQuoteUserFriendsCount(), parser.getQuoteUserFollowersCount(),
                                        parser.isQuoteUserVerified()) +
                            "</div>" +
                        "</div>" +
                        makeButtonLinkingToTweet(parser.getQuoteTweetID(), "View quote tweet") +
                    "</div>" +
                    "<div class='item date'>" +
                        "<div>" + parser.getQuoteCreatedDate() + "</div>" +
                    "</div>" +
                    (parser.getQuoteReplyToStatusID().isEmpty() ? "" : getReplyLine(parser.getQuoteReplyMentions(), parser.getQuoteReplyToStatusID())) +
                    "<div class='item text'>" +
                        formatTweetText(parser.getQuoteText(), parser.getQuoteMinDisplayTextRange(),
                                parser.getQuoteHashtags(), parser.getQuoteMentions(), parser.getQuoteURLs()) +
                    "</div>" +
                    (quoteImageUrls.isEmpty() ? "" : "<div class='media'>" + imageUrlToHtml(quoteImageUrls) + "</div>") +
                "</div>";

        return quoteHtml;
    }
}

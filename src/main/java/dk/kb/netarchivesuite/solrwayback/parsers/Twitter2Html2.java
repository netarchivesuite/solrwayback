package dk.kb.netarchivesuite.solrwayback.parsers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.parsers.json.Tweet;
import dk.kb.netarchivesuite.solrwayback.parsers.json.TweetUser;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.SolrQueryUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Twitter2Html2 {
    private static final Logger log = LoggerFactory.getLogger(Twitter2Html.class);

    private static String crawlerDate;
    private static Tweet tweet;
    private static TweetUser mainUser;
    private static Tweet mainContentTweet;
    private static TwitterHelper helper;

    // TODO this is fugly. Consider making non-static
    public static String twitter2Html(String twitterJSON, String crawlDate) throws IOException {
        crawlerDate = crawlDate;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        tweet = mapper.readValue(twitterJSON, Tweet.class);
        mainUser = tweet.getUser();
        mainContentTweet = helper.getMainContentTweet();
        helper = new TwitterHelper(tweet, crawlDate);


        String cssFromFile = IOUtils.toString(
                TwitterParser2.class.getClassLoader().getResourceAsStream("twitter_playback_style.css"),
                StandardCharsets.UTF_8);
        String reactionsCss = getIconsCSS();
        String css = cssFromFile + reactionsCss;

        TweetUser contentAuthor = helper.getMainContentAuthor();
        List<String> contentAuthorProfileImageList = Collections.singletonList(contentAuthor.getProfileImageUrl());
        List<ImageUrl> contentAuthorProfileImageUrl = new ArrayList<>();
        try {
            contentAuthorProfileImageUrl = getImageUrlsFromSolr(contentAuthorProfileImageList);
        } catch (Exception e) {
            log.warn("Failed getting profile image from solr for tweet '{}'..", tweet.getId(), e);
        }

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                  "<meta http-equiv='Content-Type' content='text/html;charset=UTF-8'>" +
                  "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                  "<title>" + getHeadTitle() + "</title>" +
                  "<style>" + css + "</style>" +
                "</head>" +
                "<body>" +
                  "<div id='wrapper'>" +
                    "<div class='tweet'>" +
                      (tweet.isRetweet() ? getRetweetHeader() : "") + // TODO throw this and below author div into own method
                      "<div class='author'>" +
                        "<div class='user-wrapper'>" +
                          "<a href='" + SolrQueryUtils.createTwitterSearchURL(
                                  "tw_user_id:" + helper.getMainContentAuthor().getId()) + "'>" +
                            "<div class='avatar'>" +
                              imageUrlToHtml(contentAuthorProfileImageUrl)+
                            "</div>" +
                            "<div class='user-handles'>" +
                              "<h2>" + helper.getMainContentAuthor().getName() + "</h2>" +
                              (helper.getMainContentAuthor().isVerified() ? "<span class='user-verified'></span>" : "") + // TODO: should probably be img
                              "<h4>@" + helper.getMainContentAuthor().getScreenName() + "</h4>" +
                            "</div>" +
                          "</a>" +
                          makeUserCard(contentAuthorProfileImageUrl, contentAuthor.getName(),
                                  contentAuthor.getScreenName(), contentAuthor.getDescription(),
                                  contentAuthor.getFriendsCount(), contentAuthor.getFollowersCount(),
                                  contentAuthor.isVerified())+
                        "</div>" +
                      "</div>" +
                      "<div class='item date'>" +
                        "<div>" + helper.getMainContentTweet().getCreationDate() + "</div>" +
                      "</div>"+
                      (helper.getMainContentTweet().getInReplyToTweetId() == null ? "" : getReplyLine(parser.getReplyMentions(), parser.getReplyToStatusID()))+
                      // Few edge cases contain no main text - e.g. if tweet is a reply containing only a quote
                      (mainTextHtml.isEmpty() ? "" : "<div class='item text'>" + mainTextHtml+ "</div>")+
                      (tweetImageUrls.isEmpty() ? "" : "<div class='media'>"+ imageUrlToHtml(tweetImageUrls) +"</div>")+
                      (parser.hasQuote() ? getQuoteHtml(parser, crawlDate) : "")+
                      "<div class='item bottom-container'>" +
                        "<div class='reactions'>" +
                          "<span class='icon replies'></span>" + // TODO: should probably be img
                          "<span class='number'>" + mainContentTweet.getReplyCount() + "</span>" +
                          "<span class='icon retweets'></span>" +
                          "<span class='number'>" + mainContentTweet.getRetweetCount() + "</span>" +
                          "<span class='icon quotes'></span>" +
                          "<span class='number'>" + mainContentTweet.getQuoteCount() + "</span>" +
                          "<span class='icon likes'></span>" +
                          "<span class='number'>" + mainContentTweet.getFavoriteCount() + "</span>" +
                        "</div>" +
                        "<span class='found-replies-text'>" + foundRepliesToTweet(tweetID) + "</span>" +
                      "</div>" +
                    "</div>" +
                  "</div>" +
                "</body>" +
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
     * Generates the html for the retweet header above a tweet stating the retweeter and their info etc.
     * @return html making up the retweet header
     */
    private static String getRetweetHeader() {
        List<ImageUrl> profileImageUrl = new ArrayList<>();
        try {
            List<String> retweetProfileImage = Collections.singletonList(mainUser.getProfileImageUrl());
            profileImageUrl = getImageUrlsFromSolr(retweetProfileImage);
        } catch (Exception e) {
            log.warn("Failed getting profile image of retweeter with name '{}'", mainUser.getName());
        }
        String html =
                "<div class='retweet-author'>" +
                  "<div class='retweet-text-wrap'>" +
                    "<div class='user-wrapper'>" +
                      "<a href='" + SolrQueryUtils.createTwitterSearchURL("tw_user_id:" + mainUser.getId()) + "'>" +
                        "<h3>" + mainUser.getName() + " Retweeted</h3>" +
                      "</a>" +
                      makeUserCard(profileImageUrl, mainUser.getName(),
                              mainUser.getScreenName(), mainUser.getDescription(),
                              mainUser.getFriendsCount(), mainUser.getFollowersCount(),
                              mainUser.isVerified()) +
                    "</div>" +
                    "<div class='date'>&middot " + tweet.getCreationDate() + "</div>" +
                  "</div>" +
                  makeButtonLinkingToTweet(tweet.getRetweetedTweet().getId(), "View original tweet") +
                "</div>";
        return html;
    }

    /**
     * Searches Solr for the provided images and returns the found ImageUrls, if any, in a list.
     * @param imageUrls The image urls to search for in Solr.
     * @return List of the found ImageUrls
     * @throws Exception If there is an issue with/while communicating with Solr.
     */
    private static List<ImageUrl> getImageUrlsFromSolr(List<String> imageUrls) throws Exception {
        String imagesSolrQuery = SolrQueryUtils.createQueryStringForUrls(imageUrls);
        ArrayList<ArcEntryDescriptor> imageEntries = NetarchiveSolrClient.getInstance()
                .findImagesForTimestamp(imagesSolrQuery, crawlerDate);
        return Facade.arcEntrys2Images(imageEntries);
    }

    /**
     * Generates the content for the html title tag. Depends on if retweet or standard tweet.
     * @return String to put inside <title/> tag.
     */
    public static String getHeadTitle() {
        String titlePrefix = tweet.isRetweet() ? "Retweet by: " : "Tweet by: ";
        return titlePrefix + mainUser.getName() + " (userID: " + mainUser.getId() + ")";
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
                                       String description, int followingCount, long followersCount, boolean verified) {
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

            buttonHTML =
                    "<div class='button-wrap'>" +
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
            String replyMentionHTML = makeTagHTML(replyMention);
            replyMentionsHTML.add(replyMentionHTML);
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

        String replyHTML =
                "<div class='reply-line'>" +
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
}

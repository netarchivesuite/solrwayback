package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Twitter2Html {
    private static final Logger log = LoggerFactory.getLogger(Twitter2Html.class);

    public static String twitter2Html(String jsonString, String crawlDate) throws Exception{
        Date date;
        long userID;
        String userName;
        String userScreenName;
        String userDescription;
        int userFriendsCount;
        int userFollowersCount;
        boolean userIsVerified;
        TwitterParser2 parser = new TwitterParser2(jsonString);

        String cssFromFile = IOUtils.toString(
                TwitterParser2.class.getClassLoader().getResourceAsStream("twitter_playback_style.css"),
                StandardCharsets.UTF_8);
        String reactionsCss = getIconsCSS();
        String css = cssFromFile + reactionsCss;

        // Get user profile image
        String tweeterProfileImage = parser.isRetweet() ? parser.getRetweetUserProfileImage() : parser.getUserProfileImage();
        List<String> tweeterProfileImageList = Collections.singletonList(tweeterProfileImage);
        List<ImageUrl> tweeterProfileImageUrl = getImageUrlsFromSolr(tweeterProfileImageList, crawlDate);

        // Get and format tweet text
        String mainTextHtml = formatTweetText(parser.getText(), parser.getTweetMinDisplayTextRange(),
                parser.getHashtags(), parser.getMentions(), parser.getURLs());

        // Get tweet images
        List<String> tweetImages = parser.getImageURLStrings();
        ArrayList<ImageUrl> tweetImageUrls = getImageUrlsFromSolr(tweetImages, crawlDate);

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
                          "<a href='"+ makeSolrSearchLink("tw_user_id:" + userID) +"'>"+
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
                      (tweetImageUrls.isEmpty() ? "" : "<div class='images'>"+ imageUrlToHtml(tweetImageUrls) +"</div>")+
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
                        "<span class='found-replies-text'>"+ foundRepliesToTweet(parser.getTweetID()) +"</span>"+
                      "</div>"+
                    "</div>"+
                  "</div>"+
                "</body>"+
                "</html>";
        return html;
    }

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

    private static ArrayList<ImageUrl> getImageUrlsFromSolr(List<String> imagesList, String crawlDate) throws Exception {
        String imagesSolrQuery = Facade.queryStringForImages(imagesList);
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

    private static void formatEntitiesWithIndices(StringBuilder textBuilder, Map<Pair<Integer, Integer>, String>[] entities) {
        // Merge hashtags, mentions and urls
        Map<Pair<Integer, Integer>, String> allEntities = new LinkedHashMap<>();
        for (Map<Pair<Integer, Integer>, String> entityType : entities) {
            allEntities.putAll(entityType);
        }

        List<Pair<Integer, Integer>> entityIndices = new ArrayList<>(allEntities.keySet());
        entityIndices.sort(Comparator.comparing(Pair::getLeft));
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

    private static String makeTagHTML(String entityTag) {
        String tagWithoutPrefixSymbol = entityTag.substring(1);
        String searchString;
        if (entityTag.charAt(0) == '#') {
            searchString = "keywords%3A" + tagWithoutPrefixSymbol;
        } else { // charAt(0) == '@'
            searchString = "(author:" + tagWithoutPrefixSymbol + " OR tw_user_mentions:"
                    + tagWithoutPrefixSymbol.toLowerCase() + ")";
        }
        String searchUrl = makeSolrSearchLink(searchString);
        return "<span><a href='" + searchUrl + "'>" + entityTag + "</a></span>";
    }

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

    private static String newline2Br(String text) {
        if (text == null){
            return "";
        }
        return text.replace("\n","<br>");
    }

    private static String getHeadTitle(TwitterParser2 parser) {
        String titlePrefix = parser.isRetweet() ? "Retweet by: " : "Tweet by: ";
        return titlePrefix + parser.getUserName() + " (userID: " + parser.getUserID() + ")";
    }

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
                            "<a href='" + makeSolrSearchLink("tw_user_id:" + parser.getUserID()) + "'>" +
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

    private static String foundRepliesToTweet(String tweetID) {
        String foundRepliesLine = "";
        try {
            SearchResult searchResult = Facade.search("tw_reply_to_tweet_id:", null);
            int resultCount = searchResult.getResults().size();
            String searchLink = makeSolrSearchLink("tw_reply_to_tweet_id:" + tweetID);
            foundRepliesLine = "Found <a href='" + searchLink + "'>" + resultCount + "</a> replies"; // TODO for some reason shows 0 right now??
        } catch (Exception e) {
            log.warn("Error while trying to find replies for tweet " + tweetID);
        }
        return foundRepliesLine;
    }

    private static String makeSolrSearchLink(String searchString) {
        String searchParams = " AND type%3A\"Twitter Tweet\"";
        return PropertiesLoader.WAYBACK_BASEURL + "search?query=" + searchString + searchParams;
    }

    public static String imageUrlToHtml(List<ImageUrl> images){
        StringBuilder b = new StringBuilder();
        for (ImageUrl image : images){
            b.append("<img src='")
                    .append(image.getDownloadUrl())
                    .append("'/>\n");
        }
        return b.toString();
    }

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

    private static String makeButtonLinkingToTweet(String tweetID, String buttonText) {
        String output = "";
        try {
            SearchResult searchResult = Facade.search("tw_tweet_id:" + tweetID, null);
            List<IndexDoc> results = searchResult.getResults();
            if (results.isEmpty()) {
                return output;
            }

            IndexDoc tweet = results.get(0);
            String filePath = tweet.getSource_file_path();
            long fileOffset = tweet.getOffset();
            output =    "<div class='button-wrap'>" +
                            "<a href='" + PropertiesLoader.WAYBACK_BASEURL + "services/viewForward?source_file_path=" +
                                filePath + "&offset=" + fileOffset + "'>" +
                                "<button type='button'>" + buttonText + "</button>" +
                            "</a>" +
                        "</div>";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

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
                                "<a href='" + makeSolrSearchLink("tw_user_id:" + parser.getQuoteUserID()) + "'>" +
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
                    (quoteImageUrls.isEmpty() ? "" : "<div class='images'>" + imageUrlToHtml(quoteImageUrls) + "</div>") +
                "</div>";

        return quoteHtml;
    }
}

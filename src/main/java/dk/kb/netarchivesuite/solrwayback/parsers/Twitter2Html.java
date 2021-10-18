package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Twitter2Html {
    private static final Logger log = LoggerFactory.getLogger(Twitter2Html.class);
    public static String twitter2Html(String jsonString, String crawlDate) throws Exception{
        StringBuilder b = new StringBuilder();
        TwitterParser2 parser = new TwitterParser2(jsonString);
        String iconsImage = PropertiesLoader.WAYBACK_BASEURL + "images/twitter_sprite.png";

        // Get user profile image
        String tweeterProfileImage = parser.isRetweet() ? parser.getRetweetUserProfileImage() : parser.getUserProfileImage();
        List<String> tweeterProfileImageList = Collections.singletonList(tweeterProfileImage);
        ArrayList<ImageUrl> tweeterProfileImageUrl = getImageUrlsFromSolr(tweeterProfileImageList, crawlDate);

        // Get and format tweet text
        String textReplaced = newline2Br(parser.getText());
        //TODO frontend fix so all other params not needed
        String otherSearchParams = " AND type%3A\"Twitter Tweet\"&start=0&filter=&imgsearch=false&imggeosearch=false&grouping=false";
        // TODO RBKR fix these methods somehow.. ugly compromise for now.
        textReplaced = formatMentions(textReplaced, parser.getMentions(), PropertiesLoaderWeb.WAYBACK_SERVER,
                otherSearchParams);
        textReplaced = formatHashtags(textReplaced, parser.getHashTags(), PropertiesLoaderWeb.WAYBACK_SERVER,
                otherSearchParams);

        // Get tweet images
        List<String> tweetImages = new ArrayList<>(parser.getImageUrlsList());
        ArrayList<ImageUrl> tweetImageUrls = getImageUrlsFromSolr(tweetImages, crawlDate);

        String cssFromFile = IOUtils.toString(
                TwitterParser2.class.getClassLoader().getResourceAsStream("twitter_playback_style.css"),
                StandardCharsets.UTF_8);
        String css = cssFromFile +
                ".item.reactions span.replies {" +
                    "background: transparent url(" + iconsImage + ") no-repeat -145px -50px;" + // Missing correct icon?
                "}" +
                ".item.reactions span.retweets {" +
                    "background: transparent url(" + iconsImage + ") no-repeat -180px -50px;" +
                "}" +
                ".item.reactions span.likes {" +
                    "background: transparent url(" + iconsImage + ") no-repeat -145px -130px;" +
                "}" +
                ".item.reactions span.quotes {" +
                    "background: transparent url(" + iconsImage + ") no-repeat -105px -50px;" + // Missing correct icon
                "}";

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
                      "<div class='item author'>"+
                        "<div class='user-wrapper'>"+
                          "<a href='#'>"+ // TODO insert search link
                            "<span class='avatar'>"+
                              imageUrlToHtml(tweeterProfileImageUrl)+
                            "</span>"+
                            "<div class='user-handles'>"+
                              "<h2>"+ (parser.isRetweet() ? parser.getRetweetUserName() : parser.getUserName()) +"</h2>"+
                              "<h4>@"+ (parser.isRetweet() ? parser.getRetweetUserScreenName() : parser.getUserScreenName()) +"</h4>"+
                            "</div>"+
                          "</a>"+
                          makeUserCard(tweeterProfileImageUrl,
                                  parser.isRetweet() ? parser.getRetweetUserName() : parser.getUserName(),
                                  parser.isRetweet() ? parser.getRetweetUserScreenName() : parser.getUserScreenName(),
                                  parser.isRetweet() ? parser.getRetweetUserDescription() : parser.getUserDescription(),
                                  parser.isRetweet() ? parser.getRetweetUserFriendsCount() : parser.getUserFriendsCount(),
                                  parser.isRetweet() ? parser.getRetweetUserFollowersCount() : parser.getUserFollowersCount())+
                        "</div>"+
                      "</div>"+
                      "<div class='item date'>"+
                        "<div>"+parser.getCreatedDate()+"</div>"+
                      "</div>"+
                      "<div class='item text'>"+
                        textReplaced+
                      "</div>"+
                      (tweetImageUrls.isEmpty() ? "" : "<span class='image'>"+ imageUrlToHtml(tweetImageUrls)) +"</span>"+ // TODO RBKR prettify
                      (parser.hasQuote() ? getQuoteHtml(parser, crawlDate) : "")+
                      "<div class='item reactions'>"+
                        "<span class='icon replies'></span>"+
                        "<span class='number'>"+parser.getReplyCount()+"</span>"+
                        "<span class='icon retweets'></span>"+
                        "<span class='number'>"+parser.getRetweetCount()+"</span>"+
                        "<span class='icon likes'></span>"+
                        "<span class='number'>"+parser.getLikeCount()+"</span>"+
                        "<span class='icon quotes'></span>"+
                        "<span class='number'>"+parser.getQuoteCount()+"</span>"+
                      "</div>"+
                    "</div>"+
                  "</div>"+
                "</body>"+
                "</html>";

        return html;
    }

    private static ArrayList<ImageUrl> getImageUrlsFromSolr(List<String> imagesList, String crawlDate) throws Exception {
        String imagesSolrQuery = Facade.queryStringForImages(imagesList);
        ArrayList<ArcEntryDescriptor> imageEntries = NetarchiveSolrClient.getInstance()
                .findImagesForTimestamp(imagesSolrQuery, crawlDate);
        return Facade.arcEntrys2Images(imageEntries);
    }

    private static String makeUserCard(ArrayList<ImageUrl> profileImageUrl, String userName, String userHandle,
                                       String description, int followingCount, int followersCount) {
        return "<div class='user-card'>" +
                    "<div class='item author'>" +
                        "<span class='avatar'>" + imageUrlToHtml(profileImageUrl) + "</span>" +
                        "<div class='user-handles'>" +
                            "<h2>" + userName + "</h2>" +
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

    private static String getHeadTitle(TwitterParser2 parser) {
        String titlePrefix = parser.isRetweet() ? "Retweet by: " : "Tweet by: ";
        return titlePrefix + parser.getUserName();
    }

    // TODO RBKR replace hashtags and mentions using direct indices instead of string replacement
    public static String formatMentions(String text, Set<String> mentions, String solrwaybackUrl, String extraSearchParams) {
        for (String mention : mentions) {
            String searchUrl = solrwaybackUrl + "?query=%40" + mention + extraSearchParams;
            String mentionWithLink = "<span><a href='" + searchUrl + "'>@" + mention + "</a></span>";
            text = text.replaceAll("@" + mention, mentionWithLink);
        }
        return text;
    }

    /*HashTags are in clear text without # in front.
     * Replace this with a link that searches for the tag.
     */
    public static String formatHashtags(String text, Set<String> tags, String solrwaybackUrl, String extraSearchParams) {
        log.info("tags replace called for text: '{}' with tags: {}", text, tags);
        for (String tag : tags) {
            log.info("replacing tag: {}", tag);
            String searchUrl = solrwaybackUrl + "?query=keywords%3A" + tag + extraSearchParams;
            String tagWithLink = "<span><a href='" + searchUrl + "'>#" + tag + "</a></span>";
            text = text.replaceAll("#" + tag, tagWithLink);
        }
        return text;
    }


    private static String getQuoteHtml(TwitterParser2 parser, String crawlDate) {
        String quoteHtml = "";

        try {
            List<String> quoteProfileImage = Collections.singletonList(parser.getQuoteUserProfileImage());
            ArrayList<ImageUrl> quoteProfileImageUrl = getImageUrlsFromSolr(quoteProfileImage, crawlDate);
            List<String> quoteImages = new ArrayList<>(parser.getQuoteImageUrlStrings());
            ArrayList<ImageUrl> quoteImageUrls = getImageUrlsFromSolr(quoteImages, crawlDate);

            quoteHtml =
                    "<div class='quote'>" +
                        "<div class='item author'>" +
                            "<div class='user-wrapper'>" +
                                "<a href='#'>" +
                                    "<span class='avatar'>" + imageUrlToHtml(quoteProfileImageUrl) + "</span>" +
                                    "<div class='user-handles'>" +
                                        "<h2>" + parser.getQuoteUserName() + "</h2>" +
                                        "<h4>@" + parser.getQuoteUserScreenName() + "</h4>" +
                                    "</div>" +
                                "</a>" +
                                makeUserCard(quoteProfileImageUrl, parser.getQuoteUserName(),
                                        parser.getQuoteUserScreenName(), parser.getQuoteUserDescription(),
                                        parser.getQuoteUserFriendsCount(), parser.getQuoteUserFollowersCount()) +
                            "</div>" +
                        "</div>" +
                        "<div class='item date'>" +
                            "<div>" + parser.getQuoteCreatedDate() + "</div>" +
                        "</div>" +
                        "<div class='item text'>" +
                            parser.getQuoteText() +
                        "</div>" +
                        (quoteImageUrls.isEmpty() ? "" : "<span class='image'>" + imageUrlToHtml(quoteImageUrls) + "</span>") +
                    "</div>";
        } catch (Exception e) {
            log.warn("Failed getting images for quote in tweet by '{}'", parser.getUserName());
        }
        return quoteHtml;
    }


    private static String newline2Br(String text){
        if (text==null){
            return "";
        }
        return text.replace("\n","<br>");

    }


    private static String getRetweetHeader(TwitterParser2 parser, String crawlDate) {
        ArrayList<ImageUrl> profileImageUrl = new ArrayList<>();
        try {
            List<String> retweetProfileImage = Collections.singletonList(parser.getUserProfileImage());
            profileImageUrl = getImageUrlsFromSolr(retweetProfileImage, crawlDate);
        } catch (Exception e) {
            log.warn("Failed getting profile image of retweeter with username '{}'", parser.getUserName());
        }
        String html =
                "<div class='retweet-author'>" +
                    "<div class='user-wrapper'>" +
                        "<a href='#'>" + // TODO insert search link for user
                            "<h3>" + parser.getUserName() + " Retweeted</h3>" +
                        "</a>" +
                        makeUserCard(profileImageUrl, parser.getUserName(),
                                        parser.getUserScreenName(), parser.getUserDescription(),
                                        parser.getUserFriendsCount(), parser.getUserFollowersCount()) +
                    "</div>" +
                    "<div class='date'>&middot " + parser.getCreatedDate() + "</div>" +
                "</div>";
        return html;
    }


    public static String imageUrlToHtml(ArrayList<ImageUrl> images){
        StringBuilder b = new StringBuilder();
        for (ImageUrl image : images){
            b.append("<img src='")
                    .append(image.getDownloadUrl())
                    .append("'/>\n");
        }
        return b.toString();
    }
}

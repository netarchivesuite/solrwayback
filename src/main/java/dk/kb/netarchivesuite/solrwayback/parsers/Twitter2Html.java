package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Twitter2Html {
    private static final Logger log = LoggerFactory.getLogger(Twitter2Html.class);
    public static String twitter2Html(String jsonString, String crawlDate) throws Exception{
        StringBuilder b = new StringBuilder();
        TwitterParser2 parser = new TwitterParser2(jsonString);
        String iconsImage = PropertiesLoader.WAYBACK_BASEURL+"images/twitter_sprite.png";

        String textReplaced = newline2Br(parser.getText());
        //TODO frontend fix so all other params not needed
        String otherSearchParams = " AND type%3A\"Twitter Tweet\"&start=0&filter=&imgsearch=false&imggeosearch=false&grouping=false";
        // TODO RBKR fix these methods somehow.. ugly compromise for now.
        textReplaced = formatMentions(textReplaced, parser.getMentions(), PropertiesLoaderWeb.WAYBACK_SERVER, otherSearchParams);
        textReplaced = formatHashtags(textReplaced, parser.getHashTags(), PropertiesLoaderWeb.WAYBACK_SERVER, otherSearchParams);

        ArrayList<String> normalizedImageURLs = new ArrayList<>();
        for (String img : parser.getImageUrlsList()){
            normalizedImageURLs.add(Normalisation.canonicaliseURL(img));
        }

        String queryStr = Facade.queryStringForImages(normalizedImageURLs); // TODO RBKR: URLs are also normalized in here, so unnecessary second time?
        ArrayList<ArcEntryDescriptor> images = NetarchiveSolrClient.getInstance().findImagesForTimestamp(queryStr, crawlDate);

        String user_image = parser.isRetweet() ? parser.getOriginalProfileImage() : parser.getProfileImage();
        String user_image_norm = Normalisation.canonicaliseURL(user_image);

        List<String> user_image_list = Collections.singletonList(user_image_norm);

        String queryStrUser = Facade.queryStringForImages(user_image_list);
        log.info("Query: '{}'", queryStrUser);
        ArrayList<ArcEntryDescriptor> images_user = NetarchiveSolrClient.getInstance().findImagesForTimestamp(queryStrUser, crawlDate);//Only 1

        ArrayList<ImageUrl> imageUrls = Facade.arcEntrys2Images(images);
        ArrayList<ImageUrl> imageUrl_user = Facade.arcEntrys2Images(images_user);

        String css =
                "body {background: #f3f3f6;color: #333333;font-family: Arial, Helvetica, sans-serif;margin: 0;}"+
                "#wrapper {"+
                "  background: white;margin: 0 auto; padding: 2em; max-width: 1000px;}"+
                "h2 {"+
                "  margin: 5px 0 0; font-size: 18px;"+
                "}"+
                "h3 {"+
                "  font-size: 14px;"+
                "  margin: 0;"+
                "}"+
                "h4 {"+
                "  color: rgb(83, 100, 113);"+
                "  margin: 0;"+
                "  font-weight: 400;"+
                "}"+
                ".tweet {"+
                "  border: 1px solid #cccccc; line-height: 1.6em;overflow: hidden; padding: 1em;"+
                "}"+
                ".item {"+
                "  padding: .5em 0;"+
                "}"+
                ".item.retweet-author {"+
                "  color: rgb(83, 100, 113);"+
                "  line-height: 1.3em;"+
                "  overflow: hidden;"+
                "}"+
                ".item.retweet-author a {"+
                "  text-decoration: none;"+
                "  color: inherit;"+
                "  float: left;"+
                "}"+
                ".item.retweet-author .date {"+
                "  font-size: 14px;"+
                "  padding-left: 0.3em;"+
                "  float: left;"+
                "}"+
                ".item.author {"+
                "  padding: 0;"+
                "  overflow: hidden;"+
                "}"+
                ".item .image {display: block;margin-top: 1em;max-width: 600px;"+
                "}"+
                ".item .image img {"+
                "  max-width: 100%;"+
                "}"+
                ".quote {"+
                "  border-radius: 16px;" +
                "  border: 1px solid rgb(207, 217, 222);" +
                "  min-height: 64px;"+
                "  padding: 1em;"+
                "  margin-top: 1em;"+
                "}"+
                ".item.reactions span {"+
                "  vertical-align: middle;"+
                "}"+
                ".item.reactions span.icon { display: inline-block; height: 20px; width: 20px;"+
                "}"+
                ".item.reactions span.number {"+
                "  display: inline-block;"+
                "  margin-right: 1.5em;"+
                "}"+
                ".item.reactions span.replies {"+
                "  background: transparent url("+iconsImage+") no-repeat -145px -50px;"+ // Missing correct icon?
                "}"+
                ".item.reactions span.retweets {"+
                "  background: transparent url("+iconsImage+") no-repeat -180px -50px;"+
                "}"+
                ".item.reactions span.likes {"+
                "  background: transparent url("+iconsImage+") no-repeat -145px -130px;"+
                "}"+
                ".item.reactions span.quotes {"+
                "  background: transparent url("+iconsImage+") no-repeat -105px -50px;"+ // Missing correct icon
                "}"+
                ".author-handles {"+
                "  line-height: 1.3em;"+
                "  float: left;"+
                "}"+
                ".avatar{"+
                "  float: left;"+
                "  margin-right: .5em;"+
                "  display: inline-flex;"+ // Fix for span being larger than img
                "}"+
                ".avatar img{"+
                "  border-radius: 50%;"+
                "}"+
                ".item.date{"+
                "  clear: both;"+
                "  color: rgb(83, 100, 113);"+
                "}";

        String html =
                "<!DOCTYPE html>"+
                "<html>"+
                "<head>"+
                  "<meta http-equiv='Content-Type' content='text/html;charset=UTF-8'>"+
                  "<meta name='viewport' content='width=device-width, initial-scale=1'>"+
                  "<title>"+getTitle(parser)+"</title>"+
                  "<style>"+css+"</style>"+
                "</head>"+
                "<body>"+
                  "<div id='wrapper'>"+
                    "<div class='tweet'>"+
                      (parser.isRetweet() ? getRetweetTitle(parser) : "")+
                      "<div class='item author'>"+
                        "<span class='avatar'>"+
                          imagesHtml(imageUrl_user)+
                        "</span>"+
                        "<div class='author-handles'>"+
                          "<h2>"+ (parser.isRetweet() ? parser.getOriginalAuthor() : parser.getAuthor()) +"</h2>"+
                          "<h4>@"+ (parser.isRetweet() ? parser.getOriginalScreenName() : parser.getScreenName()) +"</h4>"+
                        "</div>"+
                      "</div>"+
                      "<div class='item date'>"+
                        "<div>"+parser.getCreatedDate()+"</div>"+
                      "</div>"+
                      "<div class='item text'>"+
                        textReplaced+
                        "<span class='image'>"+ // TODO RBKR should only make span if tweet contains images
                          imagesHtml(imageUrls)+
                        "</span>"+
                        (parser.hasQuote() ? getQuoteHtml(parser, crawlDate) : "")+
                        "</div>"+
                      "<div class='item reactions'>"+
                        "<span class='icon replies'></span>"+
                        "<span class='number'>"+parser.getNumberOfReplies()+"</span>"+
                        "<span class='icon retweets'></span>"+
                        "<span class='number'>"+parser.getNumberOfRetweets()+"</span>"+
                        "<span class='icon likes'></span>"+
                        "<span class='number'>"+parser.getNumberOfLikes()+"</span>"+
                        "<span class='icon quotes'></span>"+
                        "<span class='number'>"+parser.getNumberOfQuotes()+"</span>"+
                        "</div>"+
                    "</div>"+
                  "</div>"+
                "</body>"+
                "</html>";

        return html;
    }

    private static String getTitle(TwitterParser2 parser) {
        String titlePrefix = parser.isRetweet() ? "Retweet by: " : "Tweet by: ";
        return titlePrefix + parser.getAuthor();
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
            String link = solrwaybackUrl + "?query=keywords%3A" + tag + extraSearchParams;
            String tagWithLink = "<span><a href='" + link + "'>#" + tag + "</a></span>";
            text = text.replaceAll("#" + tag, tagWithLink);
        }
        return text;
    }


    private static String getQuoteHtml(TwitterParser2 parser, String crawlDate) {
        String quoteHtml = "";

        try {
            List<String> quoteImages = new ArrayList<>(parser.getQuoteImageUrlStrings());
            String quoteImagesSolrQuery = Facade.queryStringForImages(quoteImages);
            ArrayList<ArcEntryDescriptor> quoteImageEntries = NetarchiveSolrClient.getInstance()
                    .findImagesForTimestamp(quoteImagesSolrQuery, crawlDate);
            ArrayList<ImageUrl> quoteImageUrls = Facade.arcEntrys2Images(quoteImageEntries);

            quoteHtml =
                    "<div class='quote'>" +
                        "<div class='item author'>" +
                            "<h2>" + parser.getQuoteUserName() + "</h2>" +
                        "</div>" +
                        "<div class='item date'>" +
                            "<div>" + parser.getQuoteCreatedDate() + "</div>" +
                        "</div>" +
                        "<div class='item text'>" +
                        parser.getQuoteText() +
                        "</div>" +
                        (parser.hasQuote() ? "<span class='image'>" + imagesHtml(quoteImageUrls) + "</span>" : "") +
                    "</div>";
        } catch (Exception e) {
            log.warn("Failed getting images for quote in tweet by '{}'", parser.getAuthor());
        }
        return quoteHtml;
    }


    private static String newline2Br(String text){
        if (text==null){
            return "";
        }
        return text.replace("\n","<br>");

    }


    private static String getRetweetTitle(TwitterParser2 parser) {
        String html =
                "<div class='item retweet-author'>" +
                    "<a href='www.example.com'>" + // TODO insert search link for user
                        "<h3>" + parser.getAuthor() + ", retweeted</h3>" +
                    "</a>" +
                    "<div class='date'>&middot " + parser.getRetweetCreatedDate() + "</div>" +
                "</div>";
        return html;
    }


    public static String imagesHtml(ArrayList<ImageUrl> images){
        StringBuilder b = new StringBuilder();
        for (ImageUrl image : images){
            b.append("<img src='")
                    .append(image.getDownloadUrl())
                    .append("'/>\n");
        }
        return b.toString();
    }
}

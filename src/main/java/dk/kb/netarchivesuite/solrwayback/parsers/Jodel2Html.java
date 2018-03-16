package dk.kb.netarchivesuite.solrwayback.parsers;

import org.json.JSONArray;
import org.json.JSONObject;

// TODO: Show "Thanks! ❤" if 'got_thanks' is true
// TODO: Rename ('replier' == 0) to 'OJ' (Original Jodler)
// See src/test/resources/example_jodel/jodel.json for Jodel JSON structure
public class Jodel2Html {

    public static final JSONUtil.JSONSingleValueRule LOCATION =
            JSONUtil.getSingleMatcher(".details.location.name", ".replies[].location.name", ".location.name")
                    .setDefault("Unknown location");
    public static final JSONUtil.JSONSingleValueRule UPDATED = JSONUtil.getSingleMatcher(".details.updated_at", ".updated_at")
            .setDefault("unknown time");;
    public static final JSONUtil.JSONSingleValueRule CREATED = JSONUtil.getSingleMatcher(".details.created_at", ".created_at")
            .setDefault("unknown time");;
    public static final JSONUtil.JSONSingleValueRule SHARE_URL = JSONUtil.getSingleMatcher(".share_url");

    // Relative to either .details or an entry in .replies[]
    public static final JSONUtil.JSONSingleValueRule MESSAGE = JSONUtil.getSingleMatcher(".message");
    public static final JSONUtil.JSONSingleValueRule COLOR = JSONUtil.getSingleMatcher(".color").setDefault("CCCCCC");
    public static final JSONUtil.JSONSingleValueRule VOTE = JSONUtil.getSingleMatcher(".vote_count").setDefault("0");
    public static final JSONUtil.JSONSingleValueRule USER = JSONUtil.getSingleMatcher(".replier").setDefault("0");
    public static final JSONUtil.JSONSingleValueRule IMAGE_URL = JSONUtil.getSingleMatcher(".image_url").setDefault("");

    public static String render(String jsonString, String crawlDate) {
        StringBuilder sb = new StringBuilder();
        JSONObject json = new JSONObject(jsonString);

        String shareUrl = SHARE_URL.match(json);
        generateHead(json, sb);

        sb.append("<body>\n");
        sb.append("  <h1>Jodel: ").append(getDesignation(json)).append("</h1>");
        if (shareUrl != null) {
            sb.append("  <p class=\"share_url\">Share URL: <a href=\"").append(shareUrl).append("\">").append(shareUrl).append("</a></p>");
        } else {
            sb.append("  <p class=\"share_url\">Share URL: Not available (legacy harvest)</a></p>");
        }
        sb.append("  <div class=\"jodelbody\">\n");
        generateMain(sb, json.getJSONObject("details"));

        generateReplies(sb, json);

        sb.append("  </div>\n");
        sb.append("</body>\n");
        return sb.toString();
    }

    private static void generateHead(JSONObject json, StringBuilder sb) {
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
        sb.append(  "<title>Jodel: ").append(getDesignation(json)).append("</title>\n");
        sb.append("  <meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\" />\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n");
        sb.append("  <link href=\"css/jodel.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
        sb.append("</head>\n");
    }

    // .details
    private static void generateMain(StringBuilder sb, JSONObject detailsJSON) {
        String message = MESSAGE.match(detailsJSON);
        String background = COLOR.match(detailsJSON);
        String vote = VOTE.match(detailsJSON);
        String user = USER.match(detailsJSON);
        String created = CREATED.match(detailsJSON);
        String lastReply = UPDATED.match(detailsJSON);
        String location = LOCATION.match(detailsJSON);
        String imageUrl = "http:" + IMAGE_URL.match(detailsJSON);

        sb.append("      <div class=\"jodelmain\" style=\"background: #").append(background).append("\">\n");
        sb.append("        <p class=\"user\">User: ").append(user).append("</p>\n");
        sb.append("        <p class=\"created\">Created: ").append(created).append("</p>\n");
        sb.append("        <p class=\"updated\">Last reply: ").append(lastReply).append("</p>\n");
        sb.append("        <p class=\"location\">Location: ").append(location).append("</p>\n");
        if (!imageUrl.equalsIgnoreCase("http:")) {
            sb.append("        <p class=\"image_url\">").append("<img src=\"").append(imageUrl).append("\"></p>\n");
        }
        sb.append("        <p class=\"message\">").append(toHTML(message)).append("</p>\n");
        sb.append("        <p class=\"vote\">Vote count: ").append(vote).append("</p>\n");
        sb.append("      </div>\n"); // end jodelreply
    }

    private static void generateReplies(StringBuilder sb, JSONObject jodelJSON) {
        sb.append("    <div class=\"jodelreplies\">\n");
        if (jodelJSON.has("replies")) {
            JSONArray replies = jodelJSON.getJSONArray("replies");
            for (int i = 0; i < replies.length(); i++) {
                generateReply(sb, replies.getJSONObject(i));
            }
        }
        sb.append("   </div>\n");
    }

    // .replies[] entry
    private static void generateReply(StringBuilder sb, JSONObject replyJSON) {
        String message = MESSAGE.match(replyJSON);
        String background = COLOR.match(replyJSON);
        String vote = VOTE.match(replyJSON);
        String user = USER.match(replyJSON);
        String created = CREATED.match(replyJSON);
        String location = LOCATION.match(replyJSON);
        String imageUrl = "http:" + IMAGE_URL.match(replyJSON);

        sb.append("      <div class=\"jodelreply\" style=\"background: #").append(background).append("\">\n");
        sb.append("        <p class=\"user\">User: ").append(user).append("</p>\n");
        sb.append("        <p class=\"created\">Created: ").append(created).append("</p>\n");
        sb.append("        <p class=\"location\">Location: ").append(location).append("</p>\n");
        if (!imageUrl.equalsIgnoreCase("http:")) {
            sb.append("        <p class=\"image_url\">").append("<img src=\"").append(imageUrl).append("></p>\n");
        }
        sb.append("        <p class=\"message\">").append(toHTML(message)).append("</p>\n");
        sb.append("        <p class=\"vote\">Vote count: ").append(vote).append("</p>\n");
        sb.append("      </div>\n"); // end jodelreply
    }

    public static String toHTML(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").
                replace("\n", "<br/>");
    }

    private static String getDesignation(JSONObject json) {
        return LOCATION.match(json) + " " + UPDATED.match(json);
    }

}

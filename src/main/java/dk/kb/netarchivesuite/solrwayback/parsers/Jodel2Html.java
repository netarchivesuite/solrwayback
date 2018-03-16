package dk.kb.netarchivesuite.solrwayback.parsers;

import org.json.JSONArray;
import org.json.JSONObject;

// See src/test/resources/example_jodel/jodel.json for Jodel JSON structure
public class Jodel2Html {

    public static final JSONUtil.JSONRule PLACE =
            JSONUtil.getSingleMatcher(".details.location.name", ".replies[].location.name")
                    .setSingleDefault("Unknown location");
    public static final JSONUtil.JSONRule UPDATED = JSONUtil.getSingleMatcher(".details.updated_at")
            .setSingleDefault("unknown time");;

    // Relative to either .details or an entry in .replies[]
    public static final JSONUtil.JSONRule MESSAGE = JSONUtil.getAllMatcher(".message");
    public static final JSONUtil.JSONRule COLOR = JSONUtil.getSingleMatcher(".color").setSingleDefault("CCCCCC");
    public static final JSONUtil.JSONRule VOTE = JSONUtil.getSingleMatcher(".vote_count").setSingleDefault("0");
    public static final JSONUtil.JSONRule USER = JSONUtil.getSingleMatcher(".replier").setSingleDefault("0");
    public static final JSONUtil.JSONRule IMAGE_URL = JSONUtil.getSingleMatcher(".image_url").setSingleDefault("");

    public static String render(String jsonString, String crawlDate) {
        StringBuilder sb = new StringBuilder();
        JSONObject json = new JSONObject(jsonString);

        generateHead(json, sb);

        sb.append("<body>\n");
        sb.append("  <div class=\"jodelbody\">\n");
        generateMain(sb, json.getJSONObject("details"));

        generateReplies(sb, json);

        sb.append("  </div>\n");
        sb.append("</body>\n");
        return sb.toString();
    }

    private static void generateHead(JSONObject json, StringBuilder sb) {
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
        sb.append(  "<title>");
        sb.append("Jodel: ");
        sb.append(PLACE.getSingleMatch(json));
        sb.append(" ");
        sb.append(UPDATED.getSingleMatch(json));
        sb.append("  </title>\n");
        sb.append("  <meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\" />\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n");
        sb.append("  <link href=\"css/jodel.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
        sb.append("</head>\n");
    }

    // .details
    private static void generateMain(StringBuilder sb, JSONObject detailsJSON) {
        String message = MESSAGE.getSingleMatch(detailsJSON);
        String background = COLOR.getSingleMatch(detailsJSON);
        String vote = VOTE.getSingleMatch(detailsJSON);
        String user = USER.getSingleMatch(detailsJSON);
        String imageUrl = "http:" + IMAGE_URL.getSingleMatch(detailsJSON);

        sb.append("      <div class=\"jodelmain\" style=\"background: #").append(background).append("\">\n");
        sb.append("        <p class=\"user\">User: ").append(user).append("</p>\n");
        if (!imageUrl.equalsIgnoreCase("http:")) {
            sb.append("        <p class=\"image_url\">").append("<img src=\"").append(imageUrl).append("\"></p>\n");
        }
        sb.append("        <p class=\"message\">").append(message).append("</p>\n");
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
        String message = MESSAGE.getSingleMatch(replyJSON);
        String background = COLOR.getSingleMatch(replyJSON);
        String vote = VOTE.getSingleMatch(replyJSON);
        String user = USER.getSingleMatch(replyJSON);
        String imageUrl = "http:" + IMAGE_URL.getSingleMatch(replyJSON);

        sb.append("      <div class=\"jodelreply\" style=\"background: #").append(background).append("\">\n");
        sb.append("        <p class=\"user\">User: ").append(user).append("</p>\n");
        if (!imageUrl.equalsIgnoreCase("http:")) {
            sb.append("        <p class=\"image_url\">").append("<img src=\"").append(imageUrl).append("></p>\n");
        }
        sb.append("        <p class=\"message\">").append(message).append("</p>\n");
        sb.append("        <p class=\"vote\">Vote count: ").append(vote).append("</p>\n");
        sb.append("      </div>\n"); // end jodelreply
    }

}

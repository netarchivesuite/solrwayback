package dk.kb.netarchivesuite.solrwayback.parsers;

import org.json.JSONObject;

public class Jodel2Html {

    public static final JSONUtil.JSONRule UPDATED = JSONUtil.getSingleMatcher(".details.updated_at");
    public static final JSONUtil.JSONRule PLACE = JSONUtil.getSingleMatcher(".details.location.name", ".replies[].location.name");
    public static final JSONUtil.JSONRule MAIN_MESSAGE = JSONUtil.getSingleMatcher(".details.message");
    public static final JSONUtil.JSONRule REPLIES = JSONUtil.getAllMatcher(".replies[].message");

    public static String render(String jsonString, String crawlDate){
        StringBuilder sb = new StringBuilder();
        JSONObject json = new JSONObject(jsonString);

        sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
        sb.append("<title>");
        sb.append("Jodel: ");
        sb.append(PLACE.getSingleMatch(json, "Unknown place"));
        sb.append(" ");
        sb.append(UPDATED.getSingleMatch(json, "unknown time"));
        sb.append("</title>\n</head>\n<body>\n");
        sb.append("<div class=\"jodelbody\">\n");

        sb.append("<div class=\"jodelmain\">\n"); // TODO: Add color, votes etc.
        sb.append("<p>");
        sb.append(MAIN_MESSAGE.getSingleMatch(json));
        sb.append("</p>\n");
        sb.append("</div>\n"); // end jodelmain

        for (String reply: REPLIES.getMatches(json)) {
            sb.append("<div class=\"jodelreply\">\n");
            sb.append("<p>");
            sb.append(reply); // TODO: Do real rendering with colors etc.
            sb.append("</p>\n");
            sb.append("</div>\n"); // end jodelreply
        }

        sb.append("</div>\n");
        sb.append("</body>\n");
        return sb.toString();
    }
}

package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;

import javax.ws.rs.core.Response;

public class TimeMap {

    public static Response getTimeMap(String originalResource, String responseFormat){
        switch (responseFormat){
            case "application/link-format":
                return  getTimeMapAsLinkFormat(originalResource);
            case "application/json":
                return getTimeMapAsJson(originalResource);
        }
        return null;
    }

    private static Response getTimeMapAsJson(String originalResource) {
        return null;
    }

    private static Response getTimeMapAsLinkFormat(String originalResource) {
        String body = "<" + originalResource + ">;rel=\"original\",\n" +
                    "<"+ PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY + "memento/timegate/" + originalResource + ">\n" +
                    "; rel=\"self\";type=\"application/link-format\"\n" +
                    "; from\"" + "TODO: First memento " + "\"" +
                    "; until\"" + "TODO: last memento " + "\"";


        return Response.ok()
                .header("Content-Length", body.length())
                .header("Content-Type", "application/link-format")
                .entity(body).build();
    }

}

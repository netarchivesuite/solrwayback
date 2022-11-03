package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;

import java.net.IDN;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class UrlUtils {

    public static void main(String[] args){

        //System.out.println(isUrlWithDomain("http://Portal_gfx/KL/farvepakker/topmenu/topmenu_markering_groen_mBo.gif"));
        //System.out.println(getDomainFromWebApiParameters("http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285"));
    }
    /*
     * Must start with http:// and have a domain(must have . as one of the characters)
     *
     */
    public static boolean isUrlWithDomain(String url){
        if (!(url.startsWith("http://") || url.startsWith("https://")) ){
            return false;
        }
        String[] tokens= url.split("/");
        if (tokens.length < 3){
            return false;
        }

        //String domain = tokens[2];

        return true;
    }

    /*
     * Url must be legal and have a domain, also remove port.
     */
    public static String getDomain(String url){
        String[] tokens= url.split("/");
        String domainWithPort =  tokens[2];
        int portIndex = domainWithPort.indexOf(":");
        if (portIndex == -1){
            return domainWithPort;
        }
        else{
            return domainWithPort.substring(0, portIndex);
        }
    }

    //http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285
    //return kl.dk
    public static String getDomainFromWebApiParameters(String fullUrl){
        int dataStart=fullUrl.indexOf("/web/");
        String waybackDataObject = fullUrl.substring(dataStart+5);
        int indexFirstSlash = waybackDataObject.indexOf("/");
        String url = waybackDataObject.substring(indexFirstSlash+1);
        return getDomain(url);
    }


    //http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285
    //return 20071221033234
    public static String getCrawltimeFromWebApiParameters(String fullUrl){
        int dataStart=fullUrl.indexOf("/web/");
        String waybackDataObject = fullUrl.substring(dataStart+5);
        int indexFirstSlash = waybackDataObject.indexOf("/");
        String crawlTime = waybackDataObject.substring(0,indexFirstSlash);
        return crawlTime;

    }


    //Extract crawltime and url (after crawltime) from and url
    //Example: https://solrwb-test.kb.dk:4000/solrwayback/services/web/20110225142047/http://www.bt.dk/debat
    //Return [20110225142047],[http://www.bt.dk/debat]
    //It pattern is not found return null;
    public static String[] getCrawltimeAndUrlFromWebProxyLeak(String fullUrl){
        Pattern p = Pattern.compile("^.+?/(\\d{14})/(.+)$");
        Matcher matcher1 = p.matcher(fullUrl);
        if (matcher1.matches()) {
            String[]  result= new String[2];
            result[0]=matcher1.group(1);
            result[1]=matcher1.group(2);
            return result;


        }else {
            return null;
        }

    }


    /*
     * last path element wit query params as well.
     */
    public static String getResourceNameFromWebApiParameters(String url){
        String[] tokens= url.split("/");
        return tokens[tokens.length-1];

    }

    /**
     * Safe version of {@link #punyCodeAndNormaliseUrl(String)} where null is returned if an Exception is thrown.
     * @param url any URL, but only HTTP and HTTPS are handled.
     * @return the URL in punicode (if needed) form and normalised, null in case of Exceptions.
     */
    public static String punyCodeAndNormaliseUrlSafe(String url) {
        try {
            return punyCodeAndNormaliseUrl(url);
        } catch (Exception e) {
            return null;
        }
    }

    public static String punyCodeAndNormaliseUrl(String url) throws Exception {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new Exception("Url not starting with http:// or https://");
        }

        URL uri = new URL(url);
        String hostName = uri.getHost();
        String hostNameEncoded = IDN.toASCII(hostName);

        String path = uri.getPath();
        if ("".equals(path)) {
            path = "/";
        }
        String urlQueryPath = uri.getQuery();
        String urlPunied = null;
        if (urlQueryPath == null) {
             urlPunied = "http://" + hostNameEncoded + path;
        }
        else {
            urlPunied = "http://" + hostNameEncoded + path +"?"+ urlQueryPath;
        }
        String urlPuniedAndNormalized = Normalisation.canonicaliseURL(urlPunied);
        return urlPuniedAndNormalized;
    }

    /**
     * Constructs a Solr query for lenient HTTP URL resolving, where the leniency is that partial matching on arguments
     * is allowed. Precise URL-matching has highest priority.
     *
     * Note: Lenient URL search is heavier than simple URL search. Consider using this only if a simple search fails.
     *
     * Sample input {@code http://example.com/IMAGES/search?q=horse&fq=animals&_=67890}
     * will result in
     * <pre>
     * url:"http://example.com/IMAGES/search?q=horse&fq=animals&_=67890"^200
     * OR
     * url_norm:"http://example.com/images/search?q=horse&fq=animals&_=67890"^100
     * OR
     * (
     *   host:"example.com"
     *   AND
     *   url_search:"example.com/images/search" )
     *   AND (
     *     host:"example.com"
     *     OR
     *     url_search:"q=horse"
     *     OR
     *     url_search:"fq=animals"
     *     OR
     *     url_search:"_=67890"
     *   )
     * )
     * </pre>
     * (without the newlines). The repetition of {@code host:"example.com"} is to ensure at least 1 match from
     * the parenthesis with argument matchers.
     *
     * if the URL is {@code http://example.com/IMAGES/search} the output will be
     * <pre>
     * url:"http://example.com/IMAGES/search?q=horse&fq=animals&_=67890"^200
     * OR
     * url_norm:"http://example.com/images/search?q=horse&fq=animals&_=67890"^100
     * </pre>
     * (again without the newlines)
     *
     * If anything goes wrong during parsing and handling, a simple {@code url:"<url>"} query is returned.
     *
     * Note: This should normally NOT be used with {@link SRequest#queries(Stream)}} as it is likely to match multiple
     * documents and give a lot of false positives. The typical use case would be for dedicated URL search with the
     * returned documents either being showed in ranked order or pruned so that only the first document is used.
     * @param url a HTTP-URL: Must start with {@code http://} or {@code https://}
     * @return a Solr query for the URL that allows partial matching on arguments.
     */
    public static String lenientURLQuery(String url) {
        String norm;
        try {
            norm = punyCodeAndNormaliseUrl(url);
        } catch (Exception e) {
            return "url:" + SolrUtils.createPhrase(url);
        }

        StringBuilder query = new StringBuilder();
        query.append("url:").append(SolrUtils.createPhrase(url)).append("^200");
        query.append(" OR ");
        query.append("url_norm:").append(SolrUtils.createPhrase(norm)).append("^100");

        Matcher argMatcher = ARG_URL.matcher(norm);

        if (!argMatcher.matches()) { // No arguments
            return query.toString();
        }

        String host = argMatcher.group(1);
        String path = argMatcher.group(2);
        String[] args = argMatcher.group(3).split("&");
        query.append(" OR (");
        query.append("host:").append(SolrUtils.createPhrase(host)).
                append(" AND url_search:").append(SolrUtils.createPhrase(path));
        query.append(" AND (");
        query.append("host:").append(SolrUtils.createPhrase(host)); // Fallback if no arguments matches
        for (String arg: args) {
            query.append(" OR url_search:").append(SolrUtils.createPhrase(arg));
        }
        query.append(")");
        query.append(")");
        return query.toString();
    }
    private static final Pattern ARG_URL = Pattern.compile("https?://([^/]+)/([^?]*)[?](.+)");
}


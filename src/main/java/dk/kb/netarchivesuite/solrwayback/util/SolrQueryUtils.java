package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrQueryUtils {
    public static final Logger log = LoggerFactory.getLogger(SolrQueryUtils.class);

    /**
     * Normalizes a given list of urls and makes a Solr search string from the result.
     *
     * Notice only maximum of 50 urls will be searched for.
     * This method is only called for image- and video search, and we don't want too many hits from same site.
     * @param urls Urls to make the search string from.
     * @return Solr query string to search Solr for the given images.
     */
    public static String createQueryStringForUrls(List<String> urls) {
        if (urls.size() > 50) {
            urls = urls.subList(0, 50);
        }

        StringBuilder query = new StringBuilder();
        query.append("(");
        for (String url : urls) {
            try {
                // Fix https etc.
                String canonicalizedUrl = Normalisation.canonicaliseURL(url);
                query.append(" url_norm:\"" + canonicalizedUrl + "\" OR");
            }
            catch (Exception e) {
                // This can happen since urls from HTML are extracted without any sanity-check by the warc-indexer.
                // Just ignore.
                log.info("Could not normalise url:" + url);
            }
        }
        query.append(" url_norm:none)"); // Just close last OR

        return query.toString();
    }

    /**
     * Make a URL to search SolrWayback for the given string.
     * @param searchString String to search for.
     * @return SolrWayback search URL for given string.
     */
    public static String createTwitterSearchURL(String searchString) {
        String searchParams = " AND type%3A\"Twitter Tweet\"";
        return PropertiesLoader.WAYBACK_BASEURL + "search?query=" + searchString + searchParams;
    }
}

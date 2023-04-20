package dk.kb.netarchivesuite.solrwayback.solr;

import com.google.gson.Gson;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Get stats through the solr <a href="https://solr.apache.org/guide/8_11/the-stats-component.html">stats component</a>.
 * Methods in this class
 */
public class SolrStats {
    private static final Logger log = LoggerFactory.getLogger(SolrStats.class);
    public static final List<String> interestingNumericFields =  Arrays.asList("content_length", "crawl_year", "content_text_length", "image_height", "image_width", "image_size");
    final List<String> interestingTextFields = Arrays.asList("links", "domain", "elements_used", "content_type",
                                                                "content_language", "links_images", "type");
    final List<String> otherNumericFields = Arrays.asList("score", "status_code", "source_file_offset", "_version_", "wayback_date");
    /**
     * Get standard solr stats for all fields given
     *
     * @param query   to generate stats for.
     * @param filters
     * @param fields  to return stats for.
     * @return all standard stats for all fields from query as a JSON string.
     */
    public static String getStatsForFields(String query, List<String> filters, List<String> fields){
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);

        for (String field: fields) {
            if (PropertiesLoaderWeb.STATS.contains(field)){
                solrQuery.setGetFieldStatistics(field);
            } else {
                log.warn("Stats can not be shown for field: " + field + " as it is not present in properties.");
            }
        }

        QueryResponse response = NetarchiveSolrClient.query(solrQuery, true);
        Gson gson = new Gson();
        String stats = gson.toJson(response.getFieldStatsInfo().values());
        return stats;
    }

    public static String getPercentilesForFields(String query, List<String> percentiles, List<String> fields){
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);

        String allPercentiles = StringUtils.join(percentiles, ",");
        String percentileQuery = "{!percentiles='" + allPercentiles + "'}";

        // When giving this a text field it calculates nothing. Should probably throw a warning or something like that
        for (String field: fields) {
            if (PropertiesLoaderWeb.STATS.contains(field)){
                solrQuery.setGetFieldStatistics(percentileQuery + field);
            } else {
                log.error("Percentile stats can not be shown for field: " + field + " as it is not present in properties.");
            }
        }

        QueryResponse response = NetarchiveSolrClient.query(solrQuery, true);
        Gson gson = new Gson();
        String stats = gson.toJson(response.getFieldStatsInfo().values());

        return stats;
    }

}

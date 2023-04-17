package dk.kb.netarchivesuite.solrwayback.solr;

import com.google.gson.Gson;
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
    public static final List<String> interestingNumericFields = Arrays.asList("content_length", "crawl_year", "content_text_length", "image_height", "image_width", "image_size");
    final List<String> interestingTextFields = Arrays.asList("links", "domain", "elements_used", "content_type",
                                                                "content_language", "links_images", "type");
    final List<String> otherNumericFields = Arrays.asList("score", "status_code", "source_file_offset", "_version_", "wayback_date");

    /**
     * Get standard solr stats for all numeric fields given.
     * @param query to generate stats for.
     * @param fields to return stats for.
     * @return all standard stats for all numeric fields from query as a JSON string.
     */
    public static String getStatsForMultipleNumericFields(String query, List<String> fields){
        //TODO: Should contain a check, that the values are actually numeric and not anything else.
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);

        for (String field: fields) {
            solrQuery.setGetFieldStatistics(field);
        }

        QueryResponse response = NetarchiveSolrClient.query(solrQuery, true);
        Gson gson = new Gson();
        String stats = gson.toJson(response.getFieldStatsInfo().values());
        return stats;
    }

    /**
     * Get standard solr stats for single numeric field.
     * @param query to generate stats for.
     * @param field to return stats for.
     * @return all standard stats for field as a JSON string.
     */
    public static String getStatsForSingleNumericField(String query, String field){
        //TODO: Should contain a check, that the values are actually numeric and not anything else.
        SolrQuery solrQuery = new SolrQuery();

        // Get all stats for input field
        solrQuery.setQuery(query).addGetFieldStatistics(field);

        QueryResponse response = NetarchiveSolrClient.query(solrQuery, true);
        Gson gson = new Gson();
        String stats = gson.toJson(response.getFieldStatsInfo().values());
        return stats;
    }

    /**
     * Get solr stats for single text field. Only return stats on count and missing values.
     * @param query to generate stats for.
     * @param field to return stats for.
     * @return stats for count and missing as a JSON string.
     */
    public static String getStatsForSingleTextField(String query, String field){
        SolrQuery solrQuery = new SolrQuery();
        // Get all stats for input field
        solrQuery.setQuery(query).addGetFieldStatistics("{!count=true missing=true}" + field);

        QueryResponse response = NetarchiveSolrClient.query(solrQuery, true);
        Gson gson = new Gson();
        String stats = gson.toJson(response.getFieldStatsInfo().values());
        return stats;
    }

}

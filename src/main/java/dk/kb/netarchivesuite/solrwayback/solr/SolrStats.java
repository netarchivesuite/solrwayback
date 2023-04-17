package dk.kb.netarchivesuite.solrwayback.solr;

import com.google.gson.Gson;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get stats through the solr <a href="https://solr.apache.org/guide/8_11/the-stats-component.html">stats component</a>.
 * Methods in this class
 */
public class SolrStats {
    private static final Logger log = LoggerFactory.getLogger(SolrStats.class);
    public static final String[] interestingNumericFields =  new String[]{"content_length", "crawl_year", "content_text_length", "image_height", "image_width", "image_size"};
    final String[] interestingTextFields = new String[]{"links", "domain", "elements_used", "content_type",
                                                                "content_language", "links_images", "type"};
    final String[] otherNumericFields = new String[]{"score", "status_code", "source_file_offset", "_version_", "wayback_date"};

    /**
     * Get standard solr stats for all fields given
     *
     * @param query   to generate stats for.
     * @param filters
     * @param fields  to return stats for.
     * @return all standard stats for all fields from query as a JSON string.
     */
    public static String getStatsForFields(String query, String[] filters, String... fields){
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

}

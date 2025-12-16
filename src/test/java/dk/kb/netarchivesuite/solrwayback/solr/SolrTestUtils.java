package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrInputDocument;

public class SolrTestUtils {

    public static String url1 = "http://example.com/nearest/1";
    public static String url2 = "http://example.com/nearest/2";

    // Documents one and two are identical except harvest time
    public static SolrInputDocument doc1 = new SolrInputDocument();
    public static SolrInputDocument doc2 = new SolrInputDocument();
    // Documents three and four are identical except harvest time
    public static SolrInputDocument doc3 = new SolrInputDocument();
    public static SolrInputDocument doc4 = new SolrInputDocument();


    public SolrTestUtils() {

        doc1.setField("id", "s1_a");
        doc1.setField("url", url1);
        doc1.setField("url_norm", url1);
        doc1.setField("source_file_path", "file1_offset");
        doc1.setField("source_file_offset", 10L);
        doc1.setField("content_type", "text/html");
        doc1.setField("record_type", "response");
        doc1.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2019-03-15T12:00:00Z"));

        doc2.setField("id", "s1_b");
        doc2.setField("url", url1);
        doc2.setField("url_norm", url1);
        doc2.setField("source_file_path", "file2_offset");
        doc2.setField("source_file_offset", 11L);
        doc2.setField("content_type", "text/html");
        doc2.setField("record_type", "response");
        doc2.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2019-03-15T12:30:00Z"));

        doc3.setField("id", "s2_a");
        doc3.setField("url", url2);
        doc3.setField("url_norm", url2);
        doc3.setField("source_file_path", "file3_offset");
        doc3.setField("source_file_offset", 12L);
        doc3.setField("content_type", "text/html");
        doc3.setField("record_type", "response");
        doc3.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2019-03-15T11:00:00Z"));

        doc4.setField("id", "s2_b");
        doc4.setField("url", url2);
        doc4.setField("url_norm", url2);
        doc4.setField("source_file_path", "file4_offset");
        doc4.setField("source_file_offset", 13L);
        doc4.setField("content_type", "text/html");
        doc4.setField("record_type", "response");
        doc4.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2019-03-15T12:45:00Z"));

    }
}

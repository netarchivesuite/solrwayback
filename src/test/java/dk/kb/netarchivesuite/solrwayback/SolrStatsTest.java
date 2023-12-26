package dk.kb.netarchivesuite.solrwayback;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.QueryPercentilesStatistics;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.QueryStatistics;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrTestClient;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStats;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SolrStatsTest {
    private static final Logger log = LoggerFactory.getLogger(SolrStatsTest.class);

    private static String solr_home= "target/test-classes/solr_9";
    private static NetarchiveSolrClient server = null;
    private static CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;

    @Before
    public void setUp() throws Exception {
        // Embedded Solr 9.1+ must have absolute home both as env and explicit param
        System.setProperty("solr.install.dir", Path.of(solr_home).toAbsolutePath().toString());
        coreContainer = CoreContainer.createAndLoad(Path.of(solr_home).toAbsolutePath());
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer,"netarchivebuilder");
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);
        server = NetarchiveSolrClient.getInstance();

        // Remove any items from previous executions:
        embeddedServer.deleteByQuery("*:*"); //This is not on the NetarchiveSolrClient API!

        String url = "http://testurl.dk/test";
        String[] links = new String[]{"http://kb.dk/", "http://kb.dk/en"};

        ArrayList<String> crawlTimes = new ArrayList<String>();
        crawlTimes.add("2018-03-15T12:31:51Z");
        crawlTimes.add("2018-03-15T12:34:37Z");
        crawlTimes.add("2018-03-15T12:35:56Z");
        crawlTimes.add("2018-03-15T12:36:14Z");
        crawlTimes.add("2018-03-15T12:36:43Z");
        crawlTimes.add("2018-03-15T12:37:32Z");
        crawlTimes.add("2018-03-15T12:37:52Z");
        crawlTimes.add("2018-03-15T12:39:15Z");
        crawlTimes.add("2018-03-15T12:40:09Z");

        int i =1;
        for (String crawl : crawlTimes){
            SolrInputDocument document = new SolrInputDocument();
            String id = ""+i++;
            String title = "title "+i;
            document.addField("source_file_offset", i+"");
            document.addField("id", id);
            document.addField("title", title);
            document.addField("url", url);
            document.addField("url_norm", url);
            document.addField("record_type","response");
            document.addField("source_file_path", "some.warc");
            document.setField("crawl_date", crawl);

            document.addField("status_code", "200");
            document.addField("content_length", i);
            document.addField("crawl_year", 2003);
            document.addField("content_text_length", i);
            document.addField("image_height", 800);
            document.addField("image_width", 600);
            document.addField("image_size", 480000);
            document.addField("wayback_date", "20230417100000");
            document.addField("links", links);
            document.addField("domain", "www.kb.dk");

            embeddedServer.add(document);

        }
        embeddedServer.commit();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        coreContainer.shutdown();
        embeddedServer.close();
    }


    @Test
    public void singleNumericFieldStatTest(){
        // Testing with hardcoded documents above
        List<String> field = Collections.singletonList("crawl_year");

        ArrayList<QueryStatistics> stats = SolrStats.getStatsForFields("*:*", null, field);

        QueryStatistics stat = stats.get(0);

        Assert.assertEquals("crawl_year", stat.getName());
        Assert.assertEquals(2003.0, (Double) stat.getMin(), 0);
        Assert.assertEquals(2003.0, (Double) stat.getMax(), 0);
        Assert.assertEquals(18027.0, (Double) stat.getSum(), 0);
        Assert.assertEquals(9, stat.getCount(), 0);
        Assert.assertEquals(0, stat.getMissing(), 0);
        Assert.assertEquals(2003.0, (Double) stat.getMean(), 0);
    }

    @Test
    public void multipleNumericFieldStatsTest(){
        // Testing with hardcoded documents above
        ArrayList<QueryStatistics> stats = SolrStats.getStatsForFields("*:*", null , SolrStats.interestingNumericFields);

        for (QueryStatistics stat: stats) {
            Assert.assertFalse(stat.toString().isEmpty());
        }
        Assert.assertEquals(SolrStats.interestingNumericFields.size(), stats.size());
    }

    @Test
    public void singleTextFieldStatTest(){
        // Testing with hardcoded documents above
        List<String> field = Collections.singletonList("domain");
        ArrayList<QueryStatistics> stats = SolrStats.getStatsForFields("*:*", null , field);
        QueryStatistics stat = stats.get(0);

        Assert.assertEquals("domain", stat.getName());
        Assert.assertEquals(Long.valueOf(9), stat.getCount());
        Assert.assertEquals(Long.valueOf(0), stat.getMissing());
    }


    @Test
    public void defaultFieldsTest(){
        ArrayList<QueryStatistics> textStats = SolrStats.getStatsForFields("*:*", null, PropertiesLoaderWeb.STATS_ALL_FIELDS);
        ArrayList<QueryStatistics> numericStats = SolrStats.getStatsForFields("*:*", null, PropertiesLoaderWeb.STATS_NUMERIC_FIELDS);

        Assert.assertEquals(13, textStats.size());
        Assert.assertEquals(6, numericStats.size());
    }


    @Test
    public void percentileTest(){
        List<Double> percentiles = Arrays.asList(25.0, 50.0, 75.0);
        List<String> fields = Arrays.asList("image_height", "image_width", "image_size");
        ArrayList<QueryPercentilesStatistics> listOfStats = SolrStats.getPercentilesForFields("*:*", percentiles, fields);

        QueryPercentilesStatistics percentilesStat = listOfStats.get(0);
        Assert.assertEquals("image_height", percentilesStat.getName());
        Assert.assertEquals(Double.valueOf(800.0), percentilesStat.getPercentiles().get(25.0));
    }

    
    private JsonObject extractFirstObjectFromJsonArrayString(String string){
        JsonArray solrStats = new Gson().fromJson(string, JsonArray.class);
        JsonElement singleEntry = solrStats.get(0);
        JsonObject entryAsJsonObject = singleEntry.getAsJsonObject();
        return entryAsJsonObject;
    }
}

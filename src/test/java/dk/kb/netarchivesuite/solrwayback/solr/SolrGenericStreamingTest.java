package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
public class SolrGenericStreamingTest {
    private static final Logger log = LoggerFactory.getLogger(SolrGenericStreamingTest.class);

    private static final String SOLR_HOME = "target/test-classes/solr";
    private static CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;

    @BeforeClass
    public static void setUp() throws Exception {
        log.info("Setting up embedded server");

        PropertiesLoader.initProperties();

        coreContainer = new CoreContainer(SOLR_HOME);
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer,"netarchivebuilder");
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);

        // Remove any items from previous executions:
        embeddedServer.deleteByQuery("*:*"); //This is not on the NetarchiveSolrClient API!

        fillSolr();
        SolrGenericStreaming.setDefaultSolrClient(embeddedServer);
        log.info("Embedded server ready");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        coreContainer.shutdown();
        embeddedServer.close();
    }


    @Test
    public void serverAvailable() throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery("*:*");
        assertTrue("There should be some results", embeddedServer.query(query).getResults().getNumFound() > 0);
    }

    @Test
    public void basicStreaming() {
        log.debug("Testing basic streaming");
        List<SolrDocument> docs = new SolrGenericStreaming(embeddedServer, 2, Collections.singletonList("id"), false, false, "title:title_5").
                stream().collect(Collectors.toList());
        assertFalse("Basic streaming should return some documents", docs.isEmpty());
    }

    @Test
    public void timeProximity() {
        List<SolrDocument> docs = SolrGenericStreaming.timeProximity(
                null, Arrays.asList("id", "crawl_date"), false, false, 0, "2019-04-15T12:31:51Z", "url", "title:title_5").
                stream().collect(Collectors.toList());
        assertEquals("Single result expected",
                     1, docs.size());
        SolrDocument doc = docs.get(0);
        assertEquals("The returned crawl_date should be the nearest",
                     "Fri Mar 15 13:31:51 CET 2019", doc.get("crawl_date").toString());
    }

    @Test
    public void timeProximityMulti() {
        List<SolrDocument> docs = SolrGenericStreaming.timeProximity(
                null, Arrays.asList("id", "crawl_date"), false, false, 0, "2019-04-15T12:31:51Z", "url", "*:*").
                stream().collect(Collectors.toList());
        assertTrue("Multiple results expected",
                     docs.size() > 1);
        Set<Object> dates = new HashSet<>();
        for (SolrDocument doc: docs) {
            dates.add(doc.get("crawl_date"));
        }
        assertTrue("There should be more than 1 unique data in the time proximity result set",
                   dates.size() > 1);
    }


    private static void fillSolr() throws SolrServerException, IOException {
        final int DOCS = 100;
        log.info("Filling embedded server with {} documents", DOCS);
        final Random r = new Random(87); // Random but not too random
        for (int i = 0 ; i < DOCS ; i++) {
            addDoc(i, r);
        }
        embeddedServer.commit();
    }

    private static void addDoc(int id, Random r) throws SolrServerException, IOException {
        final String[] CRAWL_TIMES = new String[]{
                "2018-03-15T12:31:51Z",
                "2019-03-15T12:31:51Z",
                "2020-03-15T12:31:51Z",
                "2021-03-15T12:31:51Z"
        };
        SolrInputDocument document = new SolrInputDocument();

        document.setField("id", "doc_" + id);
        document.addField("source_file_offset", id);
        document.addField("title", "title_" + id%10);
        document.addField("url", "htts://example.COM/" + id%10); // %10 to get duplicates
        document.addField("url_norm", "http://example.com/" + id%10);
        document.addField("record_type","response");
        document.addField("source_file_path", "some.warc_" + id);
        document.addField("status_code", "200");
        document.setField("crawl_date", CRAWL_TIMES[r.nextInt(CRAWL_TIMES.length)]);
        embeddedServer.add(document);
    }

}
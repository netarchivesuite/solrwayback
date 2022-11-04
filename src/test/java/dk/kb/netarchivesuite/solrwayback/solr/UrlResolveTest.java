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
package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrCore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class UrlResolveTest {
    private static final Logger log = LoggerFactory.getLogger(UrlResolveTest.class);

    private static final String SOLR_HOME = "target/test-classes/solr";
    private static CoreContainer coreContainer= null;
    private static ConvenientEmbeddedSolrServer solr = null;

    @BeforeClass
    public static void setUp() throws Exception {
        log.info("Setting up embedded server");

        PropertiesLoader.initProperties();

        coreContainer = new CoreContainer(SOLR_HOME);
        coreContainer.load();
        solr = new ConvenientEmbeddedSolrServer(coreContainer, "netarchivebuilder");
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(solr);

        // Remove any items from previous executions:
        solr.deleteByQuery("*:*");

        fillSolr();
        SolrGenericStreaming.setDefaultSolrClient(solr);
        log.info("Embedded server ready");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
      coreContainer.shutdown();
      solr.close();
    }

    private static void fillSolr() throws SolrServerException, IOException {
        log.info("Filling embedded server with documents");
        final Random r = new Random(87); // Random but not too random

        solr.addDoc("id", "doc_1",
                    "host", "example.org",
                    "url_norm", "http example org foo bar hest zoo pling",
                    "url", "https://www.EXAMPLE.org/foo?bar=hest&zoo=pling",
                    "url_norm", "http://example.org/foo?bar=hest&zoo=pling"
        );
        solr.addDoc("id", "doc_2",
                    "host", "example.org",
                    "url_search", "http example org foo bar ged zoo ooling",
                    "url", "https://www.EXAMPLE.org/foo?bar=ged&zoo=ooling",
                    "url_norm", "http://example.org/foo?bar=ged&zoo=ooling"
        );

        solr.commit();
    }

    @Test
    public void testLenientNoTrigger() {
        final String URL_1 = "https://www.EXAMPLE.org/foo?bar=hest&zoo=pling";
        final String URL_2_PARTIAL = "https://www.EXAMPLE.org/foo?bar=hest&zoo=ooling"; // hest is intentionally wrong
        final List<String> fields = Arrays.asList("id", "url", "url_norm");

        {
            long attempts = NetarchiveSolrClient.instance.getLenientAttempts();
            long success = NetarchiveSolrClient.instance.getLenientSuccesses();
            Stream<SolrDocument> docs = NetarchiveSolrClient.getInstance().searchURLs(fields, Stream.of(URL_1));
            long found = docs.count();
            assertEquals("The right amount of documents should be located for a matching search", 1, found);
            assertEquals("There should be a no change to the number of lenient attempts for matching search",
                         attempts, NetarchiveSolrClient.instance.getLenientAttempts());
            assertEquals("There should be a no change to the number of successful lenient attempts for matching search",
                         success, NetarchiveSolrClient.instance.getLenientSuccesses());
        }
    }

    @Test
    public void testLenientTriggerSucces() {
        final String URL_2_PARTIAL = "https://www.EXAMPLE.org/foo?bar=hest&zoo=ooling"; // hest is intentionally wrong
        final List<String> fields = Arrays.asList("id", "url", "url_norm");
// url:"https://www.EXAMPLE.org/foo?bar=hest&zoo=ooling"^200 OR url_norm:"http://example.org/foo?bar=hest&zoo=ooling"^100 OR (host:"example.org" AND url_search:"foo" AND (host:"example.org" OR url_search:"bar=hest" OR url_search:"zoo=ooling"))
        {
            long attempts = NetarchiveSolrClient.instance.getLenientAttempts();
            long success = NetarchiveSolrClient.instance.getLenientSuccesses();
            Stream<SolrDocument> docs = NetarchiveSolrClient.getInstance().searchURLs(fields, Stream.of(URL_2_PARTIAL));
            long found = docs.count();
            assertEquals("The right amount of documents should be located for a lenient search", 1, found);
            assertEquals("There should be one change to the number of lenient attempts for lenient search",
                         attempts+1, NetarchiveSolrClient.instance.getLenientAttempts());
            assertEquals("There should be one change to the number of successful lenient attempts for lenient search",
                         success+1, NetarchiveSolrClient.instance.getLenientSuccesses());
        }
    }

    @Test
    public void testLenientTriggerFail() {
        final String URL_NONEXISTING = "https://www.EXAMPLE.org/drop?bar=ged&zoo=ooling"; // drop is wrong
        final List<String> fields = Arrays.asList("id", "url", "url_norm");
// url:"https://www.EXAMPLE.org/foo?bar=hest&zoo=ooling"^200 OR url_norm:"http://example.org/foo?bar=hest&zoo=ooling"^100 OR (host:"example.org" AND url_search:"foo" AND (host:"example.org" OR url_search:"bar=hest" OR url_search:"zoo=ooling"))
        {
            long attempts = NetarchiveSolrClient.instance.getLenientAttempts();
            long success = NetarchiveSolrClient.instance.getLenientSuccesses();
            Stream<SolrDocument> docs = NetarchiveSolrClient.getInstance().searchURLs(fields, Stream.of(URL_NONEXISTING));
            long found = docs.count();
            assertEquals("The right amount of documents should be located for a lenient search", 0, found);
            assertEquals("There should be one change to the number of lenient attempts for lenient search",
                         attempts+1, NetarchiveSolrClient.instance.getLenientAttempts());
            assertEquals("There should be no change to the number of successful lenient attempts for lenient search",
                         success, NetarchiveSolrClient.instance.getLenientSuccesses());
        }
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
        document.addField("url", "https://example.COM/" + id%10); // %10 to get duplicates
        document.addField("url_norm", "http://example.com/" + id%10);
        document.addField("record_type","response");
        document.addField("source_file_path", "some.warc_" + id);
        document.addField("links", Arrays.asList("http://example.com/everywhere", "http://example.com/mod10_" + id % 10));
        document.addField("status_code", "200");
        document.setField("crawl_date", DateUtils.solrTimestampToJavaDate(CRAWL_TIMES[r.nextInt(CRAWL_TIMES.length)]));
        solr.add(document);
    }

    private static class ConvenientEmbeddedSolrServer extends EmbeddedSolrServer {
        public ConvenientEmbeddedSolrServer(Path solrHome, String defaultCoreName) {
            super(solrHome, defaultCoreName);
        }

        public ConvenientEmbeddedSolrServer(NodeConfig nodeConfig, String defaultCoreName) {
            super(nodeConfig, defaultCoreName);
        }

        public ConvenientEmbeddedSolrServer(SolrCore core) {
            super(core);
        }

        public ConvenientEmbeddedSolrServer(CoreContainer coreContainer, String coreName) {
            super(coreContainer, coreName);
        }

        /**
         * Create a SolrDocument from the given content and add it to the server.
         * Does not call {@link #commit()}.
         * @param content content for a SolrDocument.
         */
        public void addDoc(Map<String, Object> content) {
            SolrInputDocument document = new SolrInputDocument();
            content.forEach(document::addField);
            try {
                solr.add(document);
            } catch (Exception e) {
                throw new RuntimeException("Unable to add document", e);
            }
        }

        /**
         * Somewhat hacky convenience method for constructing a map and calling {@link #addDoc(Map)}.
         * Takes "pairs" of [String, Object] as [key, value].
         *
         * With Java 9 we could use {@code Map.of(...)}, but this is Java 8.
         */
        public void addDoc(Object... keyValues) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0 ; i < keyValues.length ; i+=2) {
                map.put((String) keyValues[i], keyValues[i + 1]);
            }
            addDoc(map);
        }
    }
}

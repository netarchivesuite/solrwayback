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

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

public class SolrGenericStreamingTest {
    private static final Logger log = LoggerFactory.getLogger(SolrGenericStreamingTest.class);

    public static final int TEST_DOCS = 100; // Changing this might make some unit tests fail
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
        List<SolrDocument> docs = SolrGenericStreaming.create(
                        SRequest.builder().
                                query("title:title_5").
                                fields("id").
                                pageSize(2)).
                stream().collect(Collectors.toList());
        assertFalse("Basic streaming should return some documents", docs.isEmpty());
    }

    @Test
    public void getIDsStreaming() {
        log.debug("Extract IDs");
        List<String> ids = SolrGenericStreaming.create(
                        SRequest.builder().
                                query("title:title_5").
                                fields("id")).
                stream().
                map(d -> d.getFieldValue("id").toString()).
                collect(Collectors.toList());
        assertFalse("Basic streaming should return some ids", ids.isEmpty());
    }

    /**
     * De-duplicate the stream on field {@code url} and get the records closest to the time {@code 2019-04-15T12:31:51Z}.
     */
    @Test
    public void timeProximity() {
        List<SolrDocument> docs = SolrGenericStreaming.create(
                        SRequest.builder().
                                query("title:title_5").
                                fields("id", "crawl_date").
                                timeProximityDeduplication("2019-04-15T12:31:51Z", "url")).
                stream().collect(Collectors.toList());
        assertEquals("Single result expected",
                     1, docs.size());
        SolrDocument doc = docs.get(0);
        assertEquals("The returned crawl_date should be the nearest",
                     "Fri Mar 15 13:31:51 CET 2019", doc.get("crawl_date").toString());
    }

    /**
     * Automatic batching of multiple queries.
     */
    @Test
    public void multiQuery() {
        SRequest request = SRequest.builder().
                queries(Stream.of("title:title_5", "title:title_6", "title:title_7")).
                fields("id").
                timeProximityDeduplication("2019-04-15T12:31:51Z", "url");

        Set<SolrDocument> docs = request.stream().collect(Collectors.toSet());
        assertEquals("There should be the right number of total returned documents",
                     3, docs.size());
    }

    /**
     * De-duplicate the stream on field {@code url} and get the records closest to the time {@code 2019-04-15T12:31:51Z}.
     *
     * Differs from {@link #timeProximity()} by having more than 1 result.
     */
    @Test
    public void timeProximityMulti() {
        SRequest request = SRequest.builder().
                query("*:*").
                fields("id", "crawl_date").
                timeProximityDeduplication("2019-04-15T12:31:51Z", "url");
        
        List<SolrDocument> docs = request.stream().collect(Collectors.toList());
        assertTrue("Multiple results expected",
                     docs.size() > 1);
        Set<Date> dates = new HashSet<>();
        for (SolrDocument doc: docs) {
            dates.add((Date) doc.get("crawl_date"));
        }
        assertTrue("There should be more than 1 unique data in the time proximity result set",
                   dates.size() > 1);
    }

    /*
    // Disabled as expandResources can only be tested by requesting WARC entries
    @Test
    public void exportPages() {
        List<SolrDocument> docs = SolrGenericStreaming.create(
                        SolrGenericStreaming.SRequest.builder().
                                query("kitten").
                                filterQueries("content_type_norm:html").
                                fields("url_norm", "source_file_path", "source_file_offset").
                                timeProximityDeduplication("2019-04-15T12:31:51Z", "url").
                                expandResources(true).
                                ensureUnique(true)).

                stream().
                collect(Collectors.toList());
    } *(

    /**
     * Specify maximum results from streaming.
     */
    @Test
    public void testLimit() {
        assertEquals("Limiting maxResults should return the desired max number of documents",
                     7,
                     SolrGenericStreaming.create(SRequest.create(
                             "*:*", "url", "links").maxResults(7)).
                             stream().count());
        assertEquals("Having maxResults above the total number of documents should work",
                     100,
                     SolrGenericStreaming.create(SRequest.create(
                             "*:*", "url", "links").maxResults(100000)).
                             stream().count());
    }

    /**
     * Flatten record with multi-value field to single-value by producing multiple single-field records.
     */
    @Test
    public void testFlatten() {
        SolrDocument multi = new SolrDocument();
        multi.setField("id", "foo");
        multi.setField("m", Arrays.asList("bar", "zoo"));

        List<SolrDocument> singles = SolrGenericStreaming.flatten(multi).collect(Collectors.toList());

        assertEquals("There should be the expected number of single-value only documents",
                     2, singles.size());
        for (SolrDocument doc: singles) {
            assertEquals("The 'm' field should only contain a single value",
                         1, doc.getFieldValues("m").size());
        }
        assertEquals("The first document should contain the expected 'm'-value",
                     "bar", singles.get(0).getFieldValue("m"));
        assertEquals("The second document should contain the expected 'm'-value",
                     "zoo", singles.get(1).getFieldValue("m"));
    }

    /**
     * Flatten record with two multi-value fields to single-value by producing multiple single-field records.
     */
    @Test
    public void testFlattenDoubleMulti() {
        SolrDocument multi = new SolrDocument();
        multi.setField("id", "foo");
        multi.setField("m1", Arrays.asList("bar", "zoo"));
        multi.setField("m2", Arrays.asList("moo", "bam", "kaboom"));

        List<SolrDocument> singles = SolrGenericStreaming.flatten(multi).collect(Collectors.toList());

        assertEquals("There should be the expected number of single-value only documents",
                     2*3, singles.size());
        for (SolrDocument doc: singles) {
            assertEquals("The 'm1' field should only contain a single value",
                         1, doc.getFieldValues("m1").size());
            assertEquals("The 'm2' field should only contain a single value",
                         1, doc.getFieldValues("m2").size());
        }
        assertEquals("The first document should contain the expected 'm1'-value",
                     "bar", singles.get(0).getFieldValue("m1"));
        assertEquals("The second document should contain the expected 'm2'-value",
                     "bam", singles.get(1).getFieldValue("m2"));
    }

    @Test
    public void testUnique() {
        SRequest request = SRequest.builder().
                query("*:*").
                fields("id").
                ensureUnique(true).
                uniqueFields("url");

        assertEquals("The expected number of documents should be returned when enabling uniqueness on 'url'",
                     10, request.stream().count());

        assertEquals("The expected number of documents should be returned when using hashing",
                     10, request.uniqueHashing(true).stream().count());


    }

    @Test
    public void testGrouping() {
        List<SolrDocument> docs = SRequest.builder().
                query("*:*").
                fields("url", "source_file_offset").
                deduplicateField("url").
                stream().collect(Collectors.toList());

        Set<String> urls = new HashSet<>();
        for (SolrDocument doc: docs) {
            String url = doc.getFieldValue("url").toString();
            assertTrue("The URL '" + url + "' should not have been seen before", urls.add(url));
        }
    }

    @Test
    public void testExportGroupingFacade() throws SolrServerException, InvalidArgumentServiceException, IOException {
        String csv = Streams.asString(Facade.exportFields(
                "url,source_file_offset", false, false, "url", false, "csv", false, "*:*"));
        assertEquals("There should be 10+1 CSV lines (10 unique URLs + header)",
                     11, csv.split("\n").length);
    }

    /**
     * Export records with a field ({@code links} that is multi-value.
     */
    @Test
    public void linksExportMulti() {
        List<SolrDocument> docs = SolrGenericStreaming.create(SRequest.builder().
                query("*:*").fields("url", "links").deduplicateField("url_norm")).
                stream().
                collect(Collectors.toList());
        for (SolrDocument doc: docs) {
            assertTrue("The 'links' field should contain multiple values",
                       doc.getFieldValues("links").size() > 1);
        }
    }

    /**
     * Export records with a field ({@code links} that is multi-value, but convert the field to single-value by
     * adding extra records.
     */
    @Test
    public void linksExportSingle() {
        List<SolrDocument> docs = SolrGenericStreaming.create(SRequest.builder().
                query("*:*").fields("url", "links").deduplicateField("url_norm")).
                stream().
                flatMap(SolrGenericStreaming::flatten).
                collect(Collectors.toList());
        for (SolrDocument doc: docs) {
            assertEquals("The 'links' field should only contain a single value",
                         1, doc.getFieldValues("links").size());
        }
    }

    /**
     * Quite misplaced as it tests a {@link Facade} method. TODO: Consider creating a Facade test class.
     */
    @Test
    public void testFacadeJSONLExport() throws SolrServerException, InvalidArgumentServiceException, IOException {
        List<String> jsons = IOUtils.readLines(
                Facade.exportFields("url, links", false, false, null, false, "jsonl", false, "title:title_5"),
                "utf-8");
        assertEquals("The right number of lines should be returned", 10, jsons.size());
        for (String line: jsons) {
            assertTrue("All lines should start with '{'. Problematic line: '" + line + "'",
                       line.startsWith("{"));
        }
        assertEquals("The first line should be as expected",
                     "{\"url\":\"https://example.COM/5\",\"links\":[\"http://example.com/everywhere\",\"http://example.com/mod10_5\"]}",
                     jsons.get(0));
    }

    /**
     * Quite misplaced as it tests a {@link Facade} method. TODO: Consider creating a Facade test class.
     */
    @Test
    public void testFacadeJSONExport() throws SolrServerException, InvalidArgumentServiceException, IOException {
        List<String> jsons = IOUtils.readLines(
                Facade.exportFields("url, links", false, false, null, false, "json", false, "title:title_5"),
                "utf-8");
        assertEquals("The right number of lines should be returned", 12, jsons.size());
        assertEquals("The second line should be as expected",
                     "{\"url\":\"https://example.COM/5\",\"links\":[\"http://example.com/everywhere\",\"http://example.com/mod10_5\"]},",
                     jsons.get(1));
    }

    /**
     * Simple test of CSV export.
     *
     * Quite misplaced as it tests a {@link Facade} method. TODO: Consider creating a Facade test class.
     */
    @Test
    public void testFacadeCSVExport() throws Exception {
        List<String> cvs = IOUtils.readLines(
                Facade.exportFields("url, links", false, false, null, false, "csv", false, "title:title_5"),
                //Facade.exportCvsStreaming("title:title_5", null, "url, links"),
                "utf-8");
        assertEquals("The right number of lines should be returned", 11, cvs.size()); // First line is header
        assertEquals("The first line should be a header line as expected",
                     "url,links",
                     cvs.get(0));
        assertEquals("The second line should be a data line as expected",
                     "\"https://example.COM/5\",\"http://example.com/everywhere\thttp://example.com/mod10_5\"",
                     cvs.get(1));
    }

    /**
     * Test of CSV export with flattening, i.e. modifying the output to use more lines instead of lines with
     * multi-value for a field.
     *
     * Quite misplaced as it tests a {@link Facade} method. TODO: Consider creating a Facade test class.
     */
    @Test
    public void testFacadeCSVExportFlatten() throws Exception {
        List<String> cvs = IOUtils.readLines(
                Facade.exportFields("url, links", false, false, null, true, "csv", false, "title:title_5"),
                //Facade.exportCvsStreaming("title:title_5", null, "url, links"),
                "utf-8");
        assertEquals("The right number of lines should be returned", 21, cvs.size()); // First line is header
        assertEquals("The first line should be a header line as expected",
                     "url,links",
                     cvs.get(0));
        assertEquals("The second line should be the first part of a flattened solr document",
                     "\"https://example.COM/5\",\"http://example.com/everywhere\"",
                     cvs.get(1));
        assertEquals("The third line should be the second part of a flattened solr document",
                     "\"https://example.COM/5\",\"http://example.com/mod10_5\"",
                     cvs.get(2));
    }

    /**
     * Test of GZIP-compression of field export
     * @throws Exception
     */
    @Test
    public void testFacadefieldExportGZIP() throws Exception {
        List<String> csv = IOUtils.readLines(new GZIPInputStream(
                Facade.exportFields("url, links", false, false, null, true, "csv", true, "title:title_5")),
                //Facade.exportCvsStreaming("title:title_5", null, "url, links"),
                "utf-8");
        System.out.println(csv);
        assertEquals("The right number of lines should be returned", 21, csv.size()); // First line is header
    }

    private static void fillSolr() throws SolrServerException, IOException {
        log.info("Filling embedded server with {} documents", TEST_DOCS);
        final Random r = new Random(87); // Random but not too random
        for (int i = 0 ; i < TEST_DOCS ; i++) {
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
        document.addField("url", "https://example.COM/" + id%10); // %10 to get duplicates
        document.addField("url_norm", "http://example.com/" + id%10);
        document.addField("record_type","response");
        document.addField("source_file_path", "some.warc_" + id);
        document.addField("links", Arrays.asList("http://example.com/everywhere", "http://example.com/mod10_" + id%10));
        document.addField("status_code", "200");
        document.setField("crawl_date", DateUtils.solrTimestampToJavaDate(CRAWL_TIMES[r.nextInt(CRAWL_TIMES.length)]));
        embeddedServer.add(document);
    }

}
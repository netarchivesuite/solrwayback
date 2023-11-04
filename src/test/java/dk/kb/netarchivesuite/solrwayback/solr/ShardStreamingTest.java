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
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Fake unit test as it requires a sharded Solr-setup running locally at
 * http://localhost:8983/solr/netarchivebuilder with documents in all shards.
 */
public class ShardStreamingTest {
    private static final Logger log = LoggerFactory.getLogger(ShardStreamingTest.class);

//    public static final String STAGE_SOLR = "http://localhost:54001/solr";
//    public static final String STAGE_SOLR = "http://localhost:53301/solr";
    public static final String STAGE_SOLR = "http://localhost:52300/solr";
    public static final String LOCAL_SOLR = "http://localhost:8983/solr";
    public static final String COLLECTION = "netarchivebuilder";
    public static final String STAGE_COLLECTION = "ns";
//    protected static SolrClient solrClient = new HttpSolrClient.Builder(LOCAL_SOLR + "/" + COLLECTION).build();
    protected static SolrClient solrClient = RestrictedSolrClient.createSolrClient(STAGE_SOLR, STAGE_COLLECTION);
    protected static boolean AVAILABLE = false;


    @BeforeClass
    public static void checkAvailability() {
        SolrQuery query = new SolrQuery("*:*");
        try {
            solrClient.query(query);
            AVAILABLE = true;
            PropertiesLoader.SOLR_SERVER = LOCAL_SOLR + "/" + COLLECTION;
            PropertiesLoader.SOLR_SERVER = STAGE_SOLR + "/" + STAGE_COLLECTION;
            NetarchiveSolrClient.initialize(PropertiesLoader.SOLR_SERVER);
        } catch (Exception e) {
            log.warn("No local Solr available at '" + LOCAL_SOLR + "/" + COLLECTION + "'. Skipping unit test", e);
        }
    }

    @Test
    public void testPlainStream() {
        if (!AVAILABLE) {
            return;
        }
        assertTrue("There should be some hits from plain stream",
                   new SRequest().query("*:*").fields("id").solrClient(solrClient).stream().findAny().isPresent());
    }

    @Test
    public void testStageBaseSharding() {
        if (!AVAILABLE) {
            return;
        }
        log.info("Starting basic shard test");
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id", "index_time", "author", "description", "keywords", "license_url", "content", "content_encoding")
                .shardDivide("always")
                .pageSize(20)
                .maxResults(200);

        long qt = -System.currentTimeMillis();
        long hits = request.stream().count();
        qt += System.currentTimeMillis();
        System.out.printf(Locale.ROOT,
                          "**** Got %d hits in %,d ms: %.2fhits/ms%n",
                          hits, qt, 1.0 * hits / qt);
    }

    @Test
    public void testStageSpeed() {
        if (!AVAILABLE) {
            return;
        }
        log.info("Starting speed test");
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .filterQueries("hash:sha1\\:G*")
                .fields("id", "index_time", "author", "description", "keywords", "license_url", "content_encoding")
//                .fields("id", "index_time", "author", "description", "keywords", "license_url", "content", "content_encoding")
                .shardDivide("always")
                .deduplicateFields("domain")
                .pageSize(100)
                .maxResults(5000);

        StringBuffer sb = new StringBuffer();

        for (int i = 0 ; i < 4 ; i++) {
            long qt = -System.currentTimeMillis();
            request.forceFilterQueries("hash:sha1\\:C" + (i+2) + "*");
            long hits = request.stream().count();
            qt += System.currentTimeMillis();
            String message = String.format(Locale.ROOT,
                    "**** Got %d hits in %,d ms: %.1f hits/s for shardDivide=%s",
                    hits, qt, 1.0 * hits * 1000 / qt, request.shardDivide);
            System.out.println(message);
            sb.append(message).append("\n");
            if (SRequest.CHOICE.always.equals(request.shardDivide)) {
                request.shardDivide(SRequest.CHOICE.never);
            } else {
                request.shardDivide(SRequest.CHOICE.always);
            }
        }
        System.out.println("-----------------");
        System.out.println(sb);
    }
//    **** Got 2000 hits in 172,489 ms: 0.01hits/ms for shardDivide=always
//    **** Got 2000 hits in 355,839 ms: 0.01hits/ms for shardDivide=never
//    **** Got 2000 hits in 150,393 ms: 0.01hits/ms for shardDivide=always
//    **** Got 2000 hits in 355,739 ms: 0.01hits/ms for shardDivide=never
//    **** Got 4000 hits in 440,551 ms: 0.01hits/ms for shardDivide=always
//    **** Got 4000 hits in 702,725 ms: 0.01hits/ms for shardDivide=never
//    **** Got 4000 hits in 466,712 ms: 0.01hits/ms for shardDivide=always
//    **** Got 4000 hits in 709,237 ms: 0.01hits/ms for shardDivide=never

    @Test
    public void testShardedSearch() throws IOException {
        if (!AVAILABLE) {
            return;
        }
        long allHits = new SRequest().query("*:*").fields("id").solrClient(solrClient)
                .stream().count();
        long shardHits = new SRequest().query("*:*").fields("id").solrClient(solrClient)
                .shards(SolrUtils.getShards(LOCAL_SOLR, COLLECTION).get(0).shardID)
                .stream().count();
        assertTrue("All hits (" + allHits + ") should be greater than single shard hits (" + shardHits + ")",
                   allHits > shardHits);
    }

    @Test
    public void checkShards() {
        if (!AVAILABLE) {
            return;
        }
        List<SolrUtils.Shard> shardNames = SolrUtils.getShards(LOCAL_SOLR, COLLECTION);
        assertTrue("There should be more than 1 shards", shardNames.size() > 1);
        log.debug("Shard names: " + shardNames);
    }

    @Test
    public void testShardDivideAlways() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(100);
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideAutoTrue() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("auto")
                .autoShardDivideLimit(10)
                .maxResults(100);
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideExpandResources() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(15)
                .expandResources(true);
//        dump(request);
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideTimeProximity() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(100)
                .timeProximityDeduplication("2023-10-10T19:47:00Z", "crawl_date");
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideTimeProximityExplicit() {
        if (!AVAILABLE) {
            return;
        }
        NetarchiveSolrClient.initialize(PropertiesLoader.SOLR_SERVER);
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id, crawl_date, score")
                .shardDivide("always")
                .maxResults(100)
                .timeProximityDeduplication("2023-10-10T19:47:00Z", "crawl_date");
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideSore() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(100);
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideSortDate() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(100)
                .sort("crawl_date asc");
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideSortDateExplicit() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id, crawl_date")
                .shardDivide("always")
                .maxResults(100)
                .sort("crawl_date asc");
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideSortDomainDate() {
        if (!AVAILABLE) {
            return;
        }
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(100)
                .sort("domain desc"); // FIXME domain does not become a part of fl!
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideDeduplicate() {
        if (!AVAILABLE) {
            return;
        }
        PropertiesLoader.SOLR_SERVER = LOCAL_SOLR + "/" + COLLECTION;
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(50)
                .deduplicateFields("domain");
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideStreaming() {
        if (!AVAILABLE) {
            return;
        }
        PropertiesLoader.SOLR_SERVER = LOCAL_SOLR + "/" + COLLECTION;
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(50)
                .deduplicateFields("domain");
        try (Stream<SolrDocument> docs = request.stream()) {
            assertTrue("More than 1 documents should be returned", docs.count() > 1);
        }
    }

    @Test
    public void testShardDivideDeduplicateDump() {
        if (!AVAILABLE) {
            return;
        }
        PropertiesLoader.SOLR_SERVER = LOCAL_SOLR + "/" + COLLECTION;
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id", "domain")
                .shardDivide("never")
                .maxResults(5)
                .deduplicateFields("domain");
        dump(request);
    }

    private void dump(SRequest request) {
        List<SolrDocument> collection = request.deepCopy().stream().collect(Collectors.toList());
        List<SolrDocument> shard = new ArrayList<>();
        try (CollectionUtils.CloseableIterator<SolrDocument> shardIs =
                     request.deepCopy().shardDivide("always").iterate()) {
            while (shardIs.hasNext()) {
                shard.add(shardIs.next());
            }
        }

        String[] fl = request.fields.toArray(new String[0]);
        for (int i = 0 ; i < Math.min(collection.size(), shard.size()) ; i++) {
            String c = toString(collection.get(i), fl);
            String s = toString(shard.get(i), fl);
            System.out.println(c + " <-> " + s + ": equal=" + Objects.equals(c, s) + " #" + i);
        }

//        System.out.println("col:\n" + toString(collection, request.fields.toArray(new String[0])));
//        System.out.println("sha:\n" + toString(shard, request.fields.toArray(new String[0])));
    }

    private String toString(List<SolrDocument> docs, String... fields) {
        return docs.stream()
                .map(doc -> toString(doc, fields))
                .collect(Collectors.joining("\n"));
    }
    private String toString(SolrDocument doc, String... fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(Arrays.stream(fields)
                          .map(field -> field + "='" + doc.getFieldValue(field) + "'")
                          .collect(Collectors.joining(", ")));
        sb.append("]");
        return sb.toString();
    }

    @Test
    public void testShardDivideDeduplicateExplicit() {
        if (!AVAILABLE) {
            return;
        }
        PropertiesLoader.SOLR_SERVER = LOCAL_SOLR + "/" + COLLECTION;
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id, domain")
                .shardDivide("always")
                .maxResults(100)
                .deduplicateFields("domain");  // FIXME domain does not become a part of fl!
        assertDocsEquals(request);
    }

    @Test
    public void testShardDivideAutoFalse() {
        if (!AVAILABLE) {
            return;
        }
        PropertiesLoader.SOLR_SERVER = LOCAL_SOLR + "/" + COLLECTION;
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("auto")
                .autoShardDivideLimit(Long.MAX_VALUE)
                .maxResults(100);
        assertDocsEquals(request);
    }

    @Test
    public void testComparator() {
        Comparator<SolrDocument> asc = SolrStreamShard.getDocumentComparator(new SRequest().sort(
                "crawl_date asc, id asc"));
        Comparator<SolrDocument> desc = SolrStreamShard.getDocumentComparator(new SRequest().sort(
                "crawl_date desc, id asc"));

        SolrDocument doc1 = new SolrDocument();
        doc1.setField("id", "1");
        doc1.setField("crawl_date", new Date().getTime());

        SolrDocument doc2 = new SolrDocument();
        doc2.setField("id", "2");
        doc2.setField("crawl_date", new Date().getTime()+100);

        assertEquals("Comparison of doc1 and doc2 should yield expected order for asc",
                     -1, asc.compare(doc1, doc2));
        assertEquals("Comparison of doc2 and doc1 should yield expected order for asc",
                     1, asc.compare(doc2, doc1));
        assertEquals("Comparison of doc1 and doc2 should yield expected order for desc",
                     1, desc.compare(doc1, doc2));
    }

    /**
     * Calls {@link SolrStreamShard#iterateSharded(SRequest, List)} on the {@code request} and extracts all IDs,
     * also sets {@link SRequest#shardDivide} to {@code false} and call {@link SolrStreamDirect#iterate(SRequest)}
     * and extracts all IDs. Finally the extracted IDs are compared.
     * @param request a request to test shard division.
     */
    private void assertDocsEquals(SRequest request) {
        try (CollectionUtils.CloseableIterator<SolrDocument> shardDocs = request.iterate();
             CollectionUtils.CloseableIterator<SolrDocument> plainDocs = request.deepCopy().shardDivide("never").iterate()) {
            long count = 0;
            while (plainDocs.hasNext()) {
                assertTrue("For doc #" + count + ", plainDocs has next so shardDocs should also have next",
                           shardDocs.hasNext());
                SolrDocument colDoc = plainDocs.next();
                SolrDocument shardDoc = shardDocs.next();
//                System.out.println(colDoc.get("id") + " <-> " + shardDoc.get("id"));
                assertEquals("For doc #" + count + ", id for plain and shard should be equal",
                             colDoc.get("id"), shardDoc.get("id"));
                count++;
            }
            assertFalse("After processing, shardDocs should have no more documents", shardDocs.hasNext());
            log.debug("Finished comparing {} documents", count);
        }
    }


}

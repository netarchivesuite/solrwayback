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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Fake unit test as it requires a sharded Solr-setup running locally at
 * http://localhost:8983/solr/netarchivebuilder with documents in all shards.
 */
public class ShardStreamingTest {
    private static final Logger log = LoggerFactory.getLogger(ShardStreamingTest.class);

    public static final String LOCAL_SOLR = "http://localhost:8983/solr";
    public static final String COLLECTION = "netarchivebuilder";
    protected static SolrClient solrClient = new HttpSolrClient.Builder(LOCAL_SOLR + "/" + COLLECTION).build();
    protected static boolean AVAILABLE = false;


    @BeforeClass
    public static void checkAvailability() {
        SolrQuery query = new SolrQuery("*:*");
        try {
            solrClient.query(query);
            AVAILABLE = true;
        } catch (Exception e) {
            log.warn("No local Solr available at '" + LOCAL_SOLR + "/" + COLLECTION + "'. Skipping unit test");
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
    public void testShardedSearch() throws IOException {
        if (!AVAILABLE) {
            return;
        }
        long allHits = new SRequest().query("*:*").fields("id").solrClient(solrClient)
                .stream().count();
        long shardHits = new SRequest().query("*:*").fields("id").solrClient(solrClient)
                .shards(SolrUtils.getShardNames(LOCAL_SOLR, COLLECTION).get(0))
                .stream().count();
        assertTrue("All hits (" + allHits + ") should be greater than single shard hits (" + shardHits + ")",
                   allHits > shardHits);
    }

    @Test
    public void checkShards() throws IOException {
        if (!AVAILABLE) {
            return;
        }
        List<String> shardNames = SolrUtils.getShardNames(LOCAL_SOLR, COLLECTION);
        assertTrue("There should be more than 1 shards", shardNames.size() > 1);
        log.debug("Shard names: " + shardNames);
    }

    @Test
    public void testShardDivideAlways() {
        if (!AVAILABLE) {
            return;
        }
        PropertiesLoader.SOLR_SERVER = LOCAL_SOLR + "/" + COLLECTION;
        SRequest request = new SRequest()
                .solrClient(solrClient)
                .query("*:*")
                .fields("id")
                .shardDivide("always")
                .maxResults(100);

        try (CollectionUtils.CloseableIterator<SolrDocument> docs = SolrStreamShard.iterateStrategy(request)) {
            long count = 0;
            while (docs.hasNext()) {
                count++;
                docs.next();
            }
            System.out.println("ShardDivide Hits: " + count);
        }
        
        long plain = request.shardDivide("never").stream().count();
        System.out.println("Plain hits: " + plain);

    }

}

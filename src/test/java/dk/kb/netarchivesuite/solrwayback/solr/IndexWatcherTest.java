package dk.kb.netarchivesuite.solrwayback.solr;

import junit.framework.TestCase;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

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
public class IndexWatcherTest {

    public static final String SOLR_SERVER = "http://localhost:8983/solr/netarchivebuilder";

    /**
     * Not a unit test!
     *
     * This method requires a running Solr and only outputs state changes.
     *
     * Use this by starting the test, then start, stop or update the Solr collection {@code netarchivebuilder} on
     * {@code localhost:8983} (default for the SolrWayback bundle) while watching the output.
     */
    public void disabledtestAgainstExistingIndex() throws InterruptedException {
        SolrClient solrClient = new HttpSolrClient.Builder(SOLR_SERVER).build();
        IndexWatcher watcher = new IndexWatcher(
                solrClient, 500,
                status -> System.out.println("New status: " + status));
        Thread.sleep(100000000);
    }
}
package dk.kb.netarchivesuite.solrwayback.solr;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrExportBufferedInputStream;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.apache.solr.common.SolrDocumentList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SolrGenericStreamingResultTest {
    private static final Logger log = LoggerFactory.getLogger(SolrGenericStreamingResultTest.class);

    public static final String SOLR = "http://localhost:50002/solr/some";

    @BeforeClass
    public static void setUp() {
        NetarchiveSolrClient.initialize(SOLR);
        try {
            PropertiesLoader.initProperties();
        } catch (Exception e) {
            throw new RuntimeException("Exception initializing properties", e);
        }
    }

    // Integration test
    @Test
    public void testLocalGeneric() throws Exception {
        final String QUERY = "content_type_norm:html";

        SolrGenericStreaming stream = new SolrGenericStreaming(
                SOLR, 1000,  null, false, true, QUERY);
        long unexpanded = count(stream);
        assertTrue("There should be at least 1 result", unexpanded > 0);

        stream = new SolrGenericStreaming(
                SOLR, 1000,  null, true, true, QUERY);
        long expanded = count(stream);

        assertTrue("The number of result with expansion should be greater than the number for unexpanded, " +
                   "but both was " + expanded, unexpanded < expanded);
        System.out.println("query=\"" + QUERY + "\" had " + unexpanded + " unexpanded hits and " + expanded + " expanded hits");
        log.info("query=\"" + QUERY + "\" had " + unexpanded + " unexpanded hits and " + expanded + " expanded hits");
        // query="content_type_norm:html" had 405 unexpanded hits and 2760 expanded hits
    }

    // Integration test
    @Test
    public void testLocalCSV() throws Exception {
        final String QUERY = "content_type_norm:html";

        SolrGenericStreaming checkStream = new SolrGenericStreaming(
                SOLR, 1000,  null, false, true, QUERY);
        long unexpanded = count(checkStream);
        System.out.println("Raw results for '" + QUERY + "' was " + unexpanded);
        
        SolrStreamingExportClient sec = SolrStreamingExportClient.createExporter(SOLR, false, QUERY);
        StreamingSolrExportBufferedInputStream stream = new StreamingSolrExportBufferedInputStream(
                sec, 50000, Integer.MAX_VALUE);

        ByteOutputStream bos = new ByteOutputStream();
        byte[] buff = new byte[8192];
        int read;
        long sum = 0;
        while ((read = stream.read(buff)) != -1) {
            sum += buff[0];
            bos.write(buff, 0, read);
        }
        System.out.println("Read full " + bos.size() + " bytes with check sum " + sum);
//        System.out.println(new String(bos.toByteArray()));
        // Read full 120278 with check sum 1201
    }

    private long count(SolrGenericStreaming stream) throws Exception {
        long count = 0;
        while (true) {
            SolrDocumentList docs = stream.nextDocuments();
            if (docs.isEmpty()) {
                break;
            }
            count += docs.size();
        }
        return count;
    }
}
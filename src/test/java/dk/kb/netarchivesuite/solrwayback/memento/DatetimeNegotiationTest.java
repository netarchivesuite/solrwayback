package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpHead;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;

public class DatetimeNegotiationTest {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiationTest.class);

    @Before
    public void setup() throws SolrServerException, IOException {
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
        PropertiesLoaderWeb.initProperties();
        log.info("'{}' docs indexed in solr instance.", NetarchiveSolrClient.getInstance().countResults("*:*"));
    }


    @Test
    public void testOriginalAsOwnTimeGate() throws Exception {

        HttpRequest request = new HttpHead("");
        request.setHeader("Host", "www.kb.dk");
        request.setHeader("Accept-Datetime", "Thu, 23 Mar 2023 14:05:57 GMT");
        request.setHeader("Connection", "Close");

        //Response response = DatetimeNegotiation.redirectToDistinctMemento(request);

    }

    @Test
    public void testMultipleHeadersCreation() throws Exception {
        Response timeGate = DatetimeNegotiation.getMemento("http://kb.dk/", "archive.xxx", "Thu, 23 Mar 2019 14:05:57 GMT", "200"  );


        System.out.println(timeGate.getHeaders());
    }
}

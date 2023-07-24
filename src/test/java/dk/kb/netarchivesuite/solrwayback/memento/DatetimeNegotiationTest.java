package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.message.BasicHttpRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class DatetimeNegotiationTest {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiationTest.class);

    @Before
    public void setup() throws SolrServerException, IOException {
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
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
}

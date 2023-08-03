package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpHead;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DatetimeNegotiationTest {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiationTest.class);

    //TODO: Mock method getMemento() to check header constructions.

    @Before
    public void setup() throws SolrServerException, IOException {
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
        PropertiesLoaderWeb.initProperties();
        log.info("'{}' docs indexed in solr instance.", NetarchiveSolrClient.getInstance().countResults("*:*"));
    }


    @Test
    public void testPatternTwoPointTwoHeaderConstruction() throws Exception {
        Response timeGate = DatetimeNegotiation.getMemento("http://kb.dk/", "Thu, 23 Mar 2019 14:05:57 GMT", "200");
        MultivaluedMap<String, Object> headers = timeGate.getHeaders();

        assertEquals("accept-datetime", headers.get("Vary").get(0));
        assertFalse(headers.get("Content-Location").isEmpty());
        assertFalse(headers.get("Memento-Datetime").isEmpty());
        assertTrue(headers.get("Link").get(0).toString().contains("rel=\"original\""));
        assertFalse(headers.get("Content-Length").isEmpty());
        assertEquals("text/html; charset=UTF-8", headers.get("Content-Type").get(0));

    }
}

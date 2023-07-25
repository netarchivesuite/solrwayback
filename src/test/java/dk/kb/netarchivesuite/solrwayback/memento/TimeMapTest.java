package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.text.ParseException;

import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getTimeMap;

public class TimeMapTest {

    @Before
    public void setUp(){
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
        PropertiesLoaderWeb.initProperties();
    }

    @Test
    public void testTimeMapLinkConstruction() throws ParseException {
        Response response = getTimeMap("http://kb.dk/", "application/link-format");

        System.out.println(response.getEntity().toString());
    }
}

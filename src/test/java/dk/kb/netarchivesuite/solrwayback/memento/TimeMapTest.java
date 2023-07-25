package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class TimeMapTest {

    @Test
    public void testLinkMapConstruction(){
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
        PropertiesLoader.initProperties();

        Response response = TimeMap.getTimeMap("http://www.kb.dk", "application/link-format");

        System.out.println(response.getEntity().toString());
    }
}

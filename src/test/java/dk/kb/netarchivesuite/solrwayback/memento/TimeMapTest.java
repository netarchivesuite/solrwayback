package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.text.ParseException;

public class TimeMapTest {

    @Test
    public void testLinkMapConstruction() throws ParseException {
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
        PropertiesLoader.initProperties();

        /*
        SRequest.builder().query("url_norm:\"http://kb.dk/\"")
                .fields("url", "url_norm", "wayback_date").stream()
                .map(doc -> doc.getFieldValue("wayback_date"))
                .forEach(System.out::println);

         */


        Response response = TimeMap.getTimeMap("http://www.kb.dk", "application/link-format");

        System.out.println(response.getEntity().toString());
    }
}

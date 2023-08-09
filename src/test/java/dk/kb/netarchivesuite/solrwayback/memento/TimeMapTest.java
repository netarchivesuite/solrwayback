package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class TimeMapTest {
    final String testTimeMap = "<http://kb.dk/>;rel=\"original\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/link/http://kb.dk/>" +
            "; rel=\"self\"; type=\"application/link-format\"" +
            "; from=\"Thu, 23 Mar 2023 14:05:57 GMT\"" +
            "; until=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/http://kb.dk/>" +
            "; rel=\"timegate\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230323140557/https://www.kb.dk/>" +
            "; rel=\"first memento\"; datetime=\"Thu, 23 Mar 2023 14:05:57 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230323140557/http://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Thu, 23 Mar 2023 14:05:57 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230721064503/http://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Fri, 21 Jul 2023 06:45:03 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230721064504/https://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Fri, 21 Jul 2023 06:45:04 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230726095312/https://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230726095312/http://www.kb.dk/>" +
            "; rel=\"last memento\"; datetime=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n";

    final String testPagedTimeMap = "<http://kb.dk/>;rel=\"original\",\n" +
                "<http://localhost:8080/solrwayback/services/memento/timemap/link/http://kb.dk/>; rel=\"self\"; type=\"application/link-format\"; from=\"Thu, 23 Mar 2023 14:05:57 GMT\"; until=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n"+
                "<http://localhost:8080/solrwayback/services/memento/http://kb.dk/>; rel=\"timegate\",\n" +
                "<http://localhost:8080/solrwayback/services/web/20230726095312/https://www.kb.dk/>; rel=\"memento\"; datetime=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n" +
                "<http://localhost:8080/solrwayback/services/web/20230726095312/http://www.kb.dk/>; rel=\"last memento\"; datetime=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n" +
                "<http://localhost:8080/solrwayback/services/memento/timemap/2/link/http://kb.dk/>; rel=\"prev\"; type=\"application/link-format\"\n";

    @Before
    public void setUp(){
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
        PropertiesLoaderWeb.initProperties();
    }


    @Test()
    public void testTimeMapLinkConstruction() throws IOException, URISyntaxException {
        // Set very high to disable paging
        PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT = 9999999;
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/link-format", null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        timeMap.write(output);
        String timeMapString = new String(output.toByteArray(), StandardCharsets.UTF_8);

        assertEquals(testTimeMap, timeMapString);
    }

    @Test
    public void testNestedTimeMapConstruction() throws URISyntaxException, IOException {
        // Set very low to enable paging
        PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT = 2;
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/link-format", null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        timeMap.write(output);
        String timeMapString = new String(output.toByteArray(), StandardCharsets.UTF_8);

        assertEquals(testPagedTimeMap, timeMapString);
    }
}

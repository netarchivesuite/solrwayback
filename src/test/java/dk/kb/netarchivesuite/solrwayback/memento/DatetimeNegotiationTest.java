package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class DatetimeNegotiationTest {

    @Test
    public void testHeadersForPatternTwoPointTwoHeaderConstruction() throws Exception {
        try (MockedStatic<DatetimeNegotiation> negotiation = Mockito.mockStatic(DatetimeNegotiation.class)) {
            negotiation.when(() -> DatetimeNegotiation.getMemento("http://kb.dk/", "Thu, 23 Mar 2019 14:05:57 GMT"))
                    .thenReturn(Response.noContent()
                            .header("Vary", "accept-datetime")
                            .header("Link", "<https://www.kb.dk/>; rel=\"original\",<http://localhost:8080/solrwayback/services/memento/timemap/https://www.kb.dk/>; rel=\"timemap\"; type=\"application/link-format\",<http://localhost:8080/solrwayback/services/memento/https://www.kb.dk/>; rel=\"timegate\"")
                            .header("Memento-Datetime", "Wed, 26 Jul 2023 09:53:12 GMT")
                            .header("Content-Length", 133114)
                            .header("Content-Type", "text/html;charset=UTF-8")
                            .header("Content-Location", "http://localhost:8080/solrwayback/services/web/20230726095312/https://www.kb.dk/")
                            .build());

            Response timeGate = DatetimeNegotiation.getMemento("http://kb.dk/", "Thu, 23 Mar 2019 14:05:57 GMT");
            MultivaluedMap<String, Object> headers = timeGate.getHeaders();

            assertEquals("accept-datetime", headers.get("Vary").get(0));
            assertFalse(headers.get("Content-Location").isEmpty());
            assertFalse(headers.get("Memento-Datetime").isEmpty());
            assertTrue(headers.get("Link").get(0).toString().contains("rel=\"original\""));
            assertFalse(headers.get("Content-Length").isEmpty());
            assertEquals("text/html;charset=UTF-8", headers.get("Content-Type").get(0));
        }
    }


    @Test
    public void testHeadersForPatternTwoPointOneHeaderConstruction() throws Exception {
        PropertiesLoader.MEMENTO_REDIRECT = true;
        try (MockedStatic<DatetimeNegotiation> negotiation = Mockito.mockStatic(DatetimeNegotiation.class)) {
            negotiation.when(() -> DatetimeNegotiation.getMemento("http://kb.dk/", "Thu, 23 Mar 2019 14:05:57 GMT"))
                    .thenReturn(Response.noContent()
                            .header("Vary", "accept-datetime")
                            .header("Content-Length", 0)
                            .header("Location", "http://localhost:8080/solrwayback/services/web/20230726095312/https://www.kb.dk/")
                            .header("Link", "<https://www.kb.dk/>; rel=\"original\",<http://localhost:8080/solrwayback/services/memento/timemap/https://www.kb.dk/>; rel=\"timemap\"; type=\"application/link-format\"")
                            .header("Content-Type", "text/html;charset=UTF-8")
                            .build());

            Response timeGate = DatetimeNegotiation.getMemento("http://kb.dk/", "Thu, 23 Mar 2019 14:05:57 GMT");
            MultivaluedMap<String, Object> headers = timeGate.getHeaders();

            assertEquals("accept-datetime", headers.get("Vary").get(0));
            assertFalse(headers.get("Location").isEmpty());
            assertFalse(headers.containsKey("Memento-Datetime"));
            assertTrue(headers.get("Link").get(0).toString().contains("rel=\"original\""));
            assertEquals(0, headers.get("Content-Length").get(0));
            assertEquals("text/html;charset=UTF-8", headers.get("Content-Type").get(0));
        }

    }

}

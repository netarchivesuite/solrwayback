package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.service.exception.InternalServiceException;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class PathResolverTest {


    @Test
    public void testMementoResolvingHttpsAndSingleSlashc() throws URISyntaxException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/https:/kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, "https://kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }

    @Test
    public void testMementoResolvingWaybackDate() throws URISyntaxException, InternalServiceException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/2013/https:/kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, "https://kb.dk/", "2013");

        assertEquals("http://kb.dk/", result.toString());
    }

    @Test
    public void testMementoResolvingWWW() throws URISyntaxException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/www.kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, "www.kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }

    @Test
    public void testMementoResolvingNoHttpOrWWW() throws URISyntaxException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, "kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }

    @Test
    public void testPagedTimemap() throws URISyntaxException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/timemap/2/json/kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/timemap/2/json/", uriInfo, "kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }



}

package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Context;
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

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, null, "https://kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }

    //TODO: Add tests for no prefixes at all
    @Test
    public void testMementoResolvingWWW() throws URISyntaxException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/www.kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, null, "www.kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }

    @Test
    public void testMementoResolvingNoHttpOrWWW() throws URISyntaxException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, null, "kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }



}

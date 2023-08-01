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
    public void testMementoResolving() throws URISyntaxException {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/services/memento/https:/kb.dk/"));

        URI result = PathResolver.mementoAPIResolver("/memento/", uriInfo, null, "https://kb.dk/");

        assertEquals("http://kb.dk/", result.toString());
    }

}

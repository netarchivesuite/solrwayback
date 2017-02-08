package dk.kb.netarchivesuite.solrwayback.service.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundServiceExceptionMapper implements ExceptionMapper<NotFoundServiceException> {
    private static final Response.Status responseStatus = Response.Status.NOT_FOUND;

    @Override
    public Response toResponse(NotFoundServiceException exc) {

        return (exc.getMessage() != null)
                ? Response.status(responseStatus).entity(exc.getMessage()).type("text/plain").build()
                : Response.status(responseStatus).build();
    }
}

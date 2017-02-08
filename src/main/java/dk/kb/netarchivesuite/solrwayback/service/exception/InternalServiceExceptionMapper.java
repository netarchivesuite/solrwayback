package dk.kb.netarchivesuite.solrwayback.service.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
             
public class InternalServiceExceptionMapper implements ExceptionMapper<InternalServiceException> {
    private static final Response.Status responseStatus = Response.Status.INTERNAL_SERVER_ERROR;

    @Override
    public Response toResponse(InternalServiceException exc) {

        return (exc.getMessage() != null)
                ? Response.status(responseStatus).entity(exc.getMessage()).type("text/plain").build()
                : Response.status(responseStatus).build();
    }
}

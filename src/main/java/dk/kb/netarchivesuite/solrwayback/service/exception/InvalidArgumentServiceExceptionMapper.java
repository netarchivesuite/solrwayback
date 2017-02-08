package dk.kb.netarchivesuite.solrwayback.service.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidArgumentServiceExceptionMapper implements ExceptionMapper<InvalidArgumentServiceException> {
    private static final Response.Status responseStatus = Response.Status.BAD_REQUEST;

    @Override
    public Response toResponse(InvalidArgumentServiceException exc) {

        return (exc.getMessage() != null)
                ? Response.status(responseStatus).entity(exc.getMessage()).type("text/plain").build()
                : Response.status(responseStatus).build();
    }
}

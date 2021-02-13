package dk.kb.netarchivesuite.solrwayback.service.exception;

import javax.ws.rs.core.Response;

public class NotFoundServiceException extends SolrWaybackServiceException {
    
    private static final long serialVersionUID = 27182818L;
    private static final Response.Status responseStatus = Response.Status.NOT_FOUND;
    
    public NotFoundServiceException() {
        super(responseStatus);
    }
    
    public NotFoundServiceException(String message) {
        super(message, responseStatus);
    }
    
    public NotFoundServiceException(String message, Throwable cause) {
        super(message, cause, responseStatus);
    }
    
    public NotFoundServiceException(Throwable cause) {
        super(cause, responseStatus);
    }
}


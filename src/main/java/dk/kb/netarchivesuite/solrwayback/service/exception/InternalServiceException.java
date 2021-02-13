package dk.kb.netarchivesuite.solrwayback.service.exception;


import javax.ws.rs.core.Response;

public class InternalServiceException extends SolrWaybackServiceException {
    
    private static final long serialVersionUID = 27182818L;
    private static final Response.Status responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
    
    public InternalServiceException() {
        super(responseStatus);
    }
    
    public InternalServiceException(String message) {
        super(message, responseStatus);
    }
    
    public InternalServiceException(String message, Throwable cause) {
        super(message, cause, responseStatus);
    }
    
    public InternalServiceException(Throwable cause) {
        super(cause, responseStatus);
    }
    
    
}



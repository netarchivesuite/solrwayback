package dk.kb.netarchivesuite.solrwayback.service.exception;

import javax.ws.rs.core.Response;

public class InvalidArgumentServiceException extends  SolrWaybackServiceException {
    
   private static final long serialVersionUID = 27182818L;
    private static final Response.Status responseStatus = Response.Status.BAD_REQUEST;
    
    public InvalidArgumentServiceException() {
        super(responseStatus);
    }
    
    public InvalidArgumentServiceException(String message) {
        super(message, responseStatus);
    }
    
    public InvalidArgumentServiceException(String message, Throwable cause) {
        super(message, cause, responseStatus);
    }
    
    public InvalidArgumentServiceException(Throwable cause) {
        super(cause, responseStatus);
    }
    
    
}


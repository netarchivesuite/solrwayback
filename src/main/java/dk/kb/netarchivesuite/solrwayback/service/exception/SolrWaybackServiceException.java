package dk.kb.netarchivesuite.solrwayback.service.exception;

import javax.ws.rs.core.Response;


public abstract class SolrWaybackServiceException extends Exception {
    private static final long serialVersionUID = 27182818L;
    private final Response.Status responseStatus;
	
	public Response.Status getResponseStatus() {
		return responseStatus;
	}
	
	public SolrWaybackServiceException(Response.Status responseStatus)
	{
        super();
		this.responseStatus = responseStatus;
	}
    
    public SolrWaybackServiceException(String message, Response.Status responseStatus) {
        super(message);
		this.responseStatus = responseStatus;
	}
    
    public SolrWaybackServiceException(String message, Throwable cause, Response.Status responseStatus) {
        super(message, cause);
		this.responseStatus = responseStatus;
	}
    
    public SolrWaybackServiceException(Throwable cause, Response.Status responseStatus) {
        super(cause);
		this.responseStatus = responseStatus;
	}
    
}

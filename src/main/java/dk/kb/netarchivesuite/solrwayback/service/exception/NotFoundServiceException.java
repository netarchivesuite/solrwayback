package dk.kb.netarchivesuite.solrwayback.service.exception;


public class NotFoundServiceException extends ServiceException  {
	
	private static final long serialVersionUID = 1L;
	
	public NotFoundServiceException() {
	        super();
	    }

	    public  NotFoundServiceException(String message) {
	        super(message);
	    }

	    public  NotFoundServiceException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public  NotFoundServiceException(Throwable cause) {
	        super(cause);
	    }	
}


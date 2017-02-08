package dk.kb.netarchivesuite.solrwayback.service.exception;


public class InternalServiceException extends ServiceException  {

	   private static final long serialVersionUID = 1L;
	
	   public InternalServiceException() {
	        super();
	    }

	    public  InternalServiceException(String message) {
	        super(message);
	    }

	    public  InternalServiceException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public  InternalServiceException(Throwable cause) {
	        super(cause);
	    }
	
	
}



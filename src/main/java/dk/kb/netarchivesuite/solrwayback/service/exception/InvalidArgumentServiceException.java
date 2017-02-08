package dk.kb.netarchivesuite.solrwayback.service.exception;


public class InvalidArgumentServiceException extends ServiceException  {
	
	private static final long serialVersionUID = 1L;
	
	public  InvalidArgumentServiceException() {
	        super();
	    }

	    public InvalidArgumentServiceException(String message) {
	        super(message);
	    }

	    public InvalidArgumentServiceException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public InvalidArgumentServiceException(Throwable cause) {
	        super(cause);
	    }
	
	
}


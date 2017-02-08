package dk.kb.netarchivesuite.solrwayback.service.exception;

public abstract class ServiceException extends Exception{

	private static final long serialVersionUID = 1L;

	public  ServiceException  () {
	        super();
	    }

	    public ServiceException  (String message) {
	        super(message);
	    }

	    public ServiceException  (String message, Throwable cause) {
	        super(message, cause);
	    }

	    public ServiceException  (Throwable cause) {
	        super(cause);
	    }
	
}

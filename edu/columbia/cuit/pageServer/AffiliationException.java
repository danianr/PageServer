package edu.columbia.cuit.pageServer;

public class AffiliationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	
	protected static final int CACHE_MISS         = 0;
	protected static final int UPDATE_REQUIRED    = 1;
	protected static final int UNKNOWN_UNI        = 2;
	protected static final int CONNECTION_UNBOUND = 4;
	protected static final int CONNECTION_RESET   = 8;
	protected static final int CONNECTION_ERROR   = 16;
	protected static final int NO_AFFILS          = 128;
	
	private final int condition;
	private final String message;
	
	
	public AffiliationException(int condition, String message){
		this.condition = condition;
		this.message = message;
	}
	
	public int getCondition() { return this.condition; };

	public String getMessage() { return this.message; };

	
}

package edu.columbia.cuit.pageServer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public interface Pagecontrol{
	
	// Return the canonical username for a given username
	String canonicalize(String username) throws AffiliationException;
	
	// Refund a previously charged transaction
	boolean credit(String transactionId, String vendor);
	
	// Assign printing dollars without a sale transaction
	boolean creditOverride(String username, String vendor, float dollarsRequested, String justification);
	
	// Called after a successful print job to debit the account
	void deduct(String printer, String username, int pages);
	
	// Refresh the cache affiliations for a user
	void forceUpdate(String username);
	
	// Check if the database is being bypassed for administrative service
	boolean getDatabaseBypass();
	
	// Full list of printers registered in the system
	List<String> getPrinters();
	
	// Check if printing dollar purchases are currently enabled
	boolean getPurchaseEnabled();
	
	// Document fragment containing the history of a user for the daysPrevious days
	DocumentFragment history(Document parentDoc, String username, int daysPrevious);
	
	// Full list of the printing affiliations for a given user
	Collection<String> listAffils(String username) throws AffiliationException;
	
	// Returns a unique SaleId every time (atomic & monotonically increasing)
	String nextSaleId();
	
	// Full DOM of all printers and their associated groupings
	DocumentFragment printerGroups(Document parentDoc);

	// Run immediately previous to submitting a job to the printer
	boolean query(String printer, String username, int pages);
	
	// Returns a DOM of the remaining quota for a given user
	DocumentFragment quota(Document parentDoc, String username);

	// Resets an individual quota specified by quotaId
	void resetQuotas(int quotaId);
	
	// Resets multiple quotas specified by an array of quotaIds
	void resetQuotas(int[] quotaIds);
	
	// Resets any quotas of weekly duration
	void resetWeeklyQuotas();
	
	// credit a user an amount (in printing dollars [typically one cent per printing dollar])
	boolean sale(String invoiceNum, String username, String vendor, int amount);
	
	// Enable or disable the administrative bypass of the database layer
	void setDatabaseBypass(boolean databaseBypass);
	
	// Enable or disable the online purchase of printing dollars
	void setPurchaseEnabled(boolean purchaseEnabled);
}

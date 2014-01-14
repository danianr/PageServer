package edu.columbia.cuit.pageServer;

import java.util.Collection;


public interface AffiliationModule {
	
			// Returns the canonical user name that will be used in
			// all transactions, access, and authentication
			String canonicalize(String username) throws AffiliationException;

			// Returns a Collection of Strings representing the full set of retrieved
			// affiliations from the module
			Collection<String> getAffiliations(String username) throws AffiliationException;

			// The number of retries any of the methods will be re-attempted in the case
			// of a AffiliationException.CONNECTION_FAILURE
			int getRetries();
}

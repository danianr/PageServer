package edu.columbia.cuit.pageServer;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.novell.ldap.*;
import com.novell.ldap.connectionpool.PoolManager;

public class LDAPAffiliationModule implements AffiliationModule{
	private static final long serialVersionUID = 1L;
	private final static int moduleRetries = 3;

	private final PoolManager manager;
	private final LDAPSearchConstraints constraints;
	private final String searchBase;
	private final String bindDn;
	private final String bindPw;
	private final LDAPSocketFactory secureSocketFactory = new LDAPJSSESecureSocketFactory();

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");

	public LDAPAffiliationModule(String ldapUrl, LDAPSearchConstraints constraints, 
			String searchBase, String bindDn, String bindPw) throws LDAPException, MalformedURLException{	

		this.constraints = constraints;
		LDAPUrl url = new LDAPUrl(ldapUrl);
		this.bindDn = bindDn;
		this.bindPw = bindPw;
		this.searchBase = searchBase;
		this.manager = new PoolManager(url.getHost(), url.getPort(), 128, 1, secureSocketFactory);
	}
	
	public String canonicalize(String username) throws AffiliationException{
		log.trace("Entry into LDAPAffiliationModule.canonicalize()");
		
		if ( username == null ) throw new AffiliationException(AffiliationException.UNKNOWN_UNI,"Null Username");
		

		// A username can be either a uni or a uid, the uni attribute
		// is the canonical username used in the kerberos principal
		StringBuffer searchFilter = new StringBuffer("(|(uni=)(uid=))");
		searchFilter.insert(13,username);
		searchFilter.insert(7, username);
			
		String[] requestedAttributes = new String[]{ "uni" };

		LDAPConnection conn = null;
		try {
			conn = manager.getBoundConnection(bindDn, bindPw.getBytes("ascii"));
				
			LDAPSearchResults results= 
				conn.search(searchBase, 1, searchFilter.toString(), requestedAttributes, false, constraints);

			if (log.isTraceEnabled()) log.trace(results.toString());


		    if ( results.hasMore() ){
			    LDAPEntry ldapResult = results.next();
			    return ldapResult.getAttribute("uni").getStringValue();	    
			    
		    }else{
			    // If there are no results returned, assume that the uni is unknown. Note if there
		    	// is a uid and not a uni, this will result in an UNKNOWN_UNI Exception
			    // This should be treated as a problem with the Identity Management system and not
			    // explicitly coded around.

			    StringBuffer sb = new StringBuffer("AffiliationException: the uni () is unknown");
			    sb.insert(31, username);
			    throw new AffiliationException(AffiliationException.UNKNOWN_UNI,sb.toString());
		    }
		    
		}catch (LDAPException e) {

			log.debug(e);
			switch(e.getResultCode()){

			    case LDAPException.CONNECT_ERROR:
				    log.error(e.getLDAPErrorMessage(), e);
					throw new AffiliationException(AffiliationException.CONNECTION_ERROR, "LDAPException: CONNECT_ERROR");
					
				case LDAPException.TIME_LIMIT_EXCEEDED:
					// If we hit the time limit, assume the server has gone catatonic
					// close the existing connection and retry this method
					if ( conn.isConnected() ) try {
						conn.disconnect();
					} catch (LDAPException e1) {
						log.debug("Caught LDAPException while trying to disconnect", e1);
					}
					throw new AffiliationException(AffiliationException.CONNECTION_ERROR, "LDAPException: TIME_LIMIT_EXCEEDED");
						
				default:
					log.error(e.getLDAPErrorMessage(), e);
					throw new AffiliationException(AffiliationException.CONNECTION_ERROR, e.getLDAPErrorMessage());
				}
				
				
		} catch (InterruptedException e) {
			log.debug("Caught an Interrupted Exception from the connection manager",e);
			throw new AffiliationException(AffiliationException.CONNECTION_ERROR, "InterruptedException");
				
		} catch (UnsupportedEncodingException e) {
			log.error("UnsupportedEncodingException for bindPw",e);
			throw new AffiliationException(AffiliationException.CONNECTION_UNBOUND, "UnsupportedEncodingException from bindPw");

		} finally{		
		
			// In the event of a connection problem to LDAP, the conn.isConnected()
			// will return false, but the connection object will still be marked as
			// in use and cause eventual deadlock in the connection pool
			manager.makeConnectionAvailable(conn);
			log.trace("Exiting LDAPAffiliationModule.canonicalize()");
		}
		
	}
	
		
	
	public Collection<String> getAffiliations(String username) throws AffiliationException{
		log.trace("Entry into LDAPAffiliationModule.getAffiliations()");
		
		StringBuffer searchFilter = new StringBuffer("(|(uni=)(uid=))");
		searchFilter.insert(13,username);
		searchFilter.insert(7, username);
		
		String[] requestedAttributes = new String[]{ "uni", "uid", "affiliation" };

		LDAPConnection conn = null;
		try {
			conn = manager.getBoundConnection(bindDn, bindPw.getBytes("ascii"));
			
			LDAPSearchResults results= 
				conn.search(searchBase, 1, searchFilter.toString(), requestedAttributes, false, constraints);

			
			if ( results.hasMore() ){
				LDAPEntry ldapResult = results.next();
				
				if (log.isTraceEnabled()) log.trace(ldapResult.toString());

				String[] affils = ldapResult.getAttribute("affiliation").getStringValueArray();

				if ( affils != null && affils.length > 0){
						
					// Create a hashSet and iterate through all entries to pass the
					// return values in a typesafe way, since Arrays.asList() performs an
					// unchecked cast
					HashSet<String> affilCollection = new HashSet<String>(affils.length);
					for (String s: affils) affilCollection.add(s);
					return affilCollection;
				}
				
				// Fall through for no affiliations (both no option and null set)
				StringBuffer sb = new StringBuffer("AffiliationException: uni () does not have any printing affils");
				sb.insert(27, username);
				throw new AffiliationException(AffiliationException.NO_AFFILS, sb.toString());
					
			}else{
				// If there are no results returned, assume that the username is unknown
				StringBuffer sb = new StringBuffer("AffiliationException: uni () is unknown");
				sb.insert(27, username);
				throw new AffiliationException(AffiliationException.UNKNOWN_UNI,sb.toString());
			}

		} catch (LDAPException e) {
			// Because of the CheckedException handling in Java, any LDAPExceptions need to be wrapped by an AffiliationException
			// TODO: Implement an actual wrapping functionality in the AffiliationException to properly bubble up Exceptions to
			// the AffiliationCache
			log.debug(e);
			switch(e.getResultCode()){
			
				case LDAPException.CONNECT_ERROR:
					throw new AffiliationException(AffiliationException.CONNECTION_ERROR, "LDAPException: CONNECT_ERROR");
				
				case LDAPException.SERVER_DOWN:
				case LDAPException.LDAP_TIMEOUT:
				case LDAPException.TIME_LIMIT_EXCEEDED:
					
					// If we hit the time limit, assume the server has gone catatonic
					// close the existing connection and retry this method
					if ( conn.isConnected() ) try {
						conn.disconnect();
					} catch (LDAPException e1) {
						log.debug("Caught LDAPException while trying to disconnect", e1);
					}
					throw new AffiliationException(AffiliationException.CONNECTION_ERROR, "LDAPException: TIME_LIMIT_EXCEEDED");
					
				default:
					log.error(e.getLDAPErrorMessage(), e);
					throw new AffiliationException(AffiliationException.CONNECTION_ERROR, e.getLDAPErrorMessage());

			}
			
			
		} catch (InterruptedException e) {
			log.debug("Caught an Interrupted Exception from the connection manager",e);
			throw new AffiliationException(AffiliationException.CONNECTION_ERROR, "InterruptedException");

		} catch (UnsupportedEncodingException e) {
			log.error("UnsupportedEncodingException for bindPw",e);
			throw new AffiliationException(AffiliationException.CONNECTION_UNBOUND, "UnsupportedEncodingException from bindPw");

		} finally{		
	
			// In the event of a connection problem to LDAP, the conn.isConnected()
			// will return false, but the connection object will still be marked as
			// in use and cause eventual deadlock in the connection pool
			manager.makeConnectionAvailable(conn);
			log.trace("Exiting LDAPAffiliationModule.getAffiliations()");
		}

	}

	
	public int getRetries(){
		return moduleRetries;
	}
}



package edu.columbia.cuit.pageServer;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.novell.ldap.*;
import com.novell.ldap.connectionpool.PoolManager;

public class AffiliationCache {
	private static final long serialVersionUID = 3L;
	private transient static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");

	private final ConcurrentHashMap<String, AffiliationEntry> affilMap;
	private final ConcurrentHashMap<String, String> canonicalUsername;
	private final long cacheDuration;
	private final Set<String> printingAffils;
    private transient final Vector<AffiliationModule> modules;
	

	public AffiliationCache(Set<String> printingAffils, long durationMillis){	
		this.modules = new Vector<AffiliationModule>(1);
		this.printingAffils = printingAffils;
		this.cacheDuration = durationMillis;

		this.affilMap = new ConcurrentHashMap<String,AffiliationEntry>();
		this.canonicalUsername = new ConcurrentHashMap<String,String>();
	}

	public void register(AffiliationModule module){
		modules.add(module);
	}
	
	public void deregister(AffiliationModule module){
		modules.remove(module);
	}

	public String canonicalize(String username) throws AffiliationException{

		// Shortcut causes for null and previously mapped usernames 
		if ( username == null ) throw new AffiliationException(AffiliationException.UNKNOWN_UNI,"Null Username");
		if ( canonicalUsername.containsKey(username) ) return canonicalUsername.get(username);

		// Fall-through processing for querying loaded modules for the canonical username
		String canonical = null;
		for (AffiliationModule am: modules){
							
			boolean awaitingResponse = true; //recover from retriable Exceptions
			int numRetries = am.getRetries();
			while (awaitingResponse){
			   try{
			     canonical = am.canonicalize(username);
			     awaitingResponse = false;
			   }catch(AffiliationException e){
				   switch (e.getCondition()){
				   		
				   		// In the case of any connection trouble decrement
				        // the retry count and allow the awaitingResponse
				   		// loop to continue.
				   		case AffiliationException.CONNECTION_ERROR:
				   		case AffiliationException.CONNECTION_UNBOUND:
				   		case AffiliationException.CONNECTION_RESET:
				   			if (numRetries-- == 0){
				   				log.error("Exhausted retries for " + am.getClass().getSimpleName());
				   				awaitingResponse = false;
				   			}
				   			break;

				   		// Assume a non-caught affiliation exception means something bad happened
				   		// and move on
				   		default:
				   			awaitingResponse = false;
				   }
			   }
			}
			if (canonical != null) break; //the first module to answer satisfies

		}  // End for(AffiliationModule am...)

		
		// If we exhaust all modules without a answer throw an UNKOWN_UNI exception 
		if (canonical == null){
			log.warn("Unable to find canonical username");
			throw new AffiliationException(AffiliationException.UNKNOWN_UNI, "No cannonical username found");
		}else{
			// Cache this entry in our canonicalUsername mapping
			canonicalUsername.putIfAbsent(username, canonical);
		}	

		return canonical;
	}
	
	
	public long size(){ return affilMap.size(); }	
	

	
	
	private Set<String> getAffilsFromCache(String username) throws AffiliationException{

		log.trace("Entry into AffiliationCache.getAffilsFromCache()");
		if (username == null) throw new AffiliationException(AffiliationException.UNKNOWN_UNI, "Null Username");

		if (affilMap.containsKey(username)){
			AffiliationEntry uniAffils = affilMap.get(username);
			log.debug(uniAffils);
			if (uniAffils.getAge() > cacheDuration){
				log.trace("Throwing UPDATE_REQUIRED from AffiliationCache.getAffilsFromCache()");
				throw new AffiliationException(AffiliationException.UPDATE_REQUIRED, username);
			}else{
				Set<String> fetchedAffils = uniAffils.getAffils();
				if (fetchedAffils == null){
					// Something went wrong if this code is running.  This prevents a nullPointerException
					// from mysteriously popping up in several other methods, but we also want to keep an
					// eye out for invocations
					log.warn("uniAffils.getAffils() returned a Null Pointer, throwing a CACHE_MISS");
					throw new AffiliationException(AffiliationException.CACHE_MISS, username);
				}

				if (fetchedAffils.isEmpty()){
					StringBuffer sb = new StringBuffer("AffiliationException: uni () does not have any printing affils");
					sb.insert(27, username);
					log.trace("Throwing NO_AFFILS from AffiliationCache.getAffilsFromCache()");
					throw new AffiliationException(AffiliationException.NO_AFFILS, sb.toString());
				}
				
				log.trace("Exiting AffiliationCache.getAffilsFromCache()");
				return fetchedAffils;
			}			
		}else {
			log.trace("Throwing CACHE_MISS from AffiliationCache.getAffilsFromCache()");
			throw new AffiliationException(AffiliationException.CACHE_MISS, username);
		}
	}

	
	
	private void updateAffiliations(String username) throws AffiliationException{
		log.trace("Entry into AffiliationCache.updateAffiliations()");
		
		Set<String> affils = new HashSet<String>();
		for (AffiliationModule am: modules){
							
			boolean awaitingResponse = true; //recover from retriable Exceptions
			int numRetries = am.getRetries();
			while (awaitingResponse){
			   try{
				   Collection<String> partialAffils = am.getAffiliations(username);
				   if (log.isDebugEnabled()){
					   log.debug("Using AffilationModule " + am.getClass().getCanonicalName());
					   log.debug(partialAffils);
				   }				   
				   affils.addAll(partialAffils);
				   awaitingResponse = false;
			   }catch(AffiliationException e){
				   log.debug("Caught AffiliationException from "+ am.getClass().getCanonicalName(), e);
				   
				   switch (e.getCondition()){
				   		
				   		// In the case of any connection trouble decrement
				        // the retry count and allow the awaitingResponse
				   		// loop to continue.
				   		case AffiliationException.CONNECTION_ERROR:
				   		case AffiliationException.CONNECTION_UNBOUND:
				   		case AffiliationException.CONNECTION_RESET:
				   			if (numRetries-- == 0){
				   				awaitingResponse = false;
				   				throw new AffiliationException(AffiliationException.CONNECTION_ERROR,
				   						am.getClass().getSimpleName() + ": unable to connect");		
				   			}
				   			break;

				   		// Assume a non-caught affiliation exception means something bad happened
				   		// and move on
				   		default:
				   			awaitingResponse = false;
				   }
			   }
			}

		}  // End for(AffiliationModule am...)
		
		log.trace("finished gathering affils from all registered AffiliationModules");
		log.trace(affils);			
		affils.retainAll(printingAffils);
		log.trace("Affils remaining after Set Intersection with printingAffils");
		log.trace(affils);
		
		// If there are no printing affils, and a AffiliationException.CONNECTION_ERROR
		// was not thrown remove the previous entry in the affiliationMap
		if (affils.isEmpty()){
			if (affilMap.containsKey(username)){
				if (log.isDebugEnabled()){
					StringBuffer sb = new StringBuffer("Removing affilMap entry for username() due to empty affils set");
					sb.insert(37, username);
					log.debug(sb.toString());
				}
				affilMap.remove(username);
			}
			StringBuffer sb = new StringBuffer("No printing affils for user ()");
			sb.insert(29, username);
			throw new AffiliationException(AffiliationException.NO_AFFILS, sb.toString());
		}
		
		if (affilMap.containsKey(username)){
				affilMap.get(username).putAffils(affils);
		}else{
				affilMap.put(username,new AffiliationEntry(username, affils));
		}	

	}

	
	private Set<String> accessCachedAffils(String uni){

		// Retrieve affils directly from cache without any exception handling
		// this method should only be accessed by exception handlers
		if (affilMap.containsKey(uni)){
			AffiliationEntry uniAffils = affilMap.get(uni);
			return uniAffils.getAffils();				
		}else {
			// TODO: is this where the unsafe null pointer access is creeping in from?
			log.error("affilMap does not contain an entry for (" + uni + ") returning null");
			return null;
		}
	}
	
	public Collection<String> getAffiliations(String username) throws AffiliationException{
		Set<String> affils = new HashSet<String>();
		String canonical = username;

		log.trace("Entry into AffiliationCache.getAffiliations()");
		try{
			canonical = canonicalize(username);
			affils = getAffilsFromCache(canonical);

		}catch (AffiliationException e){
			switch(e.getCondition()){

				// Fill the cache and then re-execute the function
				case AffiliationException.CACHE_MISS:
					updateAffiliations(canonical);
					return this.getAffiliations(canonical);
									
				// Update the cache, but use stale data if unable 
				case AffiliationException.UPDATE_REQUIRED:
					log.trace("Caught an UPDATE_REQUIRED in getAffiliations()");
					try{
						this.updateAffiliations(canonical);
						return accessCachedAffils(canonical);
					}catch(AffiliationException e1){
						if (e1.getCondition() == AffiliationException.CONNECTION_ERROR){
							log.trace("Using stale affiliations due to a CONNECTION_ERROR");
							return accessCachedAffils(canonical);
						}else{
							// re-throw exception from the nested updateAffiliations
							// and allow it to bubble up to the appropriate handler
							log.trace("rethrowing AffilationException encountered while handling an UPDATE_REQUIRED", e1);
							throw e1;
						}
					}

				case AffiliationException.UNKNOWN_UNI:
					// Note the uni does not exist and bubble up the exception
					log.info(e.getMessage());
					throw e;

				case AffiliationException.NO_AFFILS:
					// Bubble up this exception for higher-level handling
					log.trace("Rethrowing NO_AFFILS exception in getAffiliations()", e);
					throw e;
					
				default:
					log.error("Swallowing unhandled AffiliationException", e);
					break;
			}			
		}finally{
			log.trace("Exiting AffiliationCache.getAffiliations()");
		}
		return affils;
	}

	
	public String formatAffilsInSqlSet(String username) throws AffiliationException{

		log.trace("Entry into AffiliationCache.formatAffilsInSqlSet()");
		Collection<String> userAffils = getAffiliations(username);
		if (userAffils == null){
			throw new AffiliationException(AffiliationException.CACHE_MISS,
									"null pointer returned from getAffiliations");
		}else if (userAffils.isEmpty()){
			throw new AffiliationException(AffiliationException.NO_AFFILS,
										"no printing affils associated with user");
		}
		
		Iterator<String> i = userAffils.iterator();

		StringBuffer sb = new StringBuffer("IN (");
		while (i.hasNext() ){
			sb.append(" '");
			sb.append(i.next());
			sb.append('\'');
			if (i.hasNext()) sb.append(',');
		}
		sb.append(" )");
		log.trace("Exiting AffiliationCache.formatAffilsInSqlSet()");
		return sb.toString();
	}
	
	
	public long cleanCache(long expireMillis){
		
		log.trace("Entry into AffiliationCache.cleanCache()");
		Set<String> removedEntry = new HashSet<String>();
		
		// Iterate through the current contents of the cache keeping track of
		// which entries are removed.
		for (Entry<String, AffiliationEntry> cacheEntry: affilMap.entrySet()){
			// the reap method checks the current age of the cached
			// entry.  If the entry has been cached longer than the
			// the reap method returns 'true'
			if (cacheEntry.getValue().reap(expireMillis) && affilMap.remove(cacheEntry.getKey(), cacheEntry.getValue())){
					removedEntry.add(cacheEntry.getKey());
			}
		}
		
		// Move through the canonicalUser Map, Entry by Entry, deleting a mapping from
		// the canonicalUsername Map if the associated value was removed from the cache				
		for (Entry<String,String> aliasEntry: canonicalUsername.entrySet()){
			if (removedEntry.contains(aliasEntry.getValue())) {
					canonicalUsername.remove(aliasEntry.getKey());
			}
		}
	
		log.trace("Exiting AffiliationCache.cleanCache()");
		return affilMap.size();
	}
	

	// utility method to force the update of any entry regardless of age
	public void forceUpdate(String uni){
		log.trace("Entry into AffiliationCache.forceUpdate()");
		try {
			updateAffiliations(uni);
		} catch (AffiliationException e) {
			log.info(e);			
		}finally{
			log.trace("Exiting AffiliationCache.forceUpdate()");
		}
	}

	
	// Probably muddies the design to include these functions, but it's necessary to prevent
	// LDAP access from other classes and to allow introspection from support classes
	
	protected Set<Entry<String,AffiliationEntry>> exposeAffilationCache(){
		return affilMap.entrySet();
	}
	

}



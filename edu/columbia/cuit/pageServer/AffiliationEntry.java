package edu.columbia.cuit.pageServer;


import java.util.HashSet;
import java.util.Set;


public class AffiliationEntry {
	final private String uni;
	final private Set<String> affils;
	private long lastAccessTime;
	private long lastUpdateTime;

	// Populate a new LDAPAttributeSet from the argument instead
	// of using the existing object; this lets us control reference
	// links to the objects contained in the data structure 
	public AffiliationEntry(String uni, Set<String> affils){
		this.uni = uni;		
		long now = System.currentTimeMillis();
		this.lastUpdateTime = now;
		this.lastAccessTime = now;
		this.affils = new HashSet<String>();
		this.affils.addAll(affils);
	}
	
	public String getUni(){
		return uni;
	}
	
	public Set<String> getAffils(){
		this.lastAccessTime = System.currentTimeMillis();
		HashSet<String> retAffils = new HashSet<String>();
		retAffils.addAll(this.affils);
		return retAffils;
	}

	// Query the age of the cached entry; used in the decision
	// by the cache manager to update the entry; do not update
	// the access time, as no value was returned
	public long getAge(){
		return System.currentTimeMillis() - this.lastUpdateTime;
	}
	
	// The affils Collection is never deleted, but is
	// cleared and re-added to promote thread-safe operation
	public void putAffils(Set<String> affils){
		this.affils.clear();
		this.affils.addAll(affils);
		this.lastUpdateTime = System.currentTimeMillis();
	}
	
	
	// If the time period between the last update and last access exceeds
	// the threshold specified in the argument, return true to the caller
	// to indicate this entry can be removed.
	public boolean reap(long stale){
		return (lastAccessTime - lastUpdateTime > stale);
	}
}

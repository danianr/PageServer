package edu.columbia.cuit.pageServer;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.or.ObjectRenderer;

public class AffiliationCacheRenderer implements ObjectRenderer {

	
	public AffiliationCacheRenderer() { 
		// No setup of renderer is necessary
	}
	
	public String doRender(Object arg0) {
		// the Affiliation Cache is a rather large data structure
		StringBuffer sb = new StringBuffer(131072);
		sb.append("Current AffiliationCache Contents {\n");		
		AffiliationCache affilCache = (AffiliationCache) arg0;

		Set<Entry<String, AffiliationEntry>> cacheSet = affilCache.exposeAffilationCache();
		Iterator<Entry<String,AffiliationEntry>> i = cacheSet.iterator();
		while (i.hasNext()){
			Entry<String, AffiliationEntry> cachedEntry = i.next();
			String uni = cachedEntry.getKey();
			long age = cachedEntry.getValue().getAge();
			sb.append(uni);
			sb.append('[');
			sb.append(age);
			sb.append("]\n");
		}
		sb.append(" } END AffiliationCache contents");
		
		return sb.toString();
	}

}

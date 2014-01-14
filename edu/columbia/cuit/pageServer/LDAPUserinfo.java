package edu.columbia.cuit.pageServer;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import org.apache.log4j.Logger;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.LDAPSocketFactory;
import com.novell.ldap.LDAPUrl;
import com.novell.ldap.connectionpool.PoolManager;

public class LDAPUserinfo {
	
	private final PoolManager manager;
	private final LDAPSearchConstraints constraints;
	private final String searchBase;
	private final String bindDn;
	private final String bindPw;
	private final LDAPSocketFactory secureSocketFactory = new LDAPJSSESecureSocketFactory();

	
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	
	
	public LDAPUserinfo(String ldapUrl, LDAPSearchConstraints constraints, 
			String searchBase, String bindDn, String bindPw) throws LDAPException, MalformedURLException{
	
		LDAPUrl url = new LDAPUrl(ldapUrl);
		this.constraints = constraints;
		this.bindDn = bindDn;
		this.bindPw = bindPw;

		this.searchBase = searchBase;
		this.manager = new PoolManager(url.getHost(), url.getPort(), 16, 1, secureSocketFactory);
	}
				
	
	public Map<String,String> lookupUserInfo(String username) throws AffiliationException{

		log.trace("Entry into LDAPUserinfo.lookupUserInfo()");
		
		StringBuffer searchFilter = new StringBuffer("(uni=)");
		searchFilter.insert(5, username);
		
		String[] requestedAttributes = new String[]{ "uni", "cn", "title", "ou", "mail" };
		HashMap<String,String> userinfo = new HashMap<String, String>(4);
		
		LDAPConnection conn = null;
		try {
			conn = manager.getBoundConnection(bindDn, bindPw.getBytes("ascii"));
			
			LDAPSearchResults results= 
				conn.search(searchBase, 1, searchFilter.toString(), requestedAttributes, false, constraints);
			
			if ( results.hasMore() ){
				// There should only be one entry per uni, otherwise we have other problems
				LDAPEntry ldapResult = results.next();

				if (log.isTraceEnabled()) log.trace(ldapResult.toString());

				LDAPAttributeSet resultAttributes = ldapResult.getAttributeSet();		
				
				@SuppressWarnings("unchecked")
				Iterator<LDAPAttribute> attrIterator = resultAttributes.iterator();

				
				while (attrIterator.hasNext()){
					LDAPAttribute attr = attrIterator.next();
					String attrName = attr.getName();
					String attrValue = attr.getStringValue();
					userinfo.put(attrName, attrValue);
				}
	
			}else{
				// If there are no results returned, not even a uni was returned
				StringBuffer sb = new StringBuffer("AffiliationException: the uni () is unknown");
				sb.insert(31, username);
				throw new AffiliationException(AffiliationException.UNKNOWN_UNI,sb.toString());
			}
			
		} catch (UnsupportedEncodingException e) {
			
			// we should never have an encoding other than UTF-8
			log.error("Unhandled UnsupportedEncodingException", e);
			
		} catch (LDAPException e) {
			log.debug(e);
			throw new AffiliationException(AffiliationException.CONNECTION_ERROR, e.getLDAPErrorMessage());		 // NOPMD by dr2481 on 12/14/11 3:57 PM
		} catch (InterruptedException e) {
			throw new AffiliationException(AffiliationException.CONNECTION_ERROR, "Caught an InteruptedException during lookup"); // NOPMD by dr2481 on 12/14/11 3:58 PM
		} finally{		
			manager.makeConnectionAvailable(conn);
			log.trace("Exiting AffiliationCache.lookupUserInfo()");
		}
		
		return userinfo;
	}

}

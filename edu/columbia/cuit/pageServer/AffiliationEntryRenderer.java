package edu.columbia.cuit.pageServer;

import org.apache.log4j.or.ObjectRenderer;

public class AffiliationEntryRenderer implements ObjectRenderer {

	public AffiliationEntryRenderer(){
		// No initialization of render is necessary
	}
	
	public String doRender(Object arg0) {
		StringBuffer sb = new StringBuffer("AffiliationEntry() ms = { }");		
		AffiliationEntry affilEntry = (AffiliationEntry) arg0;
		
		for (String affil: affilEntry.getAffils()){
			sb.insert(sb.length() - 2, ' ');
			sb.insert(sb.length() - 2, affil);
		}
		sb.insert(19, affilEntry.getAge());
		sb.insert(17, affilEntry.getUni());

		return sb.toString();
	}

}

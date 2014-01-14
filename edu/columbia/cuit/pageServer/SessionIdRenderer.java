package edu.columbia.cuit.pageServer;

import java.util.Enumeration;

import javax.servlet.http.HttpSession;

import org.apache.log4j.or.ObjectRenderer;

public class SessionIdRenderer implements ObjectRenderer {

	
	public SessionIdRenderer() {
		// rendered does not require initialization
	}
	
	public String doRender(Object arg0) {

		if (arg0 == null) return ("No session passed");
		
		HttpSession session = (HttpSession) arg0;
		
		StringBuffer sb = new StringBuffer(300);
		sb.append("SessionId(");
		sb.append(session.getId());
		sb.append(") Creation:");
		sb.append(session.getCreationTime());
		sb.append("  LastAccessed:");
		sb.append(session.getLastAccessedTime());
		sb.append("  MaxInactiveInterval:");
		sb.append(session.getMaxInactiveInterval());
		sb.append("  IsNew:");
		sb.append(session.isNew());
		sb.append("  attributes: { ");
		@SuppressWarnings("unchecked")
		Enumeration<String> attributes = session.getAttributeNames();
		while (attributes.hasMoreElements()){
			String name = attributes.nextElement();
			sb.append(name);
			sb.append(" => ");
			sb.append(session.getAttribute(name));
			sb.append(' ');
		}
		sb.append(" }");
		
		return sb.toString();
	}

}

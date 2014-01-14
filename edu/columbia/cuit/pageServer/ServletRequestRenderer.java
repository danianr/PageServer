package edu.columbia.cuit.pageServer;

import java.security.Principal;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.or.ObjectRenderer;

public class ServletRequestRenderer implements ObjectRenderer {

	// Rendering implementation to examine HttpServletRequests
	// implemented from a ServletRequest perspective
	
	public String doRender(Object arg0) {

		if (!ServletRequest.class.isAssignableFrom(arg0.getClass())) return null;

		ServletRequest req = (ServletRequest) arg0;
		StringBuffer sb = new StringBuffer(180);
		sb.append("ServletRequest(");
		
		sb.append(req.getScheme());
		sb.append("://");
		sb.append(req.getServerName());
		sb.append(':');
		sb.append(req.getServerPort());

		sb.append(")  ");
	
		String protocol = req.getProtocol();
		
		// Most ServletRequests are really HttpServletRequests
		if (protocol.startsWith("HTTP")){
			HttpServletRequest hreq = (HttpServletRequest) req;
			
			sb.append("HttpServletRequest(");
			sb.append(hreq.getRequestURI());
			sb.append(")  RemoteAddress:");
			sb.append(hreq.getRemoteAddr());
			sb.append(':');
			sb.append(hreq.getRemotePort());
			sb.append("  RemoteUser:");
			sb.append(hreq.getRemoteUser());
			sb.append("  AuthType:");
			sb.append(hreq.getAuthType());
			sb.append("  SessionIdRequested:");
			sb.append(hreq.getRequestedSessionId());
			sb.append("  isSessionIdFromCookie: ");
			sb.append(hreq.isRequestedSessionIdFromCookie());
			Principal principal = hreq.getUserPrincipal();
			if (principal != null){
				sb.append("  User-Principal:");
				sb.append(principal.getName());
			}
			
			//Pull out any params
			@SuppressWarnings("unchecked")
			Map<String, ?> paramMap = req.getParameterMap();
			if (paramMap.isEmpty()) return sb.toString();
			
			sb.append(" Params: {");
			for (String name: paramMap.keySet()){
				sb.append(' ');
				sb.append(name);
				sb.append('=');
				sb.append(req.getParameter(name));
			}
			
		}else{
			sb.append("Not a HttpServletRequest");
		}
		
		return sb.toString();
	}
}

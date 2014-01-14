package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class RegisteredClientFilter implements Filter {

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");

	private ServletContext context;
	private AccessManager accessManager;
	private boolean clientCertAuth;
	private boolean ipBasedAuth;
	
	
	public void destroy() {
		log.trace("destroy method of RegisteredClientFilter called");
	}

	
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		log.trace("entrace: doFilter of RegisteredClientFilter");
		log.trace(req);
		
		final HttpServletRequest  httpReq = (HttpServletRequest)  req;
		final HttpServletResponse httpRes = (HttpServletResponse) res;

		if (this.accessManager == null){
			// Lazy initialization of the accessManager object
			log.trace("Attempting lazy initialization of AccessManager");
			this.accessManager = (AccessManager) this.context.getAttribute("AccessManager");
			log.trace("AccessManager found at object address (" + this.accessManager.toString() + ")");
		}

		
		if (clientCertAuth){
			log.trace("using client certificate authentication");
			// TODO: Handle authorization via client certificates
		}

		
		if (ipBasedAuth){
			
			String client = httpReq.getRemoteAddr();
			if (client == null){
				httpRes.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Unable to obtain remote address");
				log.warn("Unable to obtain remote address");
				return;
			}
		
			InetAddress clientIp = InetAddress.getByName(client);
		
			if (this.accessManager.allowedIP(clientIp)){
				log.trace("allowing client based on IP address");
				chain.doFilter(req, res);
			}else{
				log.debug("denying, sending SC_UNAUTHORIZED response");
				httpRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}			
		}
		
		
	}


	public void init(FilterConfig config) throws ServletException {
		log.trace("entrace: init of RegisteredClientFilter");
		
		// save the ServletContext so we can perform a lazy initialization
		// of the accessManager object
		this.context = config.getServletContext();
		log.debug("RegisteredClientFilter using ServletContext at object address (" + this.context.toString() +")");
		String allowIpAuth = config.getInitParameter("AllowIpBasedAuth");
		String allowCertAuth = config.getInitParameter("AllowClientCertAuth");
		
		if (allowIpAuth != null && allowIpAuth.equalsIgnoreCase("True")){
			log.trace("Allowing IP-based authorization");
			this.ipBasedAuth = true;
		}
		if (allowCertAuth != null && allowCertAuth.equalsIgnoreCase("True")){
			log.trace("Allowing Client Certificate-based authorization");
			this.clientCertAuth = true;
		}
		
	}

}

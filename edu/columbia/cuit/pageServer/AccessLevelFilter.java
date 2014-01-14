package edu.columbia.cuit.pageServer;

import java.io.IOException;

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


public class AccessLevelFilter implements Filter {

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private String filterName = "UnamedAccessLevelFilter";
	private String denyMessage;
	private AccessManager.accessLevel requiredAccess;
	private ServletContext context;
		
	public void destroy() {
		log.trace("Destroy method called for " + filterName);
	}

	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		
		AccessManager accessManager = (AccessManager) context.getAttribute("AccessManager");
		
		if (log.isTraceEnabled()){
			log.trace("entrance: " + filterName + ".doFilter");
			log.trace("Using AccessManager at object address (" + accessManager.toString() +")" );
			log.trace(req);
		}
		
		if ( (accessManager != null) && accessManager.checkAccess((HttpServletRequest) req, requiredAccess)){
			log.trace("Allowed access, calling chain.doFilter()");
			chain.doFilter(req, res);
		}else{
			log.info(denyMessage);
			HttpServletResponse hres = (HttpServletResponse) res;
			hres.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Insufficient permission to access this resource");
		}
	}

	public void init(FilterConfig conf) throws ServletException {
		
		filterName = conf.getFilterName();
		
		requiredAccess = AccessManager.accessLevel.valueOf(conf.getInitParameter("AccessLevel"));
		context = conf.getServletContext();

		denyMessage = "Access Denied to " + requiredAccess.toString() + "-level protected resource";
		log.debug("Initialized " + filterName + " for accessLevel " + requiredAccess.toString());
	}

}

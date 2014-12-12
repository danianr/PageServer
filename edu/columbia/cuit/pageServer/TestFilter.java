package edu.columbia.cuit.pageServer;
import java.io.IOException;
import java.security.Principal;
import javax.security.auth.x500.X500Principal;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class TestFilter implements Filter {

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final String x500BaseDN = ",ou=People,o=Columbia University,c=US";
	private static final String testUser = "dr2481";
	
	// Wrap the servlet request to override the user authentication methods
	class windWrappedRequest extends HttpServletRequestWrapper{
		private final String uni;
		private final Principal principal;
		
		public windWrappedRequest(HttpServletRequest req, String uni){
			super(req);
			this.uni = uni;
			principal = new X500Principal("uid=" + uni + x500BaseDN);
			if (log.isTraceEnabled()){
				log.trace("Created x500 principal: " + principal.getName());
			}
		}

		public String getAuthType(){
			return "WIND";
		}
		
		public String getRemoteUser(){
			return uni;
		}

		public Principal getUserPrincipal(){
			return principal;
		}
	}
	
	
	public void destroy() {
		log.debug("destroy method of WindFilter called");
	}	
	
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		log.trace("entrance: doFilter of TestFilter");
		log.trace(req);

		final HttpServletRequest httpReq = (HttpServletRequest) req;
		final HttpServletResponse httpRes = (HttpServletResponse) res;
		
		chain.doFilter(new windWrappedRequest(httpReq, testUser), httpRes);
		
	}

	
	public void init(FilterConfig conf) throws ServletException {
	}

		

}

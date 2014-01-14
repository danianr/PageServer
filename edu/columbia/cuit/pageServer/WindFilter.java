package edu.columbia.cuit.pageServer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WindFilter implements Filter{

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final String ticketIdParam = "ticketid";
	private static final String logoutParam = "logout";	
	private static final String x500BaseDN = ",ou=People,o=Columbia University,c=US";
	private static final String httpEncoding = "UTF-8";
	private static final int ResponseBufferSize = 4096;
	
	private static final String successNodename = "wind:authenticationSuccess";
	private static final String failureNodename = "wind:authenticationFailure";
	private static final String userNodename = "wind:user";
	private String windUri;
	private String windValidateUri;
	private String windLogoutUri;
	private String appGoodbyeUri;
	private long maxSessionLifetime;
	private boolean xmlResponse;
	private DocumentBuilderFactory builderFactory;

	
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

	
	private final void handleLogout(HttpServletResponse httpResp, HttpSession sessionId){
		if (sessionId != null){
			sessionId.invalidate();
		}
		try {
			httpResp.sendRedirect(windLogoutUri + URLEncoder.encode(appGoodbyeUri, httpEncoding));
		} catch (UnsupportedEncodingException e) {
			log.warn("Caught UnsupportedEncodingException in logout", e);
		} catch (IOException e) {
			log.warn("Caught IOException in logout", e);
		}
	}
	
	private final boolean paramComp(Map<String, String[]> reqParam, String parameterName, String target, boolean ignoreCase){
		// Helper function for parsing request parameters
		//
		// Only return true if the Parameter requested exists in the mapping and
		// one of its specified values is equal to the target; this provides more
		// complete functionality than the getParam method of HttpServletRequest
		// as it allows all values supplied to be checked.
		
		// bail out early if the argument supplied or the Mapping is null
		if ( (reqParam == null) || (parameterName == null)) return false;
		
		if (reqParam.containsKey(parameterName)){

			// Supplying a null for the target value will return true if
			// the HttpServletRequest contains any value for the specified
			// parameter, including null
			if ( target == null) return true;
			
			String [] valueArray = reqParam.get(parameterName);
			for(int i=0; i < valueArray.length; i++){	
				if (ignoreCase){
					if (valueArray[i].equalsIgnoreCase(target)) return true;
				}else{
					if (valueArray[i].equals(target)) return true;
				}
			}
		}
		return false;
	}

	
	private final String formatQueryString(Map<String, String[]> reqParam, Set<String> omitParams){
		// Support multiple values for a parameter; this is needed to allow
		// multi-valued select controls to pass through correctly
		
		
		Set<Entry<String, String[]>> paramEntries = reqParam.entrySet();
		Iterator<Entry<String,String[]>> paramIterator = paramEntries.iterator();

		StringBuffer unencodedParam = new StringBuffer("");

		while (paramIterator.hasNext()){
			Entry<String, String[]> paramEntry = paramIterator.next();
			String param = paramEntry.getKey();

			// Skip any omitted Parameters
			if (omitParams.contains(param)) continue;
			
			String[] values = paramEntry.getValue();
			if (values == null){
				unencodedParam.append(param);
				unencodedParam.append('&');
			}else{
				for(int i=0; i < values.length; i++){
					unencodedParam.append(param);
					unencodedParam.append('=');
					unencodedParam.append(values[i]);
					unencodedParam.append('&');
				}
			}
		}
		
		// trim off the terminal ampersand before converting to a string
		int lastIndex = unencodedParam.length() - 1;
		if ( lastIndex > 0  && unencodedParam.charAt(lastIndex) == '&'){
			unencodedParam.setLength(lastIndex);
		}
		
		return unencodedParam.toString();
	}
	
	
	private final boolean isValidSession(HttpServletRequest httpReq, HttpSession sessionId){

		if (sessionId == null || sessionId.isNew()){
			return false;

		}else{	
			String uni = (String) sessionId.getAttribute("username");
			String clientIP = (String) sessionId.getAttribute("clientIP");
			String remoteAddr = httpReq.getRemoteAddr();
			
			if (uni == null || clientIP == null || remoteAddr == null){
				log.debug("Invalidating session: incomplete structure");
				sessionId.invalidate();
				return false;

			}else{
			
				// Ensures that sessions will be eventually expired when repeatedly accessed
				if ( ( System.currentTimeMillis() - sessionId.getCreationTime() ) > maxSessionLifetime){
					log.debug("Invalidating session: session expired");
					sessionId.invalidate();
					return false;

				}else if( clientIP.equals(remoteAddr) ) {
					log.trace("session accepted: supplied session matches remoteAddress of the request");
					return true;

				}else{
					log.debug("Invalidating session: supplied session does not match the remoteAddress");
					sessionId.invalidate();
					return false;
				}
			}
		}
	}
	
	
	private final String paramArgN(Map<String, String[]> reqParam, String parameterName, int instance){
		// Helper function for extracting values of request parameters
		//
		// This function will extract the specified instance within the
		// argument array for a given HttpServletRequest Parameter or the
		// last specified value (as opposed to the first/undefined behavior
		// of HttpServletRequest.getParameter(String) when multi-valued
		
		
		// bail out early if the argument supplied or the Mapping is null
		if ( (reqParam == null) || (parameterName == null)) return null;
		
		if (reqParam.containsKey(parameterName)){			
			String [] valueArray = reqParam.get(parameterName);
			if (valueArray == null){
				return null;
			}else if (instance > valueArray.length){
				return valueArray[valueArray.length - 1];
			}else{
				return valueArray[instance];
			}
		}
		
		return null;
	}
	
	
	private final String getParam(Map<String, String[]> reqParam, String parameterName){
		return paramArgN(reqParam, parameterName, 0);
	}
	

	
	private final boolean validateTicket(HttpServletRequest httpReq, String ticketId, Charset charset) throws MalformedURLException, CharacterCodingException{
		// Use the return value to communicate success or failure; the
		// session will be modified by this method: creating a new session
		// binding on success or invalidating the session on failure
		//
		// The HttpServletRequest is required to set the session
		
		// Initialize the sessionId (if it's already set, this is harmless)
		HttpSession sessionId = httpReq.getSession(true);
		
		// Construct the validation URI with the ticketId
		String validationRequest = windValidateUri + ticketId;
		URL validationURL = new URL(validationRequest);

		byte[] windResponseBuffer = null;
		int responseLength = 0;
		InputStream validationStream;
		InputStream windStream;

		String uni = null;
		boolean allowed = false;
		
		
		try{
			validationStream = validationURL.openStream();

			if (log.isDebugEnabled()){

				// Insert a byte array in between the response stream
				// and the SAX Parser so we can dump the WIND response
				// text into the log.
				windResponseBuffer = new byte[ResponseBufferSize];
				int pending = 0;

				// tight while loops waiting for I/O aren't the best idea
				while ( responseLength < ResponseBufferSize){
				
					// Read the max for the first and last read, otherwise read what's available
					if (pending == 0) pending = ResponseBufferSize - responseLength;
					int bytesRead=validationStream.read(windResponseBuffer, responseLength, pending);
					if (bytesRead == -1) break ;   //break out of the loop when the stream closes
				
					responseLength += bytesRead;
					pending = validationStream.available();
				}
			
				windStream = new ByteArrayInputStream(windResponseBuffer, 0, responseLength);
				log.debug("Using a byte array backed stream for WIND response");

			}else{
				windStream = new BufferedInputStream(validationStream);
			}						

			
			// XMLResponse must be specified in the filter descriptor
			if (xmlResponse){
				log.trace("Processing WIND response as XML");

				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				Node authResult = null;
				Document windDOM = builder.parse(windStream);

				Node windResponse  = windDOM.getFirstChild();
				log.trace("windResponse NodeName: " + windResponse.getNodeName());					
				NodeList responseChildren = windResponse.getChildNodes();

				for (int i=0; i < responseChildren.getLength(); i++){
					String nodeName = responseChildren.item(i).getNodeName();
					if ( successNodename.equals(nodeName) || failureNodename.equals(nodeName) ){
						authResult = responseChildren.item(i);
					}
				}
				
				// If unable to find either a success or failure child node from the 
				// response, bail out prior to any processing; in the event of an incomplete
				// response from the input stream, rely on the parser to generate an exception
				if (authResult == null){
					log.error("Found neither an authenicationSuccess node nor an authenicationFailure node, aborting");
					return false;
				}
				
				if ( successNodename.equals(authResult.getNodeName()) ){
					log.debug("WIND authentication success");
					Node authAttr = authResult.getFirstChild();

					// walk the children until we find the user attribute node
					while ( authAttr != null){
						if (userNodename.equals(authAttr.getNodeName())){
							uni = authAttr.getTextContent();
							break;
						}
						authAttr = authAttr.getNextSibling();
					}

						// Set up the session and then fall through to the request wrapping
						sessionId.setAttribute("clientIP", httpReq.getRemoteAddr());
						sessionId.setAttribute("username", uni);
						allowed = true;
						log.trace(sessionId);
						
				}else if (failureNodename.equals(authResult.getNodeName())){
						log.info("WIND authentication rejected");
						Node errorCode = authResult.getAttributes().getNamedItem("code");
						String failureReason = authResult.getTextContent();
						log.debug("Authentication Failure code [" + errorCode.getNodeValue() + "] " + failureReason );
						sessionId.invalidate();

				}else{
						log.error("Unreachable code section: authResult is neither success or failure");
				}
				
			}else{
				
				// We're looking for the plain-text response
				log.trace("Processing WIND response as plain-text");
				
				String windResponse = null;
				uni = null;
				
				InputStreamReader in = new InputStreamReader(windStream, charset);
				BufferedReader windBuf = new BufferedReader(in);
				
				windResponse = windBuf.readLine();
				windResponse = windResponse.trim();
				
				if (windResponse.isEmpty()){
					log.error("Unable to retrieve plain-text response from WIND");
				}else{
					if (windResponse.equalsIgnoreCase("yes")){
						uni = windBuf.readLine();
						uni = uni.trim();
						log.debug("WIND  authentication success");
						sessionId.setAttribute("username", uni);
						sessionId.setAttribute("clientIP", httpReq.getRemoteAddr());
						allowed = true;
						log.trace(sessionId);
						
					}else if (windResponse.equalsIgnoreCase("no")){
						log.debug("WIND authentication rejected");
						sessionId.invalidate();
					}else{
						log.error("Unknown Wind authentication Response (" + windResponse+ ")");
					}	
				}
			}
			
		} catch (ParserConfigurationException e) {
			log.error("ParserConfigurationException for XML WIND response", e);

		} catch (SAXException e) {
				
			// Always log that an error occurred, if the log level is debug or trace
			// also include the response that caused the SAX Exception
				
			log.error("SaxException for XML WIND response", e);
			if (log.isDebugEnabled()){
				//Dump out the byte array used to buffer the windResponse;
				CharsetDecoder decoder =  charset.newDecoder();
				log.debug("WIND Response XML:\n"+ decoder.decode(ByteBuffer.wrap(windResponseBuffer, 0, responseLength)));
			}
				
		} catch (IOException e) {
			log.error("IOException occurred while validating ticketId:" + ticketId, e);

		} finally {

			if (log.isTraceEnabled()){		
				//Dump out the byte array used to buffer the windResponse;
				CharsetDecoder decoder =  charset.newDecoder();
				log.trace("WIND Response XML:\n" + decoder.decode(ByteBuffer.wrap(windResponseBuffer, 0, responseLength)));	
			}
		}

		return allowed;
	}
	
	
	
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		log.trace("entrance: doFilter of WindFilter");
		log.trace(req);

		final HttpServletRequest httpReq = (HttpServletRequest) req;
		final HttpServletResponse httpRes = (HttpServletResponse) res;
		HttpSession sessionId = httpReq.getSession(false);
		log.trace(sessionId);

		// Use the submitted HTTP Request to ascertain which host
		// and by which scheme the protected page was called, as
		// well as the ultimate destination for the request
		
		final StringBuffer windRedirect = new StringBuffer(windUri);
		final String destUrl = httpReq.getRequestURL().toString(); 
		windRedirect.append(URLEncoder.encode(destUrl, httpEncoding));

		
		String ticketId = null;

		// Check for a session before doing any ticket validation
		boolean validSession = isValidSession(httpReq, sessionId);
		
		
		// Extract the ticketId if it exists, pass on all other
		// parameters to the destinationURI parameter queryString
		
		@SuppressWarnings("unchecked")
		final Map<String, String[]> paramMap = httpReq.getParameterMap();
		
		//Check for a logout, A logout request should take precedence over any ticket validation
		if ( paramComp(paramMap, "logout", "true", true)){
				handleLogout(httpRes, sessionId);
				return;
		}

		// If a ticket has been supplied, we must validate it. 
		if ( paramMap.containsKey(ticketIdParam) ){
			ticketId = getParam(paramMap, ticketIdParam);
			log.trace("Retrieved ticketId Parameter");
			validSession = validateTicket(httpReq, ticketId, Charset.forName(httpEncoding));
		}
		
		// Encode any queryString parameters
		HashSet<String> omitParams = new HashSet<String>(2);
		omitParams.add(ticketIdParam);
		omitParams.add(logoutParam);
		String unencodedQueryString = formatQueryString(paramMap, omitParams);
		if (!unencodedQueryString.isEmpty()){
			windRedirect.append(URLEncoder.encode("?",httpEncoding));
			windRedirect.append(URLEncoder.encode(unencodedQueryString, httpEncoding));	
		}
		
		
		// Lets have a look at where we're going
		log.trace("windRedirect: " + windRedirect.toString());	

		if (validSession){
			// This is the valid session fall-through
			log.debug("Entering valid session fall-through");
			sessionId = httpReq.getSession(true);
			log.trace(sessionId);
			String uni = (String) sessionId.getAttribute("username");
			chain.doFilter(new windWrappedRequest(httpReq, uni), httpRes);
		}else{
			log.debug("Redirecting to WIND");
			httpRes.sendRedirect(windRedirect.toString());
		}
	}

	
	public void init(FilterConfig conf) throws ServletException {
		
		windUri = conf.getInitParameter("AuthenticationURI");
		windValidateUri = conf.getInitParameter("ValidationURI");
		windLogoutUri = conf.getInitParameter("LogoutURI");
		appGoodbyeUri = conf.getInitParameter("LogoutLandingURI");
		maxSessionLifetime = Long.parseLong(conf.getInitParameter("maxSessionLifetime"));
		if ( conf.getInitParameter("XMLResponse").equalsIgnoreCase("true")){
			xmlResponse = true;
		}else{
			xmlResponse = false;
		}
		builderFactory = DocumentBuilderFactory.newInstance();
	}

		

}

package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AffiliationDiagnostic extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private Pagecontrol pagecontrol;
	private LDAPUserinfo userinfoProvider;
	private Templates htmlConversion;
	
	
	public void init(){
	
		log.info("Initializing " + getServletName());
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
	
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		String xsltLocation = config.getInitParameter("htmlConversionStylesheet");	
		InputStream xsltResource = context.getResourceAsStream(xsltLocation);
		Source htmlStylesheet = new StreamSource(xsltResource);
		
		
		try {
			htmlConversion = transformerFactory.newTemplates(htmlStylesheet);
		} catch (TransformerConfigurationException e) {
			log.error("Unable to compile stylesheet into template", e);
		}
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");
		userinfoProvider = (LDAPUserinfo) context.getAttribute("UserinfoProvider");
	}


	public void doGet(HttpServletRequest req, HttpServletResponse res){

		
	  log.trace("Entry into AffiliationDiagnostic.doGet()");
	  try {
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		
		String username = null;
		String uni = null;
		String clientRequest = req.getRequestURI();
		boolean xmlResponse = false;
		boolean refreshAffils = false;
		
		if (req.getParameter("xml") != null){
			xmlResponse = true;
		}
		
		if (req.getParameter("refresh") != null){
			refreshAffils = true;
		}
		
		log.trace(clientRequest);
		Pattern affilRegexp =  Pattern.compile(".*/([a-z]{1,8}\\d{0,6})$");
		Matcher affilCommand = affilRegexp.matcher(clientRequest);

		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			int hostLength = req.getRequestURL().indexOf("/", 8);
			String baseUrl = req.getRequestURL().toString();
			baseUrl = baseUrl.substring(0, hostLength + getServletContext().getContextPath().length());
			
			builder = builderFactory.newDocumentBuilder();
			Document affilDoc = builder.newDocument();

			Element topLevelElement = affilDoc.createElement("ninja");
			topLevelElement.setAttribute("version", "3.0");
			affilDoc.appendChild(topLevelElement);
			
			Element affilReportElement = affilDoc.createElement("affiliationReport");
			topLevelElement.appendChild(affilReportElement);

			if (affilCommand.matches()){
				username = affilCommand.group(1);
				try {
					uni = pagecontrol.canonicalize(username);
					affilReportElement.setAttribute("uni", uni);
					affilReportElement.setAttribute("refreshUrl", req.getRequestURL().toString() + "?refresh");
					
					if (refreshAffils){
						log.debug("Forcing refresh of cached Affiliations");
						pagecontrol.forceUpdate(uni);
					}

					// Retrieving the userinfo from LDAP will give the previous call
					// enough time to update the cached affiliations
					Map<String, String> userinfo = userinfoProvider.lookupUserInfo(uni);

					// Copy all of the returned LDAP attributes into an XML structure
					Element userinfoElement = affilDoc.createElement("userinfo");
					affilReportElement.appendChild(userinfoElement);

					for (Entry<String, String> info: userinfo.entrySet()){
						if (info.getKey().contains(";")){
							log.debug("Skipping overloaded LDAP Attributes");
							continue;
						}
						userinfoElement.setAttribute(info.getKey(), info.getValue());
					}

					Element affilListElement = affilDoc.createElement("affilList");
					affilReportElement.appendChild(affilListElement);
					for (String affil: pagecontrol.listAffils(uni)){
						Element affilElement = affilDoc.createElement("affil");
						affilElement.appendChild(affilDoc.createTextNode(affil));
						affilListElement.appendChild(affilElement);
					}
					
					
				} catch (AffiliationException e) {

					Element errorElement = affilDoc.createElement("error");
					if ( e.getCondition() == AffiliationException.UNKNOWN_UNI){
						errorElement.setAttribute("severity", "fatal");
						errorElement.appendChild(affilDoc.createTextNode("Unable to cannonicalize username"));
						errorElement.appendChild(affilDoc.createTextNode(e.getMessage()));					
						affilReportElement.appendChild(errorElement);
					}
					
					if (e.getCondition() == AffiliationException.NO_AFFILS){
						errorElement.setAttribute("severity", "minor");
						errorElement.appendChild(affilDoc.createTextNode("There are no relevant affiliations for this user."));
						affilReportElement.appendChild(errorElement);
					}

					if ( e.getCondition() == AffiliationException.CONNECTION_ERROR ){
						errorElement.setAttribute("severity", "retryable");
						errorElement.appendChild(affilDoc.createTextNode("There was an error connecting to LDAP"));
					}
				}
				
			}else{
				log.debug("The afilRegexp did not match the requestURI, sending BAD_REQUEST error");
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The URL submitted was not of the correct format");
				return;
			}

						
			Result xmlOutput = new StreamResult(out);
			Source domTree = new DOMSource(affilDoc);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
				
				if (xmlResponse){
					// If the xml param is passed use a identity transform to return raw XML
					transformer = transformerFactory.newTransformer();
				}else{
					transformer = htmlConversion.newTransformer();
				}
				
				transformer.transform(domTree, xmlOutput);
				log.trace("Transformed history DocumentFragment into HTML");
			
			} catch (ParserConfigurationException e) {
				log.error(e);
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			} catch (TransformerConfigurationException e) {
				log.error(e);
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			} catch (TransformerException e) {
				log.error(e);
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
	  }catch (IOException e){
		  log.error(e);
	  }finally{
		  log.trace("Exiting affiliationDiagnostic.doGet()");
	  }
	}
}

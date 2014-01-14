package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
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
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.columbia.cuit.pageServer.AccessManager.accessLevel;

public class PageSale extends HttpServlet {

	
	private static final long serialVersionUID = 2L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private AccessManager accessManager;
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
		accessManager = (AccessManager) context.getAttribute("AccessManager");
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");
		userinfoProvider = (LDAPUserinfo) context.getAttribute("UserinfoProvider");
	}
	

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
		log.trace("Entry into PageSale.doGet()");
		
		
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		
		String username = null;
		String uni = null;
		String clientRequest = req.getRequestURI();
		boolean xmlResponse = false;
		boolean requestConfirmation = false;
		
		if (req.getParameter("xml") != null){
			xmlResponse = true;
		}
		
		if (req.getParameter("prompt") != null){
			requestConfirmation = true;
		}
		
		Pattern saleRegexp =  Pattern.compile(".*/([a-z]{1,8}\\d{0,6})/(\\d{1,2})\\.?(\\d0)$");
		Matcher saleCommand = saleRegexp.matcher(clientRequest);
		int amount = 0;
		
		if (saleCommand.matches()){
			username = saleCommand.group(1);
			
			try {
				uni = pagecontrol.canonicalize(username);
			} catch (AffiliationException e) {
				log.debug("Caught AffiliationException trying to canonicalize username", e);
				uni = username;
			}
			amount = Integer.parseInt(saleCommand.group(2)+saleCommand.group(3));
	
		}else{
			log.debug("The creditRegexp did not match the requestURI, sending BAD_REQUEST error");
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The URL submitted was not of the correct format");
			return;
		}
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;

		String vendor = req.getRemoteUser();
		boolean result = false;
		
			try {
				builder = builderFactory.newDocumentBuilder();
				Document saleDoc = builder.newDocument();
				
				log.trace("Retrieved history DocumentFragment");
				Element topLevelElement = saleDoc.createElement("ninja");
				topLevelElement.setAttribute("version", "3.0");
				saleDoc.appendChild(topLevelElement);

				
				
				// Permission check
				if (accessManager.checkAccess(vendor, accessLevel.SALE)){
					
					if (requestConfirmation){
						Element userinfoElement = saleDoc.createElement("userinfo");
						userinfoElement.setAttribute("uni", uni);
						try {
							for (Entry<String, String> attr: userinfoProvider.lookupUserInfo(uni).entrySet()){
								if (attr.getKey().contains(";")){
									log.debug("Skipping overloaded LDAP Attributes");
									continue;
								}
								userinfoElement.setAttribute(attr.getKey(), attr.getValue());
							}
						} catch (DOMException e) {
							log.error(e);
						} catch (AffiliationException e) {
							userinfoElement.appendChild(saleDoc.createTextNode("Unable to obtain user information"));
						}
						Element confirmElement = saleDoc.createElement("confirm");
						confirmElement.setAttribute("value", req.getRequestURL().toString().replace("\\?.*", ""));
						confirmElement.appendChild(userinfoElement);
						topLevelElement.appendChild(confirmElement);
					}else{
						Element saleResult = saleDoc.createElement("sale");
						saleResult.setAttribute("uni", uni);
						saleResult.setAttribute("amount", Float.toString(amount/100));
						result = pagecontrol.sale(pagecontrol.nextSaleId(), uni, vendor, amount);
						saleResult.setAttribute( (result)? "success" : "failure", "");
						topLevelElement.appendChild(saleResult);
					}
				}
				topLevelElement.appendChild(pagecontrol.quota(saleDoc, uni));
							
				Result xmlOutput = new StreamResult(out);
				Source domTree = new DOMSource(saleDoc);
			
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
			
	}

	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{ doGet(req, res);
	
	
	
	
	}
	
	
}

package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import edu.columbia.cuit.pageServer.AccessManager.accessLevel;

public class PageCredit extends HttpServlet {

	
	private static final long serialVersionUID = 2L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private AccessManager accessManager;
	private Pagecontrol pagecontrol;
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
	}
	

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
		log.trace("Entry into PageCredit.doGet()");
		
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		
	
		String vendor = req.getRemoteUser();
		String clientRequest = req.getRequestURI();
		String creditType = null;
		String uni = null;
		String transactionId = null;
		String justification = null;
		float dollarAmount = 0;
		boolean xmlResponse = false;
		
		if (req.getParameter("xml") != null){
			xmlResponse = true;
		}
		
		Pattern creditRegexp =  Pattern.compile(".*/transaction/(\\d{3,12})");
		Matcher creditCommand = creditRegexp.matcher(clientRequest);

		Pattern overrideRegexp = Pattern.compile(".*/override/([a-z][a-z0-9]{2,7})/(\\d{1,4}\\.\\d{1,2})");
		Matcher overrideCommand = overrideRegexp.matcher(clientRequest);
		
		if (creditCommand.matches()){
			transactionId = creditCommand.group(1);		
			creditType = "credit";
		}else if (overrideCommand.matches()){
			uni = overrideCommand.group(1);
			dollarAmount = Float.parseFloat(overrideCommand.group(2));
			creditType = "override";
			justification = req.getParameter("justification");
		}else{
			log.debug("The creditRegexp did not match the requestURI, sending BAD_REQUEST error");
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The URL submitted was not of the correct format");
			return;
		}
		
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;

		boolean result = false;
		
		// Permission check
		if ( "override".equals(creditType) && accessManager.checkAccess(req, accessLevel.OVERRIDE) ){ 

			// Using the override functionality not only requires a higher accessLevel, but also
			// a justification for why the override is being used
			
			if (justification!=null && !justification.isEmpty()){
				result = pagecontrol.creditOverride(uni, vendor, dollarAmount, justification);
			}
		}
		
		if ( "credit".equals(creditType)  && accessManager.checkAccess(req, accessLevel.CREDIT)){
			result = pagecontrol.credit(transactionId, vendor);
		}
				
		try {
				builder = builderFactory.newDocumentBuilder();
				Document creditDoc = builder.newDocument();

				Element topLevelElement = creditDoc.createElement("ninja");
				topLevelElement.setAttribute("version", "3.0");
				creditDoc.appendChild(topLevelElement);

				Element creditResult = creditDoc.createElement("credit");
				creditResult.setAttribute("operation", creditType);
				creditResult.setAttribute((result)? "success" : "failure", "");
				if ("override".equals(creditType)){
					creditResult.setAttribute("uni", uni);
					creditResult.setAttribute("amount", Float.toString(dollarAmount));
				}else{
					creditResult.setAttribute("transactionId", transactionId);
				}
								
				topLevelElement.appendChild(creditResult);
			
							
				Result xmlOutput = new StreamResult(out);
				Source domTree = new DOMSource(creditDoc);
			
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer;
				
				if (xmlResponse){
					// If the xml param is passed use a identity transform to return raw XML
					transformer = transformerFactory.newTransformer();
				}else{
					transformer = htmlConversion.newTransformer();
				}

				transformer.transform(domTree, xmlOutput);
			
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

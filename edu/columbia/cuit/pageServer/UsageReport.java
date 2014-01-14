package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.columbia.cuit.pageServer.AccessManager.accessLevel;

public class UsageReport extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	private static final int DEFAULT_HISTORY_PERIOD = 30;
	private transient AccessManager accessManager;
	private transient Pagecontrol pagecontrol;
	private transient Templates htmlConversion;
	private transient String purchaseResource;
	private transient String creditResource;
	
	
	public void init(){		
		log.info("Initializing " + getServletName());
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
		
		try {			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			String xsltLocation = config.getInitParameter("htmlConversionStylesheet");	
			InputStream xsltResource = context.getResourceAsStream(xsltLocation);
			Source htmlStylesheet = new StreamSource(xsltResource);			
			htmlConversion = transformerFactory.newTemplates(htmlStylesheet);
			
		} catch (TransformerConfigurationException e) {
			log.error("Unable to compile stylesheet into template", e);
		}
		purchaseResource = context.getInitParameter("purchaseResource");
		creditResource = context.getInitParameter("creditResource");
		accessManager = (AccessManager) context.getAttribute("AccessManager");
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");			
	}

	
	public void doGet(HttpServletRequest req, HttpServletResponse res){
		log.trace("Entry into UsageReport.doGet()");
		
		String uni = req.getRemoteUser();
		String clientRequest = req.getRequestURI();
		int numDays = DEFAULT_HISTORY_PERIOD;
		boolean xmlResponse = false;

		try {

		   if (req.getParameter("xml") != null){
			  xmlResponse = true;
		   }

		   // If the history command does not match, display as information for the remote user
		   Pattern historyRegexp =  Pattern.compile(".*/([a-z]{1,8}\\d{0,6})/(\\d{1,4})$");
		   Matcher historyCommand = historyRegexp.matcher(clientRequest);

		   if (historyCommand.matches()){
		      uni = historyCommand.group(1);
			  numDays = Integer.parseInt(historyCommand.group(2));
			  uni = pagecontrol.canonicalize(uni);
		   }
		
		   // Permission check
		   if ( accessManager.checkDisplayAccess(req, uni) ){ 
			  int hostLength = req.getRequestURL().indexOf("/", 8);
			  String baseUrl = req.getRequestURL().toString();
			  baseUrl = baseUrl.substring(0, hostLength + getServletContext().getContextPath().length());
			
			  try {	
				  DocumentBuilder builder = builderFactory.newDocumentBuilder();
				  Document presentationDOM = builder.newDocument();
				  Element topLevelElement = presentationDOM.createElement("ninja");
				  topLevelElement.setAttribute("version", "3.0");
				  presentationDOM.appendChild(topLevelElement);

				  Element appElement = presentationDOM.createElement("usageReport");
				  topLevelElement.appendChild(appElement);
		
				  if (accessManager.checkAccess(req, accessLevel.CREDIT)){
					  Element creditURL = presentationDOM.createElement("creditURL");
					  creditURL.setAttribute("href", baseUrl + creditResource);
					  topLevelElement.appendChild(creditURL);
				  }
				  
				  Element logoutURL = presentationDOM.createElement("logoutURL");
			
				  logoutURL.setAttribute("href", req.getRequestURL().toString() + "?logout=true");
				  topLevelElement.appendChild(logoutURL);
			
			
				  if (pagecontrol.getPurchaseEnabled() && uni.equals(req.getRemoteUser())){
					Element purchaseURL = presentationDOM.createElement("purchaseURL");
					purchaseURL.setAttribute("href", baseUrl + purchaseResource);
					topLevelElement.appendChild(purchaseURL);
				  }

				  
				  appElement.appendChild(pagecontrol.quota(presentationDOM, uni));
				  Node uniNode = appElement.getLastChild();
				
				  DocumentFragment historyFrag = pagecontrol.history(presentationDOM, uni, numDays);
				  uniNode.appendChild(historyFrag);
				
				  // Check the documentFragment for errors
				  NodeList errorList = appElement.getElementsByTagName("error");
				  Node errorElement = errorList.item(0);

				  if (errorElement != null && errorElement.hasAttributes()){
					NamedNodeMap errorAttrMap = errorElement.getAttributes();
					Node errorSeverity = errorAttrMap.getNamedItem("severity");
					Node errorCondition = errorAttrMap.getNamedItem("condition");
					if (errorSeverity.getNodeValue().equalsIgnoreCase("fatal")){
						log.error("Unable to display Page: " + errorCondition.getNodeValue());
						res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					}
				  }
				
				  log.trace(presentationDOM);
				  Source domSource = new DOMSource(presentationDOM);
			
				  try {
					res.setContentType("text/html");
					PrintWriter out = res.getWriter();
					Result htmlOutput = new StreamResult(out);
					
					Transformer transformer;
					if (xmlResponse){
						transformer = TransformerFactory.newInstance().newTransformer();
					}else{
						transformer = htmlConversion.newTransformer();
					}
					transformer.transform(domSource, htmlOutput);
				
				  } catch (TransformerConfigurationException e) {
					log.error(e);
				  } catch (TransformerException e) {
					log.error(e);
				  } catch (IOException e) {
					log.error(e);
				  }


			  } catch (ParserConfigurationException e2) {
				log.error(e2);
			} catch (IOException e2) {
				log.error(e2);
			}	
	
		  }else{
			try {
				res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Insufficent Access to view account");
			} catch (IOException e) {
				log.warn("Caught IOException while sending an SC_UNAUTHORIZED response in UsageReport.doGet()", e);
			}
		  }
		}catch (AffiliationException e){
			log.info("Unable to canonicalize user requested in historyCommand", e);
			try {
				res.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to canonicalize username for history request");
			} catch (IOException e1) {
				log.warn(e);
			}
		}finally{
			log.trace("Exiting UsageReport.doGet()");
		}
		
	}
		
}

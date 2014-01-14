package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.columbia.cuit.pageServer.AccessManager.accessLevel;

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

public class SupportGateway extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	private AccessManager accessManager;
	private Pagecontrol pagecontrol;
	private Templates htmlConversion;
	private String sellResource;
	private String creditResource;
	private String historyResource;
	private String accountResource;
	private String affilsResource;
	
	
	public void init(){
		log.info("Initializing " + getServletName());
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
	
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		String xsltLocation = config.getInitParameter("htmlConversionStylesheet");	
		InputStream xsltResource = context.getResourceAsStream(xsltLocation);
		Source htmlStylesheet = new StreamSource(xsltResource);
		
		
		sellResource = context.getInitParameter("sellResource");
		creditResource = context.getInitParameter("creditResource");
		accountResource = context.getInitParameter("accountResource");
		affilsResource = context.getInitParameter("affilsResource");
		historyResource = context.getInitParameter("historyResource");
		
		try {
			htmlConversion = transformerFactory.newTemplates(htmlStylesheet);
		} catch (TransformerConfigurationException e) {
			log.error("Unable to compile stylesheet into template", e);
		}
		accessManager = (AccessManager) context.getAttribute("AccessManager");
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");
	}

	
	
	public void doGet(HttpServletRequest req, HttpServletResponse res){
		log.trace("Entry into SupportGateway.doGet()");
		
		int hostLength = req.getRequestURL().indexOf("/", 8);
		String baseUrl = req.getRequestURL().toString();
		baseUrl = baseUrl.substring(0, hostLength + getServletContext().getContextPath().length());
		
		
		DocumentBuilder builder;
		
		try {			
			builder = builderFactory.newDocumentBuilder();
			Document supportDoc = builder.newDocument();
	
			Element topLevelElement = supportDoc.createElement("ninja");
			topLevelElement.setAttribute("version", "3.0");
			supportDoc.appendChild(topLevelElement);
			
			Element historyUrlElement = supportDoc.createElement("historyURL");
			historyUrlElement.setAttribute("href", baseUrl + historyResource );
			topLevelElement.appendChild(historyUrlElement);
			
			Element affilsUrlElement = supportDoc.createElement("affilsURL");
			affilsUrlElement.setAttribute("href", baseUrl + affilsResource );
			topLevelElement.appendChild(affilsUrlElement);			
						
			Element sellPagesUrlElement = supportDoc.createElement("sellPagesURL");
			sellPagesUrlElement.setAttribute("href", baseUrl + sellResource);
			topLevelElement.appendChild(sellPagesUrlElement);
						
			Element requestUrlElement = supportDoc.createElement("requestURL");
			requestUrlElement.setAttribute("href", req.getRequestURL().toString());
			topLevelElement.appendChild(requestUrlElement);		

			if (accessManager.checkAccess(req, accessLevel.MANAGER)){
				Element accountsUrlElement = supportDoc.createElement("accountsURL");
				accountsUrlElement.setAttribute("href", baseUrl + accountResource);
				topLevelElement.appendChild(accountsUrlElement);
			}
			
			topLevelElement.appendChild(pagecontrol.printerGroups(supportDoc));

			Element supportElement = supportDoc.createElement("supportTools");
			topLevelElement.appendChild(supportElement);
			Element creditOpElement;

			if (accessManager.checkAccess(req, accessLevel.OVERRIDE)){
				creditOpElement = supportDoc.createElement("overrideURL");
				creditOpElement.setAttribute("href", baseUrl + creditResource + "/override");
				creditOpElement.appendChild(supportDoc.createTextNode("Override"));
				topLevelElement.appendChild(creditOpElement);
			}
			
			if (accessManager.checkAccess(req, accessLevel.SALE)){
				Element saleElement = supportDoc.createElement("sellPages");
				saleElement.setAttribute("type", "printingDollars");
				saleElement.setAttribute("transaction", "flex");
				supportElement.appendChild(saleElement);
			}
			
			log.trace(supportDoc);
			Source domSource = new DOMSource(supportDoc);
		
			res.setContentType("text/html");
			PrintWriter out = res.getWriter();
			Result htmlOutput = new StreamResult(out);
			Transformer transformer = htmlConversion.newTransformer();
			transformer.transform(domSource, htmlOutput);
		
		
		} catch (ParserConfigurationException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} catch (TransformerConfigurationException e) {
			log.error(e);
		} catch (TransformerException e) {
			log.error(e);
		}
		
	}
	
}

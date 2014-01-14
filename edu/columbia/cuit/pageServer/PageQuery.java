package edu.columbia.cuit.pageServer;

import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;


public class PageQuery extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	private Pagecontrol pagecontrol;
	private BulletinBoard bulletinBoard;
	private TransformerFactory transformerFactory;
	


	public void init(){
		log.debug("Initializing PageQuery servlet");
		
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
		
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");
		bulletinBoard = (BulletinBoard) context.getAttribute("BulletinBoard");
		
		this.transformerFactory = TransformerFactory.newInstance();

		log.debug("Done with PageQuery init");
	}	
	
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
		log.trace("Entry into PageQuery.doGet()");
		
		int hostLength = req.getRequestURL().indexOf("/", 8);
		String baseUrl = req.getRequestURL().toString();
		baseUrl = baseUrl.substring(0, hostLength + getServletContext().getContextPath().length());
		
			
		
		int pagesRequested = 0;
		String printer = null;
		String username = null;
		String uni = null;
		String reqId = null;
		String clientRequest = req.getRequestURI();

		Pattern queryRegexp =  Pattern.compile(".*/([a-z0-9]+-ninja\\..+)/([a-z]{1,8}\\d{0,7})/([a-f0-9]{2,8})/(\\d+)$");
		Matcher queryCommand = queryRegexp.matcher(clientRequest);
		
		if (queryCommand.matches()){
			
			
			//TODO: printer name requires validation; currently generates a RunTime Exception 
			printer  = queryCommand.group(1);
			username = queryCommand.group(2);
			
			try {
				uni = pagecontrol.canonicalize(username);
			} catch (AffiliationException e) {
				uni = username;
			}
			reqId = queryCommand.group(3);
			pagesRequested = Integer.parseInt(queryCommand.group(4));

			if (log.isDebugEnabled()){
				StringBuffer sb = new StringBuffer("Query URL Request { ");
				sb.append("uni:");
				sb.append(uni);
				sb.append(" (");
				sb.append(username);
				sb.append(")  printer:");
				sb.append(printer);
				sb.append("  reqId:");
				sb.append(reqId);
				sb.append("  pages:");
				sb.append(pagesRequested);
				sb.append(" }");
				log.debug(sb);
			}

			DocumentBuilder builder;
			
	        try {
				builder = builderFactory.newDocumentBuilder();
				Document responseDoc = builder.newDocument();
				Element topLevelElement = responseDoc.createElement("ninja");
				topLevelElement.setAttribute("version", "3.0");
				responseDoc.appendChild(topLevelElement);	
				Element responseElement = responseDoc.createElement("queryResponse");
				responseElement.setAttribute("reqId", reqId);
				if (pagecontrol.query(printer,uni, pagesRequested)){
					responseElement.setAttribute("Authorized", "True");
					topLevelElement.appendChild(responseElement);
				}else{
					responseElement.setAttribute("Authorized", "False");
					DocumentFragment quota = pagecontrol.quota(responseDoc, uni);
					responseElement.appendChild(quota);
					topLevelElement.appendChild(responseElement);
				}
			
				if (bulletinBoard.hasBulletins()){
					Element bb = responseDoc.createElement("bulletinBoard");
					for (Bulletin bulletin: bulletinBoard){
						Element be = responseDoc.createElement("bulletin");
						be.setAttribute("id", bulletin.getMessageId());
						be.setAttribute("precedence", Integer.toString(bulletin.getPrecedence()));
						be.setAttribute("begins", Long.toString(bulletin.getBeginingEpoch()));
						be.setAttribute("ends", Long.toString(bulletin.getEndingEpoch()));
						if ( bulletin.hasImage() ){
							be.setAttribute("imageURL", bulletin.getImageURL());
						}
						be.setTextContent(bulletin.getMessage());
						bb.appendChild(be);
					}
					topLevelElement.appendChild(bb);
				}

				log.trace(responseDoc);
				Source domSource = new DOMSource(responseDoc);
			
				res.setContentType("text/html");
				PrintWriter out = res.getWriter();
				Result xmlOutput = new StreamResult(out);
				
				Transformer identityTransformer = transformerFactory.newTransformer();
				identityTransformer.transform(domSource, xmlOutput);
				
				
	        } catch (ParserConfigurationException e) {
	        	log.error(e);
	        } catch (TransformerConfigurationException e) {
	        	log.error(e);
	        } catch (TransformerException e) {
	        	log.error(e);
	        }
			
		}else{
			log.debug("The clientRequest did not match the queryRegexp, sent a BAD_REQUEST error");
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The requested URL was not of the correct format");
			return;
		}
		
		
		
			
		}
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{ doGet(req, res); }	
	
	
}
	


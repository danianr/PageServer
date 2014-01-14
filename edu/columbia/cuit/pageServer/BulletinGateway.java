package edu.columbia.cuit.pageServer;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class BulletinGateway extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	private BulletinBoard bulletinBoard;
	private AccessManager accessManager;
	private TransformerFactory transformerFactory;
	
	private static final Pattern messageIdPattern = Pattern.compile(".*/messageId/([^/\\?&]+)");

	
	
	public void init(){
		log.info("Initializing " + getServletName());
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
	
		this.transformerFactory = TransformerFactory.newInstance();
		
		bulletinBoard = (BulletinBoard) context.getAttribute("BulletinBoard");	
		accessManager = (AccessManager) context.getAttribute("AccessManager");
		
		log.info("Done Initializing " + getServletName());
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res){
		log.trace("entrance into BulletinGateway.doGet");
		
		DocumentBuilder builder;
		
        try {
			builder = builderFactory.newDocumentBuilder();
			Document responseDoc = builder.newDocument();
			Element topLevelElement = responseDoc.createElement("ninja");
			topLevelElement.setAttribute("version", "3.0");
			responseDoc.appendChild(topLevelElement);	
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

			log.trace(responseDoc);
			Source domSource = new DOMSource(responseDoc);
		
			res.setContentType("text/html");
			PrintWriter out = res.getWriter();
			Result xmlOutput = new StreamResult(out);
			
			Transformer identityTransformer = transformerFactory.newTransformer();
			identityTransformer.transform(domSource, xmlOutput);
        
        } catch (ParserConfigurationException e) {
        	log.error(e);
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse res){
		log.trace("entrance into BulletinGateway.doPost");
		
		try {
			res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Bulletins may not be edited, use delete/create");
		} catch (IOException e) {
			log.error("Unable to send SC_METHOD_NOT_ALLOWED from BulletinGateway.doPost");
		}
		
	}
	
	public void doPut(HttpServletRequest req, HttpServletResponse res){
		log.trace("entrance into BulletinGateway.doPut");
		
		DocumentBuilder builder;
		try {
			builder = builderFactory.newDocumentBuilder();
			builder.setErrorHandler(null);
			Document requestDoc = builder.parse(req.getInputStream());
			NodeList bulletinList = requestDoc.getElementsByTagName("bulletin");

			if (bulletinList.getLength() < 1){
				// Bail out before the for loop initializes to a non-existent item
				log.error("submitted XML lacks any bulletin elements");
				return;
			}
			
			int index=0;
			for (Node be = bulletinList.item(0); index < bulletinList.getLength(); index++ ){
				NamedNodeMap attr = be.getAttributes();
				int precedence = Integer.parseInt(attr.getNamedItem("precedence").getNodeValue());
				long begins = Long.parseLong(attr.getNamedItem("begins").getNodeValue());
				long ends = Long.parseLong(attr.getNamedItem("ends").getNodeValue());
				Node imageUrlNode = attr.getNamedItem("imageURL");
				String imageURL = null;
				String message = null;

				message = be.getTextContent();


				Calendar beginCal = Calendar.getInstance();
				Calendar endCal = Calendar.getInstance();
				beginCal.setTimeInMillis(begins);
				endCal.setTimeInMillis(ends);
				
				if (imageUrlNode != null && imageUrlNode.getNodeType() == Node.ATTRIBUTE_NODE){
					imageURL = imageUrlNode.getNodeValue();
				}

				Bulletin bulletin = new Bulletin(message, imageURL, beginCal, endCal);
				bulletin.setPrecedence(precedence);

				bulletinBoard.addBulletin(bulletin);
			}
			
			
		} catch (IOException e) {
			log.error(e);
		} catch (ParserConfigurationException e) {
			log.error(e);
		} catch (SAXException e) {
			log.error(e);
		}

	}
	
	public void doDelete(HttpServletRequest req, HttpServletResponse res){

		log.trace("entrance into BulletinGateway.doDelete");
		
		Matcher msgIdMatcher = messageIdPattern.matcher(req.getRequestURI());
		if (msgIdMatcher.matches()){
			String messageId = msgIdMatcher.group(1);
			bulletinBoard.removeBulletin(messageId);
		}
	}
	
	
}

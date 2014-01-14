package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

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
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.columbia.cuit.pageServer.AccessManager.accessLevel;

public class AccountManagement extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private ServletContext context;
	private String htmlConversionStylesheet;
	private AccessManager accessManager;
	private Pagecontrol pagecontrol;
	
	public void init(){
		log.debug("Initializing AccountManager servlet");

		ServletConfig config = getServletConfig();
		context = config.getServletContext();

		context.setAttribute("accountManagementContextPath", context.getContextPath());
		
		htmlConversionStylesheet = config.getInitParameter("htmlConversionStylesheet");	
		log.debug("Using hmtlConversionStylesheet = \"" + htmlConversionStylesheet + "\"");

		accessManager = (AccessManager) context.getAttribute("AccessManager");
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");

		log.debug("Done initializing AccountManager servlet");
	}

	
	public void doGet(HttpServletRequest req, HttpServletResponse res){

		res.setContentType("text/html");
		
		@SuppressWarnings("unchecked")
		Map<String, String[]> paramMap = req.getParameterMap();
		ArrayList<String> succeeded = new ArrayList<String>();
		ArrayList<String> failed = new ArrayList<String>();

		
		
		if (paramMap.containsKey("operation") && req.getParameter("operation").equals("refresh")){
			// Just reload the account role mappings from the database
			try {
				accessManager.refresh();
			} catch (SQLException e2) {
				log.error("Caught SQLException while trying to refresh accounts in AccessManager", e2);
				try {
					res.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Unable to refresh account listing");
				} catch (IOException e) {
					log.error("unable to send error response for SQLException in AccountManagement", e);
				}
			}
		}else if (paramMap.containsKey("operation")  && req.getParameter("operation").equals("remove")){

			// Remove operation
			String[] removeAccount = paramMap.get("username");
			
			// Yes Virgina, you can remove your own account
			for (int i=0; i < removeAccount.length; i++ ){
				log.debug("Removing account for " + removeAccount[i]);
				
				// Require ADMIN access to remove an ADMIN account
				if ( accessManager.checkAccess(removeAccount[i], accessLevel.ADMIN) && 
						accessManager.checkAccess(req,accessLevel.ADMIN) ){
					accessManager.removeUser(removeAccount[i]);
					String message = "Removed account for " + removeAccount[i];
					succeeded.add(message);
					log.info(message);
					
				}else if (accessManager.checkAccess(req, accessLevel.MANAGER)){
					//otherwise, you should have MANAGER access to do any removals
					accessManager.removeUser(removeAccount[i]);

					String message = "Removed account for " + removeAccount[i];
					succeeded.add(message);
					log.info(message);

				}else{
					String message = "Insufficent access to remove account: " + removeAccount[i];
					failed.add(message);
					log.error(message);
				}
			}
		}else if (paramMap.containsKey("operation") && req.getParameter("operation").equals("add") && paramMap.containsKey("role") ){

			String[] addAccount = paramMap.get("username");
			String role = req.getParameter("role");
			
			for (int i=0; i < addAccount.length; i++){
				log.debug("Adding account permissions for " + addAccount[i] + " to accessLevel." + role);
				String username;
				try {
					username = pagecontrol.canonicalize(addAccount[i]);
					accessLevel assignedAccess = accessLevel.valueOf(role);


					if ( ( (assignedAccess.compareTo(accessLevel.MANAGER) >= 0) ||
							(assignedAccess.compareTo(accessLevel.SALEONLY) <= 0) )
							&& accessManager.checkAccess(req, accessLevel.ADMIN) ){
		
						// Only ADMIN can add non-standard accounts 
						accessManager.addUser(username, assignedAccess);
						String message = "Added " + assignedAccess.toString() + " account for " + username;
						succeeded.add(message);
						log.info(message);
						
					}else if (accessManager.checkAccess(req, accessLevel.MANAGER)){
						// MANAGER is sufficient to add SALE, OVERRIDE, & CREDIT roles
						accessManager.addUser(username, assignedAccess);
						String message = "Added " + assignedAccess.toString() + " account for " + username;
						succeeded.add(message);
						log.info(message);
					
					}else{
						String message = "Insufficent access to add " +
								            assignedAccess.toString() + " account for " + username;
						log.error(message);
						failed.add(message);
					}
	
				} catch (AffiliationException e) {
					String message = "Unable to add account for " + addAccount[0];
					failed.add(message);
					log.error(message, e);
				}
				
			}
		}else if (paramMap.containsKey("operation") && req.getParameter("operation").equals("update") && paramMap.containsKey("role")){
			String[] updateAccount = paramMap.get("username");
			String role = req.getParameter("role");

			for (int i=0; i < updateAccount.length; i++){
				log.debug("Updating account permissions for " + updateAccount[i] + " to accessLevel." + role);
				String username;
				try {
					username = pagecontrol.canonicalize(updateAccount[i]);
					accessLevel assignedAccess = accessLevel.valueOf(role);

					if ( ( (assignedAccess.compareTo(accessLevel.MANAGER) >= 0) ||
							(assignedAccess.compareTo(accessLevel.SALEONLY) <= 0) )
							&& accessManager.checkAccess(req, accessLevel.ADMIN) ){
		
						// Only ADMIN can add non-standard accounts 
						accessManager.updateRole(username, assignedAccess);
						String message = "Updated to " + assignedAccess.toString() + " account for " + username;
						succeeded.add(message);
						log.info(message);
						
					}else if (accessManager.checkAccess(req, accessLevel.MANAGER)){
						// MANAGER is sufficient to add SALE, OVERRIDE, & CREDIT roles
						accessManager.addUser(username, assignedAccess);
						String message = "Updated to " + assignedAccess.toString() + " account for " + username;
						succeeded.add(message);
						log.info(message);
					
					}else{
						String message = "Insufficent access to update " +
								            assignedAccess.toString() + " account for " + username;
						log.error(message);
						failed.add(message);
					}
	
				} catch (AffiliationException e) {
					String message = "Unable to update account for " + updateAccount[i];
					failed.add(message);
					log.error(message, e);
				}
				
			}
		}

		
	
		try{
		
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			try {
				builder = builderFactory.newDocumentBuilder();
				Document accountDoc = builder.newDocument();

				Element topLevelElement = accountDoc.createElement("ninja");
				topLevelElement.setAttribute("version", "3.0");
				accountDoc.appendChild(topLevelElement);
				Element requestUrlElement = accountDoc.createElement("requestURL");
				requestUrlElement.setAttribute("value", req.getRequestURL().toString());
				topLevelElement.appendChild(requestUrlElement);

				Element management = accountDoc.createElement("accountManagement");
				topLevelElement.appendChild(management);
				
				management.appendChild(accessManager.accountInfo(accountDoc));
				
				if (!failed.isEmpty()){
					Element failureElement = accountDoc.createElement("failure");
					Iterator<String> failIterator = failed.iterator();
					while (failIterator.hasNext()){
						String failureMessage = failIterator.next();
						Element incident = accountDoc.createElement("accountFailure");
						incident.setTextContent(failureMessage);
						failureElement.appendChild(incident);
					}
					management.appendChild(failureElement);
				}
				
				if (!succeeded.isEmpty()){
					Element successElement = accountDoc.createElement("success");
					Iterator<String> successIterator = succeeded.iterator();
					while (successIterator.hasNext()){
						String successMessage = successIterator.next();
						Element incident = accountDoc.createElement("accountSuccess");
						incident.setTextContent(successMessage);
						successElement.appendChild(incident);
					}
					management.appendChild(successElement);
				}
				
		
				InputStream xsltBasic = context.getResourceAsStream(htmlConversionStylesheet);
				PrintWriter out = res.getWriter();

				Result xmlOutput = new StreamResult(out);
				Source domTree = new DOMSource(accountDoc);
				Source stylesheet = new StreamSource(xsltBasic);
	
		
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer(stylesheet);

				
				transformer.transform(domTree, xmlOutput);
				log.trace("Transformed accountInfo DocumentFragment into HTML");
		
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
			
		}catch (IOException e1){
			log.error(e1);
		}
		
		
	}

	
	
	
	
}

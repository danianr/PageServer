package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.StringTokenizer;
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


public class CybersourceReceipt extends HttpServlet {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final Logger deny = Logger.getLogger("edu.columbia.cuit.PageServer.deny");

	private static final String vendor = "www";
	private static final int dollarConvertValue = 100;

	private Pagecontrol pagecontrol;
	private DocumentBuilderFactory builderFactory;
	private Templates htmlConversion;
	
	private CybersourceDigest signer;
	
	private static String accessKey;
	private static String profileId;
	private static String transactionEncoding;
	
	public void init(){
		log.debug("Initializing " + getServletName());
		
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
		
		accessKey       = context.getInitParameter("accessKey");
		profileId		= context.getInitParameter("profileId");
		
		try {
			builderFactory = DocumentBuilderFactory.newInstance();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			String xsltLocation = config.getInitParameter("htmlConversionStylesheet");	
			InputStream xsltResource = context.getResourceAsStream(xsltLocation);
			Source htmlStylesheet = new StreamSource(xsltResource);
			htmlConversion = transformerFactory.newTemplates(htmlStylesheet);
			
		} catch (TransformerConfigurationException e) {
			log.error("Unable to compile the template for the PurchaseGateway", e);
		}
		
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");

		
		// Allow the user to specify a Character Encoding other than UTF8
		transactionEncoding = context.getInitParameter("transactionEncoding");
		if (transactionEncoding == null) transactionEncoding = Charset.defaultCharset().name();
		log.trace("Using characterSet \"" + transactionEncoding + "\" for transactionEncoding");
				
		
		try {
			signer = new CybersourceDigest(context.getInitParameter("sharedSecret"));
		}catch(UnsupportedEncodingException e){
			log.fatal("Unsupported characterSet for transactionEncoding");
		}catch(NullPointerException e){
			log.fatal("Caught a NullPointerException creating a signer, is sharedSecret set?");
		}
		
		log.info("Done with " + getServletName() + " init");
	}
	
	
	
    public boolean verifyTransactionSignature(HttpServletRequest req) {
    	String signedFieldNames = req.getParameter("signed_field_names");
		String signature        = req.getParameter("signature");    

		HashMap<String, String> param = new HashMap<String, String>(22);
		StringTokenizer tokenizer = new StringTokenizer(signedFieldNames, ",", false);

		while (tokenizer.hasMoreTokens()) {
			String key = tokenizer.nextToken();
			param.put(key, req.getParameter(key));
		}
		param.put("signature", signature);
		
		if (log.isDebugEnabled()){
			log.debug("[signed_field_names]=" + signedFieldNames);
			log.debug("[signature]=" + signature);
		}
			
		if (signature == null || signedFieldNames == null) return false;
	
		try {
			String messageContents = signer.composeFields(signedFieldNames, param);
			log.trace(messageContents);
			String signedFieldsDigest = signer.getDigest(messageContents);
			log.debug("[signedFieldsDigest]=" + signedFieldsDigest);
			return signature.equals(signedFieldsDigest);
		} catch (InvalidKeyException e) {
			log.error(e);
			return false;
		} catch (NoSuchAlgorithmException e) {
			log.error(e);
			return false;
		} catch (UnsupportedEncodingException e) {
			log.error(e);
			return false;
		} catch (IllegalStateException e) {
			log.error(e);
			return false;
		}
    }
	
	
	public void doGet(HttpServletRequest req, HttpServletResponse res){
		// GET operation not supported, log any access attempts
		log("GET purchase_recipt attempted by " + req.getRemoteAddr());
		try {
			res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		} catch (IOException e) {
			log.error("IOException while sending SC_METHOD_NOT_ALLOWED error", e);
		}		
	}

	

	public void doPost(HttpServletRequest req, HttpServletResponse res){
		Document presentationDOM;
		
		log.debug(req);
		
		// Require a valid transaction to proceed
		if (!verifyTransactionSignature(req)){

			log.error("Bad signature on incoming merchant POST");
			log.error(req);
			try {
				res.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Bad signature");
			} catch (IOException e) {
				log.warn("Caught IOException while trying to respond to badly signed merchant POST", e);
			}
			// make sure execution stops here, even though we've sent the error response
			return;
		}

		String uni = req.getParameter("req_merchant_defined_data1");
		String invoiceNumber = req.getParameter("req_reference_number");
		String amount = req.getParameter("req_amount");
		String decision = req.getParameter("decision");
		String transactionType = req.getParameter("req_transaction_type");
		String responseCode = req.getParameter("reason_code");
		String transactionNumber = req.getParameter("bill_trans_ref_no");
		String message = req.getParameter("message");
		
		
		float printingDollars = Float.parseFloat(amount) * dollarConvertValue;
		
		if ( uni==null || invoiceNumber==null || amount==null ||
				decision==null || transactionType==null || responseCode==null){
			log.error("One of the requried fields was null");
			if (!log.isDebugEnabled()) log.error(req);
		}
		
		// Quick check to make sure the merchantID and serialNumber agree with the submitted versions
		if (!profileId.equals(req.getParameter("req_profile_id")) ||
				!accessKey.equals(req.getParameter("req_access_key"))){
			log.warn("There seems to be a mis-match between the expected profile_id and access_key, and the POST");
			if (!log.isDebugEnabled()) log.warn(req);
		}		
			
		String username;
		try {
			username = pagecontrol.canonicalize(uni);
		} catch (AffiliationException e1) {
			log.error("Caught affiliationException while trying to process Merchant POST using supplied username", e1);
			username = uni;
		}
		
		if (decision.equals("ACCEPT") && transactionType.equals("sale")){
			pagecontrol.sale(invoiceNumber, username, vendor, (int) printingDollars);
		}else{
			pagecontrol.sale(invoiceNumber, username, vendor, 0);
			StringBuffer denyMessage = new StringBuffer();
			denyMessage.append("Invoice:");
			denyMessage.append(invoiceNumber);
			denyMessage.append(" decision:");
			denyMessage.append(decision);
			denyMessage.append(" username:");
			denyMessage.append(username);
			denyMessage.append(" transactionType:");
			denyMessage.append(transactionType);
			denyMessage.append(" amount:");
			denyMessage.append(amount);
			deny.info(denyMessage.toString());
		}
		
				
		try{			
			DocumentBuilder builder;
			builder = builderFactory.newDocumentBuilder();
			presentationDOM = builder.newDocument();
			log.debug("Created the new Document");
			
			Element parentNode = presentationDOM.createElement("ninja");
			parentNode.setAttribute("version", "3.0");
			presentationDOM.appendChild(parentNode);
			log.debug("Created the top-level element");

			
			Element appNode = presentationDOM.createElement("purchaseReceipt");
			appNode.setAttribute("customerId", uni);
			appNode.setAttribute("invoiceNumber", invoiceNumber);
			appNode.setAttribute("transId", transactionNumber);
			appNode.setAttribute("responseCode", responseCode);
			appNode.setAttribute("reasonText", message);
			parentNode.appendChild(appNode);

			// The continue URL should be the root URL of this context
			ServletContext context = getServletContext();
			String continueURL = req.getRequestURL().toString();
			log.trace(continueURL);
			continueURL = continueURL.substring(0, continueURL.lastIndexOf(context.getContextPath()))
					+ context.getContextPath();
			log.trace(continueURL);
		
			Element continueNode = presentationDOM.createElement("continueURL");
			continueNode.setAttribute("href", continueURL);
			appNode.appendChild(continueNode);
	
			Source domSource = new DOMSource(presentationDOM);
		
			log.debug("created new Source from the Document Tree");
			log.trace(presentationDOM);
			
			res.setContentType("text/html");
			PrintWriter out = res.getWriter();
			Result htmlOutput = new StreamResult(out);
			log.debug("got the PrintWriter for the response");
			Transformer transformer = htmlConversion.newTransformer();
			log.debug("created the new Transformer from the template");
			transformer.transform(domSource, htmlOutput);
			log.debug("Transformed the output into HTML");

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

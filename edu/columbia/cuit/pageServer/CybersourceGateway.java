package edu.columbia.cuit.pageServer;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;


import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class CybersourceGateway extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	
	private static String orderPageURI;
	private static String testOrderPageURI;

	private boolean testMode;
	private boolean simpleSigning;
	private String quantities;
	private SecretKey secretKey;
	private String transactionEncoding;
	private Pagecontrol pagecontrol;
	private DocumentBuilderFactory builderFactory;
	private Templates htmlConversion;
	
	private static String merchantID;
	private static String sharedSecret;
	private static String serialNumber;
	private static final String macAlgorithm = "HmacSha1";
	private static final String orderPageVersion = "7";
	private static final String currency = "usd";

	
	public void init(){
		log.debug("Initializing " + getServletName());
		
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
		
		quantities  = config.getInitParameter("purchaseQuantities");
		
		orderPageURI     = config.getInitParameter("orderPageURI");
		testOrderPageURI = config.getInitParameter("testOrderPageURI");
		merchantID       = context.getInitParameter("merchantID");
		sharedSecret     = context.getInitParameter("sharedSecret");
		serialNumber     = context.getInitParameter("serialNumber");
		
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

		String testModeSwitch = config.getInitParameter("testMode");
		if ( (testModeSwitch != null ) && testModeSwitch.equalsIgnoreCase("true")) testMode = true;

		String simpleSigningSwitch = config.getInitParameter("simpleSigning");
		if ( (simpleSigningSwitch != null ) && simpleSigningSwitch.equalsIgnoreCase("true")) simpleSigning = true;

		
		// Allow the user to specify a Character Encoding other than UTF8
		transactionEncoding = context.getInitParameter("transactionEncoding");
		if (transactionEncoding == null) transactionEncoding = Charset.defaultCharset().name();
		log.trace("Using characterSet \"" + transactionEncoding + "\" for transactionEncoding");
		
		
		// Setup the SecretKey used for the message digesting
		sharedSecret = context.getInitParameter("sharedSecret");
		log.trace("sharedSecret = \"" + sharedSecret + "\"");		
		log.trace("macAlgorithm = \"" + macAlgorithm + "\"");
			
		try {
			secretKey = new SecretKeySpec(sharedSecret.getBytes(transactionEncoding), macAlgorithm);
		} catch (UnsupportedEncodingException e) {
			log.error("Unsupported characterSet for transactionEncoding, using default");
			secretKey = new SecretKeySpec(sharedSecret.getBytes(), macAlgorithm);
			transactionEncoding = Charset.defaultCharset().name();
		}
		
		if (secretKey == null) log.error("Unable to initialize SecretKeySpec, check macAlgorithm");
		
		
		log.info("Done with " + getServletName() + " init");
	}
	
	
	public String getPublicDigest(String message) throws InvalidKeyException,NoSuchAlgorithmException,
	                                                UnsupportedEncodingException, IllegalStateException{

		// Constructor options for better agreement with the internal Sun Base64Encoder
		final byte[] lineSep = { '\n' };
		Base64 commonsCodec = new Base64(76, lineSep, false);
		    
		// MAC objects are not thread-safe, so create a new one each time
		Mac mac = Mac.getInstance(macAlgorithm);
		mac.init(secretKey);		    
		byte[] digestedBytes;
		try {
			digestedBytes = mac.doFinal(message.getBytes(transactionEncoding));
		} catch (IllegalStateException e) {
			log.warn(e);
			// try to reset the MAC, otherwise let the error bubble up
			mac.reset();
			digestedBytes = mac.doFinal(message.getBytes(transactionEncoding));
		}
		        
		// Encode as BASE64 and remove all newlines
		String publicDigest = commonsCodec.encodeToString(digestedBytes);
		return publicDigest.replaceAll("\n", "");
	}
	
	  
	
	private DocumentFragment generateFormFields(Document parentDoc, HttpServletRequest req){		
		Map<String, String> formData = new LinkedHashMap<String,String>(6);
		int size;
		try {
			size = Integer.valueOf(req.getParameter("size"));
		}catch (NumberFormatException e){
			log.error("Unable to parse size param for value, using 0");
			size = 0;
		}

		
		DocumentFragment formParent = parentDoc.createDocumentFragment();
		
		try {
		NumberFormat decimalFormat = NumberFormat.getInstance();
		decimalFormat.setMinimumFractionDigits(2);
		decimalFormat.setMaximumFractionDigits(2);
		
		String amount = decimalFormat.format(size);	
		String time = String.valueOf(System.currentTimeMillis());
		String transactionType = "sale";
		
		
		formData.put("amount", amount);
		formData.put("orderPage_transactionType", transactionType);
		formData.put("currency", currency);
		formData.put("orderPage_timestamp", time);
		formData.put("merchantID", merchantID);
		formData.put("orderPage_version", orderPageVersion);
		formData.put("orderPage_serialNumber", serialNumber);

		String orderId = pagecontrol.nextSaleId();
		formData.put("orderNumber", orderId);
		
		String uni = req.getRemoteUser();
		formData.put("merchantDefinedData1", uni);
				
		for (String field: formData.keySet()){
		
			Element fieldNode = parentDoc.createElement("field");
			fieldNode.setAttribute("name", field);
			fieldNode.setAttribute("value", formData.get(field));
			formParent.appendChild(fieldNode);
		}
		
		if (simpleSigning){
			simpleSignature(parentDoc, formParent, formData);
		}else{
			signAllFields(parentDoc, formParent, formData);
		}
		
		} catch (InvalidKeyException e) {
			log.error(e);
		} catch (NoSuchAlgorithmException e) {
			log.error(e);
		} catch (UnsupportedEncodingException e) {
			log.error(e);
		} catch (IllegalStateException e) {
			log.error(e);
		}
		
		return formParent;
		}
	

	
	public void simpleSignature(Document parentDoc, Node formParent, Map<String, String> formData) throws
					InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, IllegalStateException{

		// This method computes only the most basic of public signatures for the Cybersource
		// Hosted Order Page POST operation
		
		// Note that when the orderPage_signedFields variable is not supplied, the
		// data for the Message Authentication Digest does not contain field names, equals,
		// or separating commas; retrieve the previously set values from the formData hash
		String data = merchantID + formData.get("amount") + formData.get("currency") +
				formData.get("orderPage_timestamp");
		
		String pubDigest = getPublicDigest(data);
		
		Element sigElement = parentDoc.createElement("field");
		sigElement.setAttribute("name", "orderPage_signaturePublic");
		sigElement.setAttribute("value", pubDigest);
		formParent.appendChild(sigElement);
	}
	
	
	
	public void signAllFields(Document parentDoc, Node formParent, Map<String, String> formData) throws
					InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, IllegalStateException{	
		StringBuffer dataBuffer   = new StringBuffer(1024);
		StringBuffer fieldsBuffer = new StringBuffer(256);
		
		for (String field: formData.keySet()){
			dataBuffer.append(field);
			dataBuffer.append("=");
			dataBuffer.append(formData.get(field));
			dataBuffer.append(",");
			
			fieldsBuffer.append(field);
			fieldsBuffer.append(",");
		}

		// remove the trailing comma from the signed fields list
		fieldsBuffer.deleteCharAt(fieldsBuffer.length() - 1);

		String signedFields = fieldsBuffer.toString().trim();
		dataBuffer.append("signedFieldsPublicSignature=");
		String fieldsSignature = getPublicDigest(signedFields);
		dataBuffer.append(fieldsSignature);

		String data = dataBuffer.toString().trim();		
		String pubSignature = getPublicDigest(data);
						
		Element pubSignatureNode = parentDoc.createElement("field");
		pubSignatureNode.setAttribute("name", "orderPage_signaturePublic");
		pubSignatureNode.setAttribute("value", pubSignature);
		formParent.appendChild(pubSignatureNode);

		Element signedFieldsNode = parentDoc.createElement("field");
		signedFieldsNode.setAttribute("name", "orderPage_signedFields");
		signedFieldsNode.setAttribute("value", signedFields);
		formParent.appendChild(signedFieldsNode);	
	}
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse res){
		
		log.trace("Entrance into CybersourceGateway.doPost()");
		Document presentationDOM;
		
		DocumentBuilder builder;
		try{
			builder = builderFactory.newDocumentBuilder();
			presentationDOM = builder.newDocument();
			
			Element parentNode = presentationDOM.createElement("ninja");
			parentNode.setAttribute("version", "3.0");
			presentationDOM.appendChild(parentNode);
			Element appNode = presentationDOM.createElement("confirmPurchase");
			parentNode.appendChild(appNode);
			
			Element targetURL = presentationDOM.createElement("targetURL");
			Element logoutURL = presentationDOM.createElement("logoutURL");
			
			targetURL.setAttribute("href", (testMode)? testOrderPageURI : orderPageURI);
			logoutURL.setAttribute("href", req.getRequestURL().toString() + "?logout=true");
			appNode.appendChild(targetURL);
			appNode.appendChild(logoutURL);
			
			Element formfields = presentationDOM.createElement("formfields");

			//formfields.appendChild(generateFormFields(presentationDOM, req));
			formfields.appendChild(generateFormFields(presentationDOM, req));
			appNode.appendChild(formfields);
			
			log.trace(presentationDOM);
			Source domSource = new DOMSource(presentationDOM);
		
			try {
				res.setContentType("text/html");
				PrintWriter out = res.getWriter();
				Result htmlOutput = new StreamResult(out);
				Transformer transformer = htmlConversion.newTransformer();
				log.debug("created the new Transformer from the template");
				transformer.transform(domSource, htmlOutput);
				log.debug("Transformed the output into HTML");
			
			} catch (TransformerConfigurationException e) {
				log.error(e);
			} catch (TransformerException e) {
				log.error(e);
			} catch (IOException e) {
				log.error(e);
			}

		}catch(ParserConfigurationException e){
			log.error(e);
		}
		
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res){

		log.trace("Entrance into CybersourceGateway.doGet()");
		String uni = req.getRemoteUser();
		Document presentationDOM;
		
		DocumentBuilder builder;
		try {
			builder = builderFactory.newDocumentBuilder();
			presentationDOM = builder.newDocument();
			
			Element parentNode = presentationDOM.createElement("ninja");
			parentNode.setAttribute("version", "3.0");
			presentationDOM.appendChild(parentNode);

			Element appNode = presentationDOM.createElement("purchaseGateway");
			parentNode.appendChild(appNode);
			
			Element targetURL = presentationDOM.createElement("targetURL");
			Element logoutURL = presentationDOM.createElement("logoutURL");
			
			
			//TODO: define URL with which to access the HostedOrderPageForwarder
			targetURL.setAttribute("href", req.getRequestURL().toString());
			logoutURL.setAttribute("href", req.getRequestURL().toString() + "?logout=true");
			appNode.appendChild(targetURL);
			appNode.appendChild(logoutURL);
			
								
			for (String quantity: quantities.split("\\s+")){
				Element dollarQuantity = presentationDOM.createElement("dollarQuantity");
				dollarQuantity.setAttribute("size", quantity);
				appNode.appendChild(dollarQuantity);
			}
			
			//TODO: NullPointerException within the next call 
			appNode.appendChild(pagecontrol.quota(presentationDOM, uni));

			NodeList errorList = appNode.getElementsByTagName("error");
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
				Transformer transformer = htmlConversion.newTransformer();
				log.debug("created the new Transformer from the template");
				transformer.transform(domSource, htmlOutput);
				log.debug("Transformed the output into HTML");
				
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

		
		
	}
	
	
	
	
}

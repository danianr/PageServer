package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

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

	private SecretKey secretKey;
	private String transactionEncoding;
	private Pagecontrol pagecontrol;
	private DocumentBuilderFactory builderFactory;
	private Templates htmlConversion;
	
	private static String merchantID;
	private static String sharedSecret;
	private static String serialNumber;
	private static final String macAlgorithm = "HmacSha1";

	
	public void init(){
		log.debug("Initializing " + getServletName());
		
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
		
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
	
	
	// Identical to the function in CybersourceGateway (has to be)
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

	
	
    public boolean verifyTransactionSignature(HttpServletRequest req) {
  		    
		String transactionSignature = req.getParameter("signedDataPublicSignature");
		if (transactionSignature == null) return false;
		    
		String transactionSignatureFields = req.getParameter("signedFields");
		if (transactionSignatureFields == null) return false;
		
		StringTokenizer tokenizer = new StringTokenizer(transactionSignatureFields, ",", false);
		
		StringBuffer data = new StringBuffer();
		while (tokenizer.hasMoreTokens()) {
		    String key = tokenizer.nextToken();
		    data.append(key);
		    data.append('=');
		    data.append(req.getParameter(key));
		    data.append(',');
		}
		data.append("signedFieldsPublicSignature=");
		try {
			String signedFieldsDigest = getPublicDigest(transactionSignatureFields.trim());
			log.debug("[signedFieldsDigest]=" + signedFieldsDigest);
			data.append(signedFieldsDigest);
			String computedDigest = getPublicDigest(data.toString());
			log.debug("[computedDigest]="+ computedDigest);
			return transactionSignature.equals(computedDigest);

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

		String uni = req.getParameter("merchantDefinedData1");
		String invoiceNumber = req.getParameter("orderNumber");
		String amount = req.getParameter("orderAmount");
		String decision = req.getParameter("decision");
		String transactionType = req.getParameter("orderPage_transactionType");
		String responseCode = req.getParameter("reasonCode");
		
		float printingDollars = Float.parseFloat(amount) * dollarConvertValue;
		
		if ( uni==null || invoiceNumber==null || amount==null ||
				decision==null || transactionType==null || responseCode==null){
			log.error("One of the requried fields was null");
			if (!log.isDebugEnabled()) log.error(req);
		}
		
		// Quick check to make sure the merchantID and serialNumber agree with the submitted versions
		if (!merchantID.equals(req.getParameter("merchantID")) ||
				!serialNumber.equals(req.getParameter("orderPage_serialNumber"))){
			log.warn("There seems to be a mis-match between the expected merchantID and serialNumber, and the POST");
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
			appNode.setAttribute("responseCode", responseCode);
			//appNode.setAttribute("reasonText", "foo");
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

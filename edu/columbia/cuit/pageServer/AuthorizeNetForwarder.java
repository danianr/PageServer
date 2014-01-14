package edu.columbia.cuit.pageServer;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;


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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class AuthorizeNetForwarder extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final String KEYNAME = "key";
	private static final String KEYVALUE = "true";
	
	private String authorizeNetUri;
	private boolean purchaseEnabled;
	private boolean testMode;
	private String quantities;
	private static final int dollarConvertValue = 100;
	private SecretKey secretKey;
	private String transactionEncoding;
	private String merchantLoginId;
	private String vendor;
	private String receiptResourcePath;
	private Pagecontrol pagecontrol;
	private DocumentBuilderFactory builderFactory;
	private Templates htmlConversion;

	
	public void init(){
		log.debug("Initializing " + getServletName());
		
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();

		purchaseEnabled = true;
		testMode = false;
		
		String testModeSwitch = config.getInitParameter("testMode");
		if ( (testModeSwitch != null ) && testModeSwitch.equalsIgnoreCase("true")) testMode = true;
		log.trace("testMode = " + Boolean.toString(testMode));
		
		quantities = config.getInitParameter("purchaseQuantities");
		authorizeNetUri = config.getInitParameter("authorizeNetUri");
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
		
		receiptResourcePath = context.getInitParameter("receiptResource");
	
		
		// Allow the user to specify a Character Encoding other than UTF8
		transactionEncoding = context.getInitParameter("transactionEncoding");
		if (transactionEncoding == null) transactionEncoding = Charset.defaultCharset().name();
		log.trace("Using characterSet \"" + transactionEncoding + "\" for transactionEncoding");
		
		
		// Setup the SecretKey used for the message digesting
		String transactionKey = context.getInitParameter("transactionKey");
		log.trace("transactionKey = \"" + transactionKey + "\"");
		
		String macAlgorithm = context.getInitParameter("macAlgorithm");
		log.trace("macAlgorithm = \"" + macAlgorithm + "\"");
			
		try {
			secretKey = new SecretKeySpec(transactionKey.getBytes(transactionEncoding), macAlgorithm);
		} catch (UnsupportedEncodingException e) {
			log.error("Unsupported characterSet for transactionEncoding, using default");
			secretKey = new SecretKeySpec(transactionKey.getBytes(), macAlgorithm);
			transactionEncoding = Charset.defaultCharset().name();
		}
		
		if (secretKey == null) log.error("Unable to initialize SecretKeySpec, check macAlgorithm");

		merchantLoginId = config.getInitParameter("MerchantLoginId");
		
		
		log.info("Done with PurchaseGateway init");
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res){

		log.trace("Entrance into AuthorizeNetForwarder.doGet()");
		String uni = req.getRemoteUser();

		//shortcut for marshalling data
		String keyValue = req.getParameter(KEYNAME);
		if ( (keyValue != null) && keyValue.equals(KEYVALUE) ){
			marshallFormData(req, res);
		}else{
			try {
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "incorrect " + KEYNAME + " parameter supplied");
			} catch (IOException e) {
				log.error(e);
				log.error("while trying to send a SC_BAD_REQUEST for bad " + KEYNAME + " parameter");
			}
		}
	}
	
	public String digest(StringBuffer sb){
       	Mac mac;
    	try {
    		mac = Mac.getInstance(secretKey.getAlgorithm());
    		mac.init(secretKey);

    		StringBuffer digest = new StringBuffer();
    		byte[] encodedMessage = sb.toString().getBytes(transactionEncoding);    		
    		
    		for (byte i: mac.doFinal(encodedMessage)){
    			String hex = Integer.toHexString(i & 0xff);
    			if ( hex.length() == 1) digest.append('0');  //zero-pad single hex digits
    			digest.append(hex);
    		}
    		
    		return digest.toString();
    	
    	} catch (InvalidKeyException e) {
    		log.error("Unable to compute Hash of " + sb.toString(), e);
    	} catch (NoSuchAlgorithmException e) {
    		log.error("Unable to compute Hash of " + sb.toString(), e);
        } catch (UnsupportedEncodingException e) {
    		log.error("Unable to compute Hash of " + sb.toString(), e);
		}
        
		return "";
	}
	
	public void marshallFormData(HttpServletRequest req, HttpServletResponse res){

		// This function takes care of assembling the URI for the authorize.net
		// purchase and redirects the client to the purchase page

		String uni = req.getRemoteUser();

		NumberFormat decimalFormat = NumberFormat.getInstance();
		decimalFormat.setMinimumFractionDigits(2);
		decimalFormat.setMaximumFractionDigits(2);

		String key = req.getParameter(KEYNAME);
		String size = req.getParameter("size");
		long sizeLong = Long.parseLong(size);

		String cost = decimalFormat.format(sizeLong);

		String orderId = pagecontrol.nextSaleId();
		String timestamp = Long.toString( System.currentTimeMillis() / 1000 );

		// merchantLoginId^sequence^timestamp^cost^
		StringBuffer sb = new StringBuffer();
		sb.append(merchantLoginId);
		sb.append('^');
		sb.append(orderId);
		sb.append('^');
		sb.append(timestamp);
		sb.append('^');
		sb.append(cost);
		sb.append('^');

		String hash = digest(sb);

		// use new objects to clear the old buffer
		sb = new StringBuffer();

		//uni^order_id^size^key
		sb.append(uni);
		sb.append('^');
		sb.append(orderId);
		sb.append('^');
		sb.append(size);
		sb.append('^');
		sb.append(key);
		String hash2 = digest(sb);	


		try {
			ServletContext context = getServletContext();
			
			// Figure out where we are going to redirect to
			String requestURL = req.getRequestURL().toString();
			log.trace(requestURL);
			String responseUrl = requestURL.substring(0, requestURL.lastIndexOf(context.getContextPath()))
						+ context.getContextPath() + receiptResourcePath;
			log.trace(responseUrl);
			
			StringBuffer redirect = new StringBuffer(400);
			redirect.append(authorizeNetUri);
			redirect.append('?');
			redirect.append("x_fp_sequence=");
			redirect.append(orderId);
			redirect.append("&x_fp_timestamp=");
			redirect.append(timestamp);
			redirect.append("&x_fp_hash=");
			redirect.append(URLEncoder.encode(hash, transactionEncoding));
			redirect.append("&x_invoice_num=");
			redirect.append(orderId);
			redirect.append("&x_description=");
			redirect.append(URLEncoder.encode(size +" dollars for user " + uni, transactionEncoding));
			redirect.append("&x_login=");
			redirect.append(URLEncoder.encode(merchantLoginId, transactionEncoding));
			redirect.append("&x_amount=");
			redirect.append(cost);
			redirect.append("&x_show_form=PAYMENT_FORM");
			redirect.append("&x_email=");
			redirect.append(URLEncoder.encode(uni+"@columbia.edu", transactionEncoding));
			redirect.append("&x_relay_response=TRUE");
			redirect.append("&x_relay_url=");
			redirect.append(URLEncoder.encode(responseUrl, transactionEncoding));
			redirect.append("&x_test_request=");
			redirect.append(Boolean.toString(testMode));
			redirect.append("&x_cust_id=");
			redirect.append(URLEncoder.encode(uni, transactionEncoding));
			redirect.append("&size=");
			redirect.append(URLEncoder.encode(size, transactionEncoding));
			redirect.append('&');
			redirect.append(KEYNAME);
			redirect.append('=');
			redirect.append(URLEncoder.encode(key,transactionEncoding));
			redirect.append("&fp_hash=");
			redirect.append(URLEncoder.encode(hash2, transactionEncoding));
			redirect.append("&invoice_num=");
			redirect.append(orderId);
			redirect.append("&cust_id=");
			redirect.append(URLEncoder.encode(uni, transactionEncoding));

			res.sendRedirect(redirect.toString()); 

		} catch (UnsupportedEncodingException e) {
			log.error("Unsupported encoding for redirect String", e);
			try{
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process your purchase at this time");
			} catch (IOException e1) {
				log.error("Unable to send server error message", e1);
			}
		} catch (IOException e) {
			log.error("Unable to redirect client to Payment processor", e);
			try {
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process your purchase at this time");
			} catch (IOException e1) {
				log.error("Unable to send server error message", e1);
			}

		}



	}
	
	
	
}

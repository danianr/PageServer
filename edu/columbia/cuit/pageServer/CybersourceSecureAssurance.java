package edu.columbia.cuit.pageServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;


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


public class CybersourceSecureAssurance extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
		
	private static String standardEndpointURI;

	private Pagecontrol pagecontrol;
	private LDAPUserinfo userinfo;
	private DocumentBuilderFactory builderFactory;
	private Templates htmlConversion;

	private static CybersourceDigest signer;
	
	private static String accessKey;
	private static String profileId;
	private static String transactionEncoding;
	private static String quantities;

	private static final String currency = "usd";
	private static final String paymentMethod = "card";

	
	// Order is important, so keep as a static String and split on the comma to iterate
	private static final String signedFields =
			"access_key,profile_id,transaction_uuid,signed_field_names,unsigned_field_names,signed_date_time,locale,transaction_type,reference_number,amount,currency,payment_method,bill_to_forename,bill_to_surname,bill_to_email,bill_to_phone,bill_to_address_line1,bill_to_address_city,bill_to_address_state,bill_to_address_country,bill_to_address_postal_code,merchant_defined_data1";
	private static final String unsignedFields = "card_type,card_number,card_expiry_date";

    private final HashMap<String, String> usStates = new HashMap<String, String>(61, 0.2f){
		private static final long serialVersionUID = 1L;
		{
    	  put("arizona", "AZ");
      	  put("alabama", "AL");
      	  put("alaska", "AK");
      	  put("arkansas", "AR");
      	  put("california", "CA");
      	  put("north carolina", "NC");
      	  put("south carolina", "SC");
      	  put("colorado", "CO");
      	  put("connecticut", "CT");
      	  put("north dakota", "ND");
      	  put("south dakota", "SD");
      	  put("delaware", "DE");
      	  put("district of columbia", "DC");
      	  put("washington dc", "DC");
      	  put("florida", "FL");
      	  put("georgia", "GA");
      	  put("hawaii", "HI");
      	  put("illinois", "IL");
      	  put("indiana", "IN");
      	  put("idaho", "ID");
      	  put("iowa","IA");
      	  put("kansas", "KS");
      	  put("kentucky", "KY");
      	  put("louisianna", "LA");
      	  put("maine", "ME");
      	  put("maryland", "MD");
      	  put("massachusetts", "MA");
      	  put("michigan", "MI");
      	  put("minnesota", "MN");
      	  put("montana", "MT");
      	  put("missisippi", "MS");
      	  put("missouri", "MO");
      	  put("nebraska", "NE");
      	  put("new hampshire", "NH");
      	  put("new jersey", "NJ");
      	  put("new mexico", "NM");
      	  put("new york", "NY");
      	  put("nevada", "NV");
      	  put("ohio", "OH");
      	  put("oklahoma", "OK");
      	  put("oregon", "OR");
      	  put("pennsylvania", "PA");
      	  put("rhode island", "RI");
      	  put("tennessee", "TN");
      	  put("texas", "TX");
      	  put("washington", "WA");
      	  put("wisconsin", "WI");
      	  put("wyoming", "WY");
      	  put("utah", "UT");
      	  put("vermont", "VT");
      	  put("virginia", "VA");
      	  put("west virginia", "WV");
      	  put("puerto rico", "PR");
      	  put("american samoa", "AS");
      	  put("samoa", "AS");
      	  put("guam", "GU");
      	  put("virgin islands", "VI");
      	  put("marshall islands", "MH");
      	  put("northern mariana islands", "MP");
      	  put("federated states of micronesia", "FM");
      	  put("palau", "PW");
    	}
      };
      
    
      
      private final HashMap<String, String> caProvinces = new HashMap<String, String>(){
  		private static final long serialVersionUID = 1L;
  		{
  		   put("alberta", "AB");
  		   put("british columbia", "BC");
  		   put("columbie-britannique", "BC");
  		   put("manitoba", "MB");
  		   put("new brunswick", "NB");
  		   put("nouveau-brunswick", "NB");
  		   put("newfoundland and labrador", "NL");
  		   put("terre-neuve-et-labrador", "NL");
  		   put("newfoundland", "NL");
  		   put("terre-neuve", "NL");
  		   put("labrador", "NL");
  		   put("northwest territorites", "NT");
  		   put("territoires du nord-ouest", "NT");
  		   put("nova scotia", "NS");
  		   put("nouvelle-Écosse", "NS");
  		   put("nouvelle-écosse", "NS");
  		   put("nouvelle-ecosse", "NS");
  		   put("nunavut", "NU");
  		   put("ontario", "ON");
  		   put("prince edward island",  "PE");
  		   put("Île-du-prince-Édouard", "PE");
  		   put("île-du-prince-édouard", "PE");
  		   put("ile-du-prince-edouard", "PE");
  		   put("quebec", "QC");
  		   put("quebéc", "QC");  		   
  		   put("saskatchewan", "SK");
  		   put("yukon", "YT");
  		}
  	};

  	
  	
  	private final HashSet<String> isoCountryCodes = new HashSet<String>(186, 0.1f){
  		private static final long serialVersionUID = 1L;
  		{ add("AF"); add("AX"); add("AL"); add("DZ");
  		  add("AS"); add("AD"); add("AO"); add("AI");
  		  add("AQ"); add("AG"); add("AR"); add("AM");
  		  add("AW"); add("AU"); add("AT"); add("AZ");
  		  add("BS"); add("BH"); add("BD"); add("BB");
  		  add("BY"); add("BE"); add("BZ"); add("BJ");
  		  add("BM"); add("BT"); add("BO"); add("BQ");
  		  add("BA"); add("BW"); add("BV"); add("BR");
  		  add("IO"); add("VG"); add("BN"); add("BG");
  		  add("BF"); add("BI"); add("KH"); add("CM");
  		  add("CA"); add("CV"); add("KY"); add("CF");
  		  add("TD"); add("CL"); add("CN"); add("CX");
  		  add("CC"); add("CO"); add("KM"); add("CD");
  		  add("CG"); add("CK"); add("CR"); add("CI");
  		  add("HR"); add("CU"); add("CW"); add("CY");
  		  add("CZ"); add("DK"); add("DM"); add("DO");
  		  add("EC"); add("EG"); add("SV"); add("GQ");
  		  add("ER"); add("EE"); add("ET"); add("FK");
  		  add("FO"); add("FJ"); add("FI"); add("FR");
  		  add("GF"); add("PF"); add("TF"); add("GA");
  		  add("GM"); add("GE"); add("DE"); add("GH");
  		  add("GI"); add("GR"); add("GL"); add("GD");
  		  add("GP"); add("GU"); add("GT"); add("GG");
  		  add("GN"); add("GW"); add("GY"); add("HT");
  		  add("HM"); add("VA"); add("HN"); add("HK");
  		  add("HU"); add("IS"); add("IN"); add("ID");
  		  add("IR"); add("IQ"); add("IE"); add("IM");
  		  add("IL"); add("IT"); add("JM"); add("JP");
  		  add("JE"); add("JE"); add("JO"); add("KZ");
  		  add("KE"); add("KI"); add("KP"); add("KR");
  		  add("KW"); add("KG"); add("LA"); add("LV");
  		  add("LB"); add("LS"); add("LR"); add("LY");
  		  add("LI"); add("LT"); add("LU"); add("MO");
  		  add("MK"); add("MG"); add("MW"); add("MY");
  		  add("MV"); add("ML"); add("MT"); add("MH");
  		  add("MQ"); add("MR"); add("MU"); add("YT");
  		  add("MX"); add("FM"); add("MD"); add("MC");
  		  add("MN"); add("ME"); add("MS"); add("MA");
  		  add("MZ"); add("MM"); add("NA"); add("NR");
  		  add("NP"); add("NL"); add("AN"); add("NC");
  		  add("NZ"); add("NI"); add("NE"); add("NG");
  		  add("NU"); add("NF"); add("MP"); add("NO");
  		  add("OM"); add("PK"); add("PW"); add("PS");
  		  add("PA"); add("PG"); add("PY"); add("PE");
  		  add("PH"); add("PL"); add("PT"); add("PR");
  		  add("QA"); add("RE"); add("RO"); add("RU");
  		  add("RW"); add("BL"); add("SH"); add("KN");
  		  add("LC"); add("MF"); add("PM"); add("VC");
  		  add("WS"); add("SM"); add("ST"); add("SA");
  		  add("SN"); add("RS"); add("SC"); add("SL");
  		  add("SG"); add("SX"); add("SK"); add("SI");
  		  add("SB"); add("SO"); add("ZA"); add("GS");
  		  add("SS"); add("ES"); add("LK"); add("SD");
  		  add("SR"); add("SJ"); add("SZ"); add("SE");
  		  add("CH"); add("SY"); add("TW"); add("TJ");
  		  add("TZ"); add("TH"); add("TL"); add("TG");
  		  add("TK"); add("TO"); add("TT"); add("TN");
  		  add("TR"); add("TM"); add("TC"); add("TV");
  		  add("UG"); add("UA"); add("AE"); add("GB");
  		  add("US"); add("UM"); add("UY"); add("UZ");
  		  add("VU"); add("VE"); add("VN"); add("VI");
  		  add("WF"); add("EH"); add("YE"); add("ZM");
  		  add("ZW");
  		}
  	};


  	
	public void init(){
		log.debug("Initializing " + getServletName());
			
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
			
		quantities  = config.getInitParameter("purchaseQuantities");
			
		standardEndpointURI     = config.getInitParameter("standardTransactionEndpoint");

		accessKey       	 = context.getInitParameter("accessKey");
		profileId   	     = context.getInitParameter("profileId");
		
		
		try {
			signer = new CybersourceDigest(context.getInitParameter("sharedSecret"));
		}catch(UnsupportedEncodingException e){
			log.fatal("Unsupported characterSet for transactionEncoding");
		}catch(NullPointerException e){
			log.fatal("Caught a NullPointerException creating a signer, is sharedSecret set?");
		}
		
		try {
			builderFactory = DocumentBuilderFactory.newInstance();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			String xsltLocation = config.getInitParameter("htmlConversionStylesheet");	
			InputStream xsltResource = context.getResourceAsStream(xsltLocation);
			Source htmlStylesheet = new StreamSource(xsltResource);
			htmlConversion = transformerFactory.newTemplates(htmlStylesheet);
				
		} catch (TransformerConfigurationException e) {
			log.error("Unable to compile the template for the CybersourceSecureAssurance", e);
		}
		
		pagecontrol = (Pagecontrol) context.getAttribute("pagecontrolDatabase");
		userinfo = (LDAPUserinfo) context.getAttribute("UserinfoProvider");
			
		// Allow the user to specify a Character Encoding other than UTF8
		transactionEncoding = context.getInitParameter("transactionEncoding");
		if (transactionEncoding == null) transactionEncoding = Charset.defaultCharset().name();
		log.trace("Using characterSet \"" + transactionEncoding + "\" for transactionEncoding");						
		
		log.info("Done with " + getServletName() + " init");
	}
				
		  
	private DocumentFragment generateFormFields(Document parentDoc, HttpServletRequest req, Boolean doSigning){		
		Map<String, String> preFormat = new HashMap<String, String>(32);
		Map<String, String> formData = new LinkedHashMap<String,String>(32);
		Map<String, String> postFormat = new HashMap<String, String>(32);
        String uni = req.getRemoteUser();
        
        
        int size = 0;
        if (!doSigning){
        	// The atrociously named "size" is the parameter corresponding to the
        	// selected purchaseQuantity but is only used on the initial post
        	try {
        		size = Integer.valueOf(req.getParameter("size"));
        	}catch (NumberFormatException e){
        		log.error("Unable to parse size param for value, using 0");
        	}
        }
			
		DocumentFragment formParent = parentDoc.createDocumentFragment();

		//Setup the default values for the the first-stage form
		String forename = "";
		String surname  = "";
		String email    = "";
		String phone    = "";
		String line1    = "";
		String city     = "";
		String state    = "";
		String postalCode ="";
		String country    = "US";
		String amount     = null;
		
		if (doSigning){
				forename = req.getParameter("bill_to_forename");
				surname  = req.getParameter("bill_to_surname");
				email    = req.getParameter("bill_to_email");
				amount   = req.getParameter("amount");
				phone    = req.getParameter("bill_to_phone");
				line1    = req.getParameter("bill_to_address_line1");
				city     = req.getParameter("bill_to_address_city");
				state    = req.getParameter("bill_to_address_state");
				country  = req.getParameter("bill_to_address_country");
				postalCode   = req.getParameter("bill_to_address_postal_code");

				// Postal code fixup to ensure only a two-letter abbreviation
				// for US and Canadian locations
				if ( (country.compareToIgnoreCase("US") == 0) &&
				   usStates.containsKey(state.toLowerCase()) ){
					  log.info("Replacing full US state name with postal abbreviation");
					  state = usStates.get(state.toLowerCase());
				}else if ( (country.compareToIgnoreCase("CA") == 0) &&
				   caProvinces.containsKey(state.toLowerCase()) ){
					  log.info("Replacing full Candian province name with postal abbreviation");
					  state = caProvinces.get(state.toLowerCase());
				}
				
		}else{
		   Map<String, String> info;
		   try {
				info = userinfo.lookupUserInfo(uni);
		    } catch (AffiliationException e) {
				info = new HashMap<String, String>(2);
				info.put("sn", "");
				info.put("givenName","");
				info.put("mail", uni + "@columbia.edu");
			}
			forename =  (info.containsKey("givenName"))? info.get("givenName") : "";
			surname  =  (info.containsKey("sn"))? info.get("sn") : "";
			email =  (info.containsKey("mail"))? info.get("mail") : uni + "@columbia.edu";
		}

		
		try {
			
			if (amount == null){
				NumberFormat decimalFormat = NumberFormat.getInstance();
				decimalFormat.setMinimumFractionDigits(2);
				decimalFormat.setMaximumFractionDigits(2);
			    amount = decimalFormat.format(size);
			}
			
			formData.put("bill_to_forename", forename);
			preFormat.put("bill_to_forename", "Firstname on Card:");
			postFormat.put("bill_to_forename", " ");
			
			formData.put("bill_to_surname", surname);
			preFormat.put("bill_to_surname", "Lastname on Card:");
			postFormat.put("bill_to_surname", "<br/>");

			formData.put("bill_to_email", email);
			preFormat.put("bill_to_email", "Billing email address:");
			postFormat.put("bill_to_email", "<br/>");

			formData.put("bill_to_phone", phone);
			preFormat.put("bill_to_phone", "Billing Phone number:");
			postFormat.put("bill_to_phone", "<br/>");
			
			formData.put("bill_to_address_line1", line1);
			preFormat.put("bill_to_address_line1", "Billing address:");
			postFormat.put("bill_to_addrees_line1", "<br/>");
			
			formData.put("bill_to_address_city", city);
			preFormat.put("bill_to_address_city", "City:");
			postFormat.put("bill_to_address_city", "  ");

			formData.put("bill_to_address_state", state);
			preFormat.put("bill_to_address_state", " State:");
			postFormat.put("bill_to_address_state", "<br/>");
			
			formData.put("bill_to_address_postal_code", postalCode);
			preFormat.put("bill_to_address_postal_code", " Postal Code:");
			postFormat.put("bill_to_address_postal_code", "<p/>");
			
			formData.put("bill_to_address_country", country);
			preFormat.put("bill_to_address_country", " Country Code:");
			postFormat.put("bill_to_address_country", "  ");
						
			
			for (String field: formData.keySet()){
				Element fieldNode = parentDoc.createElement("field");
				fieldNode.setAttribute("name", field);
				fieldNode.setAttribute("value", formData.get(field));

				if (doSigning){
					fieldNode.setAttribute("displayOnly", "true");
				}
				if (preFormat.containsKey(field)){
					fieldNode.setAttribute("pre", preFormat.get(field));
				}
				if (postFormat.containsKey(field)){
					fieldNode.setAttribute("post", postFormat.get(field));
				}
				formParent.appendChild(fieldNode);
			}
			
			if (doSigning){
			   sign(parentDoc, formParent, formData, uni, amount);
			   Element unsignedNode = parentDoc.createElement("field");
			   unsignedNode.setAttribute("name", "card_type");
			   formParent.appendChild(unsignedNode);

			   unsignedNode = parentDoc.createElement("field");
			   unsignedNode.setAttribute("name", "card_number");
			   unsignedNode.setAttribute("pre", "Card Number:");
			   formParent.appendChild(unsignedNode);

			   unsignedNode = parentDoc.createElement("field");
			   unsignedNode.setAttribute("name", "card_expiry_date");
			   unsignedNode.setAttribute("pre", "Expires (format: MM-YYYY)");
			   unsignedNode.setAttribute("post", "<br/><br/>");
			   formParent.appendChild(unsignedNode);
			}else{
			   Element amountFieldNode = parentDoc.createElement("field");
			   amountFieldNode.setAttribute("name", "amount");
			   amountFieldNode.setAttribute("value", amount);
			   amountFieldNode.setAttribute("hidden", "true");
			   formParent.appendChild(amountFieldNode);
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
		
		
		
		
	@SuppressWarnings("unchecked")
	public void sign(Document parentDoc, Node formParent, Map<String, String> formData, String uni, String amount) throws
						InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, IllegalStateException{	

		LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
		fields.putAll(formData);

		Element profileIdNode = parentDoc.createElement("field");
		profileIdNode.setAttribute("name", "profile_id");
		profileIdNode.setAttribute("value", profileId);
		profileIdNode.setAttribute("hidden", "true");
		formParent.appendChild(profileIdNode);
		fields.put("profile_id", profileId);
		
		Element transactionUUIDNode = parentDoc.createElement("field");
		String transactionUUID = UUID.randomUUID().toString();
		transactionUUIDNode.setAttribute("name", "transaction_uuid");
		transactionUUIDNode.setAttribute("value", transactionUUID);
		transactionUUIDNode.setAttribute("hidden", "true");
		formParent.appendChild(transactionUUIDNode);
		fields.put("transaction_uuid", transactionUUID);
		
		Element accessKeyNode = parentDoc.createElement("field");
		accessKeyNode.setAttribute("name", "access_key");
		accessKeyNode.setAttribute("value", accessKey);
		accessKeyNode.setAttribute("hidden", "true");
		formParent.appendChild(accessKeyNode);
		fields.put("access_key", accessKey);
		
		Element signedDateTimeNode = parentDoc.createElement("field");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String formattedUTCDateTime = sdf.format(new Date());
		signedDateTimeNode.setAttribute("name", "signed_date_time");
	    signedDateTimeNode.setAttribute("value", formattedUTCDateTime);
	    signedDateTimeNode.setAttribute("hidden", "true");
	    formParent.appendChild(signedDateTimeNode);
	    fields.put("signed_date_time", formattedUTCDateTime);
	    
	    String referenceId = pagecontrol.nextSaleId();
	    Element referenceIdNode = parentDoc.createElement("field");
	    referenceIdNode.setAttribute("name", "reference_number");
	    referenceIdNode.setAttribute("value", referenceId);
	    referenceIdNode.setAttribute("hidden", "true");
	    formParent.appendChild(referenceIdNode);
	    fields.put("reference_number", referenceId);
	    
	    Element transactionTypeNode = parentDoc.createElement("field");
	    transactionTypeNode.setAttribute("name", "transaction_type");
	    transactionTypeNode.setAttribute("value", "sale");
	    transactionTypeNode.setAttribute("hidden", "true");
	    formParent.appendChild(transactionTypeNode);
	    fields.put("transaction_type", "sale");
		
	    Element currencyNode = parentDoc.createElement("field");
	    currencyNode.setAttribute("name", "currency");
	    currencyNode.setAttribute("value", currency);
	    currencyNode.setAttribute("hidden", "true");
	    formParent.appendChild(currencyNode);
	    fields.put("currency", currency);
	    
	    Element amountNode = parentDoc.createElement("field");
	    amountNode.setAttribute("name", "amount");
	    amountNode.setAttribute("value", amount);
	    amountNode.setAttribute("hidden", "true");
	    formParent.appendChild(amountNode);
	    fields.put("amount", amount);
	    
	    Element signedFieldNamesNode = parentDoc.createElement("field");
	    signedFieldNamesNode.setAttribute("name", "signed_field_names");
	    signedFieldNamesNode.setAttribute("value", signedFields);
	    signedFieldNamesNode.setAttribute("hidden", "true");
	    formParent.appendChild(signedFieldNamesNode);
	    fields.put("signed_field_names", signedFields);
	    
	    Element paymentMethodNode = parentDoc.createElement("field");
	    paymentMethodNode.setAttribute("name", "payment_method");
	    paymentMethodNode.setAttribute("value", paymentMethod);
	    paymentMethodNode.setAttribute("hidden", "true");
	    formParent.appendChild(paymentMethodNode);
	    fields.put("payment_method", paymentMethod);

	    Element unsignedFieldNamesNode = parentDoc.createElement("field");
	    unsignedFieldNamesNode.setAttribute("name", "unsigned_field_names");
	    unsignedFieldNamesNode.setAttribute("value", unsignedFields);
	    unsignedFieldNamesNode.setAttribute("hidden", "true");
	    formParent.appendChild(unsignedFieldNamesNode);
	    fields.put("unsigned_field_names", unsignedFields);
	    
	    Element localeNode = parentDoc.createElement("field");
	    localeNode.setAttribute("name", "locale");
	    localeNode.setAttribute("value", "en");
	    localeNode.setAttribute("hidden", "true");
	    formParent.appendChild(localeNode);
	    fields.put("locale", "en");
	    
	    Element merchantDefinedData1Node = parentDoc.createElement("field");
	    merchantDefinedData1Node.setAttribute("name", "merchant_defined_data1");
	    merchantDefinedData1Node.setAttribute("value", uni);
	    formParent.appendChild(merchantDefinedData1Node);
	    fields.put("merchant_defined_data1", uni);
	    
		String data = signer.composeFields(signedFields, fields);		
		String signature = signer.getDigest(data);
		
		Element signatureNode = parentDoc.createElement("field");
		signatureNode.setAttribute("name", "signature");
		signatureNode.setAttribute("id", "signature");
		signatureNode.setAttribute("value", signature);
		signatureNode.setAttribute("hidden", "true");
		formParent.appendChild(signatureNode);
	}
		
		
	public void doPost(HttpServletRequest req, HttpServletResponse res){
			
		log.trace("Entrance into CybersourceSecureAcceptance.doPost()");
		Document presentationDOM;
			
		DocumentBuilder builder;
			try{
				builder = builderFactory.newDocumentBuilder();
				presentationDOM = builder.newDocument();
				
				Element parentNode = presentationDOM.createElement("ninja");
				parentNode.setAttribute("version", "3.0");
				presentationDOM.appendChild(parentNode);
				
				Element targetURL = presentationDOM.createElement("targetURL");
				Element logoutURL = presentationDOM.createElement("logoutURL");
				Element formfields = presentationDOM.createElement("formfields");
				Element appNode = null;
				Element purchaseAmount = presentationDOM.createElement("purchaseAmount"); 
				
				// Use the inclusion of the signed_field_names parameter to check if this is
				// the signed or unsigned form generation stage
				//
				
				if (req.getParameter("bill_to_surname") != null){
					log.debug("creating the final form, for unsigned submission");
					formfields.appendChild(generateFormFields(presentationDOM, req, true));
					purchaseAmount.setAttribute("value", req.getParameter("amount"));	
					appNode = presentationDOM.createElement("unsignedForm");
					targetURL.setAttribute("href", standardEndpointURI);
					logoutURL.setAttribute("href", req.getRequestURL().toString() + "?logout=true");
				}else{
					log.debug("creating the intermediate form for billing info");
					formfields.appendChild(generateFormFields(presentationDOM, req, false));
					purchaseAmount.setAttribute("value", req.getParameter("size"));
					appNode = presentationDOM.createElement("signedForm");
					targetURL.setAttribute("href", req.getRequestURL().toString());
					logoutURL.setAttribute("href", req.getRequestURL().toString() + "?logout=true");
				}
				
				if (appNode != null){
					parentNode.appendChild(appNode);
					appNode.appendChild(targetURL);
					appNode.appendChild(logoutURL);
					appNode.appendChild(formfields);
					appNode.appendChild(purchaseAmount);
				}else{
					log.error("appNode Element was not created");
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

package edu.columbia.cuit.pageServer;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.StringTokenizer;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;


public class CybersourceDigest {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");

	private final String macAlgorithm;
	private final String transactionEncoding;
	private SecretKey secretKey = null;

	
	public CybersourceDigest(String macAlgorithm, String sharedSecret, String transactionEncoding) throws UnsupportedEncodingException {
		this.transactionEncoding = transactionEncoding;
		this.macAlgorithm = macAlgorithm;
		secretKey = new SecretKeySpec(sharedSecret.getBytes(transactionEncoding), macAlgorithm);
	}
	
	// Default to a standard UTF8 encoding
	public CybersourceDigest(String macAlgorithm, String sharedSecret) throws UnsupportedEncodingException{
		this(macAlgorithm, sharedSecret, "UTF-8");
	}
	
	// Yes, this is unnecessary, but I don't see the encoding ever changing
	public CybersourceDigest(String sharedSecret) throws UnsupportedEncodingException{
		this("HmacSha256", sharedSecret, "UTF-8");
	}
	
	// Build a message body from the ordered comma-separated fields in signedFieldNames
	public String composeFields(String signedFieldNames, Map<String, String> param){
		StringTokenizer tokenizer = new StringTokenizer(signedFieldNames, ",", false);
		
		StringBuffer data = new StringBuffer();
		while (tokenizer.hasMoreTokens()) {
		    String key = tokenizer.nextToken();
		    data.append(key);
		    data.append('=');
		    String field = param.get(key);
		    data.append(field);
		    if (tokenizer.hasMoreTokens()){
		    	data.append(',');
		    }
		}
		return data.toString();
	}
	
	public String getDigest(String message) throws InvalidKeyException,NoSuchAlgorithmException, UnsupportedEncodingException, IllegalStateException{

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
}

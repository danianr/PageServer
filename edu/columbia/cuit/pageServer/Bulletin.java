package edu.columbia.cuit.pageServer;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;


public class Bulletin {

	  private final String messageId;
	  private final String message;
	  private final Calendar begin;
	  private final Calendar end;
	  private final String imageURL;
	  private final boolean hasImage;
	  private int precedence;
	  
	

	  public Bulletin(String message, String imageURL, Calendar begin, Calendar end){
		  this.message = message;
		  this.imageURL = imageURL;
		  this.begin = begin;
		  this.end = end;
		  this.precedence = 1;
		  this.hasImage = (imageURL != null)? true : false;

		  String messageId = null;
		  MessageDigest md;
		  try {
			md = MessageDigest.getInstance("SHA-1");
			md.update(Long.toHexString(begin.getTimeInMillis() / 1000).getBytes("UTF8"));
			md.update(Long.toHexString(end.getTimeInMillis() / 1000).getBytes("UTF8"));
			md.update(message.getBytes("UTF8"));
			if (this.hasImage){
				md.update(imageURL.getBytes("UTF8"));
			}
			
			messageId = Base64.encodeBase64URLSafeString(md.digest());
			
		  } catch (NoSuchAlgorithmException e) {
			  messageId = Long.toHexString(begin.getTimeInMillis());
		  } catch (UnsupportedEncodingException e) {
			  messageId = Long.toHexString(begin.getTimeInMillis());
		  } finally {
			  this.messageId = messageId;
		  }
	  
	  }

	  public String getMessageId(){
		  return messageId;
	  }
	  
	  protected Calendar getBeginCal(){
		  return begin;
	  }
	  
	  protected Calendar getEndCal(){
		  return end;
	  }
	  
	  public long getBeginingEpoch(){
		 return this.begin.getTimeInMillis() / 1000;
	  }
	  
	  public long getEndingEpoch(){
		  return this.end.getTimeInMillis() / 1000;
	  }
	  
	  public String getMessage(){
		  return this.message;
	  }
	  
	  public boolean hasImage(){
		  return this.hasImage;
	  }
	  
	  public String getImageURL(){
		  return this.imageURL;
	  }
	  
	  public int getPrecedence(){
		  return this.precedence;
	  }
	  
	  public void setPrecedence(int precedence) {
		  this.precedence = precedence;
	  }

	  public boolean isExpired(){
		  return Calendar.getInstance().after(end);
	  }
	  

}

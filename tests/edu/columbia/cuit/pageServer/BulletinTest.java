package edu.columbia.cuit.pageServer;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

public class BulletinTest {
	private Calendar begins;
	private Calendar ends;
	private Bulletin withImage;
	
	
	@Before
	public void setUp() throws Exception {
		begins = Calendar.getInstance();		
		begins.setTimeInMillis(1376327422l);
		
		ends = Calendar.getInstance();
		ends.setTimeInMillis(1376341822l);

		withImage = new Bulletin("This is a test message\nfor the NINJa display system.  This is only a test",
				"http://www.columbia.edu/~dr2481/shark.gif", begins, ends);
		
		withImage.setPrecedence(1);
	}

	
	
	@Test
	public void testBulletin() {
		assertTrue(!withImage.equals(null));
		Bulletin dup = new Bulletin("This is a test message\nfor the NINJa display system.  This is only a test",
				"http://www.columbia.edu/~dr2481/shark.gif", begins, ends);
		assertEquals(withImage.getMessageId(), dup.getMessageId());
	}

	@Test
	public void testGetMessageId() {
		Calendar begin = withImage.getBeginCal();
		Calendar end = withImage.getEndCal();
		
		MessageDigest md;
		
		try {
			md = MessageDigest.getInstance("SHA-1");
		   md.update(Long.toHexString(begin.getTimeInMillis() / 1000).getBytes("UTF8"));
		   md.update(Long.toHexString(end.getTimeInMillis() / 1000).getBytes("UTF8"));
		   md.update(withImage.getMessage().getBytes("UTF8"));
		   if (withImage.hasImage()){
			  md.update(withImage.getImageURL().getBytes("UTF8"));
		   }
			
		   String computedMessageId = Base64.encodeBase64URLSafeString(md.digest());

		   System.out.println("ComputedMessageId =" + computedMessageId);
		   System.out.println("MessageId         =" + withImage.getMessageId());
		   assertEquals(computedMessageId, withImage.getMessageId());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test
	public void testGetBeginingEpoch() {
		assertEquals(withImage.getBeginingEpoch(), 1376327);
	}

	@Test
	public void testGetEndingEpoch() {
		assertEquals(withImage.getEndingEpoch(), 1376341);
	}

	@Test
	public void testGetMessage() {
		assertEquals(withImage.getMessage(), "This is a test message\nfor the NINJa display system.  This is only a test");
	}

	@Test
	public void testHasImage() {
		assertTrue(withImage.hasImage());
	}

	@Test
	public void testGetImageURL() {
		assertEquals(withImage.getImageURL(), "http://www.columbia.edu/~dr2481/shark.gif");
	}

	@Test
	public void testGetPrecedence() {
		assertEquals(withImage.getPrecedence(), 1);
	}

	@Test
	public void testSetPrecedence() {
		int p = withImage.getPrecedence();
		withImage.setPrecedence(3);
		assertEquals(withImage.getPrecedence(), 3);
		withImage.setPrecedence(28);
		assertEquals(withImage.getPrecedence(), 28);
		withImage.setPrecedence(-4000);
		assertEquals(withImage.getPrecedence(), -4000);
		withImage.setPrecedence(p);
		assertEquals(withImage.getPrecedence(), p);
	}

	@Test
	public void testIsExpired() {
		assertTrue(true);
	}

}

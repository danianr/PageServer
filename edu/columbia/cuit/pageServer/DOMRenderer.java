package edu.columbia.cuit.pageServer;

import java.io.ByteArrayOutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.apache.log4j.or.ObjectRenderer;
import org.w3c.dom.Document;


public class DOMRenderer implements ObjectRenderer {

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	
	public DOMRenderer() {
		// no initialization of renderer is necessary
	}
	
	
	public String doRender(Object arg0) {
		
		if (!Document.class.isAssignableFrom(arg0.getClass())) return null;
		Document doc = (Document) arg0;
		
		ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
		Result xmlResult = new StreamResult(bufStream);
		Source domSource = new DOMSource(doc);
		Transformer identityTransform;
		try {
			identityTransform = transformerFactory.newTransformer();
			identityTransform.transform(domSource, xmlResult);			
		} catch (TransformerConfigurationException e) {
			return "ERROR: " + e.getMessage();
		} catch (TransformerException e) {
			return "ERROR: " + e.getMessage();
		}
		return bufStream.toString();
	}
				
}

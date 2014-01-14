package edu.columbia.cuit.pageServer;

import org.apache.log4j.or.ObjectRenderer;

public class BulletinRenderer implements ObjectRenderer {

	public BulletinRenderer() {
		// No initialization of render is necessary
	}


	public String doRender(Object arg0) {
		Bulletin bulletin = (Bulletin) arg0;
		StringBuffer sb = new StringBuffer(256);
		sb.append("Bulletin { id:");
		sb.append(bulletin.getMessageId());
		sb.append(" starts:");
		sb.append(bulletin.getBeginCal().getTimeInMillis());
		sb.append(" ends:");
		sb.append(bulletin.getEndCal().getTimeInMillis());
		sb.append(" precedence:");
		sb.append(bulletin.getPrecedence());
		sb.append(" imageURL:");
		sb.append(bulletin.getImageURL());
		sb.append(" message:\"");
		sb.append(bulletin.getMessage());
		return sb.toString();
	}

}

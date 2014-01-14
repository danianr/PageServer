package edu.columbia.cuit.pageServer;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class BulletinBoard implements Iterable<Bulletin> {
	
	private ArrayList<Bulletin> bulletins;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	
	
	public BulletinBoard(){
		log.trace("Entrance into BulletinBoard Constructor");
		this.bulletins = new ArrayList<Bulletin>(3);
	}

	public boolean hasBulletins(){
		log.trace("Entrance into BulletinBoard.hasBulletins");
		return !this.bulletins.isEmpty();
	}
	
	public Iterator<Bulletin> iterator(){
		log.trace("Entrance into BulletinBoard.Iterator");
		return this.bulletins.iterator();
	}

	public void addBulletin(Bulletin posting){
		log.trace("entrance into BulletinBoard.addBulletin");
		this.purgeExpired();
		this.bulletins.add(posting);
	}
	
	public void purgeExpired(){
	   log.trace("entrance into BulletinBoard.purgeExpired");
	   Iterator<Bulletin> bi = bulletins.iterator();
	   while (bi.hasNext()){
	      Bulletin posted = bi.next();
	      if (posted.isExpired()){
			if (log.isDebugEnabled()){
				log.debug("Removing messageId=" + posted.getMessageId() +
						   " at objectAddress:" + posted.toString());
			}
	    	bi.remove();
	      }
	   }
	}

	public void removeBulletin(String messageId){
		log.trace("Entrance into BulletinBoard.removeBulletin(String)");
		Iterator<Bulletin> bi = bulletins.iterator();
		while (bi.hasNext()){
			Bulletin posted = bi.next();
			if (messageId.equals(posted.getMessageId())){
				if (log.isDebugEnabled()){
					log.debug("Removing messageId=" + messageId +
							" at objectAddress:" + posted.toString());
				}
				bi.remove();
			}
		}		
	}

	
	public void removeBulletin(Bulletin posted){
		log.trace("entrance into BulletinBoard.removeBulletin(Bulletin)");
		this.bulletins.remove(posted);
		if (log.isDebugEnabled()){
			log.debug("removed Bulletin ->");
			log.debug(posted);
		}
	}
	
	public void removeAll(){
		log.trace("Entrance into BulletinBoard.removeAll");
		this.bulletins.clear();
		log.debug("Removed all Bulletins");
	}

}

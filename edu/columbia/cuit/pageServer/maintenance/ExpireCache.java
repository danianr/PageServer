package edu.columbia.cuit.pageServer.maintenance;

import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.columbia.cuit.pageServer.MaintenanceTask;
import edu.columbia.cuit.pageServer.PersistenceLayer;

public class ExpireCache implements MaintenanceTask {

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final long serialVersionUID = 1l;
	
	private	final PersistenceLayer persist;
	private Date lastRun;
	private boolean pending;
	
	public ExpireCache(PersistenceLayer persist){
		this.persist = persist;
		this.lastRun = null;
		this.pending = false;
	}
	
	public Date getNextScheduledRun() {
		
		// Only schedule once per actual run
		if (pending){
			return null;
		}
		
		// Only run if there are more than N items cached
		if (persist.affilCache.size() < 2048l){
			return null;
		}
		
		// When needed, schedule a run for 4am 
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 4);
		if ( cal.before(Calendar.getInstance()) ){
			cal.add(Calendar.DATE, 1);
		}
		return cal.getTime();
	}

	public Date getPreviousRun() {
		return this.lastRun;
	}

	public boolean isRecurring() {
		return false;
	}

	public long getRecurrenceInterval() {
		return 0;
	}

	public boolean isReloadable() {
		return !pending;
	}

	public TimerTask getTimerTask() {

		pending = true;
		
		return new TimerTask() {
			
			public void run() {
				// pending and lastRun are scoped from the instantiating
				// class and provide a pathway to pass state information
				// back to the instantiating class
				
				lastRun = Calendar.getInstance().getTime();
				persist.affilCache.cleanCache(86400000l);
				log.debug("Cleared cache entries not accessed within the last day");
				pending = false;
			}
			   
		};
	  
	}

}

package edu.columbia.cuit.pageServer.maintenance;

import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.columbia.cuit.pageServer.MaintenanceTask;
import edu.columbia.cuit.pageServer.PersistenceLayer;

public class WeeklyReset implements MaintenanceTask {

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final long serialVersionUID = 2l;
	
	private	final PersistenceLayer persist;
	private Date lastRun;

	
	public WeeklyReset(PersistenceLayer persist){
		this.persist = persist;
		this.lastRun = null;
	}
	
	
	public Date getNextScheduledRun() {
		
		// Schedule for the next 1am Sunday slot 
		// Do not reset the second to allow for some degree of perturbation
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		cal.set(Calendar.HOUR_OF_DAY, 1);
		cal.set(Calendar.MINUTE, 0);

		if ( cal.before(Calendar.getInstance()) ){
			cal.add(Calendar.DATE, 7);
		}
		return cal.getTime();
	
	}

	public Date getPreviousRun() {
		return lastRun;
	}

	public boolean isRecurring() {
		return true;
	}

	public boolean isReloadable() {
		return false;
	}

	public long getRecurrenceInterval() {
		return 604800000l;
	}

	public TimerTask getTimerTask() {

		return new TimerTask() {
			
			public void run() {
				// lastRun is scoped from the instantiating class
				// and provides a pathway to return state information
				lastRun = Calendar.getInstance().getTime();
				
				// The persistence layer keeps a Pagecontrol object
				// which acts as the single point of database access
				persist.pagecontrolDatabase.resetWeeklyQuotas();
				
				log.debug("WeeklyReset of quotas has been run");
			}
			   
		};
	
	
	}

}

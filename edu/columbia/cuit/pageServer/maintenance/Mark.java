package edu.columbia.cuit.pageServer.maintenance;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.columbia.cuit.pageServer.MaintenanceTask;
import edu.columbia.cuit.pageServer.PersistenceLayer;

public class Mark implements MaintenanceTask {

	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final long serialVersionUID = 1l;
	private Date lastRun;
	private final DateFormat format;

	
	public Mark(PersistenceLayer persist){
		lastRun = null;
		format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
	}
	
	public Date getNextScheduledRun() {
		final Calendar cal = Calendar.getInstance();
		cal.clear(Calendar.SECOND);
		cal.add(Calendar.MINUTE, 2);
		return cal.getTime();
	}

	public Date getPreviousRun() {
		return this.lastRun;
	}

	public boolean isRecurring() {
		return true;
	}

	public long getRecurrenceInterval() {
		return 300000;
	}

	public boolean isReloadable() {
		return false;
	}

	public TimerTask getTimerTask() {

		
		return new TimerTask() {
			
			public void run() {
				lastRun = Calendar.getInstance().getTime();
				log.info("MARK "+ format.format(lastRun));
			}
			   
		};
	  
	}

}

package edu.columbia.cuit.pageServer.maintenance;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.columbia.cuit.pageServer.MaintenanceTask;
import edu.columbia.cuit.pageServer.PersistenceLayer;


public class PermissionsRefresh implements MaintenanceTask {
	
	private Date lastRun;
	private final PersistenceLayer persist;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");

	public PermissionsRefresh(PersistenceLayer persist){
		this.persist = persist;
		lastRun = null;
	}

	public Date getNextScheduledRun() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 3);
		return cal.getTime();
	}

	public Date getPreviousRun() {
		return lastRun;
	}

	public boolean isRecurring() {
		return true;
	}

	@Override
	public boolean isReloadable() {
		return false;
	}

	@Override
	public long getRecurrenceInterval() {
		return 180000;
	}

	@Override
	public TimerTask getTimerTask() {
		return new TimerTask() {
			
			public void run() {
				lastRun = Calendar.getInstance().getTime();
				try {
					persist.accessManager.refresh();
				} catch (SQLException e) {
					log.error("Caught SQLException in PermissionsRefresh", e);
				}
			}
			   
		};
	}

}

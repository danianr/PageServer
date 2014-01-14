package edu.columbia.cuit.pageServer;

import java.util.Date;
import java.util.TimerTask;

public interface MaintenanceTask {
	
	public abstract Date getNextScheduledRun();
	public abstract Date getPreviousRun();
	public abstract boolean isRecurring();
	public abstract boolean isReloadable();
	public abstract long getRecurrenceInterval();
	public abstract TimerTask getTimerTask();
}

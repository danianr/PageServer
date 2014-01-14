package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;


public class MaintenanceScheduler extends TimerTask{

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final String propfile = "/scheduler.properties";
	private final Calendar cal;
	private final Timer timer;
	private final Map<String,MaintenanceTask> loadedTasks;
	
	
	public MaintenanceScheduler(PersistenceLayer caller){
		log.trace("Initializing MaintenanceSchedulerClass");
		cal = Calendar.getInstance();

		loadedTasks = new HashMap<String,MaintenanceTask>();
		Properties properties = new Properties();
		try {

			if (log.isTraceEnabled()){
				URL propURL = caller.getServletContext().getResource(propfile);
				log.trace("Loading properties for scheduled tasks from " + propURL.toString());
			}
			
			InputStream schedulerProperties = caller.getServletContext().getResourceAsStream(propfile);			
			if ( schedulerProperties != null ){
				properties.load(schedulerProperties);
				log.trace("Loading classes instantiating scheduled tasks");
				
				Iterator<String> i = properties.stringPropertyNames().iterator();

				while (i.hasNext()){
					String taskName = i.next();
					Class<?> taskClass = Class.forName((String) properties.getProperty(taskName));
					if (MaintenanceTask.class.isAssignableFrom(taskClass)){
						Constructor<?> taskInstantiator =  taskClass.getDeclaredConstructor(PersistenceLayer.class);
						MaintenanceTask taskInstance = (MaintenanceTask) taskInstantiator.newInstance(caller);
						log.trace("Created new instance:" + taskInstance.toString());
						loadedTasks.put(taskName, taskInstance);
					}
				}
			}	
				
				
		} catch (IOException e) {
			log.error(e);
		} catch (ClassNotFoundException e) {
			log.error(e);
		} catch (SecurityException e) {
			log.error(e);
		} catch (NoSuchMethodException e) {
			log.error(e);
		} catch (IllegalArgumentException e) {
			log.error(e);
		} catch (InstantiationException e) {
			log.error(e);
		} catch (IllegalAccessException e) {
			log.error(e);
		} catch (InvocationTargetException e) {
			log.error(e);
		}
	
		timer = new Timer("MaintenanceTimer", true);
		Iterator<Entry<String,MaintenanceTask>> loadedEntries = loadedTasks.entrySet().iterator();
		
		
		
		while (loadedEntries.hasNext()){
			Entry<String, MaintenanceTask> taskEntry = loadedEntries.next();
			MaintenanceTask task = taskEntry.getValue();
			log.trace("Examining Loaded Task " + taskEntry.getKey() + "/" + task.toString());			
			Date nextScheduledRun = task.getNextScheduledRun();
			if (nextScheduledRun != null){   // NOPMD by dr2481 on 12/14/11 4:24 PM
				
				if (task.isRecurring()){
					timer.schedule(task.getTimerTask(), nextScheduledRun, task.getRecurrenceInterval());
					log.debug("Scheduled an initial recurring maintenance task for " + task.getClass().getName());
				}else{
					timer.schedule(task.getTimerTask(), nextScheduledRun);
					log.debug("Scheduled an initial maintenance task for " + task.getClass().getName());
				}
			}else{
				log.warn("Next Scheduled run is NULL for " + taskEntry.getKey());
			}
		}
		
		// Add self to the scheduled timer tasks so the run will update any pending fixed-schedule events
		timer.schedule(this, 3600000l, 3600000l);
		
	}


	public void run() {

		log.trace("MaintenanceScheduler refreshing tasklist");
		Iterator<MaintenanceTask> ti = loadedTasks.values().iterator();
		while (ti.hasNext()){
			MaintenanceTask task = ti.next();
			if (log.isDebugEnabled()){
			   String prevRun = String.format("Previous run of %s occurred at %tc", task.toString(), task.getPreviousRun());
			   log.debug(prevRun);
			}
			
			if ( task.isReloadable() ){
				// Only schedule tasks which are to occur in the future
				Date nextScheduledRun = task.getNextScheduledRun();
				cal.setTimeInMillis(System.currentTimeMillis());
				if ( (nextScheduledRun != null) && (cal.getTime().before(nextScheduledRun)) ){
					timer.schedule(task.getTimerTask(), nextScheduledRun);
					log.debug("Scheduled a maintenance task for " + task.getClass().getName());
				}
			}
		}
	}
	
	public void stop(){
		log.trace("Entrance into MaintenanceScheduler.stop");
		timer.cancel();
	}
}

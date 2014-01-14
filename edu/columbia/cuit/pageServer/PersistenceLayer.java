package edu.columbia.cuit.pageServer;


import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;

import oracle.jdbc.driver.OracleDriver;
import oracle.jdbc.pool.OracleDataSource;

import org.apache.log4j.*;


public class PersistenceLayer extends HttpServlet{

	private static final long serialVersionUID = 1L;
	
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	
	
		/* The PersistenceLayer instantiates the Database and LDAP
		 * connection pool objects, as well as managing the persistent
		 * cache.  Access is passed through context attributes, thus
		 * this class must be initialized before any of the other
		 * servlets.
		 * 
		 * This class will also handle reporting for the persistent
		 * objects such as number of connections, cache memory usage
		 * and general performance metrics.
		 * 
		 */
		
		private MemoryMXBean memoryBean;
	    private ThreadMXBean threadBean;
	    private List<MemoryPoolMXBean> mempoolBeans;
	    private List<GarbageCollectorMXBean> gcBeans;
	    
	    private Pcd pcdAdapter;
	    private Thread pcdAdapterThread;

		private MaintenanceScheduler scheduler; // NOPMD by dr2481 on 12/14/11 4:54 PM
	    
	    private OracleDataSource ods; // NOPMD by dr2481 on 12/14/11 4:54 PM
		public AffiliationCache affilCache;
		public Set<String> printingAffils;

		public Pagecontrol pagecontrolDatabase;
		public AccessManager accessManager;
		public LDAPUserinfo userinfo;
		public BulletinBoard bulletinBoard;
		
			
		public void init(){
				log.info("Initializing PageServer Persistence Layer");
			
				ServletConfig config  = getServletConfig();
				ServletContext context = config.getServletContext();

				log.trace("Config context is at object address (" + config.toString() + ")");
				log.trace("ServletContext is at object address (" + context.toString() + ")");
	
				int ldapTimeout =  Integer.parseInt(config.getInitParameter("ldapTimeout"));
				LDAPSearchConstraints constraints = new LDAPSearchConstraints();
				constraints.setTimeLimit(ldapTimeout);

				this.printingAffils = new HashSet<String>();

				Connection conn = null;
				try {
					String oracleURL = config.getInitParameter("oracleURL");
					this.ods = new OracleDataSource();				
					ods.setURL(oracleURL);

					conn = ods.getConnection();
					ods.setImplicitCachingEnabled(true);

					if (conn.isValid(30)){					
						log.trace("Populating affiliation filter from Database");
						Statement sql = conn.createStatement();
						ResultSet ldapAffiliations = sql.executeQuery("SELECT ldap_name FROM affiliation");

						while(ldapAffiliations.next()){
							printingAffils.add(ldapAffiliations.getString("LDAP_NAME"));
						}
						ldapAffiliations.close();
						sql.close();
					}
				
				}catch(Exception e){
					log.fatal("Unable to get the initial Affiliation filter from the Database", e);
				}finally{
					try {
						if ( (conn != null) && !conn.isClosed()) conn.close();
					} catch (SQLException e) {
						log.warn("Caught SQLException trying to close DB connection", e);
					}
				}
				
				
				// The affil cache is instantiated outside the pagecontrol Object to allow
				// serialization of the cache
				try {
						String ldapURL = config.getInitParameter("ldapURL");
						String searchBase = config.getInitParameter("ldapSearchBase");
						String bindDn     = config.getInitParameter("ldapBindDistinguishedName");
						String bindPw     = config.getInitParameter("ldapBindPassword");
						long cacheDuration = Long.parseLong(config.getInitParameter("cacheDuration"));
						
						this.affilCache = new AffiliationCache(printingAffils, cacheDuration);
						AffiliationModule ldapAffils = new LDAPAffiliationModule(ldapURL, constraints, searchBase, bindDn, bindPw);
						affilCache.register(ldapAffils);
						userinfo = new LDAPUserinfo(ldapURL, constraints, searchBase, bindDn, bindPw);
						
				} catch (LDAPException e) {
					log.fatal("LDAP Exception while creating AffiliationCache", e);
				} catch (MalformedURLException e) {
					log.fatal("Malformed LDAP URL" , e);
				}

				
				try {
					pagecontrolDatabase = new OraclePagecontrolDriver(affilCache, ods);
					context.setAttribute("pagecontrolDatabase", pagecontrolDatabase);
				} catch (SQLException e1) {
					log.error("SQLException while initializing the pagecontrolDatabase interface", e1);
				}
				
				context.setAttribute("UserinfoProvider", userinfo);
				log.trace("UserinfoProvider in use at object address (" + userinfo.toString() +")");
				
				accessManager = new AccessManager(context, ods);
				context.setAttribute("AccessManager", accessManager);
				log.trace("AccessManager in use at object address (" + accessManager.toString() + ")");
				
				bulletinBoard = new BulletinBoard();
				context.setAttribute("BulletinBoard", bulletinBoard);
				log.trace("BulletinBoard in use at object address (" + bulletinBoard.toString() + ")");
				
				String pcdAdapterName = config.getInitParameter("legacyPcdAdapter");
				
				if (pcdAdapterName != null){
					String pcdHostname = config.getInitParameter("pcdHostname");
					String pcdPort = config.getInitParameter("pcdPort");
					String pcdBacklog = config.getInitParameter("pcdBacklog");
					InetAddress pcdInetAddr = null;

					try {
						Class<?> pcdAdapterClass = Class.forName(pcdAdapterName);
						Constructor<?> pcdConstructor = pcdAdapterClass.getConstructor(Pagecontrol.class, AccessManager.class, InetAddress.class, int.class, int.class);
						if (pcdHostname != null) pcdInetAddr = InetAddress.getByName(pcdHostname);
						Object legacyAdapter = pcdConstructor.newInstance(pagecontrolDatabase, accessManager, pcdInetAddr, Integer.parseInt(pcdPort), Integer.parseInt(pcdBacklog));
						pcdAdapterClass.cast(legacyAdapter);
						if (Pcd.class.isAssignableFrom(pcdAdapterClass)){
							pcdAdapter = (Pcd) legacyAdapter;
						}
					} catch (UnknownHostException e) {
						log.fatal("Unable to instantiate pcdAdapter due to UnknownHostException",e);
					} catch (NumberFormatException e) {
						log.fatal("Unable to instantiate pcdAdapter due to NumberFormatException",e);
					} catch (ClassNotFoundException e) {
						log.fatal("Unable to instantiate pcdAdapter due to NumberFormatException", e);
					} catch (SecurityException e) {
						log.fatal("Unable to instantiate pcdAdapter due to SecurityException", e);
					} catch (NoSuchMethodException e) {
						log.fatal("Could not find a suitable constructor to instantiate pcdAdapter", e);
					} catch (IllegalArgumentException e) {
						log.fatal("Argument supplied to the pcdAdapter constructor was inncorrect", e);
					} catch (InstantiationException e) {
						log.fatal("Could not instantiate pcdAdapter",e);
					} catch (IllegalAccessException e) {
						log.fatal("IllegalAccessException attempting to instantiate pcdAdapter",e);
					} catch (InvocationTargetException e) {
						log.fatal("Unable to instantiate pcdAdapter due to a InvocationTargetException",e);
					}
				
				}
				
				// Initialize the Management Beans for this VM
				this.memoryBean   = ManagementFactory.getMemoryMXBean();
				this.mempoolBeans = ManagementFactory.getMemoryPoolMXBeans();
				this.gcBeans      = ManagementFactory.getGarbageCollectorMXBeans();
				this.threadBean   = ManagementFactory.getThreadMXBean();
				threadBean.setThreadContentionMonitoringEnabled(true);
	
								
				if (this.pcdAdapter != null){
					pcdAdapterThread = new Thread(pcdAdapter);
					pcdAdapterThread.start();
				}
		
				
				// A reference to this Persistence layer is passed into the
				// scheduler to allow the Maintenance Tasks access to methods
				// within the PersistenceLayer
				scheduler = new MaintenanceScheduler(this);
				log.trace("MaintenanceScheduler loaded at (" + scheduler.toString() + ")");
				log.info("Done with PersistenceLayer init");
			}	
		
		
		public void destroy(){
			
			
			if (pcdAdapterThread != null){
				log.info("Shutting down the pcdAdapter");
				pcdAdapter.shutdown();
			}
			
			if (scheduler != null){
				log.info("Stopping the MaintenceScheduler");
				scheduler.stop();
			}
			
			// Unregister the Oracle JDBC driver to prevent runaway thread
			// Memory leaks
			Enumeration<Driver> loadedDrivers = DriverManager.getDrivers();
			
			while (loadedDrivers.hasMoreElements()){
				Driver driver = loadedDrivers.nextElement();
				if (log.isTraceEnabled()) {
					log.trace("Examining for removal JDBC driver: " + driver.getClass().toString());
				}
				if (driver instanceof OracleDriver){
					try {
						log.debug("Attempting to deregister JDBC Driver " + driver.getClass().toString());
						DriverManager.deregisterDriver(driver);
					} catch (SQLException e) {
						log.warn("Unable to deregister the Oracle Driver",e);
					}
				}
			}
			
		}
		
		
		
		
		
		
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();

		out.println("<HTML");
		out.println("<HEAD> <TITLE> Page Server Reporting </TITLE> </HEAD>");

		@SuppressWarnings("unchecked")
		Map<String, String[]> paramMap = req.getParameterMap();
		
		// Change the main log4j logging level dynamically.  
		// Initial level should be set to DEBUG to supply relevant
		// information subsequent to the application failing to load
		if (paramMap.containsKey("loglevel")){
				String[] suppliedLevel = paramMap.get("loglevel");
				Level previousLevel = log.getLevel();
				Level loggingLevel = Level.toLevel(suppliedLevel[0]);
				log.setLevel(loggingLevel);
				out.println("<P>Logging Level for " + log.getName() + " has been set to " +
						           log.getLevel().toString() + " from " + previousLevel.toString());
		}


		// Database Bypass inhibits any database access and will authorize all query/deduct requests
		String bypassParam = req.getParameter("databasebypass");
		if (bypassParam != null){
			boolean bypass = Boolean.parseBoolean(bypassParam);
			pagecontrolDatabase.setDatabaseBypass(bypass);
			out.printf("<P>Database Bypass is currently %s\n", 
					(pagecontrolDatabase.getDatabaseBypass())? "ENABLED" : "DISABLED" );
		}
		
		
		if (paramMap.containsKey("expire")){
			String[] requestedExpiry = paramMap.get("expire");
			if (requestedExpiry.length > 0 && requestedExpiry[0] != null){
				log.debug("Recieved request to expire all cached entries older than " + requestedExpiry[0] + "milliseconds");
				try {
					long expireAfter = Long.parseLong(requestedExpiry[0]);
					long previousSize = affilCache.size();
					long cleanedSize = affilCache.cleanCache(expireAfter);
					out.printf("<P><h2>Cache Expire requested for entries older than %d ms\n<br>%d entries before<br>%d entries after\n",
							expireAfter, previousSize, cleanedSize);
				}catch(NumberFormatException enf){
					log.error("Unable to parse expire time for cache entries", enf);
				}
			}else{
				log.warn("No expire time argument supplied for cache expiry");
				out.println("<p>Cache Expire requires an argument\n");
			}
		}
		
		if (paramMap.containsKey("forcegc")){
			log.debug("Requesting a garbage collection");
			System.gc(); // NOPMD by dr2481 on 12/13/11 4:50 PM
			out.println("<P>System Garbage collection was run.");
		}
		
		
		if (paramMap.containsKey("memory")){
			out.printf("<P><TT><PRE>Heap Init:\t%d bytes\nHeap Max:\t%d bytes\nHeap Used:\t%d bytes\nHeap Committed:\t%d bytes\n", 
				memoryBean.getHeapMemoryUsage().getInit(), memoryBean.getHeapMemoryUsage().getMax(), 
				memoryBean.getHeapMemoryUsage().getUsed(), memoryBean.getHeapMemoryUsage().getCommitted());

				
			out.println("<P>Memory Pools:");
			for (MemoryPoolMXBean mempool: mempoolBeans){
	
				if (mempool.isValid() ){
					out.printf("%-20s\tPeak:%12d  Max:%12d", mempool.getName(), mempool.getPeakUsage().getUsed(),
						mempool.getPeakUsage().getMax());
					for (String memoryManager: mempool.getMemoryManagerNames() ){ 
						out.print("  ("+ memoryManager+ ")");
					}
					out.println();
				}
			}
		
		
			out.printf("\nObjects pending finalization: %d\n",memoryBean.getObjectPendingFinalizationCount());
			out.printf("affilCache: %d entries\n\n", affilCache.size());

		
			out.println("<P>Garbage Collection:");
			for (GarbageCollectorMXBean gcb: gcBeans){
				out.printf("%-20s\tCollections: %12d ElaspsedTime: %12d\n", "("+gcb.getName()+")", gcb.getCollectionCount(), gcb.getCollectionTime());
			}
		}
		

		if (paramMap.containsKey("threads")){
			out.printf("<P><TT><PRE>Current thread count: %d\nPeak thread count: %d%n", threadBean.getThreadCount(), threadBean.getPeakThreadCount());
		
			for (ThreadInfo threadInfo: threadBean.dumpAllThreads(true, true)){
				out.printf("%n[%s] %d state:%s native:%b%n",threadInfo.getThreadName(), threadInfo.getThreadId(), threadInfo.getThreadState().toString(), threadInfo.isInNative() );
				for (StackTraceElement stack: threadInfo.getStackTrace()){
					out.printf("    class:%s  method:%s  line:%d  native:%b%n", stack.getClassName(), stack.getMethodName(), stack.getLineNumber(), stack.isNativeMethod());
				}
			}
		}
			
		if (paramMap.containsKey("dumpcache")){
			log.debug(this.affilCache);
		}
		
		out.println("</TT></PRE>");
		out.println("</BODY></HTML>");
		
		
				
	}		
	
		
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{  doGet(req,res); }
	    
}
		
		
		
		




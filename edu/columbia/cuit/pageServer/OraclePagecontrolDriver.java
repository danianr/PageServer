package edu.columbia.cuit.pageServer;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Date;
import java.sql.Time;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import oracle.jdbc.pool.OracleDataSource;


public class OraclePagecontrolDriver implements Pagecontrol{ 

	/* This class packages up the Affiliation Cache Manager and the Oracle
	 * Database Connection Pool and provides standardized methods to
	 * encapsulate the business logic behind the administration of quotas
	 * and the crediting of printing dollars
	 */

	private final AffiliationCache affilCache;
	private final OracleDataSource ods;

	
	// Caches printer.hostname to printer.printer_group_id mapping
	private final Map<String,Integer> printerGroupId;
	
	// Caches printergroup.printer_group_id to printergroup.name 
	private final Map<Integer,String> printerGroupName;
	
	// Caches the result of the outer join printer.hostname printergroup.value
	private final Map<String, Integer> pageCost;
	private final Map<String, Integer> pageCostByGroupName;
	
	private boolean databaseBypass;
	private boolean purchaseEnabled;
	
	// legacy departmental values
	private static final String department = "acis";
	private static final int departmentId = 1;


	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final Logger transact = Logger.getLogger("edu.columbia.cuit.PageServer.transaction");
	private static final Logger querylog = Logger.getLogger("edu.columbia.cuit.PageServer.querylog");
	private static final Logger receipts = Logger.getLogger("edu.columbia.cuit.PageServer.receipts");
	
	private final AcademicYear academicCalendar;
	
	
	public OraclePagecontrolDriver(AffiliationCache affilCache, OracleDataSource ods) throws SQLException{
		
		log.trace("Instantiating Pagecontrol object");
		this.affilCache = affilCache;
		this.ods = ods;
		this.printerGroupId = new HashMap<String, Integer>();
		this.printerGroupName = new HashMap<Integer, String>();
		
		this.pageCost = new HashMap<String, Integer>();
		this.pageCostByGroupName = new HashMap<String, Integer>();
		this.databaseBypass = false;
		this.purchaseEnabled = true;
		this.academicCalendar = new Semester();

		Connection connection = ods.getConnection();
		
		PreparedStatement printerLookup = connection.prepareStatement("SELECT p.hostname, p.printer_group_id, pg.name, pg.value FROM printer p, printergroup pg WHERE p.printer_group_id = pg.printer_group_id");
		
		ResultSet printerResults = printerLookup.executeQuery();
		while (printerResults.next() ){
			printerGroupId.put(printerResults.getString("hostname"), printerResults.getInt("printer_group_id"));
			String pgName=printerResults.getString("name");
			int pgCost = printerResults.getInt("value");
			printerGroupName.put(printerResults.getInt("printer_group_id"), pgName);
			pageCost.put(printerResults.getString("hostname"), pgCost);
			if (!pageCostByGroupName.containsKey(pgName)) pageCostByGroupName.put(pgName, pgCost);
		}
		printerResults.close();
		printerLookup.close();
		if (connection != null && !connection.isClosed() ) connection.close();
		log.trace("Finished Instantiating Pagecontrol object");
		
	}
	
	public void setDatabaseBypass(boolean databaseBypass){
		if (databaseBypass){
			log.warn("Enabling Database Bypass: all queries will return allowed");
		}else{
			log.warn("Disabling Database Bypass: resuming normal operation");
		}
		this.databaseBypass = databaseBypass;
	}
	
	public boolean getDatabaseBypass(){ return this.databaseBypass; }
	
	public void setPurchaseEnabled(boolean purchaseEnabled){
		if (purchaseEnabled){
			log.warn("Enabling the purchase of printing dollars");
		}else{
			log.warn("Disabling the purchase of printing dollars");
		}
		this.purchaseEnabled = purchaseEnabled;
	}
	
	public boolean getPurchaseEnabled() { return this.purchaseEnabled; }
	
	
	public String canonicalize(String username) throws AffiliationException{

		log.trace("Entry into Pagecontrol.canonicalize()");
		String uni="UNKNOWN_UNI";
		
		try {
			uni = affilCache.canonicalize(username);
		} catch (AffiliationException e) {
			switch (e.getCondition()){
				case AffiliationException.UNKNOWN_UNI:
					throw e;
					
				default:
					log.debug("Unhandled AffiliationException in canonicalize", e);
					break;
			}
		}finally{
			log.trace("Exiting Pagecontrol.canonicalize()");
		}
		return uni;
	}
	
	public List<String> getPrinters(){
		
		// Copy the complete list of printers
		ArrayList<String> printerList = new ArrayList<String>(printerGroupId.size());
		printerList.addAll(printerGroupId.keySet());
		
		return printerList;
	}
	
	
	public boolean query(String printer, String username, int pages){
		
		// A query is a simple request to see if there are enough pages available
		// to print the requested document.  As such the full pagecount is not consulted,
		// and short circuit logic is used.   

		log.trace("Entry into Pagecontrol.query()");
		String logmsg = printer + " QUERY (" + username + ") " + Integer.toString(pages); 
		querylog.info(logmsg);
				
		if (databaseBypass) return true;
		
		int amountRequired = pages * pageCost.get(printer);
		
		HashMap<String,Integer> usage = new HashMap<String,Integer>();

		String ldapAffils = "IN ( 'NULLSET' )";
		String uni = username;
		boolean skipQuota = false;
		
		try {
			// Moved outside of the SQLException try to prevent the hanging
			// of an ESTABLISHED Oracle connection due to ldap connection
			// pool deadlock
			uni = affilCache.canonicalize(username);
			ldapAffils = affilCache.formatAffilsInSqlSet(uni);
			
		} catch (AffiliationException e) {
			switch (e.getCondition()){
			case AffiliationException.UNKNOWN_UNI:
				return false;
				
			case AffiliationException.NO_AFFILS:
				skipQuota = true;
				break;
				
			default:
				log.debug(e);
				break;
			}
		}

		
		Connection connection = null;
		try{
			connection = ods.getConnection();
					
			PreparedStatement dollarQuery = connection.prepareStatement("SELECT amount FROM dollar WHERE uni = ?");
			dollarQuery.setString(1, uni);
			ResultSet dollars = dollarQuery.executeQuery();
			
			if ( dollars.next()){
				amountRequired -= dollars.getInt("amount"); 
			}
			dollars.close();
			dollarQuery.close();

			// If there are enough printing dollars to cover the page cost, don't
			// bother checking the quota usage now.  This logic is only for the
			// query and should not be used in the deduct accounting.
			if (amountRequired <= 0 ) return true;

			// Early short-circuit if user has no Affiliations
			if (skipQuota) return false;
			
			
			PreparedStatement usageQuery = connection.prepareStatement("SELECT quota_id, amount FROM usage WHERE uni = ? ORDER BY quota_id");
			usageQuery.setString(1, uni);
			ResultSet usageResult = usageQuery.executeQuery();

			while(usageResult.next()){
				usage.put(usageResult.getString("quota_id"), new Integer(usageResult.getInt("amount")));
			}
			usageResult.close();
			usageQuery.close();

			PreparedStatement quotaQuery = connection.prepareStatement(
					"SELECT q.quota_id, q.amount FROM quotaassign qa, quota q, affiliation a " +
					"WHERE a.affiliation_id = qa.affiliation_id AND qa.quota_id = q.quota_id AND qa.printer_group_id = ? " +
					" AND a.ldap_name "+ ldapAffils + " ORDER BY q.quota_id");
			quotaQuery.setInt(1,  printerGroupId.get(printer));
			
			ResultSet quotaResult = quotaQuery.executeQuery();

			int quotaAvailable = 0;
			while (quotaResult.next()){
				String quota_id = quotaResult.getString("quota_id");
				if (usage.containsKey(quota_id)){
					quotaAvailable += quotaResult.getInt("amount") - usage.get(quota_id);
				}else{
					quotaAvailable += quotaResult.getInt("amount");
				}
			}
			quotaResult.close();
			quotaQuery.close();
			
			if (quotaAvailable >= amountRequired) return true;		

			return false;

		}catch(SQLException e){
			log.error(e);
			return false;
		}finally{
			try {
				if (connection != null && !connection.isClosed() ) connection.close();
			} catch (SQLException e) {
					log.warn("Caught SQLException while trying to close DB connection in query", e);
			}
			log.trace("Exiting Pagecontrol.query()");
		}
    } 
	
	
	// Deduct does not throw any errors as the handling of the charge should be atomic
	// All exceptions are to be caught and handled internally.  In the event of a Database
	// bypass, all page deduction transactions will be written to a logfile.
	public void deduct(String printer, String username, int pages){
		
		log.trace("Entry into Pagecontrol.deduct()");
		boolean skipQuota = false;
		
		String logmsg = printer + " DEDUCT (" + username + ") " + Integer.toString(pages); 
		querylog.info(logmsg);
		
		if (databaseBypass){
			log.warn("Bypassing database, transaction written to logfile");
			transact.info(logmsg);
			return;
		}
		
		String uni;
		String ldapAffils= " IN ( 'NULLSET' )";
		
		// initialize the full amount to be deducted from quotas
		final int cost = pageCost.get(printer);
		int amountPending = pages * cost;
		
		// Try to get the canonical uni from the given username
		try {
			uni = affilCache.canonicalize(username);
		
		} catch (AffiliationException e) {

			switch(e.getCondition()){
				case AffiliationException.UNKNOWN_UNI:
					log.debug(e);
					transact.info(logmsg);
					return;
			
				default:
					log.debug("Unhandled AffiliationException caught during canonicalize", e);
					uni = username;
					break;
			}
		}
		
		try {
			ldapAffils = affilCache.formatAffilsInSqlSet(uni);
		} catch (AffiliationException e) {
			switch(e.getCondition()){
				case AffiliationException.NO_AFFILS:
					skipQuota = true;
					break;
	
				default:
					log.debug("Unhandled AffiliationException in deduct", e);
					break;					
			}
		}
		
		// Connection moved outside of the try block to enable a finally statement for closing the resource
		Connection connection = null;
		try {

			List<String> quotaOrder = new LinkedList<String>();
			HashMap<String, Integer> quotaMap= new HashMap<String,Integer>();
			HashMap<String, Integer> usageMap= new HashMap<String,Integer>();
			
			connection = ods.getConnection();
			connection.setAutoCommit(false);
			PreparedStatement usageUpdate  = connection.prepareStatement("UPDATE usage SET amount = ? WHERE uni = ? AND quota_id = ?");
			PreparedStatement usageInsert  = connection.prepareStatement("INSERT INTO usage ( amount, uni, quota_id) VALUES ( ?, ?, ?)");
			PreparedStatement dollarUpdate = connection.prepareStatement("UPDATE dollar SET amount = amount - ? WHERE uni = ?");

			
			boolean doUpdate = false;
			boolean doInsert = false;
			boolean doDollar = false;

			if (!skipQuota){
				PreparedStatement quotaQuery = connection.prepareStatement(
					"SELECT q.quota_id, q.amount FROM quotaassign qa, quota q, affiliation a " +
					"WHERE a.affiliation_id = qa.affiliation_id AND qa.quota_id = q.quota_id AND qa.printer_group_id = ? " +
					" AND a.ldap_name "+ ldapAffils + " ORDER BY q.quota_id");
				quotaQuery.setInt(1,  printerGroupId.get(printer));

				ResultSet quotaResult = quotaQuery.executeQuery();
			
				while (quotaResult.next()){
					quotaMap.put(quotaResult.getString(1), new Integer(quotaResult.getInt(2)));
					quotaOrder.add(quotaResult.getString(1));
				}
				quotaResult.close();
				quotaQuery.close();
			
				PreparedStatement usageQuery = connection.prepareStatement("SELECT quota_id, amount FROM usage WHERE uni = ? ORDER BY quota_id");
				usageQuery.setString(1, uni);
				ResultSet usageResult = usageQuery.executeQuery();			

				while (usageResult.next()){
					usageMap.put(usageResult.getString("quota_id"), new Integer(usageResult.getInt("amount")));				
				}
				usageResult.close();
				usageQuery.close();

				usageUpdate.setString(2, uni);
				usageInsert.setString(2, uni);
			
				for (String quotaId: quotaOrder){
					if (amountPending < 1) break;
					int amountAvailable = quotaMap.get(quotaId);
					if (usageMap.containsKey(quotaId)) amountAvailable -= usageMap.get(quotaId);
					if (amountAvailable > 0){
						int newAmount = (amountAvailable > amountPending)? amountPending : amountAvailable;
						amountPending -= newAmount;
						if (usageMap.containsKey(quotaId)){
							newAmount += usageMap.get(quotaId);
							usageUpdate.setInt(1, newAmount);
							usageUpdate.setInt(3, Integer.parseInt(quotaId));
							usageUpdate.addBatch();
							doUpdate = true;
						}else{
							usageInsert.setInt(1, newAmount);
							usageInsert.setInt(3, Integer.parseInt(quotaId));
							usageInsert.addBatch();
							doInsert = true;
						}
					}
				}

			}
			
			if (amountPending > 0){
				// Quotas have been exhausted, using printing dollars 
				dollarUpdate.setInt(1,amountPending);
				dollarUpdate.setString(2,uni);
				doDollar = true;
			}
	
			PreparedStatement printlogInsert = connection.prepareStatement(
			     "INSERT INTO printlog ( print_log_id, print_date, uni, printer, amount, department, amount_paid ) " +
                         "VALUES ( print_log_seq.nextval, SYSDATE, ?, ?, ?, ?, ? )");
			printlogInsert.setString(1, uni);
			printlogInsert.setString(2, printer);
			printlogInsert.setInt(3, pages);
			printlogInsert.setString(4, department);
			printlogInsert.setInt(5, amountPending);

			
			// Deducts should be atomic rollback to previous state if any
			// of the updates fail or update more than a single row.
			boolean updatedCleanly = true;
			Savepoint preDeduct = connection.setSavepoint();

			try{
			   if (doUpdate){
				  int[] batchStatus = usageUpdate.executeBatch();
				  for (int rowsUpdated: batchStatus){
					   if ( (rowsUpdated == Statement.EXECUTE_FAILED) || (rowsUpdated > 1) ) updatedCleanly = false;
				  }
			   }

			   // Inserts by definition can only update one row
			   if (doInsert){
				  int[] batchStatus = usageInsert.executeBatch();
				  for (int rowsUpdated: batchStatus){
					   if ( rowsUpdated == Statement.EXECUTE_FAILED ) updatedCleanly = false;
				  }
			   }
					
			   if (doDollar){
				  int rowsUpdated = dollarUpdate.executeUpdate();
				  if ( rowsUpdated == Statement.EXECUTE_FAILED ) updatedCleanly = false;
			   }						

			   if (updatedCleanly){
				  int rowsUpdated = printlogInsert.executeUpdate();
				  if ( rowsUpdated == Statement.EXECUTE_FAILED ) updatedCleanly = false;				   
			   }
			   
			// Nested catch for exception handling of the deduct updates with rollback
			}catch(BatchUpdateException bu){
				log.error("Caught a BatchUpdateException, rolling back:", bu);				
				connection.rollback(preDeduct);
			} catch (SQLException e1) {
				log.error("Caught an SQLException, rolling back", e1);
				connection.rollback(preDeduct);
			} finally {
				if (updatedCleanly){
					connection.commit();
				}else{
					log.error("One of the deduct updates failed, rolling back");
					transact.error(logmsg);
					connection.rollback(preDeduct);
				}
				usageUpdate.close();
				usageInsert.close();
				dollarUpdate.close();
				printlogInsert.close();
			}
			
		}catch (SQLException e){
			log.error("Caught an SQLException before update attempted, no need to roll back", e);
		}finally{
			try {
				if (connection != null && !connection.isClosed() ) connection.close();
			} catch (SQLException e) {
				log.warn("Caught SQLException while trying to close DB connection in deduct", e);
			}
			log.trace("Exiting Pagecontrol.deduct()");
		}
	
	}
	
	
	public DocumentFragment printerGroups(Document parentDoc){

		log.trace("Entry into Pagecontrol.printerGroups()");
		HashMap<Integer, Element> printerGroupElementMap= new HashMap<Integer, Element>();
		Element printerGroups = parentDoc.createElement("printerGroupInfo");

		for (Integer groupId: printerGroupName.keySet() ){
			Element groupNameElement = parentDoc.createElement("printerGroup");
			groupNameElement.setAttribute("groupname",printerGroupName.get(groupId));
			groupNameElement.setAttribute("printer_group_id", groupId.toString());
			printerGroupElementMap.put(groupId, groupNameElement);
			printerGroups.appendChild(groupNameElement);
		}

						
		for (String printer: printerGroupId.keySet()){
			Element printerElement = parentDoc.createElement("printer");
			printerElement.appendChild(parentDoc.createTextNode(printer));
			Element groupNameElement = printerGroupElementMap.get(printerGroupId.get(printer));
			groupNameElement.appendChild(printerElement);
		}

		DocumentFragment retDoc =  parentDoc.createDocumentFragment();		
		retDoc.appendChild(printerGroups);

		log.trace("Exiting Pagecontrol.printerGroups()");
		return retDoc;
			
	}
	
	
	public DocumentFragment quota(Document parentDoc, String username){
		
		log.trace("Entry into Pagecontrol.quota()");
		String uni = username;
		String ldapAffils = null;
	
		DocumentFragment quotaInfo = parentDoc.createDocumentFragment();

		
		//TODO: needs to respect databaseBypass
		Connection connection = null;
		try{
			connection = ods.getConnection();
	
		} catch (SQLException e) {
			log.error(e);
			Element errorElement = parentDoc.createElement("error");
			errorElement.setAttribute("severity", "fatal");
			errorElement.setAttribute("condition", "SQLException");
			quotaInfo.appendChild(errorElement);
			return quotaInfo;
		}
		
		Element uniElement = parentDoc.createElement("uni");

		try{
			uni = affilCache.canonicalize(username);

			if (uni == null){
				log.error("null value returned from canonicalize(" + username + ") continuing with username=" + username);
				uni=username;
			}
			uniElement.setAttribute("username", uni);
			quotaInfo.appendChild(uniElement);
			ldapAffils = affilCache.formatAffilsInSqlSet(uni);

			Element quotaList = parentDoc.createElement("quotaList");
			uniElement.appendChild(quotaList);
			
			HashMap<String, Integer> usageAmount= new HashMap<String,Integer>();
			
			PreparedStatement usageQuery  = connection.prepareStatement("SELECT quota_id, amount FROM usage WHERE uni = ?");
			usageQuery.setString(1,uni);
			ResultSet usageResult = usageQuery.executeQuery();
			while (usageResult.next()){
				usageAmount.put(usageResult.getString(1), usageResult.getInt(2));				
			}

			PreparedStatement quotaQuery = connection.prepareStatement(
					"SELECT q.quota_id, q.amount, qa.printer_group_id, q.duration FROM quota q, quotaassign qa, affiliation a " +
					"WHERE q.quota_id=qa.quota_id AND qa.affiliation_id=a.affiliation_id AND a.ldap_name " +
					ldapAffils + " ORDER BY q.duration");
			
			ResultSet quotaResult = quotaQuery.executeQuery();
			
			HashMap<String, Element> quotaElementMap    = new HashMap<String, Element>();
			HashMap<String, Integer> combinedWeekly     = new HashMap<String, Integer>();
			HashMap<String, Integer> combinedSemesterly = new HashMap<String, Integer>();

			while (quotaResult.next()){
				String quotaId = quotaResult.getString(1);
				int amount = quotaResult.getInt(2);
				int pgId = quotaResult.getInt(3);
				String pgName = printerGroupName.get(pgId);			//TODO: returns null for MCBlackAndWhite(3) & MCColor(4) 
				int duration = quotaResult.getInt(4);

				if (log.isDebugEnabled()){
				    log.debug("quotaId:" + quotaId + "   amount:" + Integer.toString(amount) + "   pgId:" + Integer.toString(pgId) + 
						"  pgName: " +pgName  + "  duration:" + Integer.toString(duration));
				}
				int used = 0;
				if (usageAmount.containsKey(quotaId)) used = usageAmount.get(quotaId);
				int remaining = amount - used;

				
				if (!quotaElementMap.containsKey(quotaId)){

					Element quotaElement = parentDoc.createElement("quota");
					quotaElement.setAttribute("quotaId", quotaId);
					quotaElement.setAttribute("amount", Integer.toString(amount));
					quotaElement.setAttribute("duration", Integer.toString(duration));							
					quotaElement.setAttribute("used", Integer.toString(used));
					quotaElement.setAttribute("remaining", Integer.toString(remaining));
					quotaElementMap.put(quotaId, quotaElement);
					quotaList.appendChild(quotaElement);
				}

				
				
				// Update the aggregated quotas used for presentation
				if (duration > 7){
					if (combinedSemesterly.containsKey(pgName)){
						int updatedRemaining = combinedSemesterly.get(pgName);
						updatedRemaining += remaining;
						combinedSemesterly.put(pgName, updatedRemaining);
					}else{
						combinedSemesterly.put(pgName, remaining);			
					}
				}else{
					if (combinedWeekly.containsKey(pgName)){
						int updatedRemaining = combinedWeekly.get(pgName);
						updatedRemaining += remaining;
						combinedWeekly.put(pgName, updatedRemaining);
					}else{
						combinedWeekly.put(pgName, remaining);			
					}				
				}
				
				// Mark each printer group allowed for an individual quota by
				// adding an empty child node of the same name as the aggregated quota
				Element pgTag = parentDoc.createElement(printerGroupName.get(pgId));	//TODO: causes the NullPointerException
				quotaElementMap.get(quotaId).appendChild(pgTag);
			}
			
			
			Element aggregate = parentDoc.createElement("aggregatedQuotas");
			uniElement.appendChild(aggregate);
			
			for (String pgName: printerGroupName.values()){
				
				if ( combinedWeekly.containsKey(pgName) || combinedSemesterly.containsKey(pgName)){
					
					Element combined = parentDoc.createElement(pgName);
					Element weekly = parentDoc.createElement("weekly");
					Element semesterly = parentDoc.createElement("semesterly");
					Integer remainingWeek = new Integer(0);
					Integer remainingSemester = new Integer(0);
					
					if (combinedWeekly.containsKey(pgName)) remainingWeek = combinedWeekly.get(pgName) 
					/ pageCostByGroupName.get(pgName);
					if (combinedSemesterly.containsKey(pgName)) remainingSemester = combinedSemesterly.get(pgName)
					/ pageCostByGroupName.get(pgName);
					
					weekly.setAttribute("remaining", remainingWeek.toString());
					semesterly.setAttribute("remaining", remainingSemester.toString());
					combined.appendChild(weekly);
					combined.appendChild(semesterly);
					aggregate.appendChild(combined);
				}
			}				
			

			
		}catch(AffiliationException e){
			Element errorElement = parentDoc.createElement("error");
			switch(e.getCondition()){
				case AffiliationException.UNKNOWN_UNI:
					log.debug(e);
					errorElement.setAttribute("severity", "fatal");
					errorElement.setAttribute("condition", "unknown username");
					break;
					
				case AffiliationException.NO_AFFILS:
					log.debug(e);
					errorElement.setAttribute("severity", "minor");
					errorElement.setAttribute("condition", "no quotas");
					break;

				default:
					log.debug("Unhandled AffiliationException in quota",e);
					break;
			}
			quotaInfo.appendChild(errorElement);
			
		} catch (SQLException e) {
			log.error(e);
			Element errorElement = parentDoc.createElement("error");
			errorElement.setAttribute("severity", "fatal");
			errorElement.setAttribute("condition", "SQLException");
			quotaInfo.appendChild(errorElement);
		}	
		
		try{
			PreparedStatement dollarQuery = connection.prepareStatement("SELECT amount FROM dollar WHERE uni = ?");
			dollarQuery.setString(1, uni);
		
			ResultSet dollarResult = dollarQuery.executeQuery();
			while (dollarResult.next()){
				Element dollarElement = parentDoc.createElement("dollars");
				int rawDollars = dollarResult.getInt(1);
				float amount = rawDollars / 100f;
				NumberFormat currencyFormat=  DecimalFormat.getCurrencyInstance(java.util.Locale.US);
				dollarElement.setAttribute("amount", currencyFormat.format(amount));
			
				for ( String pgName: pageCostByGroupName.keySet() ){
					int cost = pageCostByGroupName.get(pgName);
					float currencyCost = cost / 100f; 
					int available = (cost == 0)? Integer.MAX_VALUE : rawDollars / cost; 
					Element pagecost = parentDoc.createElement(pgName);
					pagecost.setAttribute("cost", Integer.toString(cost));
					pagecost.setAttribute("currencyCost", currencyFormat.format(currencyCost));
					pagecost.setAttribute("available", Integer.toString(available));
					dollarElement.appendChild(pagecost);
				}
			
				uniElement.appendChild(dollarElement);
			}
			
		}catch(SQLException e){
			log.error(e);
			Element errorElement = parentDoc.createElement("error");
			errorElement.setAttribute("severity", "major");
			errorElement.setAttribute("condition", "Unable to obtain dollar amount");
			quotaInfo.appendChild(errorElement);			
		}finally{
			try {
				if (connection != null && !connection.isClosed()) connection.close();
				} catch (SQLException e) {
					log.warn("Caught SQLException while closing DB Connection in quota", e);
				}
				log.trace("Exiting Pagecontrol.quota()");
		}

		return quotaInfo;	
	}

	
	
	
	public DocumentFragment history(Document parentDoc, String uni, int daysPrevious){
			
		log.trace("Entry into Pagecontrol.history()");
		DocumentFragment historyFrag = parentDoc.createDocumentFragment();
		DateFormat printdateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

		
		//TODO: needs to respect databaseBypass
		Connection connection = null;
		try {
			connection = ods.getConnection();
			Statement sql = connection.createStatement();
			
			// Date is formatted by Oracle to keep the full accuracy, as the java.sql.Date class does not include time information
			PreparedStatement historyQuery = connection.prepareStatement(
					"SELECT print_date, print_log_id, amount, amount_paid, printer FROM printlog WHERE uni= ? AND print_date > SYSDATE - ?");  
			historyQuery.setString(1, uni);
			historyQuery.setInt(2, daysPrevious);
			ResultSet historyResults = historyQuery.executeQuery(); 
			
			
			Element historyElement = parentDoc.createElement("history");
			historyElement.setAttribute("uni", uni);
			historyElement.setAttribute("daysPrevious", Integer.toString(daysPrevious));
			
			while (historyResults.next()){

				// Use GMT for retrieval of Date / Time from the database 
				Calendar printDate = Calendar.getInstance();
				Calendar sqlDateOffset = Calendar.getInstance();
				
				//TODO: This should be replaced by java.sql.Timestamp
				Date sqlPrintDate = historyResults.getDate("print_date");				
				Time sqlPrintTime = historyResults.getTime("print_date");
								
				printDate.setTimeInMillis(sqlPrintTime.getTime());
				sqlDateOffset.setTimeInMillis(sqlPrintDate.getTime());
				printDate.set(sqlDateOffset.get(Calendar.YEAR), sqlDateOffset.get(Calendar.MONTH),
						sqlDateOffset.get(Calendar.DAY_OF_MONTH));
				
				String printLogId = historyResults.getString("print_log_id");
				String pages = historyResults.getString("amount");
				String amountPaid = historyResults.getString("amount_paid");
				String printer = historyResults.getString("printer");
				
				Element transactionElement = parentDoc.createElement("transaction");
				transactionElement.setAttribute("type", "print");
				transactionElement.setAttribute("id", printLogId);
				transactionElement.setAttribute("timestamp", Long.toString(printDate.getTimeInMillis()));
				transactionElement.setAttribute("printer", printer);
				transactionElement.setAttribute("amount", pages);
				transactionElement.setAttribute("amount_paid", amountPaid);
				transactionElement.setIdAttribute("id", true);
				transactionElement.setTextContent(printdateFormat.format(printDate.getTime()));
				historyElement.appendChild(transactionElement);
			}
			historyResults.close();
			historyQuery.close();

			PreparedStatement salelogQuery = connection.prepareStatement(
					"SELECT s.sale_date, s.sale_log_id, s.amount, s.type, s.vendor, c.print_log_id " +
					   "FROM salelog s left outer join credit c on s.sale_log_id = c.sale_log_id WHERE uni = ? AND sale_date > SYSDATE - ?");
			salelogQuery.setString(1, uni);
			salelogQuery.setInt(2, daysPrevious);
			
			historyResults = salelogQuery.executeQuery();
						
			while (historyResults.next()){
				Calendar saleDate = Calendar.getInstance();
				Calendar sqlDateOffset = Calendar.getInstance();

				//TODO: This should be replaced by java.sql.Timestamp
				Date sqlSaleDate = historyResults.getDate(1);
				Time sqlSaleTime = historyResults.getTime(1);
				
				saleDate.setTimeInMillis(sqlSaleTime.getTime());
				sqlDateOffset.setTimeInMillis(sqlSaleDate.getTime());
				saleDate.set(sqlDateOffset.get(Calendar.YEAR), sqlDateOffset.get(Calendar.MONTH),
						sqlDateOffset.get(Calendar.DAY_OF_MONTH));
				
				String saleLogId = historyResults.getString(2);
				String amount = historyResults.getString(3);
				String type = historyResults.getString(4);
				String vendor = historyResults.getString(5);
				
				Element transactionElement = parentDoc.createElement("transaction");
				transactionElement.setAttribute("type", type);
				transactionElement.setAttribute("id", saleLogId);
				transactionElement.setAttribute("timestamp", Long.toString(saleDate.getTimeInMillis()));
				transactionElement.setAttribute("amount",amount);
				transactionElement.setAttribute("vendor", vendor);
				if ("credit".equals(type)){
					String printLogId = historyResults.getString(6);
					transactionElement.setAttribute("voided", printLogId);
				}
				transactionElement.setIdAttribute("id", true);
				transactionElement.setTextContent(printdateFormat.format(saleDate.getTime()));
				historyElement.appendChild(transactionElement);
			}
			historyResults.close();
			salelogQuery.close();
			
			historyFrag.appendChild(historyElement);
			
		} catch (SQLException e) {
			log.error(e);
			Element errorElement = parentDoc.createElement("error");
			errorElement.appendChild(parentDoc.createTextNode("Unable to process history request"));
			Element exceptionElement = parentDoc.createElement("sqlException");
			exceptionElement.appendChild(parentDoc.createTextNode(e.getMessage()));
			errorElement.appendChild(exceptionElement);
			
			historyFrag.appendChild(errorElement);
		}finally{
			try {
				if (connection != null && !connection.isClosed() ) connection.close();
			} catch (SQLException e) {
				log.warn("Caught SQLException while trying to close DB connection in history", e);
			}
			log.trace("Exiting Pagecontrol.history()");
		}
		
		return historyFrag;
		
	}
	
	public boolean sale(String invoiceNum, String uni, String vendor, int amount){
		boolean saleProcessed = false;

		log.trace("Entry into Pagecontrol.sale()");
		StringBuffer sb = new StringBuffer(80);
		sb.append("SALE invoice:");
		sb.append(invoiceNum);
		sb.append("  UNI:");
		sb.append(uni);
		sb.append("  vendor:");
		sb.append(vendor);
		sb.append("  amount:");
		sb.append(amount);
		receipts.info(sb.toString());

		
		//TODO:  needs to respect databaseBypass
		Connection connection = null;
		try {
		   connection = ods.getConnection();
		   connection.setAutoCommit(false);
		   PreparedStatement dollarQuery = connection.prepareStatement("SELECT amount FROM dollar WHERE uni = ?");
		   dollarQuery.setString(1, uni);
		   ResultSet dollarResult = dollarQuery.executeQuery();

		   PreparedStatement dollarSql;
		   
		   if (dollarResult.next()){
			   dollarSql = connection.prepareStatement( "UPDATE dollar SET amount = amount + ? WHERE uni = ? AND department_id = ?");
		   }else{
			   dollarSql = connection.prepareStatement( "INSERT INTO dollar ( amount, uni, department_id ) VALUES ( ?, ?, ? )" );
		   }
		   dollarQuery.close();

		   dollarSql.setInt(1, amount);
		   dollarSql.setString(2, uni);
		   dollarSql.setInt(3, departmentId);

		   Savepoint preSale = connection.setSavepoint();		   
		   log.debug("Created savepoint for rollback before updating dollar table");
		   int updateStatus = -1;
		   int salelogStatus = -1;
		   
		     try{  // salelog try/catch beginning
		       updateStatus = dollarSql.executeUpdate();
		       log.debug("Updated printing dollars");

		       if ( updateStatus==1 ) {
		    	  log.debug("Preparing to insert into the salelog table");
			      PreparedStatement salelogInsert = connection.prepareStatement(
					   "INSERT INTO salelog ( sale_log_id, uni, vendor, amount, sale_date, type, department ) VALUES ( ?, ?, ?, ?, SYSDATE, ?, ? )" );
			      salelogInsert.setInt(1, Integer.parseInt(invoiceNum));
			      salelogInsert.setString(2, uni);
			      salelogInsert.setString(3, vendor);
			      salelogInsert.setInt(4, amount);
			      salelogInsert.setString(5, "sale");
			      salelogInsert.setString(6, department);

			      salelogStatus = salelogInsert.executeUpdate();
			   
			      if ( salelogStatus == 1){
			    	  log.debug("Committing update.");
				     connection.commit();
				     saleProcessed = true;
			      }else{
			    	  log.debug("Unable to update Salelog, rolling back");
				     connection.rollback(preSale);
			      }
			      salelogInsert.close();
		       }else{
			      connection.rollback(preSale);
		       }
		       dollarSql.close();		   
		  }catch(SQLIntegrityConstraintViolationException integrityException){

			// If the sale violated constraints, just rollback and don't bother
			// updating the salelog table
			if (updateStatus != 1){
				connection.rollback(preSale);
				return false;
			}
			
			// The Integrity exception is likely do the violation of the salelog ID unique constraint
			// this now occurs after [multiple] rejections and a single successful sale; unfortuately
			// all of the posted transactions, even the rejections, will have the same salelog_id
			if (salelogStatus != 1){
				if (amount > 0){
					log.info("Overwriting previous sale information");
					
					 PreparedStatement salelogOverwrite = connection.prepareStatement(
							   "UPDATE salelog SET amount = ?  WHERE sale_log_id = ?" );
					 salelogOverwrite.setInt(1, amount);
					 salelogOverwrite.setInt(2, Integer.parseInt(invoiceNum));
					 salelogOverwrite.executeUpdate();
				}else{
					log.info("Another REJECTED transaction has been submitted for invoiceNumber:" + invoiceNum);
					connection.rollback(preSale);
				}
				return true;
			}
	
		  }catch (SQLException e1){
			log.error("An SQLException Occurred during the updating of sale invoice:" + invoiceNum + ", rolling back", e1);
			connection.rollback(preSale);
		  } // End of Salelog Inner try/catch
		     
		}catch (SQLException e){
			log.error("An SQLException Occurred during the processing of sale invoice:" + invoiceNum, e);				

		
		}finally{

			try {
				if (connection != null && !connection.isClosed()) connection.close();
			} catch (SQLException e) {
				log.warn("Caught SQLException while closing DB connection in sale", e);
			}
			log.trace("Exiting Pagecontrol.sale()");
		}
		return saleProcessed;
	}
	
	
	public boolean credit(String transactionId, String vendor) {
		
		log.trace("Entry into Pagecontrol.credit()");
		
		String uni;
		String printer;
		String saleLogId;
		int pagesPrinted;
		int creditAmount;
		Connection connection = null;

		//TODO: needs to respect databaseBypass
		try{
		connection = ods.getConnection();
		connection.setAutoCommit(false);
		
		
		PreparedStatement transactionSelect = connection.prepareStatement("SELECT uni, printer, amount FROM printlog WHERE print_log_id = ?");
		transactionSelect.setLong(1, Long.parseLong(transactionId));
		ResultSet transaction = transactionSelect.executeQuery();
		
		if (transaction.next()){
			uni = transaction.getString(1);
			printer = transaction.getString(2);
			pagesPrinted = transaction.getInt(3);
		}else{
			log.info("Transaction not found");
			return false;
		}
		transactionSelect.close();

		PreparedStatement prevCreditSelect = connection.prepareStatement("SELECT print_log_id, sale_log_id FROM credit WHERE print_log_id = ?");
		prevCreditSelect.setLong(1, Long.parseLong(transactionId));
		ResultSet prevCreditResults = prevCreditSelect.executeQuery();
		
		if (prevCreditResults.next()){
			saleLogId = prevCreditResults.getString(2);
			StringBuffer sb = new StringBuffer();
			sb.append("Unable to credit transaction ");
			sb.append(transactionId);
			sb.append(" as it was previously credited under salelogId:");
			sb.append(saleLogId);
			log.info(sb.toString());
			prevCreditResults.close();
			prevCreditSelect.close();

			return false;
		}
		prevCreditResults.close();
		prevCreditSelect.close();		
		
		creditAmount = pageCost.get(printer) * pagesPrinted;
		saleLogId = nextSaleId();
		}catch (SQLException e){
			log.error("Caught SQLException prior to update transactions in Pagecontrol.credit() ",e);
			try{ if (!connection.isClosed()) connection.close();
			} catch (SQLException e1) { log.warn(e); }
			return false;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append(vendor);
		sb.append(" requesting credit for transaction ");
		sb.append(transactionId);
		sb.append(" on behalf of ");
		sb.append(uni);
		log.info(sb.toString());
		
		Savepoint preCredit = null;

		try{
			connection.setAutoCommit(false);
			preCredit = connection.setSavepoint();
			// Passed the fall through logic, process the credit from here out
			PreparedStatement creditTransaction = connection.prepareStatement("UPDATE dollar set amount = amount + ? WHERE uni = ?");
			creditTransaction.setInt(1, creditAmount);
			creditTransaction.setString(2, uni);
			int rowCount = creditTransaction.executeUpdate();
			creditTransaction.close();
			if ( rowCount == Statement.EXECUTE_FAILED || rowCount > 1){
				log.warn("dollar UPDATE did not complete successfully, rolling back and exiting");
				connection.rollback(preCredit);
				return false;
			}else if ( rowCount == 0){
				// workaround if there is not yet an entry in the dollar table for a given user
				creditTransaction.close();
				creditTransaction = connection.prepareStatement("INSERT INTO dollar (uni, amount, department_id) VALUES ( ?, ?, ?)");
				creditTransaction.setString(1, uni);
				creditTransaction.setInt(2, creditAmount);
				creditTransaction.setInt(3, departmentId);
				rowCount = creditTransaction.executeUpdate();
				if ( rowCount == Statement.EXECUTE_FAILED ){
					connection.rollback(preCredit);
					return false;
				}
			}
			log.debug("Updated dollar amount");
			
			PreparedStatement saleLogInsert = connection.prepareStatement("INSERT INTO salelog (sale_log_id, uni, vendor, amount, sale_date, type, department) VALUES ( ?, ?, ?, ?, SYSDATE, ?, ?)");
			saleLogInsert.setLong(1,Long.parseLong(saleLogId));
			saleLogInsert.setString(2, uni);
			saleLogInsert.setString(3, vendor);
			saleLogInsert.setInt(4,creditAmount);
			saleLogInsert.setString(5, "credit");
			saleLogInsert.setInt(6, departmentId);
			rowCount = saleLogInsert.executeUpdate();
			saleLogInsert.close();
			if (rowCount == Statement.EXECUTE_FAILED || rowCount > 1){
				log.warn("salelog INSERT did not complete successfully, rolling back and exiting");
				connection.rollback(preCredit);
				return false;
			}
			log.debug("Inserted entry into the salelog table for the credit operation");
			
			PreparedStatement creditLogInsert = connection.prepareStatement("INSERT INTO credit (sale_log_id, print_log_id) VALUES ( ?, ?)");
			creditLogInsert.setLong(1, Long.parseLong(saleLogId));
			creditLogInsert.setLong(2, Long.parseLong(transactionId));
			rowCount = creditLogInsert.executeUpdate();
			creditLogInsert.close();
			if ( rowCount == Statement.EXECUTE_FAILED || rowCount > 1){
				log.warn("credit INSERT did not complete successfully, rolling back and exiting");
				connection.rollback(preCredit);
				return false;
			}
			log.debug("Inserted mapping into credit table mapping the saleId");

			connection.commit();
			log.debug("Committed credit transactions successfully");
			connection.setAutoCommit(true);
		
			return true;

		}catch(SQLException e){
			log.error("Caught an SQLException during the update transactions of Pagecontrol.credit(), rolling back", e);
			try{ if (preCredit != null) connection.rollback(preCredit);}catch (SQLException e1){ log.warn(e); }
			return false;
		}finally{
			try { if (!connection.isClosed()) connection.close(); }catch (SQLException e1){ log.warn(e1); }
		}
	}
	
		
	public boolean creditOverride(String uni, String vendor, float dollarsRequested, String justification){

		log.trace("Entry into Pagecontrol.creditOverride()");
		
		int  creditRequested = (int) ( dollarsRequested * 100 );
		StringBuffer sb = new StringBuffer(256);
		sb.append(vendor);
		sb.append(" requesting credit OVERRIDE for ");
		sb.append(uni);
		sb.append(" in the amount of $");
		sb.append(dollarsRequested);
		sb.append("justification: \"");
		sb.append(justification);
		sb.append('"');
		receipts.info(sb.toString());

		
		//TODO: needs to respect databaseBypass
		Connection connection = null;
		try{
			connection = ods.getConnection();
			connection.setAutoCommit(false);
			
			Savepoint preCredit = connection.setSavepoint();

			PreparedStatement creditUpdate = connection.prepareStatement(
			"UPDATE dollar SET amount = amount + ? WHERE uni = ?");
			creditUpdate.setString(2, uni);
			creditUpdate.setInt(1, creditRequested);
	
			int rowsUpdated = creditUpdate.executeUpdate();
			creditUpdate.close();

			if ( rowsUpdated == Statement.EXECUTE_FAILED || rowsUpdated > 1 ){
				 log.error("Caught an unexpected result from the dollar update, rolling back.");
				 connection.rollback(preCredit);
				 return false;
			}else if ( rowsUpdated == 0){
				// workaround if there is not yet an entry in the dollar table for a given user
				creditUpdate.close();
				creditUpdate = connection.prepareStatement("INSERT INTO dollar (uni, amount, department_id) VALUES ( ?, ?, ?)");
				creditUpdate.setString(1, uni);
				creditUpdate.setInt(2, creditRequested);
				creditUpdate.setInt(3, departmentId);
				rowsUpdated = creditUpdate.executeUpdate();
				if ( rowsUpdated == Statement.EXECUTE_FAILED ){
					connection.rollback(preCredit);
					return false;
				}
			}

			PreparedStatement salelogInsert = connection.prepareStatement(
						"INSERT INTO salelog ( sale_log_id, uni, vendor, amount, sale_date, type, department ) " +
						"VALUES ( sale_log_seq.nextval, ?, ?, ?, SYSDATE, ?, ? )");

			salelogInsert.setString(1, uni);
			salelogInsert.setString(2, vendor);
			salelogInsert.setInt(3, creditRequested);
			salelogInsert.setString(4, "credit");
			salelogInsert.setString(5, department);
			salelogInsert.executeUpdate();
			salelogInsert.close();
			if ( rowsUpdated == Statement.EXECUTE_FAILED || rowsUpdated > 1){
				 log.error("Caught an unexpected result from the salelog update, rolling back.");
				 connection.rollback(preCredit);
				 return false;				
			}
	
			connection.commit();
			connection.setAutoCommit(true);
			return true;
	
		}catch (SQLException e){
			log.error("Caught SQLException during the creditOverride operation", e);
			return false;
		}finally{
			try{
				if (connection != null && !connection.isClosed()) connection.close();
			}catch(SQLException e){
				log.warn("Caught SQLException while closing DB connection in creditOverride",e);
			}
			log.trace("Exiting Pagecontrol.creditOverride()");
		}
	}
	
	
	public Date periodStart(){
		log.trace("Entry into Pagecontrol.periodStart()");
		Date starting = Date.valueOf("2010-01-01");  //initialize
		starting.setTime(academicCalendar.current());
		log.trace("Exiting Pagecontrol.periodStart()");
		return starting;
	}

		
	public void resetQuotas(int quotaId){
		int[] wrapper = new int[1]; 
		wrapper[0] = quotaId;
		resetQuotas(wrapper);
	}
	
	
	public void resetQuotas(int[] quotaIds){

	   log.trace("Entry into Pagecontrol.resetQuotas()");
	   Connection connection = null;
	   try{
		   connection = ods.getConnection();
		   connection.setAutoCommit(true);
		   PreparedStatement sql = connection.prepareStatement("UPDATE usage SET amount=0 WHERE quota_id = ?");

		   for (int i=0; i < quotaIds.length; i++){
			  StringBuffer sb = new StringBuffer("( )");
			  sb.insert(1, quotaIds[i]);
			  if ( i > 0  ){
				sb.insert(',', 1);
			  }
			  sb.insert(1, quotaIds[i]);
		
		      sql.setInt(1, quotaIds[i]);
		      sql.addBatch();
		   }
		   sql.executeBatch();
		}catch(SQLException e){
			log.error("Unable to reset quotas");
		}finally{
			try{
				if (connection != null && !connection.isClosed()) connection.close();
			}catch(SQLException e){
				log.warn("Caught SQLException while closing DB connection in resetQuotas",e);
			}
			log.trace("Exiting Pagecontrol.resetQuotas()");
		}
		
	}
	
	
	public void resetWeeklyQuotas(){
		
		log.trace("Entry into Pagecontrol.resetWeeklyQuotas()");
		Connection connection = null;
		try{
		   connection = ods.getConnection();
		   connection.setAutoCommit(true);
		   Statement sql = connection.createStatement();
		   sql.executeUpdate("UPDATE usage SET amount=0 WHERE quota_id IN ( SELECT quota_id FROM quota WHERE duration=7 )");
		}catch(SQLException e){
			log.error("Unable to reset Weekly quotas", e);
		}finally{
			try{
				if (connection != null && !connection.isClosed()) connection.close();
			}catch(SQLException e){
				log.warn("Caught SQLException while closing DB connection in resetWeeklyQuotas",e);
			}
			log.trace("Exiting Pagecontrol.resetWeeklyQuotas()");
		}
	}
	
	
	public String nextSaleId() {
		
		log.trace("Entry into Pagecontrol.nextSaleId()");
		String saleId = null;
		Connection connection = null;
		try {
			connection = ods.getConnection();
			PreparedStatement sql = connection.prepareStatement("SELECT sale_log_seq.nextval id from dual");
			ResultSet saleIdResult = sql.executeQuery();
			while (saleIdResult.next()){
				saleId = saleIdResult.getString(1);
			}
		}catch(SQLException e){
			log.error("Unable to get next sale_log_id for PurchaseGateway");
		}finally{
			try{
				if (connection != null && !connection.isClosed()) connection.close();
			}catch(SQLException e){
				log.warn("Caught SQLException while closing DB connection in nextSaleId",e);
			}
			log.trace("Exiting Pagecontrol.nextSaleId()");
		}
		return saleId;
	}

	
	public void forceUpdate(String uni){
		affilCache.forceUpdate(uni);
	}

	public Collection<String> listAffils(String uni) throws AffiliationException{

		log.trace("Entry into Pagecontrol.listAffils()");
		
		try {
			return affilCache.getAffiliations(uni);
		} catch (AffiliationException e) {
			switch (e.getCondition()){
				case AffiliationException.NO_AFFILS:
					return new ArrayList<String>();
				
				case AffiliationException.UPDATE_REQUIRED:
					return new ArrayList<String>();
					
				default:
					// Re-throw the exception if unhandled
					throw e;
			}
		}finally{
			log.trace("Exiting Pagecontrol.listAffils()");
		}
	}
	
	
}
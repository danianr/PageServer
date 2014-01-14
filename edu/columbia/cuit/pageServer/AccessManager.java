package edu.columbia.cuit.pageServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import oracle.jdbc.pool.OracleDataSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

public class AccessManager {
	
	private static final long serialVersionUID = 3L;
	public enum accessLevel { ADMIN, MANAGER, SALE, OVERRIDE, CREDIT, SALEONLY }
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private final OracleDataSource ods;
	private static final int DBConnectTimeout = 3;  // seconds
	
	private Map<String, accessLevel> permissionMap;
	private Set<InetAddress> ipAccessList;

	private final String roleMappingSql;
	private final String updateRoleSql;
	private final String addUserSql;
	private final String removeUserSql;
	

	public AccessManager(ServletContext context, OracleDataSource ods) {
		
		// This is wasteful for the initial load, but prevents an unreachable database
		// Connection from storing a null reference instead of an empty HashMap
		permissionMap = new HashMap<String, accessLevel>();
		ipAccessList = new HashSet<InetAddress>();
		
		this.ods = ods;
		
		// The only IN Parameter is accessLevel 
		roleMappingSql = context.getInitParameter("roleMappingSQL");

		// IN Parameters are all (accessLevel, username)
		updateRoleSql = context.getInitParameter("updateRoleSQL");
		addUserSql = context.getInitParameter("addUserSQL");
		removeUserSql = context.getInitParameter("removeUserSQL");
	
		try {
			this.refresh();
			this.refreshAddressList();
		}catch (SQLException e){
			log.error(e);
		}
	}
	
	
	public final void refresh() throws SQLException{

		HashMap<String, accessLevel> updatedPermissionMap = new HashMap<String, accessLevel>();
		
		Connection dbConnection = ods.getConnection();
		
		if ( dbConnection.isValid(DBConnectTimeout) ){
			log.trace("Refreshing AccessManager permission mapping");
			
			// Iterate through each of the access levels using a prepared statement
			// to populate the permissionsMap with a username -> accessLevel mapping
			PreparedStatement roleMapping= dbConnection.prepareStatement(roleMappingSql);

			accessLevel[] role = accessLevel.values();
			for(int i=0; i < role.length; i++){
	
				roleMapping.setString(1, role[i].toString());
				ResultSet roleResults = roleMapping.executeQuery();
				while (roleResults.next()){
					String name = roleResults.getString(1);
					updatedPermissionMap.put(name, role[i]);
					if (log.isTraceEnabled()){
						log.trace("Added " + updatedPermissionMap.get(name).toString() + " for " + name);
					}
				}
				roleResults.close();
			}
			
			// Make the update an atomic action to enable access while the
			// accessManager is refreshing its user -> accessLevel mapping
			permissionMap = updatedPermissionMap;
			
		} // dbConnectionValid check			
	}
	
	
	public final void refreshAddressList() throws SQLException{
		HashSet<InetAddress> updatedAddressList = new HashSet<InetAddress>();

		Connection dbConnection = null;
		try{
		   dbConnection = ods.getConnection();
		   if (dbConnection.isValid(DBConnectTimeout)){
			PreparedStatement sql = dbConnection.prepareStatement("SELECT hostname FROM printer");
			ResultSet allowedHostnames = sql.executeQuery(); 
			while(allowedHostnames.next()){
				String hostname = allowedHostnames.getString("hostname");
			    try{
			    	InetAddress hostIP = InetAddress.getByName(hostname);
				   	updatedAddressList.add(hostIP);
				   	if (log.isTraceEnabled()){
					   log.trace("clientIP.add(" + hostIP.getHostAddress() + ") for " + hostname);
				   	}
			    } catch (UnknownHostException e){
			    	log.warn("Not adding unknown host ("+ hostname +") to AddressList");
			    } 
			}
		    allowedHostnames.close();
			sql.close();		
		    ipAccessList = updatedAddressList;
		   } //dbConnectionValid check
		}finally{
			if ( (dbConnection != null) && (!dbConnection.isClosed()) ) dbConnection.close();
		}
		
	}
	
	
	public final boolean checkAccess(String username, accessLevel required){

		if (username != null && permissionMap.containsKey(username)){
		
			accessLevel role = permissionMap.get(username);

			// SALEONLY allows SALE access without access to usage info
			if ( required.equals(accessLevel.SALE) && role.equals(accessLevel.SALEONLY) ){
				return true;
			}

			if ( role.compareTo(required) <= 0 ) return true;
		}
		
		return false;
	}
	
	
	public final boolean checkDisplayAccess(HttpServletRequest req, String user){

		// Display access is a special case that allows all users to
		// see their own usage and quota; otherwise CREDIT access
		// is required to see usage and quota for any user
		
		String remoteUser = req.getRemoteUser();		
		if (remoteUser.equalsIgnoreCase(user)) return true;
		
		return checkAccess(remoteUser, accessLevel.CREDIT);
	}

	
	public final boolean checkAccess(HttpServletRequest req, accessLevel required){
		
		// Convenience method remote user from the request
		String remoteUser = req.getRemoteUser();
		return checkAccess(remoteUser, required);
	}
	
	
	public final void addUser(String username, accessLevel role){
		
		Connection dbConnection;
		try {
			dbConnection = ods.getConnection();
			dbConnection.setAutoCommit(true);
			PreparedStatement addUser = dbConnection.prepareStatement(addUserSql);
			addUser.setString(1, role.toString());
			addUser.setString(2, username);
			addUser.executeUpdate();
			addUser.close();
		} catch (SQLException e) {
			log.error("Unable to add username(" + username + ")   accessLevel(" +
										role.toString() +") to the AccessManager DB");
		}
	}
		
	
	public final void removeUser(String username){
		
		Connection dbConnection;
		try{
			dbConnection = ods.getConnection();
			dbConnection.setAutoCommit(true);
			PreparedStatement removeUser = dbConnection.prepareStatement(removeUserSql);
			removeUser.setString(1, username);
			removeUser.executeUpdate();
			removeUser.close();
		}catch (SQLException e){
			log.error("Unable to remove username(" + username + ") from the AccessManager DB", e);
		}
	}
	
	public void updateRole(String username, accessLevel role){
		
		Connection dbConnection;
		try{
			dbConnection = ods.getConnection();
			dbConnection.setAutoCommit(true);
			PreparedStatement updateRole = dbConnection.prepareStatement(updateRoleSql);
			updateRole.setString(1, role.toString());
			updateRole.setString(2, username);
			updateRole.executeUpdate();
			updateRole.close();
		}catch (SQLException e){
			log.error("Unable to update role for username(" + username + ") -> role(" + role + ")" );
		}
	}
	
	
	public DocumentFragment accountInfo(Document parentDoc){
	
		Element toplevelElement = parentDoc.createElement("accountInfo");
		accessLevel[] roles = accessLevel.values();
		
		Element accessLevelElement = parentDoc.createElement("accessLevels");
		for(int i=0; i< roles.length; i++){
			Element roleElement = parentDoc.createElement("role");
			roleElement.setAttribute("name", roles[i].toString());
			roleElement.setAttribute("ordinal", Integer.toString(roles[i].ordinal()));
			accessLevelElement.appendChild(roleElement);
		}
		toplevelElement.appendChild(accessLevelElement);
	
		Element activeAccountsElement = parentDoc.createElement("activeAccounts");
		toplevelElement.appendChild(activeAccountsElement);
		Set<Entry<String,accessLevel>> userMapping = permissionMap.entrySet();
		Iterator<Entry<String, accessLevel>> i = userMapping.iterator();
		while (i.hasNext()){
			Entry<String, accessLevel> accountEntry = i.next();
			Element accountElement = parentDoc.createElement("account"); 
			accountElement.setAttribute("username",  accountEntry.getKey());
			accountElement.setAttribute("role", accountEntry.getValue().toString());
			activeAccountsElement.appendChild(accountElement);
		}
		DocumentFragment retDoc = parentDoc.createDocumentFragment();
		retDoc.appendChild(toplevelElement);
		
		return retDoc;
	}
	
	
	public boolean allowedIP(InetAddress inetaddr){
		if (inetaddr != null) return ipAccessList.contains(inetaddr);
		return false;
	}
	
}
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">

	
 
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>
 
	
 
  <xsl:template match="/ninja/accountManagement">
 
	
    <html>
      <head>
      <title>Account Manager Interface</title>
  
      </head>
      <body>
      
      	  <h1>Account Manager Interface</h1>
      	  <xsl:variable name="formAction"><xsl:value-of select="/ninja/requestURL/@value" /></xsl:variable>
		  <form id="accountInterface" action="{$formAction}" method="get">
		  	<table padding="2">
			 <tr>
				<td>			
				  <input type="submit" name="operation" value="add"/> account 		  
				</td>
			    <td align="top">
			       <xsl:text>Username: </xsl:text><input type="text" name="username" size="8"/>
			    </td>
			    <td><select name="role">
			    	<option value="" disabled="true">AccessLevel</option>
			    	<xsl:for-each select="accountInfo/accessLevels/role">
			    		<option><xsl:value-of select="./@name"/></option>	
			    	</xsl:for-each>
			        </select></td>
			    </tr>
			</table>
			
		  
		  </form>
		  <p/>
		  <form id="accountPicker" action="{$formAction}" method="get">

		  <h2>Account Modification</h2>
		  <table>
		  
		    <tr><th>ADMIN</th> <th>MANAGER</th> <th>SALE</th> <th>OVERRIDE</th> <th>CREDIT</th> </tr>
			<tr>
			<td>
			<select id="admin" multiple="true" name="username" size="10">
			<option selected="true" value="" disabled="true">ADMIN</option>
            <xsl:for-each select="accountInfo/activeAccounts/child::account[attribute::role='ADMIN'] ">
            	<option><xsl:value-of select="./@username"/></option>	
            </xsl:for-each>
			</select>
			</td>

			<td>
			<select id="manager" multiple="true" name="username" size="10">
			<option selected="true" value="" disabled="true">MANAGER</option>
            <xsl:for-each select="accountInfo/activeAccounts/child::account[attribute::role='MANAGER'] ">
            	<option><xsl:value-of select="./@username"/></option>	
            </xsl:for-each>
			</select>
			</td>
			
			<td>
			<select id="sale" multiple="true" name="username" size="10">
			<option selected="true" value="" disabled="true">SALE</option>
            <xsl:for-each select="accountInfo/activeAccounts/child::account[attribute::role='SALE'] ">
            	<option><xsl:value-of select="./@username"/></option>	
            </xsl:for-each>
			</select>
			</td>

			<td>
			<select id="override" multiple="true" name="username" size="10">
			<option selected="true" value="" disabled="true">OVERRIDE</option>
            <xsl:for-each select="accountInfo/activeAccounts/child::account[attribute::role='OVERRIDE'] ">
            	<option><xsl:value-of select="./@username"/></option>	
            </xsl:for-each>
			</select>
			</td>
			
			<td>
			<select id="credit" multiple="true" name="username" size="10">
			<option selected="true" value="" disabled="true">CREDIT</option>
            <xsl:for-each select="accountInfo/activeAccounts/child::account[attribute::role='CREDIT'] ">
            	<option><xsl:value-of select="./@username"/></option>	
            </xsl:for-each>
			</select>
			</td>
			</tr>
		  </table>
			<br clear="all" />
			<select name="role">
			    <option value="" disabled="true">AccessLevel</option>
			    <xsl:for-each select="accountInfo/accessLevels/role">
			    	<option><xsl:value-of select="./@name"/></option>	
			    </xsl:for-each>
			</select>
		  
		  <input type="reset" value="Reset"/> 
		  <br clear="all" />
		  <input type="submit" name="operation" value="update"/> Selected Accounts
		  <br clear="all" />
		  <input type="submit" name="operation" value="remove"/> Selected Accounts

		  </form>

		  <br clear="all"/><p/>
		  <form id="refresh" action="{$formAction}" method="get">
		    <input type="submit" name="operation" value="refresh"/> Refresh Account Info
		  </form>
		  
		  
		   <xsl:if test="success"><h2>Successful Actions</h2>
		   			<xsl:for-each select="success/descendant::text()" >
					    <br/><xsl:value-of select="."/>
		   			</xsl:for-each>
		   </xsl:if>
		   
		   <xsl:if test="failure"><h2> Actions</h2>
				   	<xsl:apply-templates select="failure/descendant::text()" >
				    	<br/><font color="red"><xsl:value-of select="."/></font>
					</xsl:apply-templates>
		   </xsl:if>		   


      </body>
    </html>
  </xsl:template>

 
</xsl:stylesheet>

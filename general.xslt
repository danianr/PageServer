<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">

	
 
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>
 
 
 
  <xsl:template match="/ninja">
  
  <html>
   <head>
      <link rel="shortcut icon" href="http://cuit.columbia.edu/sites/all/libraries/ias/favicon-crown.png" type="image/png" />
      <title>NINJa Information</title>
   </head>
   
   <body>
   
   <xsl:apply-templates/>
   
   
   </body>  
  
  
  </html>
  
  
  </xsl:template>
 
 
 
  <xsl:template match="usageReport">
 
    <strong>Purchased Printing Dollars</strong> (<xsl:value-of select="descendant::dollars/@amount"/>)
	<p/>
	<h2><xsl:text>Quota Remaining for </xsl:text>
	<xsl:value-of select="uni/@username"/>
	</h2>


     <xsl:choose>
        <xsl:when test="/descendant::error/@condition='no quotas'">
           You do not have any printing quotas available
        </xsl:when>

        <xsl:otherwise>
          <xsl:apply-templates select="descendant::aggregatedQuotas/child::*"/>
        </xsl:otherwise>
     </xsl:choose>

    <xsl:apply-templates select="descendant::history"/>

    <p/>

</xsl:template>


<xsl:template match = "affiliationReport">

   <h2>Affiliation Report for <xsl:value-of select="./@uni"/></h2>
   <xsl:apply-templates select="userinfo"/>
   <xsl:apply-templates select="affilList"/>

   <xsl:variable name="forceUpdateUrl"><xsl:value-of select="./@refreshUrl"/></xsl:variable>
   <form id="forceUpdateForm" action="{$forceUpdateUrl}" METHOD="GET">
       <input type="submit" value="Update Cached Affiliations"/>
   </form>
</xsl:template>


<xsl:template match="/ninja/confirm">
<h2>Confirm information before Selling Printing Dollars</h2>
<xsl:apply-templates select="userinfo"/>
<xsl:variable name="confirmUrl"><xsl:value-of select="/ninja/confirm/@value" /></xsl:variable>
<form id="confirmForm" action="{$confirmUrl}" METHOD="GET">
    <input type="submit" value="Confirm Sale"/>
</form>
</xsl:template>


<xsl:template match ="userinfo">
   <pre>
       Full Name: <xsl:value-of select="./@cn" />
       Department: <xsl:value-of select="./@ou" />
       Title: <xsl:value-of select="./@title" />
       Email: <xsl:value-of select="./@mail"/>
       <xsl:value-of select="text()"/>
   </pre>
</xsl:template>


<xsl:template match="affilList">
   <ul>Assigned Affiliations:
      <xsl:for-each select="./affil">
         <li><xsl:value-of select="./text()"/></li>
      </xsl:for-each>
   </ul>
</xsl:template>


<xsl:template match = "credit">

<xsl:choose>
<xsl:when test="./@success">
   <xsl:if test="./@transactionId" >
      <strong>Transaction <xsl:value-of select="./@transactionId"/> has been credited into printing dollars</strong>
   </xsl:if>
	<xsl:if test="contains(./@operation, 'override')">
	  User <xsl:value-of select="./@uni"/> has been allocated $<xsl:value-of select="format-number(number(./@amount), '0.00')"/> in printing dollars.
	</xsl:if>
</xsl:when>

<xsl:when test="./@failure">
   <font color="red"><strong>The credit transaction could not be completed.</strong></font>
</xsl:when>

</xsl:choose>
</xsl:template>

<xsl:template match ="aggregatedQuotas/child::*">

 <table summary="Quota Remaining" cellspacing="0" cellpadding="4">

 <xsl:choose>
   <xsl:when test="self::AcISBlackAndWhite">
       <tr><th colspan="3" align="left">Morningside Heights printers:</th></tr>
   </xsl:when>
 	
   <xsl:when test="self::SSWBlackAndWhite">
     <tr><th colspan="3" align="left">School of Social Work printers:</th></tr>
   </xsl:when>
 	
   <xsl:when test="self::BCBlackAndWhite">
     <tr><th colspan="3" align="left">Barnard College printers:</th></tr>
   </xsl:when>
 	
   <xsl:otherwise>
     <tr><th colspan="3" align="left">Other printers:</th></tr>
   </xsl:otherwise>
 </xsl:choose>
 
 <xsl:variable name="weeklyQuota"><xsl:number value="./weekly/@remaining"/></xsl:variable>
 <xsl:variable name="semesterlyQuota"><xsl:number value="./semesterly/@remaining"/></xsl:variable>
	
 <tr><td/><td align="left">Weekly Quota remaining</td>
     <td align="right"><xsl:value-of select="$weeklyQuota"/></td>
 </tr>
 <tr><td/><td align="right">Semesterly Quota remaining</td>
     <td align="right"><xsl:value-of select="$semesterlyQuota"/></td>
 </tr>
	
 <tr><td align="left" style="border-top: 1px solid #333">
     <strong>Total pages available</strong></td>
     <td style="border-top: 1px solid #333" colspan="2" align="right">
     <strong><xsl:number value="$weeklyQuota + $semesterlyQuota"/></strong></td>
 </tr>
 <tr><td/></tr>

 </table>

</xsl:template>


<xsl:template match="sale">

<h2>Sale result for <xsl:value-of select="./@uni"/></h2>

<xsl:choose>
   <xsl:when test="./@success">
      The sale of $<xsl:value-of select="format-number(number(./@amount), '0.00')"/> to <xsl:value-of select="./@uni"/> was successful.
   </xsl:when>
   
   
   <xsl:when test="./@failure">
      <font color="red"><strong>Unable to complete sale.</strong></font>
   </xsl:when>
</xsl:choose>

<p/>
<strong>Current Printing Dollars for <xsl:value-of select="./@uni"/></strong> (<xsl:value-of select="/descendant::dollars/@amount"/>)

</xsl:template>


<xsl:template match="history">         
    <h2>
      <xsl:text>Printing History for </xsl:text>
        <xsl:value-of select="./@uni"/><xsl:text> (last </xsl:text>
        <xsl:value-of select="./@daysPrevious"/><xsl:text> days)
      </xsl:text>
    </h2>

    <table cellpadding="1" cellspacing="2">
      <tr>
         <th align="left">Time</th> <th align="left">Action</th> <th align="left">Amount</th> <th>Agent/Printer</th>
      </tr>
      <xsl:for-each select="./transaction">
        <xsl:sort select="./@timestamp"/>
        <tr>
	  <td><xsl:value-of select="."/></td>
	  <td width="65"><xsl:value-of select="./@type"/></td>
	  
	  
	  <xsl:choose>
	     <xsl:when test="contains(./@type, 'print')" >
	         <td width="70"><xsl:value-of select="./@amount"/> sheet(s)</td>    
	     </xsl:when>
			

         <xsl:otherwise>
	         <td width="70">$ <xsl:value-of select="format-number(number(./@amount) div 100, '0.00')"/></td>    
         </xsl:otherwise>

      </xsl:choose>


          <td align="left">
             <xsl:value-of select="./@printer"/>
             <xsl:value-of select="./@vendor"/>
          </td>
			 <xsl:if test="/ninja/creditURL">
				<xsl:if test="contains(./@type,'print')">
			       <xsl:variable name="creditUrl"><xsl:value-of select="/ninja/creditURL/@href"/>/transaction/<xsl:value-of select="./@id"/></xsl:variable>
	 			   	<xsl:variable name="transactionId"><xsl:value-of select="./@id"/></xsl:variable>
				   	<xsl:choose>
				   	
				   	    <xsl:when test="/descendant::transaction[attribute::voided=$transactionId]">
							<td>VOIDED</td>
						</xsl:when>
						
						<xsl:otherwise>
			       		    <td><form action="{$creditUrl}" METHOD="GET"><input type="submit" value="Credit"/></form></td>
						</xsl:otherwise>
					</xsl:choose>
                </xsl:if>
			 </xsl:if>
        </tr>
      </xsl:for-each>
    </table>
</xsl:template>
 
 
</xsl:stylesheet>
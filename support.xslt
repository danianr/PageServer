<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">

	
 
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>
 
	
 
  <xsl:template match="/ninja">
 
	
	<html xmlns="http://www.w3.org/1999/xhtml">
      <head>
      <title>NINJa Support Tools</title>
  


<script language="javascript">
function pagehistory() {
      var formElement = document.getElementById("historyForm");
      var user = formElement.childNodes.item(1).value;
      var days = formElement.childNodes.item(3).value;
      var destURL = "<xsl:value-of select="/ninja/historyURL/@href" />" +
                    "/" + user + "/" + days;
      location.assign(destURL);
}

function affils() {
      var formElement = document.getElementById("affilsForm");
      var user = formElement.childNodes.item(1).value;
      var destURL = "<xsl:value-of select="/ninja/affilsURL/@href" />/" + user;
      location.assign(destURL);
}


<xsl:if test="/ninja/overrideURL">
function overridecredit() {
      var formElement = document.getElementById("overrideForm");
      var user = formElement.childNodes.item(1).value;
      var dollars = formElement.childNodes.item(3).value;
      var justification = formElement.childNodes.item(5).value;
      var justificationEnc = escape(justification);
      var destURL = "<xsl:value-of select="/ninja/overrideURL/@href" />" +
                    "/" + user + "/" +  dollars + "?justification=" +
                    justificationEnc;
      location.assign(destURL);
}
</xsl:if>

function sellpages() {
      var formElement = document.getElementById("saleForm");
      var user = formElement.childNodes.item(1).value;
      var dollars = formElement.childNodes.item(3).value;
      dollars.replace(".","");  // remove the decimal point
      var destURL = "<xsl:value-of select="/ninja/sellPagesURL/@href" />" +
                    "/" + user + "/" + dollars + "?prompt";
      location.assign(destURL);
}

</script>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>NINJa Support Tools</title>
</head>
<body>

<h1>NINJa Support Tools</h1>


<h3>Printing History</h3>

<form id="historyForm">
Uni: <input type="text" name="username" maxlength="8" size="8" />
TimePeriod: <select name="period">
                 <option value="10" default="true">10 days</option>
                 <option value="21">3 weeks</option>
                 <option value="42">6 weeks</option>
                 <option value="92">3 months</option>
                 <option value="8000">All</option>
             </select>
<input type="button" value="Get Printing History" onclick="javascript:pagehistory()" />             
</form>
Check a user's quota, printing history, amount of printing dollars; also void individual <br/>
transactions charged to the user for streaked, faint, and crumpled print jobs.  Jobs which do <br/>
not complete, such as paper jams, are not charged to the user and will not appear in this list. 
<br clear="all" />


<h3>Check Printing Affiliations</h3>

<form id="affilsForm">
Uni: <input type="text" name="username" maxlength="8" size="8" />
<input type="button" value="Check Affils" onclick="javascript:affils()" />             
</form>
Check the printing affiliations cached for a specific user.<br/>
<br clear="all" />


<xsl:if test="/ninja/overrideURL">
<h3>Emergency printing dollar allocation</h3>

<form id="overrideForm">
Uni: <input type="text" name="username" maxlength="8" size="8" />
Dollars: <input type="text" name="pages" value="0.00" maxlength="5" size="5"/>
Justification: <input type="text" name="justification" maxlength="200" size="32"/>
<input type="button" value="Credit Dollars" onclick="javascript:overridecredit()" />
</form>
Allocate the user printing dollars directly, for use only in exceptional circumstances.<br/>
For the normal credit of printing dollars, use the Printing History page above.

</xsl:if>


<xsl:if test="/ninja/supportTools/sellPages">
<p/>
<h3>Printing Dollars (Flex Transactions only)</h3>
<form id="saleForm">
Uni: <input type="text" name="username" maxlength="8" size="8" />
Dollars: <input type="text" name="amount" value="0.00" />
<input type="button" value="Sell Printing Dollars" onclick="javascript:sellpages()" />
</form>
Enter the dollar amount charged to the flex account, complete with decimal.<br/>
Note accompanying paperwork may be required for audit purposes. 
</xsl:if>

<xsl:if test="/ninja/accountsURL">
<p/>
<xsl:variable name="accountsURL" select="/ninja/accountsURL/@href"/>
<a href="{$accountsURL}"><h2>Account Management</h2></a>
Add users, remove users, and modify privileges for the NINJa Support Tools
</xsl:if>


</body>
</html>

</xsl:template> 
</xsl:stylesheet>

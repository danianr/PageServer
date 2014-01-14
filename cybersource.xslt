<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">

	
 
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>
 
  <xsl:template match="/ninja">
<html>
<head>
<title>Printing Account</title>
<Meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>


<base href="http://www.columbia.edu/acis/facilities/" />
<link rel="stylesheet" href="http://www.columbia.edu/acis/facilities/printers/cc/style.css"/>
<style type="text/css">@import "/acis/templates/advanced.css";</style> 

 
<script LANGUAGE="JavaScript" type="text/javascript">


function newImage(arg) {
	if (document.images) {
		rslt = new Image();
		rslt.src = arg;
		return rslt;
	}
}

function changeImages() {
	if (document.images &amp;&amp; (preloadFlag == true)) {
		for (var i=0; i&lt;changeImages.arguments.length; i+=2) {
			document[changeImages.arguments[i]].src = changeImages.arguments[i+1];
		}
	}
}

var preloadFlag = false;
function preloadImages() {
	if (document.images) {
	acisover = newImage("/acis/facilities/images/acison.gif");
     groupover = newImage("/acis/facilities/images/facilities-on.gif");	
      printover = newImage("/acis/templates/images/printeron.gif");	
      simover = newImage("/acis/templates/images/similaron.gif");
		preloadFlag = true;
	}
}


</script>
   


<META name="description" content="An introduction to AcIS public facilities at 
Columbia." />
<META name="keywords" content="facilities, account, accounts, printers" />



</head>
<body bgcolor="#ffffff" onLoad="preloadImages();">


<table border="0" cellspacing="0" cellpadding="0" width="100%"><tr>
<td class="topbg" nowrap="true">
<div class="navtab"> </div></td></tr>
<tr><td bgcolor="#3399ff" height="30" class="header" align="left" valign="top" nowrap="true">
<!-- This part then brings in the proper group and AcIS logo.  -->  <table border="0" cellspacing="0" cellpadding="0" width="100%">
	<tr>
	  <td align="left" valign="top"> 
		<table border="0" cellpadding="0" cellspacing="0">
		  <tr><td align="right" nowrap="true"><a HREF="/acis/" ONMOUSEOVER="changeImages('acis', '/acis/facilities/images/acison.gif'); return true;" ONMOUSEOUT="changeImages('acis', '/acis/facilities/images/acis.gif'); return true;" class="navtab"><img NAME="acis" SRC="/acis/facilities/images/acis.gif" border="0" width="89" height="20" alt="AcIS"/></a></td>
		<td align="left" nowrap="true"> <a HREF="http://www.columbia.edu/acis/facilities/" ONMOUSEOVER="changeImages('group', '/acis/facilities/images/facilities-on.gif'); return true;" ONMOUSEOUT="changeImages('group', '/acis/facilities/images/facilities.gif'); return true;" class="navtab"><img NAME="group" SRC="/acis/facilities/images/facilities.gif" width="111" height="20" border="0" alt="Facilities"/></a></td>
		  </tr>
		  <tr> 
			<td colspan="2"> <img src="/acis/facilities/images/academicinfosys.gif" width="200" height="10" alt="academic information systems"/></td>
		  </tr>
		</table>
	  </td>
	<td align="right" valign="top"> 
		<form method="get" action="http://usearch.cc.columbia.edu/query.html">
		  <table cellpadding="0" cellspacing="0" border="0" align="right">
			<tr>
			  <td height="17" nowrap="true" valign="bottom"> 
				<input type="radio" name="qp" value="+url:acis/facilities" checked="true" style="background: #3399ff; color:  #000000"/>
			  </td>
			  <td height="17" nowrap="true"><img src="/acis/facilities/images/facilitiesonly.gif" width="80" height="25" alt="facilities only"/></td>
			  <td rowspan="2" align="center" valign="bottom"> 
				<table cellpadding="0" cellspacing="0" border="0">
				  <tr><td>
				<input type="text" name="qt" size="20" value="" maxlength="2033"/>
				<input type="hidden" name="col" value="cuweb"/>
				<input type="hidden" name="qs" value=""/>
				<input type="hidden" name="qc" value=""/>
				<input type="hidden" name="pw" value="100%"/>
				<input type="hidden" name="ws" value="0"/>
				<input type="hidden" name="la" value=""/>
				<input type="hidden" name="qm" value="0"/>
				<input type="hidden" name="st" value="1"/>
				<input type="hidden" name="nh" value="10"/>
				<input type="hidden" name="lk" value="1"/>
				<input type="hidden" name="rf" value="0"/>
				<input type="hidden" name="oq" value=""/>
				<input type="hidden" name="rq" value="0"/></td>
				    <td align="center"> 
					  <input type="image" src="/acis/facilities/images/searchbutton.gif" alt="search" />
				</td>
					<td align="center"> <a href="http://usearch.cc.columbia.edu/help/" class="navtab"><img src="/acis/facilities/images/searchhelp.gif" width="21" height="21" border="0" alt="search tips" vspace="2" hspace="2"/></a> 
					</td>
				  </tr>
				  </table>
			  </td>
			</tr>
			<tr>
			  <td nowrap="true" valign="middle"> 
				<input type="radio" name="qp" value="+url:acis" style="background: #3399ff; color:  #000000"/>
			  </td>
			  <td nowrap="true"><img alt="all of AcIS" src="/acis/facilities/images/allofacis.gif" width="80" height="15"/></td>
			</tr>
		  </table>
</form>
</td>
	</tr>
  </table>
</td>
  </tr>

  <tr>
  <td class="topmiddlebg"><!-- hack so NN doesn't collapse cell --><div class="navtab">  </div></td>
</tr>


  <tr align="center"> 
  <td class="middlebg" align="center" nowrap="true"><!-- This part then brings in the proper navbar based on the set variable.  --> <table border="0" cellspacing="0" cellpadding="0"> <tr>
<td><a href="http://www.columbia.edu/acis/facilities/labs/" class="navtab"><img src="/acis/facilities/images/labs.gif" width="59" height="21" border="0" alt="Computer Labs"/></a></td>
<td><a href="http://www.columbia.edu/acis/facilities/classrooms/" class="navtab"><img src="/acis/facilities/images/classrooms.gif" width="120" height="21" border="0" alt="Electronic Classrooms"/></a></td>
<td><a href="http://www.columbia.edu/acis/facilities/cnet/" class="navtab"><img src="/acis/facilities/images/columbianet.gif" width="127" height="21" border="0" alt="ColumbiaNet"/></a></td>
<td><a href="http://www.columbia.edu/acis/facilities/printers/" class="navtab"><img src="/acis/facilities/images/printers-tab.gif" width="91" height="21" border="0" alt="Printers"/></a></td>
<td><a href="http://www.columbia.edu/acis/facilities/software/" class="navtab"><img src="/acis/facilities/images/software.gif" width="100" height="21" border="0" alt="Software"/></a></td>
<td><a href="/acis/facilities/schedules/" class="navtab"><img src="/acis/facilities/images/schedules.gif" width="103" height="21" border="0" alt="Schedules"/></a></td></tr></table>
</td>
  </tr>
  <tr>
  <td bgcolor="#82e8ff" valign="top" nowrap="true"><table border="0" align="center">
<!-- Insert text links here. the stop index and then start is a message to ultraseek. --> <!--stopindex--><tr>
<td>

<!--Set the printer level path -->


<a href="http://www.columbia.edu/acis/facilities/printers/locations.html" class="menu">Locations</a>  

</td>
<td class="divider">&#149;</td>
<td>
<a href="http://www.columbia.edu/acis/facilities/printers/ninja.html" class="menu">Using NINJa</a>

</td> 
<td class="divider">&#149;</td>
<td>

<a href="http://www.columbia.edu/acis/facilities/printers/quota.html" class="menu">Quota</a>

</td> 
<td class="divider">&#149;</td>
<td>
<a href="http://www.columbia.edu/acis/facilities/printers/purchase.html" class="menu">Purchase Printing Dollars</a>

</td>

<td class="divider">&#149;</td>
<td>
<a href="../../access/printing" class="menu">Printing to NINJa</a>

</td> 
</tr>


<!--startindex-->
</table>
  </td>
  </tr>
<tr><td class="bottombg" height="10"><div align="center">  </div></td>
  </tr>
</table><!-- begin breadcrumb trail -->
<div class="toppath" align="center"><table border="0" cellspacing="0" cellpadding="0" width="98%"><tr><td align="left" valign="top" class="toppath">
<a href="http://www.columbia.edu/acis/" class="path">AcIS</a> &gt;  <a href="http://www.columbia.edu/acis/facilities" class="path">facilities</a> &gt; Printing Account 
</td><!-- end breadcrumb trail -->

<td> 
</td> 
</tr></table></div>				




<!-- BODY BEGIN -->

<div class="content">

<xsl:choose>

   <xsl:when test="/ninja/purchaseGateway">

	<table summary="select page value and value in account" border="0" align="center" cellpadding="3" cellspacing="0" style="width: 50em">
            <tr><th colspan="2" align="left">	
                <h1>Purchase CUIT Printing Dollars  <font color='#ff0000'></font></h1></th></tr>
            <tr><th colspan="2" align="left">
                
                    <img src="http://www.columbia.edu/acis/facilities/images/shop.gif" alt="add dollars to cart"/> <br/>
                
                Your available page total is displayed to the right.</th></tr>
                <tr><td>
                
                    <xsl:variable name="targetURL"><xsl:value-of select="purchaseGateway/targetURL/@href" disable-output-escaping = "yes"/></xsl:variable>

                    
                    <form method="post" style="margin: 15px 2px; padding: 5px" action="{$targetURL}">
                        <fieldset style="border-style: none; margin:0; padding:5px 0 20px 0; line-height: 180%">
                            <legend align="top" style="font: normal 12px/100% Verdana, Arial, Helvetica, sans-serif; margin:0; padding:0"> 
                            Select the amount of CUIT Printing Dollars to add: </legend>

							<xsl:apply-templates select="purchaseGateway/dollarQuantity"/>


						</fieldset>


<strong>Add more or logout:</strong>
<br/>
<br/>

 <xsl:variable name="logoutURL"><xsl:value-of select="purchaseGateway/logoutURL/@href" disable-output-escaping = "yes"/></xsl:variable>

 <input type="submit" onclick="MM_goToURL('parent','{$logoutURL}');return document.MM_returnValue" value="Logout"/>
                        <script language="JavaScript" type="text/JavaScript">
                        function MM_goToURL() { //v3.0
                          var i, args=MM_goToURL.arguments; document.MM_returnValue = false;
                          for (i=0; i&lt;(args.length-1); i+=2) eval(args[i]+".location='"+args[i+1]+"'");
                        }
                        </script> 
                
                        <input type="submit" value="Continue Purchase"/>
                    </form>
				
                
                </td>
                
                    
                     
                <td nowrap="true">





<table summary="total amount available in account" cellspacing="0" cellpadding="4">
    <tr><th colspan="2" align="left">Page Totals for <xsl:value-of select="purchaseGateway/uni/@username"/></th></tr>

	<xsl:choose>
	   <xsl:when test="purchaseGateway/error/@condition='no quotas'">
	     <tr><th colspan="2" align="left">You do not have any Quota available</th></tr>
	   </xsl:when>

       <xsl:otherwise>
	     <xsl:apply-templates select="/descendant::aggregatedQuotas/child::*"/>
	   </xsl:otherwise>

	</xsl:choose>
	     

	<xsl:choose>
	   <xsl:when test="purchaseGateway/error/@condition='Unable to obtain dollar amount'">
	     <tr><th colspan="2" align="left">You do not have any Quota available</th></tr>
       </xsl:when>

       <xsl:otherwise>
 	     <xsl:apply-templates select="/descendant::dollars"/>
	   </xsl:otherwise>

   </xsl:choose>
	
</table>
</td>
            </tr>
        </table>

   </xsl:when>
   
   
   
   
   <xsl:when test="confirmPurchase">

		  You are about to be redirected to our third-party payment card processor.
		  All purchases of printing dollars are final and non-refundable.  Please click
		  on the "Continue Purchase" button below to proceed to the checkout.
             
          <xsl:variable name="targetURL"><xsl:value-of select="confirmPurchase/targetURL/@href" disable-output-escaping = "yes"/></xsl:variable>          
          <form method="post" style="margin: 15px 2px; padding: 5px" action="{$targetURL}">

			 <xsl:apply-templates select="/descendant::formfields/field"/>


 <xsl:variable name="logoutURL"><xsl:value-of select="confirmPurchase/logoutURL/@href" disable-output-escaping = "yes"/></xsl:variable>

 <input type="submit" onclick="MM_goToURL('parent','{$logoutURL}');return document.MM_returnValue" value="Logout"/>
                        <script language="JavaScript" type="text/JavaScript">
                        function MM_goToURL() { //v3.0
                          var i, args=MM_goToURL.arguments; document.MM_returnValue = false;
                          for (i=0; i&lt;(args.length-1); i+=2) eval(args[i]+".location='"+args[i+1]+"'");
                        }
                        </script> 
                
                        <input type="submit" value="Continue Purchase"/>
                    </form>
				

   </xsl:when>
   
   
   
   
   <xsl:when test="purchaseReceipt">

         <xsl:variable name="CustomerId"><xsl:value-of select="./purchaseReceipt/@customerId"/></xsl:variable>
         <xsl:variable name="InvoiceNum"><xsl:value-of select="./purchaseReceipt/@invoiceNumber"/></xsl:variable>
         <xsl:variable name="TransactionId"><xsl:value-of select="./purchaseReceipt/@transId"/></xsl:variable>
         <xsl:variable name="ResponseCode"><xsl:value-of select="./purchaseReceipt/@responseCode"/></xsl:variable>
         <xsl:variable name="ReasonText"><xsl:value-of select="./purchaseReceipt/@reasonText"/></xsl:variable>

        <xsl:choose>
            <xsl:when test="$InvoiceNum=''">
               <img src="http://www.columbia.edu/acis/facilities/images/confirm.gif" alt="cc"/><br/><br/>
               <strong><xsl:value-of select="$ReasonText"/></strong><br/>
               Your <xsl:value-of select="$CustomerId"/> account has already been credited.<br/>
               Your credit card was charged once for order number #<xsl:value-of select="$TransactionId"/>.<br/>
            </xsl:when>
         
            <xsl:when test="$ResponseCode=1">
              <img src="http://www.columbia.edu/acis/facilities/images/confirm.gif" alt="cc"/><br/><br/>
              <strong> <xsl:value-of select="$ReasonText"/> </strong><br/>
              Your <xsl:value-of select="$CustomerId"/> account has been credited.<br/>
              Your order number is <strong>#<value-of select="$InvoiceNum"/></strong>.
            </xsl:when>
        
            <xsl:otherwise>
              <img src="http://www.columbia.edu/acis/facilities/images/credit-card.gif" alt="cc"/><br/><br/>
              There was an error processing your order.<br/>
              <strong><xsl:value-of select="$ReasonText"/></strong><br/>
              Please contact <a href="mailto:pagecontrol@columbia.edu">pagecontrol@columbia.edu</a> for additional details.<br/>
            </xsl:otherwise>
            
        </xsl:choose>

        <tr><td>
         <xsl:variable name="ContinueURL"><xsl:value-of select="./purchaseReceipt/continueURL/@href"/></xsl:variable>
         <xsl:if test="$ResponseCode!=1">
            <a href="javascript:history.back()">Go Back</a><xsl:text>  </xsl:text>
         </xsl:if>
            <a href="{$ContinueURL}">Continue</a>
        </td></tr>

   </xsl:when>
   
</xsl:choose>   
   

    <xsl:if test="purchaseGateway">
   	    <table summary="informational messages" border="0" align="center" cellpadding="3" cellspacing="0" style="width: 50em">
            
                <tr><td>
                    <br/>NOTE:<ul>
                    <li><strong>There is no refund once CUIT Printing Dollars are purchased.</strong></li>
                </ul>
                </td></tr>
            
        </table>
    </xsl:if>


</div>

<!-- FOOTER BEGIN -->

    
    



<table border="0" cellspacing="0" cellpadding="0" width="100%" class="footer">
<!-- was for accessible style <tr align="right" class="bottomfooterbg"> --> 
<tr align="right" class="bottombg"><td valign="top" width="1%"> </td><td valign="top" width="80%" align="right">  
<table border="0" cellspacing="0" cellpadding="0">

<tr><td align="right" class="footertxt" nowrap="true"><a href="http://www.columbia.edu/acis/" class="footermenu"><b>AcIS</b></a> <b>&#187;</b>  <a href="http://www.columbia.edu/acis/welcome/" class="footermenu"> About</a> | <a href="http://www.columbia.edu/acis/publications/acisover.html" class="footermenu">Services</a> | <a href="http://www.columbia.edu/acis/news/" class="footermenu">News</a> | <a href="http://www.columbia.edu/acis/accounts/" class="footermenu">Accounts</a> | <a href="http://www.columbia.edu/acis/email/" class="footermenu">Email</a> | <a href="http://www.columbia.edu/acis/facilities/" class="footermenu">Facilities</a> | <a href="http://www.columbia.edu/acis/networks/" class="footermenu">Networks</a> | <a href="http://www.columbia.edu/acis/support/training/" class="footermenu">Training</a> | <a href="http://www.columbia.edu/acis/software/" class="footermenu">Software</a> | <a href="http://www.columbia.edu/acis/security/" class="footermenu">Security</a> | <a href="http://www.columbia.edu/acis/eds/" class="footermenu">EDS</a> | <a href="http://www.columbia.edu/acis/policy/" class="footermenu">Policies</a></td></tr>
</table></td>
<td valign="top" width="19%"> </td>
</tr>
<tr align="right"><td class="footertxt2"> </td><td class="footertxt2">Last modified 
  
  

March 17, 2005

</td><td class="footertxt2"> </td></tr>
<tr><td nowrap="true" valign="top" align="right"> </td>
<td nowrap="true" valign="top" align="right">
	<table border="0" cellspacing="0" cellpadding="0" width="100%"><tr><td align="left"> 
<a href="http://www.columbia.edu" class="footermenu"><img hspace="10" src="/acis/templates/images/columbiaprint2.gif" width="268" height="94" title="go to Columbia University's home page" alt="Columbia" border="0"/></a></td>
<td nowrap="true" valign="middle" align="right" class="footertxt">
		<table border="0" cellspacing="0" cellpadding="0"><tr><td nowrap="true" valign="middle" align="right" class="footertxt"><a href="http://www.columbia.edu/acis/support/" class="footertxt">Help Desk</a>: (212)854-1919 | <a href="http://www.columbia.edu/acis/support/consultant/" class="footertxt">consultant@columbia.edu</a><br/>Columbia University Information Technology, &#169;2000-2010 Columbia University</td></tr></table></td></tr></table>
</td><td nowrap="true" valign="top" align="center"> </td>
</tr>
</table>

</body>
</html>


<!-- FOOTER END -->
</xsl:template>


<xsl:template match ="dollarQuantity">
   <xsl:variable name="N"><xsl:value-of select="./@size"/></xsl:variable>
   <xsl:choose>
	  <xsl:when test="position() = 1">   
        <input type="radio" name="size" value="{$N}" id="size_{$N}" checked="true">
                                <label for="size_{$N}" style="cursor: hand">$ <xsl:value-of select="./@size"/></label>
	    </input>
                                <br/>
	  </xsl:when>
	  <xsl:otherwise>
        <input type="radio" name="size" value="{$N}" id="size_{$N}">
                                <label for="size_{$N}" style="cursor: hand">$ <xsl:value-of select="./@size"/></label>
   	    </input>
                                <br/>
	  </xsl:otherwise>
    </xsl:choose>

</xsl:template>

	



<xsl:template match ="aggregatedQuotas/child::*">
	
	
 	<xsl:choose>
 		<xsl:when test="self::AcISBlackAndWhite">
		 	<tr><th colspan="2" align="left">Morningside Heights printers:</th></tr>		
 		</xsl:when>
 	
		<xsl:when test="self::SSWBlackAndWhite">
		 	<tr><th colspan="2" align="left">School of Social Work printers:</th></tr>		
 		</xsl:when>
 	
		<xsl:when test="self::BCBlackAndWhite">
		 	<tr><th colspan="2" align="left">Barnard College printers:</th></tr>		
 		</xsl:when>
 	
 		<xsl:otherwise>
		 	<tr><th colspan="2" align="left">Other printers:</th></tr>		 		
 		</xsl:otherwise>
 	</xsl:choose>
 
	<xsl:variable name="weeklyQuota"><xsl:number value="./weekly/@remaining"/></xsl:variable>
	<xsl:variable name="semesterlyQuota"><xsl:number value="./semesterly/@remaining"/></xsl:variable>
	
	<tr><td align="left">Weekly Quota remaining</td>
        <td align="right"><xsl:value-of select="$weeklyQuota"/></td></tr>
	<tr><td align="left">Semesterly Quota remaining</td>
        <td align="right"><xsl:value-of select="$semesterlyQuota"/></td></tr>
	
	
    <tr><td align="left" style="border-top: 1px solid #333"><strong>Total pages available</strong></td>
    	<td style="border-top: 1px solid #333"  align="right"><strong><xsl:number value="$weeklyQuota + $semesterlyQuota"/></strong></td>
    </tr>
		

</xsl:template>




<xsl:template match = "dollars">
	<tr><td align="left">Purchased (<xsl:value-of select="./@amount"/>)</td></tr>
	<tr><td align="left">Black &amp; White Cost is <xsl:value-of select="./AcISBlackAndWhite/@currencyCost"/>/page</td></tr>
    <tr><td align="right">Pages Available <xsl:value-of select="./AcISBlackAndWhite/@available"/></td></tr>
	<tr><td align="left">Color Cost is <xsl:value-of select="./AcISColor/@currencyCost"/>/page</td></tr>
    <tr><td align="right">Pages Available <xsl:value-of select="./AcISColor/@available"/></td></tr>
</xsl:template>


<xsl:template match = "field">
    <xsl:variable name="fieldName"><xsl:value-of select="./@name" /></xsl:variable>
    <xsl:variable name="fieldValue"><xsl:value-of select="./@value" /></xsl:variable>    
    <input type="hidden" name="{$fieldName}" value="{$fieldValue}" />

</xsl:template>

</xsl:stylesheet>
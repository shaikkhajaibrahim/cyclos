<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<cyclos:script src="/pages/reports/currentStateReportForm.js" />

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
	<ssl:form method="post" action="${formAction}">
	    <tr>
    	    <td class="tdHeaderTable"><bean:message key="reports.current.presentation"/></td>
        	<cyclos:help page="reports#current_state"/> 
    	</tr>
	    <tr>
    	    <td colspan="2" align="left" class="tdContentTableForms">
				<table class="defaultTable">
	            	<tr>
	            		<td align="left" colspan="2"><b><bean:message key="reports.stats.chooseStatistics"/></b></td>
	            	</tr>
	            	<tr>
	            		<td align="left" colspan="2"><html:checkbox property="currentStateReport(memberGroupInformation)" value="true" styleClass="checkbox"/>&nbsp;<bean:message key="reports.current.member_group_information"/></td>
                	</tr>
	            	<tr>
	            		<td align="left" colspan="2"><html:checkbox property="currentStateReport(ads)" styleClass="checkbox" value="true"/>&nbsp;<bean:message key="reports.current.ads_information"/>&nbsp;</td>
                	</tr>
	            	<tr>
	            		<td align="left" colspan="2"><html:checkbox property="currentStateReport(systemAccountInformation)" value="true" styleClass="checkbox"/>&nbsp;<bean:message key="reports.current.system_account"/>&nbsp;</td>
                	</tr>
	            	<tr>
	            		<td align="left" colspan="2"><html:checkbox property="currentStateReport(memberAccountInformation)" value="true" styleClass="checkbox"/>&nbsp;<bean:message key="reports.current.member_account_information"/></td>
                	</tr>
	            	<tr>
	            		<td align="left" colspan="2"><html:checkbox property="currentStateReport(invoices)" value="true" styleClass="checkbox"/>&nbsp;<bean:message key="reports.current.invoices"/>&nbsp;</td>
                	</tr>
	            	<tr>
	            		<td align="left" colspan="2"><html:checkbox property="currentStateReport(loans)" value="true" styleClass="checkbox"/>&nbsp;<bean:message key="reports.current.loans"/>&nbsp;</td>
                	</tr>
	            	<tr>
	            		<td align="left" colspan="2"><html:checkbox property="currentStateReport(references)" styleClass="checkbox" value="true"/>&nbsp;<bean:message key="reports.current.references"/>&nbsp;</td>
                	</tr>
                	<tr>
                    	<td align="left">
                    		<input id="selectAllButton" type="button" class="button" value="<bean:message key="global.selectAll"/>">&nbsp;&nbsp;
							<input id="selectNoneButton" type="button" class="button" value="<bean:message key="global.selectNone"/>">&nbsp;&nbsp;
                    	</td>
                    	<td align="right">
                     		<html:submit styleClass="button" tabindex="12" >
                     			<bean:message key="reports.current.show_btn"/>
                     		</html:submit>
                    	</td>
                    </tr>
				</table>
			</td>
		</tr>
    </ssl:form>
</table>
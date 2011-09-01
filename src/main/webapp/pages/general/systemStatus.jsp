<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<script>
	var uptimeMessage = "<cyclos:escapeJS><bean:message key="home.admin.status.systemUptime.message" arg0="#days#" arg1="#hours#"/></cyclos:escapeJS>";
</script>
<cyclos:script src="/pages/general/systemStatus.js" />

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="home.admin.status.title"/></td>
        <cyclos:help page="home#home_status"/>
    </tr>
    <tr> 
        <td class="tdContentTableForms" colspan="2">
   	        <table class="defaultTable">
                <tr>
                   	<td width="35%" class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.cyclosVersion"/></td>
               	    <td colspan="2">${applicationStatus.cyclosVersion}</td>
                </tr>
                <tr>
                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.systemUptime"/></td>
               	    <td colspan="2" id="uptime"><bean:message key="home.admin.status.systemUptime.message" arg0="${applicationStatus.uptimeDays}" arg1="${applicationStatus.uptimeHours}"/></td>
                </tr>
                <c:if test="${cyclos:granted('adminMemberMessages', 'view')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.unreadMessages"/></td>
	               	    <td colspan="2"><a id="unreadMessages">${applicationStatus.unreadMessages}</a></td>
	                </tr>
                </c:if>
                <c:if test="${cyclos:granted('adminMemberInvoices', 'accept') || cyclos:granted('adminMemberInvoices', 'cancel') || cyclos:granted('adminMemberInvoices', 'deny')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.openInvoices"/></td>
	               	    <td colspan="2"><a id="openInvoices">${applicationStatus.openInvoices}</a></td>
	                </tr>
                </c:if>
                <c:if test="${cyclos:granted('systemStatus', 'viewConnectedAdmins')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.connectedAdmins"/></td>
	               	    <td colspan="2"><a id="connectedAdmins">${applicationStatus.connectedAdmins}</a></td>
	                </tr>
	            </c:if>
                <c:if test="${cyclos:granted('systemStatus', 'viewConnectedMembers')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.connectedMembers"/></td>
	               	    <td colspan="2"><a id="connectedMembers">${applicationStatus.connectedMembers}</a></td>
	                </tr>
                </c:if>
                <c:if test="${cyclos:granted('systemStatus', 'viewConnectedBrokers')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.connectedBrokers"/></td>
	               	    <td colspan="2"><a id="connectedBrokers">${applicationStatus.connectedBrokers}</a></td>
	                </tr>
                </c:if>
                <c:if test="${cyclos:granted('systemStatus', 'viewConnectedOperators')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.connectedOperators"/></td>
	               	    <td colspan="2"><a id="connectedOperators">${applicationStatus.connectedOperators}</a></td>
	                </tr>
                </c:if>
                <c:if test="${cyclos:granted('systemAlerts', 'viewSystemAlerts')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.systemAlerts"/></td>
	               	    <td colspan="2"><a id="systemAlerts">${applicationStatus.systemAlerts}</a></td>
	                </tr>
	            </c:if>
	            <c:if test="${cyclos:granted('systemAlerts', 'viewMemberAlerts')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.memberAlerts"/></td>
	               	    <td colspan="2"><a id="memberAlerts">${applicationStatus.memberAlerts}</a></td>
	                </tr>
                </c:if>
	            <c:if test="${cyclos:granted('systemErrorLog', 'view')}">
	                <tr>
	                   	<td class="headerLabel" nowrap="nowrap"><bean:message key="home.admin.status.errors"/></td>
	               	    <td><a id="errors">${applicationStatus.errors}</a></td>
	               	    <td align="right" nowrap="nowrap"><a id="refreshStatus" class="default"><bean:message key="home.admin.status.refresh"/></a></td>
	                </tr>
                </c:if>
			</table>
        </td>
   </tr>
</table>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<jsp:include page="/pages/members/includes/profileOfMemberByMember.jsp"/>
<br/>

<%-- The broker can manage password if any of the below permissions are granted --%>
<c:set var="canManagePasswords" value="${cyclos:granted('brokerMemberAccess', 'changePassword')}"/>
<c:set var="canManageExternalAccess" value="${cyclos:granted('brokerMemberAccess', 'changePin') || cyclos:granted('brokerMemberAccess', 'changeChannelsAccess')}"/>
<c:set var="managePasswordsKey" value="profile.action.manageLoginPassword"/>
<c:if test="${member.group.memberSettings.sendPasswordByEmail && cyclos:granted('brokerMemberAccess', 'resetPassword')}">
	<c:set var="canManagePasswords" value="${true}"/>
</c:if>
<c:if test="${member.group.basicSettings.transactionPassword.used && cyclos:granted('brokerMemberAccess', 'transactionPassword')}">
	<c:set var="canManagePasswords" value="${true}"/>
	<c:set var="managePasswordsKey" value="profile.action.managePasswords"/>
</c:if>

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="profile.action.byBroker.title" arg0="${member.name}"/></td>
        <cyclos:help page="profiles#actions_for_member_by_broker" />
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableForms">
			<cyclos:layout className="defaultTable" columns="4">
				<c:if test="${!removed}">
					<c:choose>
						<c:when test="${cyclos:granted('brokerAds', 'manage')}">
							<cyclos:cell width="35%" className="label"><bean:message key="profile.action.manageAds"/></cyclos:cell>
							<cyclos:cell align="left"><input type="button" class="linkButton" linkURL="memberAds?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
						</c:when>
		            </c:choose>
				</c:if>
				<c:if test="${cyclos:granted('brokerPreferences', 'manageNotifications')}">
					<cyclos:cell width="35%" className="label"><bean:message key="profile.action.manageNotifications"/></cyclos:cell>
					<cyclos:cell align="left"><input type="button" class="linkButton" linkURL="notificationPreferences?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>				
				<c:if test="${hasAccounts && cyclos:granted('brokerAccounts', 'information')}">
					<cyclos:cell width="35%" className="label"><bean:message key="profile.action.accountInformation"/></cyclos:cell>
					<cyclos:cell align="left"><input type="button" class="linkButton" linkURL="accountOverview?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerAccounts', 'scheduledInformation')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.scheduledPayments"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="searchScheduledPayments?&memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${hasAccounts && cyclos:granted('brokerAccounts', 'authorizedInformation')}">
					<cyclos:cell width="35%" className="label"><bean:message key="profile.action.transferAuthorizations"/></cyclos:cell>
					<cyclos:cell align="left"><input type="button" class="linkButton" linkURL="searchTransferAuthorizations?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerMemberPayments', 'paymentAsMemberToMember')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.paymentAsMemberToMember"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="payment?selectMember=true&from=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerMemberPayments', 'paymentAsMemberToSelf')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.memberSelfPayment"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="selfPayment?from=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerMemberPayments', 'paymentAsMemberToSystem')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.paymentAsMemberToSystem"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="payment?toSystem=true&from=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerReferences', 'manage')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.manageReferences"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="references?nature=GENERAL&memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerLoans', 'view')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.manageLoans"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="searchLoans?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerInvoices', 'view')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.invoices"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="searchInvoices?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerInvoices', 'sendAsMemberToMember')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.invoiceAsMemberToMember"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="sendInvoice?selectMember=true&from=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerInvoices', 'sendAsMemberToSystem')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.invoiceAsMemberToSystem"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="sendInvoice?toSystem=true&from=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && hasAccounts && cyclos:granted('brokerAccounts', 'brokerSimulateConversion')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.brokerSimulateConversion"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="simulateConversion?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && cyclos:granted('brokerLoanGroups', 'view')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.loanGroups"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="memberLoanGroups?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
                <c:if test="${!removed && canManagePasswords}">
                    <cyclos:cell width="35%" className="label"><bean:message key="${managePasswordsKey}"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="managePasswords?userId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>
                <c:if test="${!removed && canManageExternalAccess && memberCanAccessExternalChannels}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.manageExternalAccess"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="manageExternalAccess?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>
                <c:forEach var="entry" items="${countByRecordType}">
					<c:set var="recordType" value="${entry.key}" />
					<c:set var="recordCount" value="${entry.value}" />
                    <cyclos:cell width="35%" className="label">${recordType.label} (${recordCount})</cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="${cyclos:name(recordType.layout) == 'FLAT' ? 'flatMemberRecords' : 'searchMemberRecords'}?global=false&elementId=${member.id}&typeId=${recordType.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
	            </c:forEach>             
				<c:if test="${!removed && (cyclos:granted('brokerDocuments', 'view') || cyclos:granted('brokerDocuments', 'viewMember') || cyclos:granted('brokerDocuments', 'manageMember'))}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.viewDocuments"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="selectDocument?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${!removed && cyclos:granted('brokerMembers', 'manageContracts')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.listBrokerCommissionContracts"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="listBrokerCommissionContracts?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
				<c:if test="${cyclos:granted('brokerMemberSms', 'view')}">
	                <cyclos:cell width="35%" className="label"><bean:message key="profile.action.smsLogs"/></cyclos:cell>
	                <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="searchSmsLogs?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
	            </c:if>
                <c:if test="${cyclos:granted('brokerReports', 'view')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.activities"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="activities?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>	            
	            <c:if test="${!removed && cyclos:granted('brokerCards', 'view') && hasCardType && byBroker}">
	                <cyclos:cell width="35%" className="label"><bean:message key="profile.action.manageCards"/></cyclos:cell>
	                <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="searchCards?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>	                
	            </c:if>
	            <c:if test="${!removed && cyclos:granted('brokerPos', 'view')}">
	                <cyclos:cell width="35%" className="label"><bean:message key="profile.action.memberPos"/></cyclos:cell>
	                <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="memberPos?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
	                <c:set var="show" value="${true}"/>
	            </c:if>
            </cyclos:layout>
       	</td>
   	</tr>
</table>
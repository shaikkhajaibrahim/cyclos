<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<cyclos:menu url="/do/admin/home" key="menu.admin.home" />
<cyclos:menu key="menu.admin.personal">
	<cyclos:menu url="/do/admin/adminProfile" key="menu.admin.personal.profile" />
	<cyclos:menu url="/do/admin/changePassword" key="menu.admin.personal.changePassword" />
	<cyclos:menu url="/do/admin/mailPreferences" key="menu.admin.personal.mailPreferences" />
</cyclos:menu>

<cyclos:menu key="menu.admin.alerts">
	<cyclos:menu url="/do/admin/systemAlerts" key="menu.admin.alerts.system" module="systemAlerts" operation="viewSystemAlerts" />
	<cyclos:menu url="/do/admin/memberAlerts" key="menu.admin.alerts.member" module="systemAlerts" operation="viewMemberAlerts" />
	<c:if test="${cyclos:granted('systemAlerts', 'viewSystemAlerts') || cyclos:granted('systemAlerts', 'viewMemberAlerts')}">
		<cyclos:menu url="/do/admin/searchAlerts" key="menu.admin.alerts.alertHistory" />
	</c:if>
	<cyclos:menu url="/do/admin/viewErrorLog" key="menu.admin.alerts.errorLog" module="systemErrorLog" operation="view" />
	<cyclos:menu url="/do/admin/searchErrorLog" key="menu.admin.alerts.errorLogHistory" module="systemErrorLog" operation="view" />
</cyclos:menu>

<cyclos:menu key="menu.admin.accounts">
	<cyclos:menu url="/do/admin/listCurrencies" key="menu.admin.accounts.currencies" module="systemCurrencies" operation="view" />
	<cyclos:menu url="/do/admin/listAccountTypes" key="menu.admin.accounts.manage" module="systemAccounts" operation="view" />
	<cyclos:menu url="/do/admin/accountOverview" key="menu.admin.accounts.details" module="systemAccounts" operation="information" />
	<c:if test="${cyclos:granted('adminMemberInvoices', 'accept') || cyclos:granted('adminMemberInvoices', 'cancel') || cyclos:granted('adminMemberInvoices', 'deny')}">
		<cyclos:menu url="/do/admin/searchInvoices" key="menu.admin.accounts.invoices"/>
	</c:if>
	<c:if test="${cyclos:granted('systemPayments', 'authorize') || cyclos:granted('adminMemberPayments', 'authorize')}">
		<cyclos:menu url="/do/admin/transfersAwaitingAuthorization" key="menu.admin.accounts.transfersAwaitingAuthorization" />
		<cyclos:menu url="/do/admin/searchTransferAuthorizations" key="menu.admin.accounts.transfersAuthorizations" />
	</c:if>
	<cyclos:menu url="/do/admin/searchScheduledPayments" key="menu.admin.accounts.scheduledPayments" module="systemAccounts" operation="scheduledInformation" />
	<cyclos:menu url="/do/admin/selfPayment" key="menu.admin.accounts.systemPayment" module="systemPayments" operation="payment" />
	<cyclos:menu url="/do/admin/payment?selectMember=true" key="menu.admin.accounts.memberPayment" module="adminMemberPayments" operation="directPayment" />
	<cyclos:menu url="/do/admin/sendInvoice?selectMember=true" key="menu.admin.accounts.memberInvoice" module="adminMemberInvoices" operation="directSend" />
	<cyclos:menu url="/do/admin/listAccountFeeLog" key="menu.admin.accounts.accountFees" module="systemAccountFees" operation="view" />
	<cyclos:menu url="/do/admin/searchLoans" key="menu.admin.accounts.loans" module="adminMemberLoans" operation="view" />
	<cyclos:menu url="/do/admin/searchLoanPayments" key="menu.admin.accounts.loanPayments" module="adminMemberLoans" operation="view" />
</cyclos:menu>

<c:if test="${cyclos:granted('adminMemberTokens', 'resetTokenPin')}">
  <cyclos:menu key="menu.member.tokens">
	<cyclos:menu url="/do/admin/resetPinToken" key="menu.admin.tokens.resetPin" />
  </cyclos:menu>
</c:if>

<cyclos:menu key="menu.admin.bookkeeping">
	<cyclos:menu url="/do/admin/listExternalAccounts" key="menu.admin.bookkeeping.accounts" module="systemExternalAccounts" operation="view" />
	<cyclos:menu url="/do/admin/overviewExternalAccounts" key="menu.admin.bookkeeping.overview" module="systemExternalAccounts" operation="details" />	
</cyclos:menu>
<cyclos:menu key="menu.admin.guarantees">
	<cyclos:menu url="/do/admin/listGuaranteeTypes" key="menu.admin.guarantees.listGuaranteeTypes" module="systemGuaranteeTypes" operation="view"/>
	<cyclos:menu url="/do/admin/searchCertifications" key="menu.admin.guarantees.searchCertifications" module="adminMemberGuarantees" operation="viewCertifications"/>
	<cyclos:menu url="/do/admin/searchGuarantees" key="menu.admin.guarantees.searchGuarantees" module="adminMemberGuarantees" operation="viewGuarantees"/>
	<cyclos:menu url="/do/admin/searchPaymentObligations" key="menu.admin.guarantees.searchPaymentObligations" module="adminMemberGuarantees" operation="viewPaymentObligations"/>
</cyclos:menu>
<cyclos:menu key="menu.admin.usersGroups">
	<cyclos:menu url="/do/admin/searchMembers" key="menu.admin.usersGroups.members" module="adminMembers" operation="view" />
	<c:if test="${cyclos:granted('adminMemberBulkActions', 'changeGroup') || cyclos:granted('adminMemberBulkActions', 'changeBroker')}">
		<cyclos:menu url="/do/admin/memberBulkActions" key="menu.admin.usersGroups.membersBulkAction"/>
	</c:if>
	<cyclos:menu url="/do/admin/importMembers" key="menu.admin.usersGroups.importMembers" module="adminMembers" operation="import" />
	<cyclos:menu url="/do/admin/searchAdmins" key="menu.admin.usersGroups.admins" module="adminAdmins" operation="view" />
	<c:if test="${cyclos:granted('systemStatus', 'viewConnectedAdmins') || cyclos:granted('systemStatus', 'viewConnectedBrokers') || cyclos:granted('systemStatus', 'viewConnectedMembers')}">
		<cyclos:menu url="/do/admin/listConnectedUsers" key="menu.admin.usersGroups.connectedUsers"/>
	</c:if>
	<cyclos:menu url="/do/admin/searchPendingMembers" key="menu.admin.usersGroups.pendingMembers" module="adminMembers" operation="managePending" />
	<cyclos:menu url="/do/admin/listRegistrationAgreements" key="menu.admin.usersGroups.registrationAgreements" module="systemRegistrationAgreements" operation="view" />
	<c:if test="${cyclos:granted('systemAdminGroups', 'view') || cyclos:granted('adminMemberGroups', 'view') }">
		<cyclos:menu url="/do/admin/listGroups" key="menu.admin.usersGroups.groups"/>
	</c:if>
	<cyclos:menu url="/do/admin/listGroupFilters" key="menu.admin.usersGroups.groupFilters" module="systemGroupFilters" operation="view" />
	<cyclos:menu url="/do/admin/searchLoanGroups" key="menu.admin.usersGroups.loanGroups" module="systemLoanGroups" operation="view" />
	<cyclos:menu url="/do/admin/listMemberRecordTypes" key="menu.admin.usersGroups.memberRecordTypes" module="systemMemberRecordTypes" operation="view" />
	<c:forEach var="memberRecordType" items="${memberRecordTypesInMenu}">
		<cyclos:menu url="/do/admin/searchMemberRecords?typeId=${memberRecordType.id}&global=true" label="${memberRecordType.label}" />
	</c:forEach>
</cyclos:menu>
<cyclos:menu key="menu.admin.ads">
	<cyclos:menu url="/do/admin/searchAds" key="menu.admin.ads.search" module="adminMemberAds" />
	<cyclos:menu url="/do/admin/listAdCategories" key="menu.admin.ads.categories" module="systemAdCategories" />
	<cyclos:menu url="/do/admin/importAds" key="menu.admin.ads.importAds" module="adminMemberAds" operation="import" />
	<cyclos:menu url="/do/admin/manageAdCategories" key="menu.admin.ads.categories.file" module="systemAdCategories" operation="file" />
</cyclos:menu>
<cyclos:menu key="menu.admin.accessDevices">
	<cyclos:menu url="/do/admin/manageCardTypes" key="menu.admin.accessDevices.cardType.manage" module="systemCardTypes" operation="view" />
	<cyclos:menu url="/do/admin/searchCards" key="menu.admin.accessDevices.cards.search" module="adminMemberCards" operation="view" />
	<cyclos:menu url="/do/admin/searchPos" key="menu.admin.accessDevices.pos.search" module="adminMemberPos" operation="view" />
</cyclos:menu>
<cyclos:menu key="menu.admin.messages">
	<cyclos:menu url="/do/admin/searchMessages" key="menu.admin.messages.messages" module="adminMemberMessages" operation="view" />
	<cyclos:menu url="/do/admin/listMessageCategories" key="menu.admin.messages.messageCategory" module="systemMessageCategories" operation="view"/>
	<cyclos:menu url="/do/admin/searchSmsMailings" key="menu.admin.messages.smsMailings" module="adminMemberSmsMailings" operation="view"/>
</cyclos:menu>
<cyclos:menu key="menu.admin.settings">
	<cyclos:menu url="/do/admin/editLocalSettings" key="menu.admin.settings.local" module="systemSettings" operation="view" />
	<cyclos:menu url="/do/admin/editAlertSettings" key="menu.admin.settings.alert" module="systemSettings" operation="view" />
	<cyclos:menu url="/do/admin/editAccessSettings" key="menu.admin.settings.access" module="systemSettings" operation="view" />
	<cyclos:menu url="/do/admin/editMailSettings" key="menu.admin.settings.mail" module="systemSettings" operation="view" />
	<cyclos:menu url="/do/admin/editLogSettings" key="menu.admin.settings.log" module="systemSettings" operation="view" />
	<cyclos:menu url="/do/admin/listChannels" key="menu.admin.settings.channels" module="systemChannels" operation="view" />
	<cyclos:menu url="/do/admin/searchServiceClients" key="menu.admin.settings.serviceClients" module="systemServiceClients" operation="view" />
	<cyclos:menu url="/do/admin/adminTasks" key="menu.admin.settings.adminTasks" module="systemTasks" />
	<cyclos:menu url="/do/admin/manageSettings" key="menu.admin.settings.file" module="systemSettings" operation="file" />
</cyclos:menu>
<cyclos:menu key="menu.admin.customFields" module="systemCustomFields">
	<cyclos:menu url="/do/admin/listCustomFields?nature=MEMBER" key="menu.admin.customFields.memberFields"/>
	<cyclos:menu url="/do/admin/listCustomFields?nature=ADMIN" key="menu.admin.customFields.adminFields"/>
	<cyclos:menu url="/do/admin/listCustomFields?nature=AD" key="menu.admin.customFields.adFields"/>
	<cyclos:menu url="/do/admin/listCustomFields?nature=LOAN_GROUP" key="menu.admin.customFields.loanGroupFields"/>
</cyclos:menu>
<cyclos:menu key="menu.admin.contentManagement">
	<cyclos:menu url="/do/admin/listCustomizedFiles?type=STATIC_FILE" key="menu.admin.contentManagement.staticFiles" module="systemCustomizedFiles" operation="view" />
	<cyclos:menu url="/do/admin/listCustomizedFiles?type=HELP" key="menu.admin.contentManagement.helpFiles" module="systemCustomizedFiles" operation="view" />
	<cyclos:menu url="/do/admin/listCustomizedFiles?type=STYLE" key="menu.admin.contentManagement.cssFiles" module="systemCustomizedFiles" operation="view" />
	<cyclos:menu url="/do/admin/listCustomizedFiles?type=APPLICATION_PAGE" key="menu.admin.contentManagement.applicationPage" module="systemCustomizedFiles" operation="view" />
	<cyclos:menu url="/do/admin/systemImages" key="menu.admin.contentManagement.systemImages" module="systemCustomImages" operation="view" />
	<cyclos:menu url="/do/admin/customImages?nature=CUSTOM" key="menu.admin.contentManagement.customImages" module="systemCustomImages" operation="view" />
	<cyclos:menu url="/do/admin/customImages?nature=STYLE" key="menu.admin.contentManagement.styleImages" module="systemCustomImages" operation="view" />
	<cyclos:menu url="/do/admin/selectTheme" key="menu.admin.contentManagement.manageThemes" module="systemThemes" />
	<c:if test="${cyclos:granted('adminMemberDocuments', 'manageDynamic') || cyclos:granted('adminMemberDocuments', 'manageStatic') }">
		<cyclos:menu url="/do/admin/listDocuments" key="menu.admin.contentManagement.documents"/>
	</c:if>
	<cyclos:menu url="/do/admin/searchInfoTexts" key="menu.admin.messages.infoTexts" module="systemInfoTexts" operation="view"/>	
</cyclos:menu>
<cyclos:menu key="menu.admin.translation">
	<cyclos:menu url="/do/admin/searchTranslationMessages" key="menu.admin.translation.application" module="systemTranslation" operation="view" />
	<cyclos:menu url="/do/admin/listMessageSettings" key="menu.admin.translation.internalMessages" module="systemTranslation" operation="manageNotification" />
	<cyclos:menu url="/do/admin/editMailTranslation" key="menu.admin.translation.mails" module="systemTranslation" operation="manageMailTranslation" />
	<cyclos:menu url="/do/admin/manageTranslationMessages" key="menu.admin.translation.file" module="systemTranslation" operation="file" />
</cyclos:menu>
<cyclos:menu key="menu.admin.reports">
	<cyclos:menu url="/do/admin/reportsCurrentState" key="menu.admin.reports.current" module="systemReports" operation="current" />
	<cyclos:menu url="/do/admin/membersListReport" key="menu.admin.reports.members.list" module="systemReports" operation="memberList" />
	<cyclos:menu url="/do/admin/membersTransactionsReport" key="menu.admin.reports.members" module="systemReports" operation="memberList" />
	<cyclos:menu url="/do/admin/membersSmsLogsReport" key="menu.admin.reports.sms" module="systemReports" operation="smsLogs" />
	<cyclos:menu url="/do/admin/statistics" key="menu.admin.reports.statistics" module="systemReports" operation="statistics" />
</cyclos:menu>
<cyclos:menu key="menu.admin.help">
	<cyclos:menu url="/do/admin/manual" key="menu.admin.help.manual" />
	<cyclos:menu url="/do/admin/about" key="menu.about" />
</cyclos:menu>
<cyclos:menu url="/do/logout" key="menu.admin.logout" confirmationKey="menu.logout.confirmationMessage"/>
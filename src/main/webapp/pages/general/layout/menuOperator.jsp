<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<cyclos:menu url="/do/operator/home" key="menu.operator.home" />
<cyclos:menu key="menu.operator.personal">
	<cyclos:menu url="/do/operator/operatorProfile" key="menu.operator.personal.profile" />
	<cyclos:menu url="/do/operator/changePassword" key="menu.operator.personal.changePassword" />
</cyclos:menu>

<!-- check this menu -- change it -->
<cyclos:menu key="menu.operator.member" >
	<cyclos:menu url="/do/operator/memberAds" key="menu.operator.member.ads" module="operatorAds" operation="publish" />
	<cyclos:menu url="/do/operator/searchMessages" key="menu.operator.member.messages" module="operatorMessages" operation="view" />
	<cyclos:menu url="/do/operator/contacts" key="menu.operator.member.contacts" module="operatorContacts" operation="view" />
	<c:if test="${loggedMemberHasGeneralReferences}">
		<cyclos:menu url="/do/operator/references?nature=GENERAL" key="menu.operator.member.references" module="operatorReferences" operation="manageMemberReferences" />
	</c:if>
	<c:if test="${loggedMemberHasTransactionFeedbacks}">
		<cyclos:menu url="/do/operator/references?nature=TRANSACTION" key="menu.operator.member.transactionFeedbacks" module="operatorReferences" operation="manageMemberTransactionFeedbacks" />
	</c:if>
	<cyclos:menu url="/do/operator/activities" key="menu.operator.member.activities" module="operatorReports" operation="viewMember" />
</cyclos:menu>

<!-- Operator Menu -->
<cyclos:menu key="menu.operator.guarantees">
	<c:if test="${cyclos:granted('operatorGuarantees', 'issueCertifications') || cyclos:granted('operatorGuarantees', 'buyWithPaymentObligations')}">
		<cyclos:menu url="/do/operator/searchCertifications" key="menu.operator.guarantees.searchCertifications"/>
	</c:if>
	<c:if test="${loggedMemberHasGuarantees}">
		<cyclos:menu url="/do/operator/searchGuarantees" key="menu.operator.guarantees.searchGuarantees" />
	</c:if>
	<c:if test="${cyclos:granted('operatorGuarantees', 'sellWithPaymentObligations') || cyclos:granted('operatorGuarantees', 'buyWithPaymentObligations')}">
		<cyclos:menu url="/do/operator/searchPaymentObligations" key="menu.operator.guarantees.searchPaymentObligations" />
	</c:if>
</cyclos:menu>

<c:if test="${loggedMemberHasAccounts}">
	<%-- Show the account menu only if the member has at least one account --%>
	<cyclos:menu key="menu.operator.account">
		<cyclos:menu url="/do/operator/accountOverview" key="menu.operator.account.accountInformation" />
		<c:if test="${cyclos:granted('operatorPayments', 'authorize')}">
			<cyclos:menu url="/do/operator/transfersAwaitingAuthorization" key="menu.operator.account.transfersAwaitingAuthorization" />
			<cyclos:menu url="/do/operator/searchTransferAuthorizations" key="menu.operator.account.transfersAuthorizations" />
		</c:if>	
		<cyclos:menu url="/do/operator/searchScheduledPayments" key="menu.operator.account.scheduledPayments" module="operatorAccount" operation="scheduledInformation" />
		<cyclos:menu url="/do/operator/searchInvoices" key="menu.operator.account.invoices" module="operatorInvoices" operation="view"/>
		<cyclos:menu url="/do/operator/searchLoans" key="menu.operator.account.loans" module="operatorLoans" operation="view"/>
		<cyclos:menu url="/do/operator/payment?selectMember=true" key="menu.operator.account.memberPayment" module="operatorPayments" operation="directPaymentToMember" />
		<cyclos:menu url="/do/operator/payment?toSystem=true" key="menu.operator.account.systemPayment" module="operatorPayments" operation="paymentToSystem" />
		<cyclos:menu url="/do/operator/selfPayment" key="menu.operator.account.selfPayment" module="operatorPayments" operation="paymentToSelf" />
		<cyclos:menu url="/do/operator/requestPayment" key="menu.operator.account.requestPayment" module="operatorPayments" operation="request"  />
		<c:if test="${cyclos:granted('operatorInvoices', 'directSendToMember') && cyclos:granted('operatorInvoices', 'sendToMember')}">
			<cyclos:menu url="/do/operator/sendInvoice?selectMember=true" key="menu.operator.account.memberInvoice"/>
		</c:if>
		<cyclos:menu url="/do/operator/sendInvoice?toSystem=true" key="menu.operator.account.systemInvoice" module="operatorInvoices" operation="sendToSystem" />
		<cyclos:menu url="/do/operator/simulateConversion" key="menu.operator.account.simulateConversion" module="operatorAccount" operation="simulateConversion" />
	</cyclos:menu>
</c:if>

<cyclos:menu key="menu.operator.search">
	<cyclos:menu url="/do/operator/searchMembers" key="menu.operator.search.members" module="memberProfile" operation="view" />
	<cyclos:menu url="/do/operator/searchAds" key="menu.operator.search.ads" module="memberAds" operation="view" />
</cyclos:menu>

<cyclos:menu key="menu.operator.help">
	<cyclos:menu url="/do/operator/contactUs" key="menu.contact" />
	<cyclos:menu url="/do/operator/manual" key="menu.operator.help.manual" />
	<cyclos:menu url="/do/operator/about" key="menu.about" />
</cyclos:menu>

<cyclos:menu url="/do/logout" key="menu.operator.logout" confirmationKey="menu.logout.confirmationMessage"/>
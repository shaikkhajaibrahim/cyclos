<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<cyclos:menu url="/do/member/home" key="menu.member.home" />
<cyclos:menu key="menu.member.personal">
	<cyclos:menu url="/do/member/profile" key="menu.member.personal.profile" />
	<cyclos:menu url="/do/member/searchMessages" key="menu.member.personal.messages" module="memberMessages" operation="view" />
	<cyclos:menu url="/do/member/memberAds" key="menu.member.personal.ads" module="memberAds" operation="publish" />
	<cyclos:menu url="/do/member/contacts" key="menu.member.personal.contacts" />
	<c:if test="${loggedMemberHasGeneralReferences}">
		<cyclos:menu url="/do/member/references?nature=GENERAL" key="menu.member.personal.references" />
	</c:if>
	<c:if test="${loggedMemberHasTransactionFeedbacks}">
		<cyclos:menu url="/do/member/references?nature=TRANSACTION" key="menu.member.personal.transactionFeedbacks" />
	</c:if>
	<cyclos:menu url="/do/member/activities" key="menu.member.personal.activities" />
	<c:if test="${loggedMemberHasDocuments}">
		<cyclos:menu url="/do/member/selectDocument" key="menu.member.personal.documents"/>
	</c:if>
	<cyclos:menu url="/do/member/changePassword" key="menu.member.personal.changePassword" />
	<c:if test="${hasExternalChannels && cyclos:granted('memberProfile', 'manageExternalAccess')}">
		<cyclos:menu url="/do/member/manageExternalAccess" key="menu.member.personal.manageExternalAccess" />
	</c:if>
	<cyclos:menu url="/do/member/searchSmsLogs" key="menu.member.personal.sms" module="memberSms" operation="view"/>
	<c:if test="${not empty loggedElement.broker}">
		<cyclos:menu url="/do/member/listBrokerCommissionContracts" key="menu.member.personal.commissionChargeStatus" module="memberCommissions" operation="view"/>
	</c:if>
	<c:if test="${hasCards}">
		<cyclos:menu url="/do/member/searchCards?memberId=${loggedUser.id}" key="menu.member.personal.cards" module="memberCards" operation="view"/>
	</c:if>
	<c:if test="${hasPos}">
		<c:choose>
			<c:when test="${uniqueMemberPosId}">
				<cyclos:menu url="/do/member/editPos?id=${uniqueMemberPosId}" key="menu.member.personal.pos.editPos" />
			</c:when>
			<c:otherwise>
				<cyclos:menu url="/do/member/memberPos?memberId=${loggedUser.id}" key="menu.member.personal.pos.memberPos" />
			</c:otherwise>

		</c:choose>
	</c:if>
</cyclos:menu>
<c:if test="${loggedMemberHasAccounts}">
	<%-- Show the account menu only if the member has at least one account --%>
	<cyclos:menu key="menu.member.account">
		<cyclos:menu url="/do/member/accountOverview" key="menu.member.account.accountInformation" />
		<c:if test="${!isBroker && cyclos:granted('memberPayments', 'authorize')}">
			<cyclos:menu url="/do/member/transfersAwaitingAuthorization" key="menu.member.account.transfersAwaitingAuthorization" />
			<cyclos:menu url="/do/member/searchTransferAuthorizations" key="menu.member.account.transfersAuthorizations" />
		</c:if>
		<cyclos:menu url="/do/member/searchScheduledPayments" key="menu.member.account.scheduledPayments" module="memberAccount" operation="scheduledInformation" />
		<cyclos:menu url="/do/member/searchInvoices" key="menu.member.account.invoices" module="memberInvoices" operation="view"/>
		<cyclos:menu url="/do/member/searchLoans" key="menu.member.account.loans" module="memberLoans" operation="view"/>
		<c:if test="${loggedMemberHasLoanGroups}">
			<cyclos:menu url="/do/member/memberLoanGroups" key="menu.member.account.loanGroups" />
		</c:if>
		<cyclos:menu url="/do/member/payment?selectMember=true" key="menu.member.account.memberPayment" module="memberPayments" operation="directPaymentToMember" />
		<cyclos:menu url="/do/member/payment?toSystem=true" key="menu.member.account.systemPayment" module="memberPayments" operation="paymentToSystem"  />
		<cyclos:menu url="/do/member/selfPayment" key="menu.member.account.selfPayment" module="memberPayments" operation="paymentToSelf" />
		<cyclos:menu url="/do/member/requestPayment" key="menu.member.account.requestPayment" module="memberPayments" operation="request"  />
		<c:if test="${cyclos:granted('memberInvoices', 'directSendToMember') && cyclos:granted('memberInvoices', 'sendToMember')}">
			<cyclos:menu url="/do/member/sendInvoice?selectMember=true" key="menu.member.account.memberInvoice"/>
		</c:if>
		<cyclos:menu url="/do/member/sendInvoice?toSystem=true" key="menu.member.account.systemInvoice" module="memberInvoices" operation="sendToSystem" />
		<cyclos:menu url="/do/member/simulateConversion" key="menu.member.account.simulateConversion" module="memberAccount" operation="simulateConversion" />
	</cyclos:menu>

	<cyclos:menu key="menu.member.tokens">
		<cyclos:menu url="/do/member/listTokens" key="menu.member.tokens.list" />
		<c:if test="${isBroker}">
		    <cyclos:menu url="/do/member/redeemToken" key="menu.member.tokens.redeem" />
            <cyclos:menu url="/do/member/refundToken" key="menu.member.tokens.refund" />
            <cyclos:menu url="/do/member/senderTokenRedemption" key="menu.member.tokens.senderTokenRedemption" />
		</c:if>
	</cyclos:menu>

</c:if>
<cyclos:menu key="menu.member.operators" module="memberOperators" operation="manage">
	<cyclos:menu url="/do/member/searchOperators" key="menu.member.operators"/>
	<cyclos:menu url="/do/member/listConnectedUsers" key="menu.member.connectedOperators"/>
	<cyclos:menu url="/do/member/listGroups" key="menu.member.operators.groups"/>
	<cyclos:menu url="/do/member/listCustomFields?nature=OPERATOR" key="menu.member.operators.customFields"/>
</cyclos:menu>

<cyclos:menu key="menu.member.preferences" module="memberPreferences">
	<cyclos:menu url="/do/member/notificationPreferences" key="menu.member.preferences.notification" module="memberPreferences" operation="manageNotifications"/>
	<cyclos:menu url="/do/member/listAdInterests" key="menu.member.preferences.adInterests" module="memberPreferences" operation="manageAdInterests"/>
</cyclos:menu>
<cyclos:menu key="menu.member.search">
	<cyclos:menu url="/do/member/searchMembers" key="menu.member.search.members" module="memberProfile" operation="view" />
	<cyclos:menu url="/do/member/searchAds" key="menu.member.search.ads" module="memberAds" operation="view" />
</cyclos:menu>
<cyclos:menu key="menu.member.guarantees">
	<c:if test="${cyclos:granted('memberGuarantees', 'issueCertifications') || cyclos:granted('memberGuarantees', 'buyWithPaymentObligations')}">
		<cyclos:menu url="/do/member/searchCertifications" key="menu.member.guarantees.searchCertifications"/>
	</c:if>
	<c:if test="${loggedMemberHasGuarantees}">
		<cyclos:menu url="/do/member/searchGuarantees" key="menu.member.guarantees.searchGuarantees" />
	</c:if>
	<c:if test="${cyclos:granted('memberGuarantees', 'sellWithPaymentObligations') || cyclos:granted('memberGuarantees', 'buyWithPaymentObligations')}">
		<cyclos:menu url="/do/member/searchPaymentObligations" key="menu.member.guarantees.searchPaymentObligations" />
	</c:if>
</cyclos:menu>
<c:if test="${isBroker}">
	<cyclos:menu key="menu.member.broker">
		<cyclos:menu url="/do/member/createMember" key="menu.member.broker.registerMember" module="brokerMembers" operation="register" />
		<cyclos:menu url="/do/member/listBrokerings" key="menu.member.broker.listMembers" />
		<cyclos:menu url="/do/member/searchPendingMembers" key="menu.member.broker.pendingMembers" module="brokerMembers" operation="managePending"  />
		<cyclos:menu url="/do/member/sendMessage?toBrokeredMembers=true" key="menu.member.broker.messageToMembers" module="brokerMessages" operation="sendToMembers"/>
		<c:if test="${cyclos:granted('brokerSmsMailings', 'freeSmsMailings') || cyclos:granted('brokerSmsMailings', 'paidSmsMailings')}">
			<cyclos:menu url="/do/member/searchSmsMailings" key="menu.member.broker.smsMailings"/>
		</c:if>
		<c:if test="${cyclos:granted('brokerMemberPayments', 'authorize')}">
			<cyclos:menu url="/do/member/transfersAwaitingAuthorization" key="menu.member.account.transfersAwaitingAuthorization" />
			<cyclos:menu url="/do/member/searchTransferAuthorizations" key="menu.member.account.transfersAuthorizations" />
		</c:if>
		<cyclos:menu url="/do/member/defaultBrokerCommissions" key="menu.member.broker.defaultBrokerCommissions" module="brokerMembers" operation="manageDefaults" />
		<cyclos:menu url="/do/member/searchBrokerCommissionContracts" key="menu.member.broker.brokerCommissionContracts" module="brokerMembers" operation="manageContracts" />
		<c:forEach var="memberRecordType" items="${memberRecordTypesInMenu}">
			<cyclos:menu url="/do/member/searchMemberRecords?typeId=${memberRecordType.id}&global=true" label="${memberRecordType.label}" />
		</c:forEach>
		<cyclos:menu url="/do/member/searchCardsAsBroker" key="menu.member.personal.cards" module="brokerCards" operation="view" />
		<cyclos:menu url="/do/member/searchPos" key="menu.admin.accessDevices.pos.search" module="brokerPos" operation="view" />
	</cyclos:menu>
</c:if>
<cyclos:menu key="menu.member.help">
	<cyclos:menu url="/do/member/contactUs" key="menu.contact" />
	<cyclos:menu url="/do/member/manual" key="menu.member.help.manual" />
	<cyclos:menu url="/do/member/about" key="menu.about" />
</cyclos:menu>
<cyclos:menu url="/do/logout" key="menu.member.logout" confirmationKey="menu.logout.confirmationMessage"/>

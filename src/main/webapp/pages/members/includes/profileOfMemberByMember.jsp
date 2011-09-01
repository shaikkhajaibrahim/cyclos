<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<c:choose><c:when test="${byBroker}">
	<c:set var="titleMessageKey" value="profile.action.byMember.title"/>
</c:when><c:otherwise>
	<c:set var="titleMessageKey" value="profile.action.title"/>
</c:otherwise></c:choose>

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="${titleMessageKey}" arg0="${member.name}"/></td>
        <cyclos:help page="profiles#actions_for_member" />
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableForms">
			<cyclos:layout className="defaultTable" columns="4">
				<c:if test="${hasAccounts && cyclos:granted('memberPayments', 'paymentToMember')}">
					<cyclos:cell width="35%" className="label"><bean:message key="profile.action.payment"/></cyclos:cell>
					<cyclos:cell align="left"><input type="button" class="linkButton" linkURL="payment?to=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
               	<c:if test="${hasAccounts && cyclos:granted('memberReferences', 'view')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.references"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="references?nature=GENERAL&memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>
               	<c:if test="${hasAccounts && hasTransactionFeedbacks}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.transactionFeedbacks"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="references?nature=TRANSACTION&memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>
               	<c:if test="${hasAccounts && cyclos:granted('memberInvoices', 'sendToMember')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.sendInvoice"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="sendInvoice?to=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>
                <c:if test="${cyclos:granted('memberAds', 'view')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.viewAds"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="memberAds?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>
                <c:if test="${not empty member.email and not member.hideEmail}">
	                <cyclos:cell width="35%" className="label"><bean:message key="profile.action.mail"/></cyclos:cell>
	                <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="mailto:${member.email}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
                <c:if test="${cyclos:granted('memberMessages', 'sendToMember')}">
	                <cyclos:cell width="35%" className="label"><bean:message key="profile.action.message"/></cyclos:cell>
	                <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="sendMessage?toMemberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
				</c:if>
                <c:if test="${not byBroker && hasAccounts && cyclos:granted('memberReports', 'view')}">
                    <cyclos:cell width="35%" className="label"><bean:message key="profile.action.activities"/></cyclos:cell>
                    <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="activities?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                </c:if>
                <cyclos:cell width="35%" className="label"><bean:message key="profile.action.addContact"/></cyclos:cell>
                <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="addContact?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>
                <c:if test="${!removed && (cyclos:granted('memberCards', 'view') && hasCardType && !profileOfOtherMember) || (cyclos:granted('brokerCards', 'view') && hasCardType && byBroker)}">
	                <cyclos:cell width="35%" className="label"><bean:message key="profile.action.manageCards"/></cyclos:cell>
	                <cyclos:cell align="left"><input type="button" class="linkButton" linkURL="searchCards?memberId=${member.id}" value="<bean:message key="global.submit"/>"></cyclos:cell>	                
	            </c:if>
            </cyclos:layout>
       	</td>
   	</tr>
</table>
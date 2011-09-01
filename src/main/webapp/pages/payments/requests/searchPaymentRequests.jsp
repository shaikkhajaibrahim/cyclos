<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %> 

<c:if test="${not empty loggedUser}">

<c:choose><c:when test="${empty tickets}">
	<div align="center" style="font-weight:bold; margin: 10px;">
		<bean:message key="paymentRequest.search.empty" />
	</div>
</c:when><c:otherwise>

	<table class="defaultTable" cellspacing="0" cellpadding="0">
		<tr>
			<td class="tdHeaderContents"><bean:message key="member.member"/></td>
			<td class="tdHeaderContents" width="15%"><bean:message key="transfer.amount"/></td>
			<td class="tdHeaderContents" width="15%"><bean:message key="ticket.status"/></td>
			<td class="tdHeaderContents" width="10%"><bean:message key="ticket.fromChannel"/></td>
			<c:if test="${empty singleChannel}">
				<td class="tdHeaderContents" width="10%"><bean:message key="ticket.toChannel"/></td>
			</c:if>
			<td class="tdHeaderContents" width="10%"><bean:message key="ticket.date"/></td>
		</tr>
		<c:forEach var="ticket" items="${tickets}">
			<c:set var="currency" value="${ticket.currency}" />
			<c:if test="${empty currency}">
				<c:set var="currency" value="${ticket.transferType.from.currency}" />
			</c:if>
			<tr class="<t:toggle>ClassColor1|ClassColor2</t:toggle>">
				<td><a class="profileLink resultLink" memberId="${ticket.from.id}">${ticket.from.name}</a></td>
				<td><cyclos:format number="${ticket.amount}" unitsPattern="${currency.pattern}" /></td>
				<td><bean:message key="ticket.status.${ticket.status}" /></td>
				<td><c:out value="${ticket.fromChannel.displayName}"/></td>
				<c:if test="${empty singleChannel}">
					<td><c:out value="${ticket.toChannel.displayName}"/></td>
				</c:if>
				<td><cyclos:format dateTime="${ticket.creationDate}"/></td>
			</tr>
		</c:forEach>
	</table>
	<div id="paginationContainer" style="display:none"><cyclos:pagination items="${tickets}"/></div>
	<script>
		$$('a.resultLink').each(layoutBehaviour.a);
		$('paginationDisplay').innerHTML = $('paginationContainer').innerHTML;
	</script>
</c:otherwise></c:choose>

</c:if>

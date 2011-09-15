<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<cyclos:script src="/pages/tokens/listTokens.js" />
<script>
	var removeConfirmationMessage = "<cyclos:escapeJS><bean:message key="channel.removeConfirmation"/></cyclos:escapeJS>";
</script>

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="tokens.list.title"/></td>
        <cyclos:help page="settings#tokens"/>
    </tr>
	<tr>
        <td colspan="2" align="left" class="tdContentTableLists">
            <table class="defaultTable">
                <tr>
                    <th class="tdHeaderContents"><bean:message key='tokens.tokenId'/></th>
                    <th class="tdHeaderContents"><bean:message key='tokens.status'/></th>
                    <th class="tdHeaderContents"><bean:message key='tokens.amount'/></th>
                    <th class="tdHeaderContents" width="10%">&nbsp;</th>

                </tr>
				<c:forEach items="${tokens}" var="token">
	                <tr class="<t:toggle>ClassColor1|ClassColor2</t:toggle>">




	                    <td align="left">${token.transferFrom.transactionNumber}</td>
                        <td align="left"><bean:message key='tokens.status.${token.status}'/></td>
	                    <td align="right" nowrap="nowrap">${token.amount}</td>
						<td align="center">
						<c:if test="${token.status == 'ISSUED'}">
						    <img transactionId="${token.transferFrom.transactionNumber}" class="senderTokenRedemption" src="<c:url value="/pages/images/delete.gif"/>">
                        </c:if>
						</td>


	                 </tr>
				</c:forEach>
			</table>
		</td>
    </tr>
</table>

<table class="defaultTableContentHidden">
		<tr>
			<td align="right">
	        	<span class="label">
	        		<bean:message key="tokens.action.generate" />
	        	</span>
	        	<input type="button" class="button" value="<bean:message key="global.submit" />" id="newButton">
			</td>
		</tr>
</table>

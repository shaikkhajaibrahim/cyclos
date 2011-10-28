<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<cyclos:script src="/pages/tokens/confirmGenerateToken.js"/>

<ssl:form method="post" action="${formAction}">

    <html:hidden property="token(pin)" />
    <html:hidden property="token(tokenId)" />

    <table class="defaultTableContent" cellspacing="0" cellpadding="0">
        <tr>
            <td class="tdHeaderTable"><bean:message key="tokens.redeemTokenConfirm.title"/></td>
            <cyclos:help page="tokens#redeem_confirm"/>
        </tr>
        <tr>
            <td colspan="2" align="left" class="tdContentTable">
                <table class="defaultTable">
                    <tr>
                        <td colspan="2" class="label" style="text-align:center;padding-bottom:5px">
                            <cyclos:escapeHTML><bean:message key="tokens.redeemTokenConfirm.header"/></cyclos:escapeHTML>
                        </td>
                    </tr>
                    <c:if test="${not empty token.senderMobilePhone}">
                    <tr>
                        <td class="headerLabel" width="35%"><bean:message key='tokens.redeemToken.tokenSender'/></td>
                        <td>
                                ${token.senderMobilePhone}
                        </td>
                    </tr>
                    </c:if>
                    <tr>
                        <td class="headerLabel" width="35%"><bean:message key='tokens.redeemToken.tokenRecipient'/></td>
                        <td>
                                ${token.recipientMobilePhone}
                        </td>
                    </tr>
                    <tr>
                        <td class="headerLabel"><bean:message key='tokens.redeemToken.amount'/></td>
                        <td><cyclos:format number="${finalAmount}" unitsPattern="${unitsPattern}"/></td>
                    </tr>


                    <c:if test="${not empty fees}">
                        <tr>
                            <td class="headerLabel" valign="top"><bean:message
                                    key="tokens.redeemTokenConfirm.appliedFees"/></td>
                            <td>
                                <c:forEach var="fee" items="${fees}">
                                    <c:if test="${fee.key.external}">
                                        <div>
                                            <span style="font-style:italic">${fee.key.name}</span>.&nbsp;
                                            <span class="label"><bean:message key='tokens.redeemToken.amount'/>:</span>
                                            <cyclos:format number="${fee.value}"
                                                           unitsPattern="${fee.key.generatedTransferType.from.currency.pattern}"/>
                                        </div>
                                    </c:if>
                                </c:forEach>
                            </td>
                        </tr>
                    </c:if>
                    <tr>
               				<td colspan="2" align="right"><input type="submit" class="button" value="<bean:message key='global.submit'/>"></td>
               		</tr>

                </table>
            </td>
        </tr>
    </table>

    <table class="defaultTableContentHidden">
        <tr>
            <td>
                <input type="button" class="button" id="backButton" value="<bean:message key='global.back'/>">
            </td>
        </tr>
    </table>
</ssl:form>
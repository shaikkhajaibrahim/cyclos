<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %>


<ssl:form method="post" action="${formAction}">
<html:hidden property="token(transactionId)"/>
<html:hidden property="token(pin)"/>

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable">
        	<bean:message key="tokens.refundToken.title"/>
        </td>
		<cyclos:help page="tokens#refundToken"/>
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableForms">
            <table class="defaultTable">
                    <tr>
                        <td class="label" valign="top"><bean:message key="tokens.refundToken.confirm"/></td>

						<td align="right" colspan="2">
							<input type="submit" id="refundToken" class="button" value="<bean:message key="global.submit"/>">&nbsp;
						</td>
	                </tr>
            </table>
		</td>
    </tr>
</table>

<table class="defaultTableContentHidden">
	<tr>
		<td align="left">
			<input type="button" class="button" id="backButton" value="<bean:message key="global.back"/>">
		</td>
	</tr>
</table>
</ssl:form>

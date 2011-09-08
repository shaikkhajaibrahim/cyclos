<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %>

<cyclos:script src="/pages/tokens/generateToken.js" />

<ssl:form method="post" action="${formAction}">

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable">
        	<bean:message key="tokens.generateToken.title}"/>
        </td>
		<cyclos:help page="tokens#generate"/>
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableForms">
            <table class="defaultTable">
                <tr>
                    <td class="label" width="25%"><bean:message key="tokens.generateToken.amount"/></td>
                    <td>
	                    <html:text property="token(amount)"/>

                    </td>
                </tr>
                <tr>
                    <td class="label" valign="top"><bean:message key="tokens.generateToken.tokenSender"/></td>
                    <td><html:textarea rows="6" property="token(tokenSender)" /></td>
                </tr>
                    <tr>
						<td align="right" colspan="2">
							<input type="submit" id="generateButton" class="button" value="<bean:message key="global.submit"/>">&nbsp;
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

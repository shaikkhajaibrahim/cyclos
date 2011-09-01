<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<cyclos:script src="/pages/members/acceptRegistrationAgreement.js" />
<script>
	var agreementPrintTitle = "<cyclos:escapeJS>${localSettings.applicationName} - <bean:message key="group.registrationAgreement" /></cyclos:escapeJS>";
	var registrationAgreementNotCheckedMessage = "<cyclos:escapeJS><bean:message key="createMember.error.registrationAgreementCheck" /></cyclos:escapeJS>";
</script>

<ssl:form action="${formAction}" method="post">
<html:hidden property="key"/>

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="registrationAgreement.title.accept"/></td>
        <cyclos:help page="registration#accept_agreement"/>
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableForms">

			<table class="defaultTable">
				<tr>
					<td colspan="3" align="center">
						<bean:message key="group.registrationAgreement.explanation" />
					</td>
				</tr>
				<tr>
					<td colspan="3" class="label" style="text-align:center">
						
						<bean:message key="group.registrationAgreement" />
						<span style="font-weight: normal">
						(<a class="default" id="printAgreement"><bean:message key="global.print"/></a>)
						</span>
					</td>
				</tr>
				<tr>
					<td colspan="3" align="center">
						<div id="registrationAgreement" class="fakeField" style="text-align:justify;height:150px;width:80%;overflow:scroll">${registrationAgreement.contents}</div>
					</td>
				</tr>
				<tr>
					<td colspan="3" align="center">
						
						<label>
						<input type="checkbox" class="checkbox" id="registrationAgreementCheck">
						<bean:message key="createMember.registrationAgreementButton" />
						</label>
					</td>
				</tr>
				<tr>
					<td colspan="3" align="right">
						
						<input type="submit" class="button" value="<bean:message key="global.submit" />">
					</td>
				</tr>
			</table>        

    	</td>
    </tr>
</table>


<table class="defaultTableContentHidden" cellspacing="0" cellpadding="0">
	<tr>
		<td><input type="button" id="cancelButton" class="button" value="<bean:message key="global.cancel"/>"></td>
	</tr>
</table>

</ssl:form>

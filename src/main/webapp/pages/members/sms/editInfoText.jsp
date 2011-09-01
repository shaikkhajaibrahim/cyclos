<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %> 

<cyclos:script src="/pages/members/sms/editInfoText.js" />

<ssl:form method="post" action="${formAction}">
<input type="hidden" name="infoTextId" value="${currentInfoText.id}"/>

<c:choose>
	<c:when test="${empty currentInfoText}">
		<c:set var="titleKey" value="infoText.title.new"/>
	</c:when>
	<c:otherwise>
		<c:set var="titleKey" value="infoText.title.edit"/>		
	</c:otherwise>
</c:choose>

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="${titleKey}"/></td>
        <cyclos:help page="infoText"/>
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableForms">
            <table class="defaultTable">
          			<tr>
            			<td class="nestedLabel">
            				<span class="lastLabel"><bean:message key="infotext.aliases"/></span>
            			</td>
            			<td colspan="2">
	            			<html:text styleId="aliases" property="infoText(aliases)" value="${currentInfoText.aliasesString}" styleClass="full InputBoxDisabled" readonly="true"/>
            			</td>
          			</tr>
          			<tr>
            			<td class="nestedLabel" valign="top">
            				<span class="label" ><bean:message key="infotext.subject"/></span>
            			</td>
            			<td colspan="2">
            				<html:textarea styleId="subject" property="infoText(subject)" rows="6" value="${currentInfoText.subject}"  readonly="true" styleClass="full InputBoxDisabled"/>
            			</td>
          			</tr>
          			<tr>
            			<td class="nestedLabel" valign="top">
            				<span class="lastLabel"><bean:message key="infotext.body"/></span>
            			</td>
            			<td colspan="2">
            				<html:textarea styleId="body" property="infoText(body)" rows="6" value="${currentInfoText.body}" styleClass="full InputBoxDisabled" readonly="true"/>
            			</td>
          			</tr>
          			<tr>
            			<td class="nestedLabel">
            			            				
            				<span class="lastLabel"><bean:message key="infoText.validity"/></span>
            				<span class="lastLabel"><bean:message key="global.range.from"/></span>
            			</td>
            			<td colspan="2">
            			
            				<input type="text" id="validityBegin" name="infoText(validity).begin" class="InputBoxDisabled date small" value="<cyclos:format  date="${currentInfoText.validity.begin}"/>" readonly="readonly"/>
            				&nbsp;
            				<span class="label"><bean:message key="global.range.to"/></span>
            				<input id="validityEnd" type="text" name="infoText(validity).end" class="InputBoxDisabled date small" value="<cyclos:format date="${currentInfoText.validity.end}"/>" readonly="readonly"/>
            			</td>
           			</tr>
          			<tr>
          				<td class="nestedLabel">
          					<span class="label"><bean:message key="infoText.enabled"/></span>
          				</td>
          				<td width="20px" colspan="2"><input name="infoText(enabled)" type="checkbox" ${currentInfoText.enabled ? 'checked="checked"' : ''} value="true" disabled="disabled" ></td>
          			</tr>
          			<tr>
            			<c:if test="${hasManagePermissions}">
	            			<td colspan="3" align="right">
	            				<c:if test="${currentInfoText != null}">
	            					<input type="button" id="modifyButton" value="<bean:message key="global.change"/>" class="button"/>
	            				</c:if>
	            				<input type="submit" id="saveButton" class="ButtonDisabled" disabled="disabled" value="<bean:message key="global.submit"/>"/>
	            			</td>
            			</c:if>
          			</tr>
        	</table>
        </td>
    </tr>
</table>

<br/>
<input type="button" id="backButton" value="<bean:message key="global.back"/>" class="button"/>

<c:if test="${currentInfoText == null}">
<script language="javascript">
	enableFormFields.apply(document.forms[0], []);
</script>
</c:if>
</ssl:form>
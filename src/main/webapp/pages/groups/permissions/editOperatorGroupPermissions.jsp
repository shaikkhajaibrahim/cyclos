<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %> 
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>

<cyclos:script src="/pages/groups/permissions/editOperatorGroupPermissions.js" />

<ssl:form action="${formAction}" method="post">
<html:hidden property="groupId" />
<html:hidden property="permission(group)" value="${group.id}" />

<c:set var="oldType" value="${null}"/>
<c:forEach var="module" items="${modules}">
	
	<c:if test="${oldType != module.type}">

		<c:if test="${not empty oldType}">
			<%-- close the current window  --%>
					</td>
				</tr>
			</table>
			
		</c:if>

		<%-- Begin a new window for this new type --%>
		<table class="defaultTableContent" cellspacing="0" cellpadding="0">
		    <tr>
		        <td class="tdHeaderTable"><bean:message key="permission.module.type.${module.type}" arg0="${group.name}"/></td>
		        <c:choose>
		        	<c:when test="${module.type=='BASIC'}">
				        <cyclos:help page="operators#manage_group_permissions_basic" />
		        	</c:when>
		        	<c:otherwise>
				        <cyclos:help page="operators#manage_operator_group_permissions" />
		        	</c:otherwise>
		        </c:choose>
		    </tr>
		    <tr>
		        <td colspan="2" align="left" class="tdContentTableForms">
	</c:if>
	
	<c:set var="oldType" value="${module.type}"/>

	<fieldset>
		<legend><bean:message key="${module.messageKey}" /></legend>

		<table class="nested" width="100%">
		<c:forEach var="operation" items="${module.operations}">
			<c:set var="selected" value="${cyclos:contains(group.permissions, operation)}"/>
			<c:choose>
				
				<c:when test="${module.name == 'operatorAccount' && operation.name == 'accountInformation'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="canViewInformationOf" name="permission(canViewInformationOf)" size="5" disabled="true">
											<c:forEach var="mat" items="${memberAccountTypes}">
												<cyclos:option value="${mat.id}" text="${mat.name}" selected="${cyclos:contains(group.canViewInformationOf, mat)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'operatorGuarantees' && operation.name == 'issueGuarantees'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="guaranteeTypes" name="permission(guaranteeTypes)" size="5" disabled="true">
											<c:forEach var="guaranteeType" items="${guaranteeTypes}">
												<cyclos:option value="${guaranteeType.id}" text="${guaranteeType.name}" selected="${cyclos:contains(group.guaranteeTypes, guaranteeType)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:otherwise>
					<tr>
						<td width="20px"><input class="checkbox" id="chk_${operation.id}" name="permission(operations)" type="checkbox" ${selected ? 'checked="checked"' : ''} disabled="disabled" value="${operation.id}"></td>
						<td><label for="chk_${operation.id}"><bean:message key="${operation.messageKey}"/></label></td>
					</tr>
				</c:otherwise>
			</c:choose>
			
		</c:forEach>
		</table>
	</fieldset>

</c:forEach>

<%-- close the current window  --%>
		</td>
	</tr>
</table>


<table class="defaultTableContentHidden" cellspacing="0" cellpadding="0">
	<tr>
		<td align="left">
			<input type="button" class="button" id="backButton" value="<bean:message key="global.back"/>">
			<c:if test="${cyclos:granted('memberOperators', 'manage')}">
				&nbsp;<input type="button" class="button" id="groupSettingsButton" value="<bean:message key="group.settings"/>">
			</c:if>
		</td>
		<td align="right" colspan="2">
			<input type="button" id="modifyButton" class="button" value="<bean:message key="global.change"/>">&nbsp;
			<input type="submit" id="saveButton" class="ButtonDisabled" disabled="disabled" value="<bean:message key="global.submit"/>">
		</td>
	</tr>
</table>

</ssl:form>
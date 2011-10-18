<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %> 
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>

<cyclos:script src="/pages/groups/permissions/editGroupPermissions.js" />
<script language="JavaScript">
	var groupNature = '${group.nature}';
</script>

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
		        <cyclos:help page="groups#manage_group_permissions_${fn:toLowerCase(cyclos:name(module.type))}" />
		    </tr>
		    <tr>
		        <td colspan="2" align="left" class="tdContentTableForms">
		        	<c:if test="${cyclos:name(module.type) == 'ADMIN_MEMBER'}">
						<table class="nested" width="100%">
							<tr>
								<td class="label" width="5%" nowrap="nowrap"><bean:message key="permission.admin.managesGroups"/></td>
								<td>
									<cyclos:multiDropDown name="permission(managesGroups)" size="5" disabled="true" onchange="managedGroupsChanged()">
										<c:forEach var="current" items="${memberGroups}">
											<cyclos:option value="${current.id}" text="${current.name}" selected="${cyclos:contains(group.managesGroups, current)}" />
										</c:forEach>
									</cyclos:multiDropDown>
								</td>
							</tr>
						</table>
		        	</c:if>
        <c:if test="${cyclos:name(module.type) == 'ADMIN_ADMIN'}">
			<table class="nested" width="100%">
				<tr>
					<td class="label" width="5%" nowrap="nowrap"><bean:message key="permission.admin.managesAdminGroups"/></td>
					<td>
						<cyclos:multiDropDown name="permission(managesAdminGroups)" size="5" disabled="true" onchange="managedGroupsChanged()">
							<c:forEach var="current" items="${adminGroups}">
								<cyclos:option value="${current.id}" text="${current.name}" selected="${cyclos:contains(group.managesAdminGroups, current)}" />
							</c:forEach>
						</cyclos:multiDropDown>
					</td>
				</tr>
			</table>
       	</c:if>
	</c:if>
	<c:set var="oldType" value="${module.type}"/>

	<fieldset>
		<legend><bean:message key="${module.messageKey}" /></legend>

		<table class="nested" width="100%">
		<c:forEach var="operation" items="${module.operations}">
			<c:set var="selected" value="${cyclos:contains(group.permissions, operation)}"/>
			<c:choose>
				
				<c:when test="${module.name == 'adminAdminRecords' && operation.name == 'view'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(viewAdminRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.viewAdminRecordTypes, memberRecordType)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminAdminRecords' && operation.name == 'create'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(createAdminRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.createAdminRecordTypes, memberRecordType)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminAdminRecords' && operation.name == 'modify'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(modifyAdminRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<c:if test="${memberRecordType.editable}">
													<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.modifyAdminRecordTypes, memberRecordType)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminAdminRecords' && operation.name == 'delete'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(deleteAdminRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<c:if test="${memberRecordType.editable}">
													<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.deleteAdminRecordTypes, memberRecordType)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${(module.name == 'adminMemberDocuments' && operation.name == 'details') || (module.name == 'memberDocuments' && operation.name == 'view')}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(documents)" size="5" disabled="true">
											<c:forEach var="doc" items="${documents}">
												<cyclos:option value="${doc.id}" text="${doc.name}" selected="${cyclos:contains(group.documents, doc)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberLoans' && operation.name == 'grant'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="grantLoanTTs" name="permission(grantLoanTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${loanTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${(module.name == 'adminMemberMessages' && operation.name == 'view') || (module.name == 'memberMessages' && operation.name == 'sendToAdministration')}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(messageCategories)" size="5" disabled="true">
											<c:forEach var="mc" items="${messageCategories}">
												<cyclos:option value="${mc.id}" text="${mc.name}" selected="${cyclos:contains(group.messageCategories, mc)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberPayments' && operation.name == 'chargeback'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="memberChargebackTTs" name="permission(memberChargebackTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${memberChargebackTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.chargebackTransferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberPayments' && operation.name == 'payment'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="systemToMemberTTs" name="permission(systemToMemberTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${systemMemberTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberPayments' && operation.name == 'paymentAsMemberToMember'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="asMemberToMemberTTs" name="permission(asMemberToMemberTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${asMemberToMemberTTs}">
												<c:if test="${tt.context.payment}">
													<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypesAsMember, tt)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberPayments' && operation.name == 'paymentAsMemberToSelf'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="asMemberToSelfTTs" name="permission(asMemberToSelfTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${asMemberToSelfTTs}">
												<c:if test="${tt.context.selfPayment}">
													<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypesAsMember, tt)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberPayments' && operation.name == 'paymentAsMemberToSystem'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="asMemberToSystemTTs" name="permission(asMemberToSystemTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${asMemberToSystemTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypesAsMember, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberRecords' && operation.name == 'view'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(viewMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.viewMemberRecordTypes, memberRecordType)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberRecords' && operation.name == 'create'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(createMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.createMemberRecordTypes, memberRecordType)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberRecords' && operation.name == 'modify'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(modifyMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<c:if test="${memberRecordType.editable}">
													<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.modifyMemberRecordTypes, memberRecordType)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberRecords' && operation.name == 'delete'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(deleteMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<c:if test="${memberRecordType.editable}">
													<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.deleteMemberRecordTypes, memberRecordType)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'adminMemberGuarantees' && operation.name == 'registerGuarantees'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(guaranteeTypes)" size="5" disabled="true">
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
				
				<c:when test="${module.name == 'brokerDocuments' && operation.name == 'view'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(brokerDocuments)" size="5" disabled="true">
											<c:forEach var="doc" items="${documents}">
												<cyclos:option value="${doc.id}" text="${doc.name}" selected="${cyclos:contains(group.brokerDocuments, doc)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'brokerMemberPayments' && operation.name == 'paymentAsMemberToMember'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="asMemberToMemberTTs" name="permission(asMemberToMemberTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${asMemberToMemberTTs}">
												<c:if test="${tt.context.payment}">
													<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypesAsMember, tt)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'brokerMemberPayments' && operation.name == 'paymentAsMemberToSelf'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="asMemberToSelfTTs" name="permission(asMemberToSelfTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${asMemberToSelfTTs}">
												<c:if test="${tt.context.selfPayment}">
													<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypesAsMember, tt)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:when test="${module.name == 'brokerMemberPayments' && operation.name == 'paymentAsMemberToSystem'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="asMemberToSystemTTs" name="permission(asMemberToSystemTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${asMemberToSystemTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypesAsMember, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'brokerMemberRecords' && operation.name == 'view'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(brokerMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.brokerMemberRecordTypes, memberRecordType)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'brokerMemberRecords' && operation.name == 'create'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(brokerCreateMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.brokerCreateMemberRecordTypes, memberRecordType)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'brokerMemberRecords' && operation.name == 'modify'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(brokerModifyMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<c:if test="${memberRecordType.editable}">
													<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.brokerModifyMemberRecordTypes, memberRecordType)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'brokerMemberRecords' && operation.name == 'delete'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(brokerDeleteMemberRecordTypes)" size="5" disabled="true">
											<c:forEach var="memberRecordType" items="${memberRecordTypes}">
												<c:if test="${memberRecordType.editable}">
													<cyclos:option value="${memberRecordType.id}" text="${memberRecordType.name}" selected="${cyclos:contains(group.brokerDeleteMemberRecordTypes, memberRecordType)}" />
												</c:if>
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'brokerReports' && operation.name == 'showAccountInformation'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="brokerCanViewInformationOf" name="permission(brokerCanViewInformationOf)" size="5" disabled="true">
											<c:forEach var="at" items="${accountTypes}">
												<cyclos:option value="${at.id}" text="${at.name}" selected="${cyclos:contains(group.brokerCanViewInformationOf, at)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberAds' && operation.name == 'view'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(canViewAdsOfGroups)" size="5" disabled="true">
											<c:forEach var="mg" items="${memberGroups}">
												<cyclos:option value="${mg.id}" text="${mg.name}" selected="${cyclos:contains(group.canViewAdsOfGroups, mg)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				<c:when test="${module.name == 'memberGuarantees' && operation.name == 'issueGuarantees'}">
					<tr class="toHideIssuerGroup">
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="mdd_guaranteeTypes" onchange="showHideBuyersAndSellersPermissions();" name="permission(guaranteeTypes)" size="5" disabled="true">
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
				
				<c:when test="${module.name == 'memberGuarantees' && operation.name == 'issueCertifications'}">
					<tr class="toHideIssuerGroup">
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="mdd_canIssueCertificationToGroups" onchange="showHideBuyersAndSellersPermissions();" name="permission(canIssueCertificationToGroups)" size="5" disabled="true">
											<c:forEach var="mg" items="${memberGroups}">
												<cyclos:option value="${mg.id}" text="${mg.name}" selected="${cyclos:contains(group.canIssueCertificationToGroups, mg)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberGuarantees' && operation.name == 'buyWithPaymentObligations'}">
					<tr class="toHideBuyerAndSellerGroup">
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="mdd_canBuyWithPaymentObligationsFromGroups" onchange="showHideIssuersPermissions();" name="permission(canBuyWithPaymentObligationsFromGroups)" size="5" disabled="true">
											<c:forEach var="mg" items="${memberGroups}">
												<cyclos:option value="${mg.id}" text="${mg.name}" selected="${cyclos:contains(group.canBuyWithPaymentObligationsFromGroups, mg)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:when test="${module.name == 'memberGuarantees' && operation.name == 'sellWithPaymentObligations'}">
					<tr class="toHideBuyerAndSellerGroup">
						<td width="20px"><input class="checkbox" id="chk_sellWithPaymentObligations" name="permission(operations)" type="checkbox" ${selected ? 'checked="checked"' : ''} disabled="disabled" value="${operation.id}"></td>
						<td><label for="chk_${operation.id}"><bean:message key="${operation.messageKey}"/></label></td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberPayments' && operation.name == 'paymentToSelf'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="selfPaymentTTs" name="permission(selfPaymentTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${memberSelfTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberPayments' && operation.name == 'paymentToMember'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="memberToMemberTTs" name="permission(memberToMemberTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${memberMemberTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberPayments' && operation.name == 'paymentToSystem'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="memberToSystemTTs" name="permission(memberToSystemTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${memberSystemTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberPayments' && operation.name == 'request'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(requestPaymentByChannels)" size="5" disabled="true">
											<c:forEach var="channel" items="${channelsSupportingPaymentRequest}">
												<cyclos:option value="${channel.id}" text="${channel.displayName}" selected="${cyclos:contains(group.requestPaymentByChannels, channel)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberProfile' && operation.name == 'view'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(canViewProfileOfGroups)" size="5" disabled="true" onchange="canViewProfileOfGroupsChanged()">
											<c:forEach var="mg" items="${memberGroups}">
												<cyclos:option value="${mg.id}" text="${mg.name}" selected="${cyclos:contains(group.canViewProfileOfGroups, mg)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'memberReports' && operation.name == 'showAccountInformation'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="canViewInformationOf" name="permission(canViewInformationOf)" size="5" disabled="true">
											<c:forEach var="at" items="${accountTypes}">
												<cyclos:option value="${at.id}" text="${at.name}" selected="${cyclos:contains(group.canViewInformationOf, at)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:when test="${module.name == 'memberPayments' && operation.name == 'chargeback'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="chargebackTTs" name="permission(chargebackTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${chargebackTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.chargebackTransferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'systemAccounts' && operation.name == 'information'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(viewInformationOf)" size="5" disabled="true" onchange="systemAccountTypesChanged()">
											<c:forEach var="acct" items="${systemAccounts}">
												<cyclos:option value="${acct.id}" text="${acct.name}" selected="${cyclos:contains(group.viewInformationOf, acct)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:when test="${module.name == 'systemPayments' && operation.name == 'chargeback'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="systemChargebackTTs" name="permission(systemChargebackTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${systemChargebackTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.chargebackTransferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'systemPayments' && operation.name == 'payment'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="systemToSystemTTs" name="permission(systemToSystemTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${systemSystemTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.transferTypes, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>
				
				<c:when test="${module.name == 'systemStatus' && operation.name == 'viewConnectedAdmins'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown name="permission(viewConnectedAdminsOf)" size="5" disabled="true">
											<c:forEach var="adminGroup" items="${adminGroups}">
												<cyclos:option value="${adminGroup.id}" text="${adminGroup.name}" selected="${cyclos:contains(group.viewConnectedAdminsOf, adminGroup)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:when test="${operation.name == 'simulateConversion'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="conversionSimulationTTs" name="permission(conversionSimulationTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${conversionSimulationTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.conversionSimulationTTs, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:when test="${module.name == 'brokerAccounts' && operation.name == 'brokerSimulateConversion'}">
					<tr>
						<td width="20px"></td>
						<td>
							<table class="nested" width="100%">
								<tr>
									<td width="30%" nowrap="nowrap"><bean:message key="${operation.messageKey}"/></td>
									<td>
										<cyclos:multiDropDown varName="brokerConversionSimulationTTs" name="permission(brokerConversionSimulationTTs)" size="5" disabled="true">
											<c:forEach var="tt" items="${brokerConversionSimulationTTs}">
												<cyclos:option value="${tt.id}" text="${tt.name}" selected="${cyclos:contains(group.brokerConversionSimulationTTs, tt)}" />
											</c:forEach>
										</cyclos:multiDropDown>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:when>

				<c:when test="${module.name == 'basic' && operation.name == 'quickAccess'}">
					<%-- Although the quick access permission is in the basic module, it's only applicable for members --%>
					<c:if test="${group.nature == 'MEMBER' || group.nature == 'BROKER'}">
						<tr>
							<td width="20px"><input class="checkbox" id="chk_${operation.id}" name="permission(operations)" type="checkbox" ${selected ? 'checked="checked"' : ''} disabled="disabled" value="${operation.id}"></td>
							<td><label for="chk_${operation.id}"><bean:message key="${operation.messageKey}"/></label></td>
						</tr>
					</c:if>
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
			<c:if test="${(cyclos:name(group.nature)=='ADMIN' && cyclos:granted('systemAdminGroups', 'view')) ||
                         ((cyclos:name(group.nature)=='BROKER' || cyclos:name(group.nature)=='MEMBER') && cyclos:granted('adminMemberGroups','view') && cyclos:contains(managesGroups, group))}">
				&nbsp;<input type="button" class="button keepEnabled" id="groupSettingsButton" value="<bean:message key="group.settings"/>">
			</c:if>
		</td>
		<c:if test="${editable}"> 
			<td align="right" colspan="2">
				<input type="button" id="modifyButton" class="button" value="<bean:message key="global.change"/>">&nbsp;
				<input type="submit" id="saveButton" class="ButtonDisabled" disabled="disabled" value="<bean:message key="global.submit"/>">
			</td>
		</c:if>
	</tr>
</table>

</ssl:form>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %> 

<cyclos:script src="/pages/accounts/accountFees/listAccountFeeLog.js" />
<script>
	var cancelConfirmation = "<cyclos:escapeJS><bean:message key="accountFee.action.cancel.confirmation"/></cyclos:escapeJS>";
	var runConfirmation = "<cyclos:escapeJS><bean:message key="accountFee.action.run.confirmation"/></cyclos:escapeJS>";
	var rechargeConfirmation = "<cyclos:escapeJS><bean:message key="accountFee.action.recharge.confirmation"/></cyclos:escapeJS>";
</script>

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="accountFee.title.list"/></td>
        <cyclos:help page="account_management#account_fee_overview"/>
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableLists">
            <table class="defaultTable">
                <tr>
                    <th rowspan="2" class="tdHeaderContents"><bean:message key="accountFee.name"/></th>
                    <th rowspan="2" class="tdHeaderContents"><bean:message key="accountFee.amount"/></th>
                    <th class="tdHeaderContents" colspan="2"><bean:message key="accountFee.lastExecution"/></th>
                    <th rowspan="2" class="tdHeaderContents"><bean:message key="accountFee.nextExecution"/></th>
                </tr>
                <tr>
                    <th class="tdHeaderContents" width="15%"><bean:message key="accountFeeLog.date"/></th>
                    <th class="tdHeaderContents" width="15%"><bean:message key="accountFeeLog.status"/></th>
                </tr>
                
                <c:forEach var="fee" items="${accountFees}">
	                <c:set var="lastExecution" value="${fee.lastExecution}"/>
	                <c:set var="nextExecution" value="${fee.nextExecution}"/>
	                <c:set var="isRunning" value="${not empty lastExecution and cyclos:name(lastExecution.status) == 'RUNNING'}"/>
	                <tr class="<t:toggle>ClassColor1|ClassColor2</t:toggle>">
	                    <td align="left">${fee.name}</td>
	                    <td align="left"><cyclos:format amount="${fee.amountValue}" unitsPattern="${fee.accountType.currency.pattern}" /></td>
	                    
	                    <c:choose><c:when test="${empty lastExecution}">
							<td align="center" colspan="2">
								<bean:message key='accountFeeLog.status.NEVER_RAN'/>
							</td>
	                    </c:when><c:otherwise>
		                    <td align="center"><cyclos:format date="${lastExecution.date}"/></td>
		                    <td align="center">
								<bean:message key='accountFeeLog.status.${lastExecution.status}'/>
		                    </td>
	                    </c:otherwise></c:choose>
	                    
	                    <td align="center">
		                   	<c:choose><c:when test="${fee.manual}">
		                   		<%-- Fee runs manually --%>
			                    <c:choose><c:when test="${cyclos:granted('systemAccountFees', 'charge')}">
			                    	<c:choose>
				                    	<c:when test="${isRunning}">
					                    	<a class="linkList cancelFee" accountFeeLogId="${lastExecution.id}"><bean:message key='accountFee.action.cancel'/></a>
					                    </c:when>
					                    <c:when test="${cyclos:name(lastExecution.status) == 'PARTIALLY_FAILED'}">
											<a class="linkList rechargeFee" accountFeeLogId="${lastExecution.id}"><bean:message key='accountFee.action.recharge'/></a>
										</c:when>
				                    	<c:otherwise>
					                    	<a class="linkList runFee" accountFeeId="${fee.id}"><bean:message key='accountFee.action.run'/></a>
				                    	</c:otherwise>
				                    </c:choose>
			                    </c:when><c:otherwise>
			                    	<bean:message key='accountFee.runMode.MANUAL'/>
			                    </c:otherwise></c:choose>
		                   	</c:when><c:otherwise>
		                   		<%-- Fee is scheduled --%>
		                   		<cyclos:format date="${nextExecution.date}"/>
		                   	</c:otherwise></c:choose>
	                    </td>
	               </tr>
	            </c:forEach>
            </table>
        </td>
    </tr>
</table>		

<table class="defaultTableContent" cellspacing="0" cellpadding="0">
    <tr>
        <td class="tdHeaderTable"><bean:message key="accountFee.title.history"/></td>
        <cyclos:help page="account_management#account_fee_history"/>
    </tr>
    <tr>
        <td colspan="2" align="left" class="tdContentTableLists">

            <table class="defaultTable">
                <tr>
                	<th class="tdHeaderContents"><bean:message key='accountFeeLog.date'/></th>
                    <th class="tdHeaderContents"><bean:message key='accountFeeLog.fee'/></th>
                    <th class="tdHeaderContents"><bean:message key='accountFeeLog.amount'/></th>
                    <th class="tdHeaderContents"><bean:message key='accountFeeLog.status'/></th>
                    <c:if test="${cyclos:granted('systemAccountFees', 'charge')}">
                    	<th class="tdHeaderContents" width="5%" align="center">&nbsp;</th>
                    </c:if>
                </tr>                

				<c:forEach var="log" items="${accountFeeLogs}">
					<c:set var="isRunning" value="${cyclos:name(log.status) == 'RUNNING'}"/>
	                <tr class="<t:toggle>ClassColor1|ClassColor2</t:toggle>">
	                    <td align="center"><cyclos:format date="${log.date}"/></td>
	                    <td align="left">${log.accountFee.name}</td>
	                    <td align="left"><cyclos:format amount="${log.amountValue}" unitsPattern="${log.accountFee.accountType.currency.pattern}"/></td>
	                    <td align="center"><bean:message key='accountFeeLog.status.${log.status}'/></td>
	                    <c:if test="${cyclos:granted('systemAccountFees', 'charge')}">
		                    <td align="center">
								<c:choose>
									<c:when test="${isRunning}">
										<a class="linkList cancelFee" accountFeeLogId="${log.id}"><bean:message key='accountFee.action.cancel'/></a>
									</c:when>
									<c:when test="${log.status.canRecharge}">
										<a class="linkList rechargeFee" accountFeeLogId="${log.id}"><bean:message key='accountFee.action.recharge'/></a>
									</c:when>
								</c:choose>
		                    </td>
		                </c:if>
	               	</tr>
				</c:forEach>
            </table>
        </td>
    </tr>
</table>		

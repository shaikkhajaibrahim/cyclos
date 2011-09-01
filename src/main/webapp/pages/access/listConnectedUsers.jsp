<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://sslext.sf.net/tags/sslext" prefix="ssl" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%@ taglib uri="http://www.servletsuite.com/servlets/toggletag" prefix="t" %> 

<script>
	var nothingSelectedMessage = "<cyclos:escapeJS><bean:message key="global.error.nothingSelected"/></cyclos:escapeJS>";
	var disconnectTooltip = "<cyclos:escapeJS><bean:message key="connectedUsers.disconnectToolTip"/></cyclos:escapeJS>";
</script>
<cyclos:script src="/pages/access/listConnectedUsers.js" />

<c:if test="${isAdmin}">
	<ssl:form method="post" action="${formAction}">
	
	<table class="defaultTableContent" cellspacing="0" cellpadding="0">
	    <tr>
	        <td class="tdHeaderTable"><bean:message key="connectedUsers.title"/></td>
	        <cyclos:help page="user_management#connected_users"/>
	    </tr>
	    <tr>
	        <td colspan="2" align="right" class="tdContentTableForms">
	            <table class="defaultTable">
	          		<tr>
	            		<td class="label" width="25%"><bean:message key="connectedUsers.nature"/></td>
	            		<td>
	            			<cyclos:multiDropDown name="nature" emptyLabelKey="global.search.all">
	            				<c:forEach var="nature" items="${groupNatures}">
	            					<c:set var="natureLabel"><bean:message key="group.nature.${nature}"/></c:set>
	            					<cyclos:option value="${nature}" text="${natureLabel}" selected="${cyclos:contains(selectedNatures, nature)}"/>
	            				</c:forEach>
	            			</cyclos:multiDropDown>
	            		</td>
	            		<td width="10%"><input type="submit" class="button" value="<bean:message key="global.submit"/>"></td>
	          		</tr>
	        	</table>
	        </td>
	    </tr>
	</table>
	</ssl:form>
	
</c:if>
<c:if test="${not empty users}">
	<table class="defaultTableContent" cellspacing="0" cellpadding="0">
	    <tr>
	        <td class="tdHeaderTable"><bean:message key="global.searchResults"/></td>
	        <cyclos:help page="user_management#connected_users_result"/>
	    </tr>
	    <tr>
	        <td colspan="2" align="left" class="tdContentTableLists">
	            <table class="defaultTable" cellspacing="0" cellpadding="0">
	                <tr>
	                    <td class="tdHeaderContents" width="20%"><bean:message key="member.username"/></td>
						<td class="tdHeaderContents" width="35%"><bean:message key="member.name"/></td>
						<td class="tdHeaderContents" width="25%"><bean:message key="connectedUsers.loggedAt"/></td>
						<td class="tdHeaderContents" width="15%"><bean:message key="connectedUsers.remoteAddress"/></td>
						<td class="tdHeaderContents" width="5%"></td>
	                </tr>
					<c:forEach var="dto" items="${users}">
	                	<c:set var="user" value="${dto.user}"/>
	                	<c:set var="nature" value="${cyclos:name(user.element.nature)}"/>
	                	<c:set var="sessions" value="${dto.sessions}" />
	                	<c:set var="sessionCount" value="${fn:length(sessions)}" />
						<c:set var="trClass"><t:toggle>ClassColor1|ClassColor2</t:toggle></c:set>
		                <tr class="${trClass}">
		                    <td align="left" valign="top" rowspan="${sessionCount}">
		                    	<c:choose><c:when test="${nature == 'OPERATOR' && isAdmin}">
		                    		<c:set var="member" value="${user.operator.member}"/>
			                    	<a class="linkList elementProfileLink" nature="MEMBER" elementId="${member.id}">${member.username}</a>
			                    	/ ${user.username}
		                    	</c:when><c:otherwise>
			                    	<a class="linkList elementProfileLink" nature="${nature}" elementId="${user.id}">${user.username}</a>
		                    	</c:otherwise></c:choose>
		                    </td>
		                    <td align="left" valign="top" rowspan="${sessionCount}">
		                    	<c:choose><c:when test="${nature == 'OPERATOR' && isAdmin}">
			                    	${user.element.name}
		                    	</c:when><c:otherwise>
			                    	<a class="linkList elementProfileLink" nature="${nature}" elementId="${user.id}">${user.element.name}</a>
		                    	</c:otherwise></c:choose>
							</td>
							<c:forEach var="currentSession" items="${sessions}" varStatus="loop">
								<c:if test="${loop.count > 1}" ><tr class="${trClass}"></c:if>
			                    <td align="center"><cyclos:format dateTime="${currentSession.loginDate}" /></td>
			                    <td align="center">${currentSession.remoteAddress}</td>
			                    <td align="center" valign="middle">
			                    	<c:set var="allowDisconnect" value="${true}"/>
			                    	<c:if test="${isAdmin}">
					                    <c:choose>
					                    	<c:when test="${currentSession.sessionId == pageContext.session.id}">
					                    		<%-- Cannot disconnect your same session --%>
					                    		<c:set var="allowDisconnect" value="${false}"/>
					                    	</c:when>
					                    	<c:otherwise>
					                    		<c:set var="allowDisconnect" value="${currentIsAdmin ? canDisconnectAdmin : canDisconnectMember}"/>
					                    	</c:otherwise>
					                    </c:choose>
			                    	</c:if>
			                    	<c:if test="${allowDisconnect}">
					                    <img class="disconnect" userId="${user.id}" sessionId="${currentSession.sessionId}" src="<c:url value="/pages/images/delete.gif" />" />
			                    	</c:if>
			                    </td>
			                   <c:if test="${loop.count > 1}"></tr></c:if>
			              </c:forEach>
		                </tr>
		            </c:forEach>
	            </table>
	        </td>
	    </tr>
	</table>
</c:if>
<c:if test="${isMember && empty users}">
	<div class="footerNote" helpPage="user_management#connected_users_result"><bean:message key="connectedUsers.noOperators"/></div>
</c:if>
<c:if test="${fn:endsWith(navigation.previous, '/home')}">
	
	<table class="defaultTableContentHidden">
		<tr>
			<td align="left">
				<input class="button" type="button" id="backButton" value="<bean:message key='global.back'/>">
			</td>
		</tr>
	</table>
</c:if>
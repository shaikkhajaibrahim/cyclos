<%@ page import="nl.strohalm.cyclos.entities.settings.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>
<%
LocalSettings settings = (LocalSettings) application.getAttribute("localSettings");
response.setContentType("text/html;charset=" + settings.getCharset());
%>

<html>

<head>
	<c:if test="${not empty pageContext.request.queryString}">
		<c:set var="queryString" value="?${pageContext.request.queryString}" />
	</c:if>
	<meta http-equiv="refresh" content="0;URL=${pageContext.request.contextPath}/do/${queryString}">
	<title><%=settings.getApplicationName()%></title>
	<cyclos:customizedFilePath type="style" name="style.css" var="styleUrl" />
	<link rel="stylesheet" href="<c:url value="${styleUrl}" />">
	<link rel="shortcut icon" href="<c:url value="/systemImage?image=icon"/>">
</head>

<body>
	<div style="width:98%;text-align:center;position:absolute;top:20%;">
		<cyclos:customImage type="system" name="systemLogo" /><br/>
		<br/>
		<span class="loadingSystem">
			<bean:message key="global.loadingSystem"/>
		</span>
	</div>
</body>

</html>

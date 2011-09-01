<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<html:html>

<c:choose><c:when test="${not empty loggedUser}">
	<head>
		<meta http-equiv="refresh" content="0;URL=${pathPrefix}/home">
	</head>
</c:when><c:otherwise>

	<jsp:include page="/pages/general/layout/head.jsp" />
	<cyclos:customizedFilePath type="style" name="login.css" var="loginUrl" groupId="${param.login ? param.groupId : ''}" groupFilterId="${param.login ? param.groupFilterId : ''}" />	
	<link rel="stylesheet" href="<c:url value="${loginUrl}" />">	

	
	<jsp:include flush="true" page="/pages/access/includes/loginDefinitions.jsp" />
	
	<cyclos:includeCustomizedFile type="static" name="login.jsp" groupId="${param.login ? param.groupId : ''}" groupFilterId="${param.login ? param.groupFilterId : ''}" />
	
	<script>
		<c:if test="${not empty loginElement}">
			var login = $('cyclosUsername');
			if (login) {
				login.value = '${loginElement.username}';
				disableField(login);
			}
		</c:if>
		if (is.webkit) {
			var td = $('loginRegistration');
			var div = $('loginRegistrationDiv');
			if (td && div) {
				div.style.height = (td.getHeight() - 10) + "px";
			}
		}
		ensureLoginForm();
	</script>

</c:otherwise></c:choose>

</html:html>

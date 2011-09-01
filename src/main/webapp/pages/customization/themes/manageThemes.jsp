<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<c:if test="${cyclos:granted('systemThemes', 'select') || cyclos:granted('systemThemes', 'remove')}">
	<jsp:include flush="true" page="selectTheme.jsp"/><br/>
</c:if>
<c:if test="${cyclos:granted('systemThemes', 'import')}">
	<jsp:include flush="true" page="importTheme.jsp"/><br/>
</c:if>
<c:if test="${cyclos:granted('systemThemes', 'export')}">
	<jsp:include flush="true" page="exportTheme.jsp"/><br/>
</c:if>

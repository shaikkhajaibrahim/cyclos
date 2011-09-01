<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags/struts-html" prefix="html" %>

<html:html>
<jsp:include page="/pages/general/layout/head.jsp" />
<body class="bodyImage">
<div id="containerDiv">
<tiles:insert attribute="body" />
</div>
</body>
<script>
init();
</script>
</html:html>
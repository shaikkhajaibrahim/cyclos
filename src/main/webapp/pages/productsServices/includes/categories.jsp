<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://devel.cyclos.org/tlibs/cyclos-core" prefix="cyclos" %>

<c:if test="${not empty category}">
	<c:set var="level" value="${category.level}" />
	<li class="categoryLevel${level}">
		<c:if test="${localSettings.showCountersInAdCategories}">
		    <c:set var="countProp" value="${cyclos:name(query.tradeType) == 'SEARCH' ? 'countSearch':'countOffer'}"></c:set>
		    <c:set var="counter">
		    	(<span class="categoryCountText">${category[countProp]}</span>)
		    </c:set>
		</c:if>
		<span class="category categoryLevel${level}Text" categoryId="${category.id}">
			${category.name} ${counter}
		</span>
		<c:if test="${not empty category.children}">
			<ul class="categoryLevel${level}">
				<c:forEach var="child" items="${category.children}">
					<c:set var="category" scope="request" value="${child}"/>
					<jsp:include flush="true" page="/pages/productsServices/includes/categories.jsp"/>
				</c:forEach>
			</ul>
		</c:if>
	</li>
</c:if>
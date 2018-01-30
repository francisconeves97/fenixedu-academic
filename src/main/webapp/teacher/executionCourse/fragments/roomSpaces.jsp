<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:forEach var="rooms" items="${rooms}">
    <logic:notEmpty name="rooms" property="children">
        <c:if test="${fn:length(rooms.path)-1 != 0}">
	        <c:forEach begin="0" end="${fn:length(rooms.path)-1}" varStatus="loop">
	            <c:set var="indent" value="${indent}&nbsp;&nbsp;"/>
	        </c:forEach>
        </c:if>
        <optgroup label="${indent}${rooms.fullName}" ></optgroup>
    </logic:notEmpty>
    <logic:empty name="rooms" property="children">
        <c:forEach begin="0" end="${fn:length(rooms.path)-1}" varStatus="loop">
		    <c:set var="indent" value="${indent}&nbsp;&nbsp;"/>
		</c:forEach>
        <option>${indent}${rooms.fullName}</option>
    </logic:empty>
    <c:set var="indent" value=""/>
    <c:set var="rooms" value="${rooms.children}" scope="request"/>
    <jsp:include page="roomSpaces.jsp"/>
</c:forEach>
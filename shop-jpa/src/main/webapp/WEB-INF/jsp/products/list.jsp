<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Products</title>
    <style>
        .low { background-color: #ffcccc; }
        .flash { color: green; font-weight: bold; }
        table { border-collapse: collapse; width: 60%; margin-top: 10px; }
        th, td { border: 1px solid #ccc; padding: 6px; text-align: left; }
    </style>
</head>
<body>
<h1>Products (${currency})</h1>
<c:if test="${not empty message}">
    <p class="flash"><c:out value="${message}"/></p>
</c:if>
<form method="get">
    <input name="q" value="<c:out value='${q}'/>" placeholder="search">
    <button>Go</button>
</form>
<table>
    <tr><th>Name</th><th>Net</th><th>Gross</th><th>Stock</th></tr>
    <c:forEach var="p" items="${products}">
        <tr class="${p.stock < threshold ? 'low' : ''}">
            <td><c:out value="${p.name}"/></td>
            <td><fmt:formatNumber value="${p.price}" minFractionDigits="2"/></td>
            <td><fmt:formatNumber value="${p.gross}" minFractionDigits="2"/></td>
            <td>${p.stock}</td>
        </tr>
    </c:forEach>
</table>
<br>
<a href="/products/new">Add product</a>
</body>
</html>
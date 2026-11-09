<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <title>Add Product</title>
    <style>
        .error { color: red; font-size: 0.9em; }
        .field { margin-bottom: 12px; }
    </style>
</head>
<body>
<h1>Add Product</h1>
<form:form method="post" action="/products" modelAttribute="createProduct">
    <div class="field">
        <label>Name:</label><br>
        <form:input path="name"/>
        <form:errors path="name" cssClass="error"/>
    </div>
    <div class="field">
        <label>Price:</label><br>
        <form:input path="price" type="number" step="0.01"/>
        <form:errors path="price" cssClass="error"/>
    </div>
    <div class="field">
        <label>Stock:</label><br>
        <form:input path="stock" type="number"/>
        <form:errors path="stock" cssClass="error"/>
    </div>
    <button type="submit">Save</button>
    <a href="/products">Cancel</a>
</form:form>
</body>
</html>
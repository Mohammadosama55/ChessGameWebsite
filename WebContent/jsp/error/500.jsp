<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>500 - Internal Server Error</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
    <div class="container text-center mt-5">
        <h1 class="display-1">500</h1>
        <p class="lead">Internal Server Error</p>
        <p>Sorry, something went wrong on our end. Please try again later.</p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary">Go to Homepage</a>
    </div>
</body>
</html>

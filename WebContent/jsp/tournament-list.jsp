<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chess Tournaments</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/styles.css" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="${pageContext.request.contextPath}/dashboard">Chess Game</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/game">Play</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/puzzle">Puzzles</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link active" href="${pageContext.request.contextPath}/tournament">Tournaments</a>
                    </li>
                </ul>
                <span class="navbar-text">
                    Welcome, ${sessionScope.username}!
                    <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-light ms-3">Logout</a>
                </span>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1>Chess Tournaments</h1>
            <c:if test="${sessionScope.userRole eq 'ADMIN'}">
                <a href="${pageContext.request.contextPath}/tournament/create" class="btn btn-primary">
                    <i class="bi bi-plus-circle"></i> Create Tournament
                </a>
            </c:if>
        </div>

        <!-- Upcoming Tournaments -->
        <div class="card mb-4">
            <div class="card-header bg-primary text-white">
                <h5 class="card-title mb-0">Upcoming Tournaments</h5>
            </div>
            <div class="card-body">
                <div class="row">
                    <c:forEach items="${upcomingTournaments}" var="tournament">
                        <div class="col-md-6 col-lg-4 mb-3">
                            <div class="card h-100">
                                <div class="card-body">
                                    <h5 class="card-title">${tournament.name}</h5>
                                    <p class="card-text">${tournament.description}</p>
                                    <div class="mb-2">
                                        <small class="text-muted">
                                            <i class="bi bi-calendar"></i> 
                                            <fmt:formatDate value="${tournament.startDate}" pattern="MMM dd, yyyy HH:mm"/>
                                        </small>
                                    </div>
                                    <div class="mb-2">
                                        <span class="badge bg-info">${tournament.tournamentType}</span>
                                        <span class="badge bg-secondary">
                                            ${tournament.timeControlMinutes}+${tournament.timeIncrementSeconds}
                                        </span>
                                    </div>
                                    <div class="progress mb-2" style="height: 20px;">
                                        <div class="progress-bar" role="progressbar" 
                                             style="width: ${(fn:length(tournament.participants) / tournament.maxParticipants) * 100}%;">
                                            ${fn:length(tournament.participants)}/${tournament.maxParticipants} Players
                                        </div>
                                    </div>
                                    <a href="${pageContext.request.contextPath}/tournament/${tournament.tournamentId}" 
                                       class="btn btn-outline-primary">View Details</a>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                    <c:if test="${empty upcomingTournaments}">
                        <div class="col">
                            <p class="text-muted">No upcoming tournaments.</p>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>

        <!-- In Progress Tournaments -->
        <div class="card mb-4">
            <div class="card-header bg-success text-white">
                <h5 class="card-title mb-0">Tournaments in Progress</h5>
            </div>
            <div class="card-body">
                <div class="row">
                    <c:forEach items="${inProgressTournaments}" var="tournament">
                        <div class="col-md-6 col-lg-4 mb-3">
                            <div class="card h-100">
                                <div class="card-body">
                                    <h5 class="card-title">${tournament.name}</h5>
                                    <p class="card-text">${tournament.description}</p>
                                    <div class="mb-2">
                                        <span class="badge bg-info">${tournament.tournamentType}</span>
                                        <span class="badge bg-secondary">Round ${tournament.rounds.size()}</span>
                                    </div>
                                    <a href="${pageContext.request.contextPath}/tournament/${tournament.tournamentId}" 
                                       class="btn btn-outline-success">View Games</a>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                    <c:if test="${empty inProgressTournaments}">
                        <div class="col">
                            <p class="text-muted">No tournaments in progress.</p>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>

        <!-- Completed Tournaments -->
        <div class="card">
            <div class="card-header bg-secondary text-white">
                <h5 class="card-title mb-0">Completed Tournaments</h5>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Tournament</th>
                                <th>Type</th>
                                <th>Players</th>
                                <th>Winner</th>
                                <th>Date</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${completedTournaments}" var="tournament">
                                <tr>
                                    <td>${tournament.name}</td>
                                    <td><span class="badge bg-info">${tournament.tournamentType}</span></td>
                                    <td>${tournament.participants.size()}</td>
                                    <td>
                                        <c:set var="winner" value="${tournament.participants[0]}"/>
                                        ${winner.username}
                                    </td>
                                    <td>
                                        <fmt:formatDate value="${tournament.endDate}" pattern="MMM dd, yyyy"/>
                                    </td>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/tournament/${tournament.tournamentId}" 
                                           class="btn btn-sm btn-outline-secondary">View Results</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty completedTournaments}">
                                <tr>
                                    <td colspan="6" class="text-center text-muted">No completed tournaments.</td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

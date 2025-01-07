<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tournament History - Chess Game</title>
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
        <div class="row">
            <div class="col-md-12">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h3 class="card-title mb-0">Tournament History</h3>
                        <div class="btn-group">
                            <button type="button" class="btn btn-outline-primary" data-bs-toggle="modal" data-bs-target="#filterModal">
                                <i class="bi bi-funnel"></i> Filter
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th>Tournament</th>
                                        <th>Type</th>
                                        <th>Date</th>
                                        <th>Players</th>
                                        <th>Winner</th>
                                        <th>Your Result</th>
                                        <th>Rating Change</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${tournaments}" var="tournament">
                                        <tr>
                                            <td>
                                                <a href="${pageContext.request.contextPath}/tournament/${tournament.id}/stats">
                                                    ${tournament.name}
                                                </a>
                                            </td>
                                            <td>${tournament.type}</td>
                                            <td>
                                                <fmt:formatDate value="${tournament.startDate}" pattern="MMM dd, yyyy"/>
                                            </td>
                                            <td>${tournament.playerCount}</td>
                                            <td>${tournament.winner}</td>
                                            <td>${tournament.userResult}</td>
                                            <td class="${tournament.userRatingChange > 0 ? 'text-success' : 
                                                       tournament.userRatingChange < 0 ? 'text-danger' : ''}">
                                                <c:if test="${tournament.userRatingChange > 0}">+</c:if>
                                                ${tournament.userRatingChange}
                                            </td>
                                            <td>
                                                <div class="btn-group">
                                                    <button type="button" class="btn btn-sm btn-outline-secondary dropdown-toggle" 
                                                            data-bs-toggle="dropdown">
                                                        Export
                                                    </button>
                                                    <ul class="dropdown-menu">
                                                        <li>
                                                            <a class="dropdown-item" href="${pageContext.request.contextPath}/tournament/${tournament.id}/export/json">
                                                                <i class="bi bi-filetype-json"></i> JSON
                                                            </a>
                                                        </li>
                                                        <li>
                                                            <a class="dropdown-item" href="${pageContext.request.contextPath}/tournament/${tournament.id}/export/excel">
                                                                <i class="bi bi-file-earmark-excel"></i> Excel
                                                            </a>
                                                        </li>
                                                        <li>
                                                            <a class="dropdown-item" href="${pageContext.request.contextPath}/tournament/${tournament.id}/export/pgn">
                                                                <i class="bi bi-file-text"></i> PGN
                                                            </a>
                                                        </li>
                                                    </ul>
                                                </div>
                                                <a href="${pageContext.request.contextPath}/tournament/${tournament.id}/certificate" 
                                                   class="btn btn-sm btn-outline-primary" 
                                                   title="Download Certificate">
                                                    <i class="bi bi-award"></i>
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                        
                        <!-- Pagination -->
                        <nav aria-label="Tournament history navigation" class="mt-4">
                            <ul class="pagination justify-content-center">
                                <li class="page-item ${page == 1 ? 'disabled' : ''}">
                                    <a class="page-link" href="?page=${page - 1}${filter != null ? '&filter=' += filter : ''}" tabindex="-1">
                                        Previous
                                    </a>
                                </li>
                                <c:forEach begin="1" end="${totalPages}" var="i">
                                    <li class="page-item ${page == i ? 'active' : ''}">
                                        <a class="page-link" href="?page=${i}${filter != null ? '&filter=' += filter : ''}">
                                            ${i}
                                        </a>
                                    </li>
                                </c:forEach>
                                <li class="page-item ${page == totalPages ? 'disabled' : ''}">
                                    <a class="page-link" href="?page=${page + 1}${filter != null ? '&filter=' += filter : ''}">
                                        Next
                                    </a>
                                </li>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Statistics Cards -->
        <div class="row mt-4">
            <div class="col-md-3">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 class="card-title">Tournaments Played</h5>
                        <p class="card-text display-4">${userStats.tournamentsPlayed}</p>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 class="card-title">Tournament Wins</h5>
                        <p class="card-text display-4">${userStats.tournamentWins}</p>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 class="card-title">Average Position</h5>
                        <p class="card-text display-4">
                            <fmt:formatNumber value="${userStats.averagePosition}" maxFractionDigits="1"/>
                        </p>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 class="card-title">Rating Progress</h5>
                        <p class="card-text display-4 ${userStats.ratingProgress > 0 ? 'text-success' : 
                                                      userStats.ratingProgress < 0 ? 'text-danger' : ''}">
                            <c:if test="${userStats.ratingProgress > 0}">+</c:if>
                            ${userStats.ratingProgress}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Filter Modal -->
    <div class="modal fade" id="filterModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Filter Tournaments</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form action="" method="GET">
                        <div class="mb-3">
                            <label class="form-label">Date Range</label>
                            <div class="input-group">
                                <input type="date" class="form-control" name="startDate" value="${param.startDate}">
                                <span class="input-group-text">to</span>
                                <input type="date" class="form-control" name="endDate" value="${param.endDate}">
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Tournament Type</label>
                            <select class="form-select" name="type">
                                <option value="">All Types</option>
                                <option value="swiss" ${param.type == 'swiss' ? 'selected' : ''}>Swiss</option>
                                <option value="roundrobin" ${param.type == 'roundrobin' ? 'selected' : ''}>Round Robin</option>
                                <option value="elimination" ${param.type == 'elimination' ? 'selected' : ''}>Elimination</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Result</label>
                            <select class="form-select" name="result">
                                <option value="">All Results</option>
                                <option value="winner" ${param.result == 'winner' ? 'selected' : ''}>Winner</option>
                                <option value="podium" ${param.result == 'podium' ? 'selected' : ''}>Podium Finish</option>
                            </select>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Apply Filters</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

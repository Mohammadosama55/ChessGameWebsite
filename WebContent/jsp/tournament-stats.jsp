<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${tournament.name} - Statistics</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/styles.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
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
            <div class="col-md-12 mb-4">
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title mb-0">Tournament Overview</h3>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-3">
                                <div class="stats-card text-center p-3 border rounded">
                                    <h4>${stats.totalGames}</h4>
                                    <small class="text-muted">Total Games</small>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="stats-card text-center p-3 border rounded">
                                    <h4><fmt:formatNumber value="${stats.averageMoves}" maxFractionDigits="1"/></h4>
                                    <small class="text-muted">Average Moves per Game</small>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="stats-card text-center p-3 border rounded">
                                    <h4>
                                        <fmt:formatNumber value="${stats.averageGameLength / 60}" maxFractionDigits="1"/>
                                    </h4>
                                    <small class="text-muted">Average Game Length (minutes)</small>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="stats-card text-center p-3 border rounded">
                                    <h4>${tournament.participants.size()}</h4>
                                    <small class="text-muted">Total Players</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row">
            <!-- Game Results Chart -->
            <div class="col-md-6 mb-4">
                <div class="card h-100">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Game Results</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="resultsChart"></canvas>
                    </div>
                </div>
            </div>

            <!-- Top Openings -->
            <div class="col-md-6 mb-4">
                <div class="card h-100">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Most Popular Openings</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="openingsChart"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- Player Statistics -->
        <div class="card mb-4">
            <div class="card-header">
                <h5 class="card-title mb-0">Player Statistics</h5>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Rank</th>
                                <th>Player</th>
                                <th>Games</th>
                                <th>Score</th>
                                <th>Win %</th>
                                <th>Performance</th>
                                <th>Rating Change</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${playerStats}" var="stat" varStatus="status">
                                <tr>
                                    <td>${status.index + 1}</td>
                                    <td>${stat.username}</td>
                                    <td>${stat.stats.gamesPlayed}</td>
                                    <td>
                                        <fmt:formatNumber value="${stat.stats.score}" maxFractionDigits="1"/>
                                    </td>
                                    <td>
                                        <fmt:formatNumber value="${stat.stats.winPercentage}" maxFractionDigits="1"/>%
                                    </td>
                                    <td>${stat.stats.performanceRating}</td>
                                    <td class="${stat.stats.ratingChange > 0 ? 'text-success' : 
                                               stat.stats.ratingChange < 0 ? 'text-danger' : ''}">
                                        <c:if test="${stat.stats.ratingChange > 0}">+</c:if>
                                        ${stat.stats.ratingChange}
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Game History -->
        <div class="card">
            <div class="card-header">
                <h5 class="card-title mb-0">Game History</h5>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Round</th>
                                <th>White</th>
                                <th>Black</th>
                                <th>Result</th>
                                <th>Opening</th>
                                <th>Moves</th>
                                <th>Duration</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${gameHistory}" var="game">
                                <tr>
                                    <td>${game.roundNumber}</td>
                                    <td>${game.whitePlayer}</td>
                                    <td>${game.blackPlayer}</td>
                                    <td>${game.result}</td>
                                    <td>${game.openingName}</td>
                                    <td>${game.numMoves}</td>
                                    <td>
                                        <fmt:formatNumber value="${game.lengthSeconds / 60}" maxFractionDigits="1"/> min
                                    </td>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/game/${game.gameId}" 
                                           class="btn btn-sm btn-outline-primary">
                                            View Game
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Game Results Chart
        const resultsCtx = document.getElementById('resultsChart').getContext('2d');
        new Chart(resultsCtx, {
            type: 'pie',
            data: {
                labels: ['White Wins', 'Black Wins', 'Draws'],
                datasets: [{
                    data: [${stats.whiteWins}, ${stats.blackWins}, ${stats.draws}],
                    backgroundColor: ['#28a745', '#343a40', '#6c757d']
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });

        // Top Openings Chart
        const openingsCtx = document.getElementById('openingsChart').getContext('2d');
        new Chart(openingsCtx, {
            type: 'bar',
            data: {
                labels: [
                    <c:forEach items="${stats.topOpenings}" var="opening" varStatus="status">
                        '${opening.key}'${!status.last ? ',' : ''}
                    </c:forEach>
                ],
                datasets: [{
                    label: 'Games',
                    data: [
                        <c:forEach items="${stats.topOpenings}" var="opening" varStatus="status">
                            ${opening.value}${!status.last ? ',' : ''}
                        </c:forEach>
                    ],
                    backgroundColor: '#007bff'
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
    </script>
</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Chess Tournament</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="../css/tournament.css" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="home">Chess Tournament</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="tournaments">Tournaments</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="leaderboard">Leaderboard</a>
                    </li>
                </ul>
                <ul class="navbar-nav">
                    <li class="nav-item">
                        <a class="nav-link" href="profile">${sessionScope.user.username}</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="logout">Logout</a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="row">
            <!-- Tournament Info -->
            <div class="col-md-4">
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Tournament Information</h5>
                    </div>
                    <div class="card-body">
                        <h6>${tournament.name}</h6>
                        <p>${tournament.description}</p>
                        <ul class="list-unstyled">
                            <li>Format: ${tournament.format}</li>
                            <li>Status: ${tournament.status}</li>
                            <li>Players: ${tournament.playerCount}/${tournament.maxParticipants}</li>
                            <li>Current Round: ${tournament.currentRound}/${tournament.roundsCount}</li>
                        </ul>
                        <c:if test="${tournament.status == 'UPCOMING'}">
                            <form action="tournament" method="post">
                                <input type="hidden" name="action" value="register">
                                <input type="hidden" name="tournamentId" value="${tournament.id}">
                                <button type="submit" class="btn btn-primary">Register</button>
                            </form>
                        </c:if>
                    </div>
                </div>
            </div>

            <!-- Tournament Bracket -->
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Tournament Bracket</h5>
                    </div>
                    <div class="card-body">
                        <div id="bracketContainer"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Current Round -->
        <div class="row mt-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Current Round</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th>White</th>
                                        <th>Rating</th>
                                        <th>Result</th>
                                        <th>Black</th>
                                        <th>Rating</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${currentRound.games}" var="game">
                                        <tr>
                                            <td>${game.whitePlayer}</td>
                                            <td>${game.whiteRating}</td>
                                            <td>${game.result}</td>
                                            <td>${game.blackPlayer}</td>
                                            <td>${game.blackRating}</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${game.status == 'IN_PROGRESS'}">
                                                        <a href="game?id=${game.id}" class="btn btn-sm btn-primary">Watch</a>
                                                    </c:when>
                                                    <c:when test="${game.status == 'COMPLETED'}">
                                                        <a href="game?id=${game.id}&mode=replay" class="btn btn-sm btn-secondary">Replay</a>
                                                    </c:when>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Tournament Chat -->
        <div class="row mt-4">
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Tournament Chat</h5>
                    </div>
                    <div class="card-body">
                        <div id="chatMessages" class="chat-container mb-3"></div>
                        <div class="input-group">
                            <input type="text" id="chatInput" class="form-control" placeholder="Type your message...">
                            <button class="btn btn-primary" id="sendMessage">Send</button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Tournament Statistics -->
            <div class="col-md-4">
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Statistics</h5>
                    </div>
                    <div class="card-body">
                        <div id="tournamentStats"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="../js/tournament.js"></script>
</body>
</html>

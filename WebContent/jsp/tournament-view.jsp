<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${tournament.name} - Tournament Details</title>
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
        <!-- Tournament Header -->
        <div class="card mb-4">
            <div class="card-body">
                <div class="row">
                    <div class="col-md-8">
                        <h1 class="card-title">${tournament.name}</h1>
                        <p class="card-text">${tournament.description}</p>
                        <div class="mb-3">
                            <span class="badge bg-primary">${tournament.tournamentType}</span>
                            <span class="badge bg-secondary">
                                ${tournament.timeControlMinutes}+${tournament.timeIncrementSeconds}
                            </span>
                            <span class="badge bg-${tournament.status eq 'UPCOMING' ? 'info' : 
                                                   tournament.status eq 'IN_PROGRESS' ? 'success' : 'secondary'}">
                                ${tournament.status}
                            </span>
                        </div>
                        <div class="row">
                            <div class="col-md-6">
                                <small class="text-muted">
                                    <i class="bi bi-calendar"></i> Start:
                                    <fmt:formatDate value="${tournament.startDate}" pattern="MMM dd, yyyy HH:mm"/>
                                </small>
                            </div>
                            <div class="col-md-6">
                                <small class="text-muted">
                                    <i class="bi bi-calendar"></i> End:
                                    <fmt:formatDate value="${tournament.endDate}" pattern="MMM dd, yyyy HH:mm"/>
                                </small>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4 text-end">
                        <c:if test="${tournament.status eq 'UPCOMING'}">
                            <c:choose>
                                <c:when test="${tournament.isRegistered(sessionScope.userId)}">
                                    <button class="btn btn-outline-success" disabled>
                                        <i class="bi bi-check-circle"></i> Registered
                                    </button>
                                </c:when>
                                <c:when test="${tournament.canRegister()}">
                                    <form action="${pageContext.request.contextPath}/tournament" method="post" 
                                          style="display: inline;">
                                        <input type="hidden" name="action" value="register">
                                        <input type="hidden" name="tournamentId" value="${tournament.tournamentId}">
                                        <button type="submit" class="btn btn-primary">
                                            <i class="bi bi-person-plus"></i> Register
                                        </button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <button class="btn btn-outline-secondary" disabled>
                                        Registration Closed
                                    </button>
                                </c:otherwise>
                            </c:choose>
                            
                            <c:if test="${sessionScope.userRole eq 'ADMIN' && tournament.participants.size() >= 4}">
                                <form action="${pageContext.request.contextPath}/tournament" method="post"
                                      style="display: inline;">
                                    <input type="hidden" name="action" value="start">
                                    <input type="hidden" name="tournamentId" value="${tournament.tournamentId}">
                                    <button type="submit" class="btn btn-success">
                                        <i class="bi bi-play-circle"></i> Start Tournament
                                    </button>
                                </form>
                            </c:if>
                        </c:if>
                    </div>
                </div>
            </div>
        </div>

        <div class="row">
            <!-- Tournament Rounds -->
            <div class="col-md-8">
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Rounds</h5>
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <c:when test="${not empty tournament.rounds}">
                                <div class="accordion" id="roundsAccordion">
                                    <c:forEach items="${tournament.rounds}" var="round">
                                        <div class="accordion-item">
                                            <h2 class="accordion-header">
                                                <button class="accordion-button ${round.roundNumber eq tournament.rounds.size() ? '' : 'collapsed'}"
                                                        type="button" data-bs-toggle="collapse"
                                                        data-bs-target="#round${round.roundNumber}">
                                                    Round ${round.roundNumber}
                                                </button>
                                            </h2>
                                            <div id="round${round.roundNumber}" 
                                                 class="accordion-collapse collapse ${round.roundNumber eq tournament.rounds.size() ? 'show' : ''}"
                                                 data-bs-parent="#roundsAccordion">
                                                <div class="accordion-body">
                                                    <div class="table-responsive">
                                                        <table class="table table-hover">
                                                            <thead>
                                                                <tr>
                                                                    <th>White</th>
                                                                    <th>Black</th>
                                                                    <th>Result</th>
                                                                    <th>Actions</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                <c:forEach items="${round.pairings}" var="pairing">
                                                                    <tr>
                                                                        <td>${pairing.player1Name}</td>
                                                                        <td>${pairing.player2Name}</td>
                                                                        <td>
                                                                            <c:choose>
                                                                                <c:when test="${pairing.isDraw}">
                                                                                    ½ - ½
                                                                                </c:when>
                                                                                <c:when test="${not empty pairing.winnerId}">
                                                                                    <c:choose>
                                                                                        <c:when test="${pairing.winnerId eq pairing.player1Id}">
                                                                                            1 - 0
                                                                                        </c:when>
                                                                                        <c:otherwise>
                                                                                            0 - 1
                                                                                        </c:otherwise>
                                                                                    </c:choose>
                                                                                </c:when>
                                                                                <c:otherwise>
                                                                                    * - *
                                                                                </c:otherwise>
                                                                            </c:choose>
                                                                        </td>
                                                                        <td>
                                                                            <c:if test="${not empty pairing.gameId}">
                                                                                <a href="${pageContext.request.contextPath}/game/${pairing.gameId}"
                                                                                   class="btn btn-sm btn-outline-primary">
                                                                                    View Game
                                                                                </a>
                                                                            </c:if>
                                                                        </td>
                                                                    </tr>
                                                                </c:forEach>
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <p class="text-muted">Tournament hasn't started yet.</p>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- Tournament Standings -->
            <div class="col-md-4">
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Standings</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th>#</th>
                                        <th>Player</th>
                                        <th>Score</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${tournament.participants}" var="participant" varStatus="status">
                                        <tr>
                                            <td>${status.index + 1}</td>
                                            <td>${participant.username}</td>
                                            <td>${participant.score}</td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
                
                <!-- Tournament Chat -->
                <div class="card mt-4">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Tournament Chat</h5>
                    </div>
                    <div class="card-body">
                        <div id="chatMessages" class="chat-messages mb-3" style="height: 300px; overflow-y: auto;">
                            <!-- Chat messages will be inserted here -->
                        </div>
                        <form id="chatForm" class="chat-form">
                            <div class="input-group">
                                <input type="text" id="chatInput" class="form-control" 
                                       placeholder="Type your message...">
                                <button type="submit" class="btn btn-primary">Send</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // WebSocket connection for tournament chat
        const tournamentId = ${tournament.tournamentId};
        const ws = new WebSocket('ws://' + window.location.host + 
                                '${pageContext.request.contextPath}/tournament-chat/' + tournamentId);
        
        ws.onmessage = function(event) {
            const message = JSON.parse(event.data);
            appendMessage(message);
        };
        
        function appendMessage(message) {
            const messageDiv = document.createElement('div');
            messageDiv.className = 'chat-message mb-2';
            messageDiv.innerHTML = `
                <small class="text-muted">${message.timestamp}</small>
                <strong>${message.username}:</strong>
                ${message.content}
            `;
            document.getElementById('chatMessages').appendChild(messageDiv);
            document.getElementById('chatMessages').scrollTop = 
                document.getElementById('chatMessages').scrollHeight;
        }
        
        document.getElementById('chatForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const input = document.getElementById('chatInput');
            const message = input.value.trim();
            
            if (message) {
                ws.send(JSON.stringify({
                    type: 'CHAT',
                    content: message
                }));
                input.value = '';
            }
        });
        
        // Auto-refresh tournament data
        if ('${tournament.status}' === 'IN_PROGRESS') {
            setInterval(function() {
                fetch('${pageContext.request.contextPath}/tournament/${tournament.tournamentId}?format=json')
                    .then(response => response.json())
                    .then(data => {
                        // Update standings and current round
                        // Implementation depends on the JSON structure
                    });
            }, 30000); // Refresh every 30 seconds
        }
    </script>
</body>
</html>

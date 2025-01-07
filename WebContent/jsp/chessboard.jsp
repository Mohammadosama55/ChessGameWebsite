<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chess Game</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/chessboard-js/1.0.0/chessboard-1.0.0.min.css">
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
                        <a class="nav-link" href="${pageContext.request.contextPath}/puzzle">Puzzles</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/tournament">Tournaments</a>
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
            <div class="col-md-8">
                <div class="card shadow">
                    <div class="card-body">
                        <div id="game-board" style="width: 100%"></div>
                        <div class="game-controls mt-3">
                            <button id="startBtn" class="btn btn-primary">Start New Game</button>
                            <button id="flipBtn" class="btn btn-secondary">Flip Board</button>
                            <button id="undoBtn" class="btn btn-warning">Undo Move</button>
                            <button id="resignBtn" class="btn btn-danger">Resign</button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card shadow mb-4">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Game Information</h5>
                    </div>
                    <div class="card-body">
                        <div id="game-status"></div>
                        <div id="move-history" class="mt-3">
                            <h6>Move History</h6>
                            <div id="pgn" class="move-list"></div>
                        </div>
                    </div>
                </div>
                
                <div class="card shadow">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Chat</h5>
                    </div>
                    <div class="card-body">
                        <div id="chat-messages" class="chat-container mb-3"></div>
                        <div class="input-group">
                            <input type="text" id="chat-input" class="form-control" placeholder="Type a message...">
                            <button class="btn btn-primary" id="send-message">Send</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Game Over Modal -->
    <div class="modal fade" id="gameOverModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Game Over</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p id="game-result"></p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="button" class="btn btn-primary" id="newGameBtn">New Game</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/chess.js/0.10.3/chess.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/chessboard-js/1.0.0/chessboard-1.0.0.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/chessboard.js"></script>
    <script>
        // Initialize the game with the current user's information
        const currentUserId = ${sessionScope.userId};
        const gameId = ${param.gameId != null ? param.gameId : 'null'};
        initializeGame(currentUserId, gameId);
    </script>
</body>
</html>

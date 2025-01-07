<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Game Analysis - Chess Game</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/styles.css" rel="stylesheet">
    <link href="https://unpkg.com/@chrisoakman/chessboardjs@1.0.0/dist/chessboard-1.0.0.min.css" rel="stylesheet">
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
                        <a class="nav-link" href="${pageContext.request.contextPath}/tournament">Tournaments</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link active" href="${pageContext.request.contextPath}/analysis">Analysis</a>
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
        <c:choose>
            <c:when test="${empty game}">
                <!-- Game List View -->
                <div class="row">
                    <div class="col-12">
                        <h1 class="mb-4">Game Analysis</h1>
                        <div class="card">
                            <div class="card-header">
                                <h5 class="card-title mb-0">Your Games</h5>
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-hover">
                                        <thead>
                                            <tr>
                                                <th>Date</th>
                                                <th>White</th>
                                                <th>Black</th>
                                                <th>Result</th>
                                                <th>Type</th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach items="${games}" var="game">
                                                <tr>
                                                    <td><fmt:formatDate value="${game.createdAt}" pattern="MMM dd, yyyy HH:mm"/></td>
                                                    <td>${game.whitePlayerName}</td>
                                                    <td>${game.blackPlayerName}</td>
                                                    <td>${game.result}</td>
                                                    <td>${game.gameType}</td>
                                                    <td>
                                                        <a href="${pageContext.request.contextPath}/analysis/${game.gameId}" 
                                                           class="btn btn-primary btn-sm">Analyze</a>
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
            </c:when>
            <c:otherwise>
                <!-- Game Analysis View -->
                <div class="row">
                    <div class="col-md-8">
                        <div class="card mb-4">
                            <div class="card-header d-flex justify-content-between align-items-center">
                                <h5 class="card-title mb-0">Game Analysis</h5>
                                <div>
                                    <button id="startBtn" class="btn btn-outline-secondary btn-sm">
                                        <i class="bi bi-skip-start-fill"></i>
                                    </button>
                                    <button id="prevBtn" class="btn btn-outline-secondary btn-sm">
                                        <i class="bi bi-caret-left-fill"></i>
                                    </button>
                                    <button id="nextBtn" class="btn btn-outline-secondary btn-sm">
                                        <i class="bi bi-caret-right-fill"></i>
                                    </button>
                                    <button id="endBtn" class="btn btn-outline-secondary btn-sm">
                                        <i class="bi bi-skip-end-fill"></i>
                                    </button>
                                </div>
                            </div>
                            <div class="card-body">
                                <div id="analysisBoard" style="width: 100%"></div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card mb-4">
                            <div class="card-header">
                                <h5 class="card-title mb-0">Game Information</h5>
                            </div>
                            <div class="card-body">
                                <p><strong>White:</strong> ${game.whitePlayerName}</p>
                                <p><strong>Black:</strong> ${game.blackPlayerName}</p>
                                <p><strong>Date:</strong> <fmt:formatDate value="${game.createdAt}" pattern="MMM dd, yyyy"/></p>
                                <p><strong>Result:</strong> ${game.result}</p>
                            </div>
                        </div>
                        
                        <div class="card mb-4">
                            <div class="card-header">
                                <h5 class="card-title mb-0">Move List</h5>
                            </div>
                            <div class="card-body">
                                <div id="moveList" class="move-list"></div>
                            </div>
                        </div>
                        
                        <div class="card">
                            <div class="card-header d-flex justify-content-between align-items-center">
                                <h5 class="card-title mb-0">Analysis</h5>
                                <button id="analyzeBtn" class="btn btn-primary btn-sm">Analyze Position</button>
                            </div>
                            <div class="card-body">
                                <div id="analysisResults"></div>
                                <div class="mt-3">
                                    <div class="input-group">
                                        <input type="text" id="commentInput" class="form-control" placeholder="Add a comment...">
                                        <button id="addCommentBtn" class="btn btn-secondary">Add</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <script>
                    const gameData = {
                        id: ${game.gameId},
                        userId: ${sessionScope.userId},
                        pgn: `${game.pgn}`,
                        whitePlayer: "${game.whitePlayerName}",
                        blackPlayer: "${game.blackPlayerName}",
                        result: "${game.result}"
                    };
                </script>
            </c:otherwise>
        </c:choose>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://unpkg.com/@chrisoakman/chessboardjs@1.0.0/dist/chessboard-1.0.0.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/chess.js/0.10.3/chess.min.js"></script>
    
    <c:if test="${not empty game}">
        <script src="${pageContext.request.contextPath}/js/analysis.js"></script>
    </c:if>
</body>
</html>

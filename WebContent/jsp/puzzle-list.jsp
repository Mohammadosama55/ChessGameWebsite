<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chess Puzzles</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
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
                        <a class="nav-link active" href="${pageContext.request.contextPath}/puzzle">Puzzles</a>
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
            <div class="col-md-3">
                <div class="card shadow mb-4">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Themes</h5>
                    </div>
                    <div class="card-body">
                        <div class="list-group">
                            <c:forEach items="${themes}" var="theme">
                                <a href="${pageContext.request.contextPath}/puzzle/theme/${theme}" 
                                   class="list-group-item list-group-item-action 
                                   ${theme eq currentTheme ? 'active' : ''}">
                                    ${theme}
                                </a>
                            </c:forEach>
                        </div>
                    </div>
                </div>
                
                <div class="card shadow">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Quick Start</h5>
                    </div>
                    <div class="card-body">
                        <div class="d-grid gap-2">
                            <a href="${pageContext.request.contextPath}/puzzle/random?difficulty=EASY" 
                               class="btn btn-success">Easy Puzzle</a>
                            <a href="${pageContext.request.contextPath}/puzzle/random?difficulty=MEDIUM" 
                               class="btn btn-warning">Medium Puzzle</a>
                            <a href="${pageContext.request.contextPath}/puzzle/random?difficulty=HARD" 
                               class="btn btn-danger">Hard Puzzle</a>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="col-md-9">
                <div class="card shadow">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="card-title mb-0">
                            ${currentTheme != null ? currentTheme : 'All'} Puzzles
                        </h5>
                        <c:if test="${sessionScope.userRole eq 'ADMIN'}">
                            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#createPuzzleModal">
                                Create Puzzle
                            </button>
                        </c:if>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <c:forEach items="${puzzles}" var="puzzle">
                                <div class="col-md-6 mb-4">
                                    <div class="card h-100">
                                        <div class="card-body">
                                            <h5 class="card-title">${puzzle.title}</h5>
                                            <p class="card-text">${puzzle.description}</p>
                                            <div class="d-flex justify-content-between align-items-center">
                                                <span class="badge bg-${puzzle.difficulty eq 'EASY' ? 'success' : 
                                                                      puzzle.difficulty eq 'MEDIUM' ? 'warning' : 'danger'}">
                                                    ${puzzle.difficulty}
                                                </span>
                                                <span class="text-muted">Rating: ${puzzle.rating}</span>
                                            </div>
                                        </div>
                                        <div class="card-footer">
                                            <a href="${pageContext.request.contextPath}/puzzle/${puzzle.puzzleId}" 
                                               class="btn btn-primary w-100">Solve Puzzle</a>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Create Puzzle Modal -->
    <c:if test="${sessionScope.userRole eq 'ADMIN'}">
        <div class="modal fade" id="createPuzzleModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Create New Puzzle</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="createPuzzleForm">
                            <div class="mb-3">
                                <label for="title" class="form-label">Title</label>
                                <input type="text" class="form-control" id="title" name="title" required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="description" class="form-label">Description</label>
                                <textarea class="form-control" id="description" name="description" rows="3"></textarea>
                            </div>
                            
                            <div class="mb-3">
                                <label for="initialPosition" class="form-label">Initial Position (FEN)</label>
                                <input type="text" class="form-control" id="initialPosition" name="initialPosition" required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="solution" class="form-label">Solution</label>
                                <input type="text" class="form-control" id="solution" name="solution" required>
                                <div class="form-text">Enter moves in algebraic notation, separated by spaces</div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="difficulty" class="form-label">Difficulty</label>
                                <select class="form-select" id="difficulty" name="difficulty" required>
                                    <option value="EASY">Easy</option>
                                    <option value="MEDIUM">Medium</option>
                                    <option value="HARD">Hard</option>
                                </select>
                            </div>
                            
                            <div class="mb-3">
                                <label for="theme" class="form-label">Theme</label>
                                <input type="text" class="form-control" id="theme" name="theme" required>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-primary" onclick="createPuzzle()">Create</button>
                    </div>
                </div>
            </div>
        </div>
    </c:if>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function createPuzzle() {
            const formData = new FormData(document.getElementById('createPuzzleForm'));
            const data = Object.fromEntries(formData.entries());
            
            fetch('${pageContext.request.contextPath}/puzzle?action=create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams(data)
            })
            .then(response => response.json())
            .then(puzzle => {
                window.location.reload();
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Failed to create puzzle');
            });
        }
    </script>
</body>
</html>

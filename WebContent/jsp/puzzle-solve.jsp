<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Solve Puzzle - ${puzzle.title}</title>
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
            <div class="col-md-8">
                <div class="card shadow">
                    <div class="card-body">
                        <div id="puzzle-board" style="width: 100%"></div>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card shadow mb-4">
                    <div class="card-header">
                        <h5 class="card-title mb-0">${puzzle.title}</h5>
                    </div>
                    <div class="card-body">
                        <p class="card-text">${puzzle.description}</p>
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <span class="badge bg-${puzzle.difficulty eq 'EASY' ? 'success' : 
                                              puzzle.difficulty eq 'MEDIUM' ? 'warning' : 'danger'}">
                                ${puzzle.difficulty}
                            </span>
                            <span class="text-muted">Theme: ${puzzle.theme}</span>
                        </div>
                        <div id="puzzle-status" class="alert alert-info">
                            Find the best move!
                        </div>
                        <div class="d-grid gap-2">
                            <button id="resetBtn" class="btn btn-secondary">Reset Puzzle</button>
                            <button id="hintBtn" class="btn btn-info">Get Hint</button>
                            <a href="${pageContext.request.contextPath}/puzzle/random?difficulty=${puzzle.difficulty}" 
                               class="btn btn-primary">Try Another Puzzle</a>
                        </div>
                    </div>
                </div>
                
                <div class="card shadow">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Your Moves</h5>
                    </div>
                    <div class="card-body">
                        <div id="move-history" class="move-list"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Success Modal -->
    <div class="modal fade" id="successModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Congratulations!</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>You've solved the puzzle correctly!</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <a href="${pageContext.request.contextPath}/puzzle/random?difficulty=${puzzle.difficulty}" 
                       class="btn btn-primary">Next Puzzle</a>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/chess.js/0.10.3/chess.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/chessboard-js/1.0.0/chessboard-1.0.0.min.js"></script>
    <script>
        let board = null;
        let game = new Chess();
        let moves = [];
        let attempts = 0;
        
        // Initialize the puzzle
        function initializePuzzle() {
            const config = {
                draggable: true,
                position: '${puzzle.initialPosition}',
                onDragStart: onDragStart,
                onDrop: onDrop,
                onSnapEnd: onSnapEnd
            };
            
            board = Chessboard('puzzle-board', config);
            game.load('${puzzle.initialPosition}');
            
            $(window).resize(board.resize);
        }
        
        function onDragStart(source, piece, position, orientation) {
            if (game.game_over()) return false;
            
            if ((game.turn() === 'w' && piece.search(/^b/) !== -1) ||
                (game.turn() === 'b' && piece.search(/^w/) !== -1)) {
                return false;
            }
        }
        
        function onDrop(source, target) {
            const move = game.move({
                from: source,
                to: target,
                promotion: 'q'
            });
            
            if (move === null) return 'snapback';
            
            moves.push(move.san);
            updateMoveHistory();
            checkSolution();
        }
        
        function onSnapEnd() {
            board.position(game.fen());
        }
        
        function updateMoveHistory() {
            const history = $('#move-history');
            history.empty();
            
            moves.forEach((move, index) => {
                const moveNumber = Math.floor(index / 2) + 1;
                if (index % 2 === 0) {
                    history.append(`<span>${moveNumber}. ${move} </span>`);
                } else {
                    history.append(`<span>${move} </span><br>`);
                }
            });
        }
        
        function checkSolution() {
            attempts++;
            
            fetch('${pageContext.request.contextPath}/puzzle?action=verify', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    puzzleId: ${puzzle.puzzleId},
                    moves: moves.join(','),
                    attempts: attempts
                })
            })
            .then(response => response.json())
            .then(result => {
                if (result.correct) {
                    $('#puzzle-status').removeClass('alert-info').addClass('alert-success')
                        .text('Correct! Well done!');
                    new bootstrap.Modal(document.getElementById('successModal')).show();
                } else if (moves.length >= '${puzzle.solution}'.split(' ').length) {
                    $('#puzzle-status').removeClass('alert-info').addClass('alert-danger')
                        .text('Incorrect solution. Try again!');
                    setTimeout(resetPuzzle, 1500);
                }
            })
            .catch(error => {
                console.error('Error:', error);
            });
        }
        
        function resetPuzzle() {
            game.load('${puzzle.initialPosition}');
            board.position('${puzzle.initialPosition}');
            moves = [];
            $('#puzzle-status').removeClass('alert-success alert-danger').addClass('alert-info')
                .text('Find the best move!');
            updateMoveHistory();
        }
        
        function showHint() {
            const solution = '${puzzle.solution}'.split(' ');
            if (moves.length < solution.length) {
                $('#puzzle-status').text(`Hint: Try to find ${solution[moves.length]}`);
            }
        }
        
        // Event listeners
        $('#resetBtn').on('click', resetPuzzle);
        $('#hintBtn').on('click', showHint);
        
        // Initialize the puzzle when the page loads
        $(document).ready(initializePuzzle);
    </script>
</body>
</html>

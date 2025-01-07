let board = null;
let game = new Chess();
let socket = null;
let currentUserId = null;
let currentGameId = null;
let playerColor = 'white';
let isPlayerTurn = true;

function initializeGame(userId, gameId) {
    currentUserId = userId;
    currentGameId = gameId;
    
    // Initialize WebSocket connection
    initializeWebSocket();
    
    // Initialize the chessboard
    const config = {
        draggable: true,
        position: 'start',
        onDragStart: onDragStart,
        onDrop: onDrop,
        onSnapEnd: onSnapEnd
    };
    
    board = Chessboard('game-board', config);
    
    // If there's a game ID, load the existing game
    if (gameId) {
        loadExistingGame(gameId);
    }
    
    // Add event listeners
    $('#startBtn').on('click', startNewGame);
    $('#flipBtn').on('click', board.flip);
    $('#undoBtn').on('click', undoMove);
    $('#resignBtn').on('click', resignGame);
    $('#send-message').on('click', sendChatMessage);
    
    // Handle window resize
    $(window).resize(board.resize);
    
    updateStatus();
}

function initializeWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}${window.location.contextPath}/game-socket`;
    
    socket = new WebSocket(wsUrl);
    
    socket.onopen = function(event) {
        console.log('WebSocket connection established');
        if (currentGameId) {
            socket.send(JSON.stringify({
                type: 'join',
                gameId: currentGameId,
                userId: currentUserId
            }));
        }
    };
    
    socket.onmessage = function(event) {
        const message = JSON.parse(event.data);
        handleWebSocketMessage(message);
    };
    
    socket.onerror = function(error) {
        console.error('WebSocket error:', error);
    };
    
    socket.onclose = function(event) {
        console.log('WebSocket connection closed');
    };
}

function handleWebSocketMessage(message) {
    switch (message.type) {
        case 'gameState':
            updateGameState(message.fen, message.pgn);
            break;
        case 'move':
            handleOpponentMove(message.move);
            break;
        case 'chat':
            addChatMessage(message.username, message.text);
            break;
        case 'gameOver':
            handleGameOver(message.result);
            break;
    }
}

function onDragStart(source, piece) {
    // Don't allow moves if the game is over or it's not player's turn
    if (game.game_over() || !isPlayerTurn ||
        (game.turn() === 'w' && piece.search(/^b/) !== -1) ||
        (game.turn() === 'b' && piece.search(/^w/) !== -1)) {
        return false;
    }
}

function onDrop(source, target) {
    // Try to make the move
    const move = game.move({
        from: source,
        to: target,
        promotion: 'q' // Always promote to queen for simplicity
    });
    
    // If illegal move, snap back
    if (move === null) return 'snapback';
    
    // Send move to server
    sendMove(source, target);
    
    updateStatus();
}

function onSnapEnd() {
    board.position(game.fen());
}

function sendMove(source, target) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: 'move',
            gameId: currentGameId,
            userId: currentUserId,
            move: {
                from: source,
                to: target
            }
        }));
    }
}

function handleOpponentMove(move) {
    game.move(move);
    board.position(game.fen());
    updateStatus();
}

function updateStatus() {
    let status = '';
    let moveColor = game.turn() === 'b' ? 'Black' : 'White';
    
    // Checkmate?
    if (game.in_checkmate()) {
        status = 'Game over, ' + moveColor + ' is in checkmate.';
        handleGameOver(moveColor + ' wins by checkmate');
    }
    // Draw?
    else if (game.in_draw()) {
        status = 'Game over, drawn position';
        handleGameOver('Game drawn');
    }
    // Game still on
    else {
        status = moveColor + ' to move';
        // Check?
        if (game.in_check()) {
            status += ', ' + moveColor + ' is in check';
        }
    }
    
    $('#game-status').html(status);
    $('#pgn').html(game.pgn({ max_width: 5, newline_char: '<br>' }));
}

function startNewGame() {
    if (confirm('Are you sure you want to start a new game?')) {
        game.reset();
        board.start();
        updateStatus();
        
        // Send new game request to server
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: 'newGame',
                userId: currentUserId
            }));
        }
    }
}

function undoMove() {
    if (confirm('Are you sure you want to undo your last move?')) {
        game.undo();
        board.position(game.fen());
        updateStatus();
        
        // Send undo request to server
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: 'undo',
                gameId: currentGameId,
                userId: currentUserId
            }));
        }
    }
}

function resignGame() {
    if (confirm('Are you sure you want to resign?')) {
        // Send resign request to server
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: 'resign',
                gameId: currentGameId,
                userId: currentUserId
            }));
        }
    }
}

function handleGameOver(result) {
    $('#game-result').text(result);
    new bootstrap.Modal(document.getElementById('gameOverModal')).show();
}

function loadExistingGame(gameId) {
    // Send request to load existing game
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: 'loadGame',
            gameId: gameId,
            userId: currentUserId
        }));
    }
}

function updateGameState(fen, pgn) {
    game.load(fen);
    board.position(fen);
    updateStatus();
}

function sendChatMessage() {
    const messageInput = $('#chat-input');
    const message = messageInput.val().trim();
    
    if (message && socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: 'chat',
            gameId: currentGameId,
            userId: currentUserId,
            message: message
        }));
        messageInput.val('');
    }
}

function addChatMessage(username, text) {
    const chatMessages = $('#chat-messages');
    chatMessages.append(`<p><strong>${username}:</strong> ${text}</p>`);
    chatMessages.scrollTop(chatMessages[0].scrollHeight);
}

// Handle page unload
window.onbeforeunload = function() {
    if (socket) {
        socket.close();
    }
};

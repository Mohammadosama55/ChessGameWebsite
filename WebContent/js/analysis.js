// Chess Game Analysis Module
class ChessAnalysis {
    constructor(gameData) {
        this.gameId = gameData.id;
        this.userId = gameData.userId;
        this.game = new Chess();
        this.currentMove = 0;
        this.moves = [];
        this.comments = new Map();
        this.variations = new Map();
        
        // Load the game PGN
        if (gameData.pgn) {
            this.game.load_pgn(gameData.pgn);
            this.moves = this.game.history({ verbose: true });
        }
        
        // Initialize the board
        this.board = Chessboard('analysisBoard', {
            position: 'start',
            draggable: true,
            onDrop: (source, target) => this.onPieceDrop(source, target)
        });
        
        this.setupEventListeners();
        this.updateMoveList();
    }
    
    setupEventListeners() {
        document.getElementById('startBtn').addEventListener('click', () => this.goToStart());
        document.getElementById('prevBtn').addEventListener('click', () => this.prevMove());
        document.getElementById('nextBtn').addEventListener('click', () => this.nextMove());
        document.getElementById('endBtn').addEventListener('click', () => this.goToEnd());
        document.getElementById('addCommentBtn').addEventListener('click', () => this.addComment());
        document.getElementById('analyzeBtn').addEventListener('click', () => this.analyzePosition());
        
        // Add click handlers to moves in the move list
        document.getElementById('moveList').addEventListener('click', (e) => {
            if (e.target.classList.contains('move')) {
                const moveNumber = parseInt(e.target.dataset.moveNumber);
                this.goToMove(moveNumber);
            }
        });
    }
    
    onPieceDrop(source, target) {
        // Check if the move is legal
        const move = this.game.move({
            from: source,
            to: target,
            promotion: 'q' // Always promote to queen for simplicity
        });
        
        if (move === null) {
            return 'snapback';
        }
        
        // Add the move as a variation if we're not at the end of the game
        if (this.currentMove < this.moves.length) {
            this.addVariation(this.game.fen(), move);
        }
        
        this.updateBoard();
    }
    
    goToStart() {
        this.currentMove = 0;
        this.game.reset();
        this.updateBoard();
    }
    
    prevMove() {
        if (this.currentMove > 0) {
            this.currentMove--;
            this.game.undo();
            this.updateBoard();
        }
    }
    
    nextMove() {
        if (this.currentMove < this.moves.length) {
            const move = this.moves[this.currentMove];
            this.game.move(move);
            this.currentMove++;
            this.updateBoard();
        }
    }
    
    goToEnd() {
        while (this.currentMove < this.moves.length) {
            this.nextMove();
        }
    }
    
    goToMove(moveNumber) {
        this.goToStart();
        for (let i = 0; i < moveNumber; i++) {
            this.nextMove();
        }
    }
    
    updateBoard() {
        this.board.position(this.game.fen());
        this.updateMoveList();
        this.updateAnalysis();
        
        // Update navigation buttons
        document.getElementById('prevBtn').disabled = this.currentMove === 0;
        document.getElementById('nextBtn').disabled = this.currentMove === this.moves.length;
        
        // Save current position for analysis
        this.currentPosition = this.game.fen();
    }
    
    updateMoveList() {
        const moveList = document.getElementById('moveList');
        let html = '';
        let moveNumber = 1;
        
        for (let i = 0; i < this.moves.length; i += 2) {
            html += `<div class="move-row">`;
            html += `<span class="move-number">${moveNumber}.</span>`;
            
            // White's move
            html += `<span class="move ${i === this.currentMove - 1 ? 'current' : ''}" 
                          data-move-number="${i}">${this.moves[i].san}</span>`;
            
            // Black's move if it exists
            if (i + 1 < this.moves.length) {
                html += `<span class="move ${i + 1 === this.currentMove - 1 ? 'current' : ''}" 
                              data-move-number="${i + 1}">${this.moves[i + 1].san}</span>`;
            }
            
            html += `</div>`;
            moveNumber++;
        }
        
        moveList.innerHTML = html;
    }
    
    updateAnalysis() {
        const analysisResults = document.getElementById('analysisResults');
        const currentFen = this.game.fen();
        
        // Get comments for current position
        const comments = this.comments.get(currentFen) || [];
        
        // Get variations for current position
        const variations = this.variations.get(currentFen) || [];
        
        let html = '';
        
        // Display comments
        if (comments.length > 0) {
            html += '<div class="comments-section mb-3">';
            html += '<h6>Comments:</h6>';
            comments.forEach(comment => {
                html += `<div class="comment">${comment}</div>`;
            });
            html += '</div>';
        }
        
        // Display variations
        if (variations.length > 0) {
            html += '<div class="variations-section">';
            html += '<h6>Variations:</h6>';
            variations.forEach(variation => {
                html += `<div class="variation">${variation.moves.join(' ')}</div>`;
            });
            html += '</div>';
        }
        
        analysisResults.innerHTML = html;
    }
    
    addComment() {
        const input = document.getElementById('commentInput');
        const comment = input.value.trim();
        
        if (comment) {
            const currentFen = this.game.fen();
            if (!this.comments.has(currentFen)) {
                this.comments.set(currentFen, []);
            }
            this.comments.get(currentFen).push(comment);
            
            // Save comment to server
            this.saveComment(currentFen, comment);
            
            // Clear input and update display
            input.value = '';
            this.updateAnalysis();
        }
    }
    
    addVariation(fen, move) {
        if (!this.variations.has(fen)) {
            this.variations.set(fen, []);
        }
        
        const variation = {
            moves: [move.san],
            evaluation: null
        };
        
        this.variations.get(fen).push(variation);
        this.updateAnalysis();
    }
    
    async analyzePosition() {
        const analysisBtn = document.getElementById('analyzeBtn');
        analysisBtn.disabled = true;
        analysisBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Analyzing...';
        
        try {
            const response = await fetch(`${window.location.origin}/analysis/analyze`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    gameId: this.gameId,
                    fen: this.game.fen(),
                    depth: 20
                })
            });
            
            if (response.ok) {
                const analysis = await response.json();
                this.displayAnalysis(analysis);
            } else {
                throw new Error('Failed to analyze position');
            }
        } catch (error) {
            console.error('Analysis error:', error);
            alert('Failed to analyze position. Please try again.');
        } finally {
            analysisBtn.disabled = false;
            analysisBtn.textContent = 'Analyze Position';
        }
    }
    
    displayAnalysis(analysis) {
        const resultsDiv = document.getElementById('analysisResults');
        
        let html = '<div class="analysis-results">';
        html += `<div class="evaluation">Evaluation: ${analysis.evaluation}</div>`;
        html += '<div class="best-moves">';
        html += '<h6>Best Moves:</h6>';
        html += '<ul>';
        analysis.bestMoves.forEach(move => {
            html += `<li>${move.san} (${move.evaluation})</li>`;
        });
        html += '</ul>';
        html += '</div>';
        
        if (analysis.threats) {
            html += '<div class="threats mt-3">';
            html += '<h6>Threats:</h6>';
            html += '<ul>';
            analysis.threats.forEach(threat => {
                html += `<li>${threat}</li>`;
            });
            html += '</ul>';
            html += '</div>';
        }
        
        html += '</div>';
        resultsDiv.innerHTML = html;
    }
    
    async saveComment(fen, comment) {
        try {
            await fetch(`${window.location.origin}/analysis/comment`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    gameId: this.gameId,
                    fen: fen,
                    comment: comment
                })
            });
        } catch (error) {
            console.error('Failed to save comment:', error);
        }
    }
}

// Initialize analysis when the page loads
document.addEventListener('DOMContentLoaded', () => {
    if (typeof gameData !== 'undefined') {
        window.analysis = new ChessAnalysis(gameData);
    }
});

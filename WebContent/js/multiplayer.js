// Multiplayer WebSocket Client for Chess Game
class ChessMultiplayer {
    constructor(gameId, userId) {
        this.gameId = gameId;
        this.userId = userId;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000; // Start with 1 second delay
        this.setupWebSocket();
    }

    setupWebSocket() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}${window.location.pathname}/ws/game/${this.gameId}`;
        
        this.ws = new WebSocket(wsUrl);
        
        this.ws.onopen = () => {
            console.log('WebSocket connection established');
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;
            this.sendJoinGame();
        };

        this.ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                this.handleMessage(message);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        };

        this.ws.onclose = () => {
            console.log('WebSocket connection closed');
            this.attemptReconnect();
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
    }

    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            setTimeout(() => {
                console.log(`Attempting to reconnect (${this.reconnectAttempts + 1}/${this.maxReconnectAttempts})`);
                this.setupWebSocket();
                this.reconnectAttempts++;
                this.reconnectDelay *= 2; // Exponential backoff
            }, this.reconnectDelay);
        } else {
            console.error('Max reconnection attempts reached');
            this.showReconnectionError();
        }
    }

    sendJoinGame() {
        this.sendMessage({
            type: 'JOIN',
            gameId: this.gameId,
            userId: this.userId
        });
    }

    sendMove(move) {
        this.sendMessage({
            type: 'MOVE',
            gameId: this.gameId,
            userId: this.userId,
            move: move
        });
    }

    sendResignation() {
        this.sendMessage({
            type: 'RESIGN',
            gameId: this.gameId,
            userId: this.userId
        });
    }

    sendDrawOffer() {
        this.sendMessage({
            type: 'DRAW_OFFER',
            gameId: this.gameId,
            userId: this.userId
        });
    }

    sendDrawResponse(accepted) {
        this.sendMessage({
            type: 'DRAW_RESPONSE',
            gameId: this.gameId,
            userId: this.userId,
            accepted: accepted
        });
    }

    sendMessage(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        } else {
            console.error('WebSocket is not connected');
        }
    }

    handleMessage(message) {
        switch (message.type) {
            case 'GAME_STATE':
                this.updateGameState(message.state);
                break;
            case 'MOVE':
                this.handleOpponentMove(message.move);
                break;
            case 'DRAW_OFFER':
                this.handleDrawOffer(message.fromUserId);
                break;
            case 'DRAW_RESPONSE':
                this.handleDrawResponse(message.accepted);
                break;
            case 'RESIGNATION':
                this.handleResignation(message.userId);
                break;
            case 'GAME_OVER':
                this.handleGameOver(message.result);
                break;
            case 'ERROR':
                this.handleError(message.error);
                break;
            default:
                console.warn('Unknown message type:', message.type);
        }
    }

    updateGameState(state) {
        if (window.game && typeof window.game.updateState === 'function') {
            window.game.updateState(state);
        }
    }

    handleOpponentMove(move) {
        if (window.game && typeof window.game.makeMove === 'function') {
            window.game.makeMove(move);
        }
    }

    handleDrawOffer(fromUserId) {
        if (fromUserId !== this.userId) {
            const accept = confirm('Your opponent has offered a draw. Do you accept?');
            this.sendDrawResponse(accept);
        }
    }

    handleDrawResponse(accepted) {
        if (accepted) {
            alert('Draw offer accepted. Game ended in a draw.');
        } else {
            alert('Draw offer declined. Game continues.');
        }
    }

    handleResignation(userId) {
        const winner = userId === this.userId ? 'Opponent' : 'You';
        alert(`${winner} won by resignation!`);
    }

    handleGameOver(result) {
        alert(`Game Over: ${result}`);
    }

    handleError(error) {
        console.error('Game error:', error);
        alert(`Error: ${error}`);
    }

    showReconnectionError() {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger';
        errorDiv.innerHTML = `
            Connection lost. Unable to reconnect to the game.
            <button class="btn btn-primary btn-sm ms-3" onclick="location.reload()">
                Reload Page
            </button>
        `;
        document.body.insertBefore(errorDiv, document.body.firstChild);
    }

    disconnect() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// Initialize multiplayer when the page loads and game ID is available
document.addEventListener('DOMContentLoaded', () => {
    const gameId = document.getElementById('gameId')?.value;
    const userId = document.getElementById('userId')?.value;
    
    if (gameId && userId) {
        window.multiplayer = new ChessMultiplayer(gameId, userId);
    }
});

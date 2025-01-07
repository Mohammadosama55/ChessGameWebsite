// Tournament WebSocket
let tournamentSocket = null;
let chatSocket = null;
let spectatorSocket = null;

// Initialize tournament page
document.addEventListener('DOMContentLoaded', function() {
    initializeTournamentSocket();
    initializeChatSocket();
    initializeSpectatorSocket();
    initializeBracket();
    initializeStatistics();
    
    // Chat message sending
    document.getElementById('sendMessage').addEventListener('click', sendChatMessage);
    document.getElementById('chatInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendChatMessage();
        }
    });
});

// Tournament WebSocket functions
function initializeTournamentSocket() {
    const tournamentId = getTournamentId();
    tournamentSocket = new WebSocket(`ws://${window.location.host}/websocket/tournament`);
    
    tournamentSocket.onopen = function() {
        console.log('Tournament WebSocket connected');
        // Send authentication message
        tournamentSocket.send(JSON.stringify({
            type: 'auth',
            tournamentId: tournamentId
        }));
    };
    
    tournamentSocket.onmessage = function(event) {
        const message = JSON.parse(event.data);
        handleTournamentMessage(message);
    };
    
    tournamentSocket.onclose = function() {
        console.log('Tournament WebSocket closed');
        // Attempt to reconnect after delay
        setTimeout(initializeTournamentSocket, 5000);
    };
}

function handleTournamentMessage(message) {
    switch (message.type) {
        case 'bracket_update':
            updateBracket(message.data);
            break;
        case 'round_update':
            updateRound(message.data);
            break;
        case 'stats_update':
            updateStatistics(message.data);
            break;
    }
}

// Chat WebSocket functions
function initializeChatSocket() {
    const tournamentId = getTournamentId();
    chatSocket = new WebSocket(`ws://${window.location.host}/websocket/tournament-chat`);
    
    chatSocket.onopen = function() {
        console.log('Chat WebSocket connected');
        chatSocket.send(JSON.stringify({
            type: 'auth',
            tournamentId: tournamentId
        }));
    };
    
    chatSocket.onmessage = function(event) {
        const message = JSON.parse(event.data);
        handleChatMessage(message);
    };
    
    chatSocket.onclose = function() {
        console.log('Chat WebSocket closed');
        setTimeout(initializeChatSocket, 5000);
    };
}

function handleChatMessage(message) {
    const chatContainer = document.getElementById('chatMessages');
    
    switch (message.type) {
        case 'message':
            appendChatMessage(message);
            break;
        case 'status':
            appendStatusMessage(message);
            break;
        case 'error':
            showError(message.content);
            break;
    }
    
    // Auto-scroll to bottom
    chatContainer.scrollTop = chatContainer.scrollHeight;
}

function sendChatMessage() {
    const input = document.getElementById('chatInput');
    const content = input.value.trim();
    
    if (content && chatSocket.readyState === WebSocket.OPEN) {
        chatSocket.send(JSON.stringify({
            type: 'message',
            content: content
        }));
        input.value = '';
    }
}

// Spectator WebSocket functions
function initializeSpectatorSocket() {
    spectatorSocket = new WebSocket(`ws://${window.location.host}/websocket/tournament-spectator`);
    
    spectatorSocket.onopen = function() {
        console.log('Spectator WebSocket connected');
    };
    
    spectatorSocket.onmessage = function(event) {
        const message = JSON.parse(event.data);
        handleSpectatorMessage(message);
    };
    
    spectatorSocket.onclose = function() {
        console.log('Spectator WebSocket closed');
        setTimeout(initializeSpectatorSocket, 5000);
    };
}

function handleSpectatorMessage(message) {
    switch (message.type) {
        case 'game_state':
            updateGameState(message.data);
            break;
        case 'move':
            updateGameMove(message.data);
            break;
    }
}

// Tournament bracket functions
function initializeBracket() {
    const tournamentId = getTournamentId();
    fetch(`/tournament/bracket?id=${tournamentId}`)
        .then(response => response.json())
        .then(data => {
            renderBracket(data);
        })
        .catch(error => {
            console.error('Error loading bracket:', error);
            showError('Failed to load tournament bracket');
        });
}

function renderBracket(data) {
    const container = document.getElementById('bracketContainer');
    container.innerHTML = ''; // Clear existing content
    
    switch (data.type) {
        case 'SINGLE_ELIMINATION':
            renderSingleEliminationBracket(container, data.rounds);
            break;
        case 'DOUBLE_ELIMINATION':
            renderDoubleEliminationBracket(container, data.winnersBracket, data.losersBracket);
            break;
        case 'SWISS':
            renderSwissBracket(container, data.rounds, data.standings);
            break;
        case 'ROUND_ROBIN':
            renderRoundRobinBracket(container, data.rounds, data.crosstable);
            break;
    }
}

// Tournament statistics functions
function initializeStatistics() {
    const tournamentId = getTournamentId();
    fetch(`/tournament/stats?id=${tournamentId}`)
        .then(response => response.json())
        .then(data => {
            updateStatistics(data);
        })
        .catch(error => {
            console.error('Error loading statistics:', error);
            showError('Failed to load tournament statistics');
        });
}

function updateStatistics(data) {
    const container = document.getElementById('tournamentStats');
    
    // Create statistics HTML
    const html = `
        <div class="mb-3">
            <h6>Games</h6>
            <p>Total: ${data.totalGames}</p>
            <p>Completed: ${data.completedGames}</p>
        </div>
        <div class="mb-3">
            <h6>Results</h6>
            <p>White wins: ${data.whiteWins} (${data.whiteWinPercentage}%)</p>
            <p>Black wins: ${data.blackWins} (${data.blackWinPercentage}%)</p>
            <p>Draws: ${data.draws} (${data.drawPercentage}%)</p>
        </div>
        <div>
            <h6>Game Length</h6>
            <p>Average: ${data.avgGameLength} moves</p>
            <p>Shortest: ${data.minGameLength} moves</p>
            <p>Longest: ${data.maxGameLength} moves</p>
        </div>
    `;
    
    container.innerHTML = html;
}

// Utility functions
function getTournamentId() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('id');
}

function appendChatMessage(message) {
    const chatContainer = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'chat-message';
    
    const timestamp = new Date(message.timestamp).toLocaleTimeString();
    messageDiv.innerHTML = `
        <span class="chat-timestamp">[${timestamp}]</span>
        <span class="chat-username">${message.username}:</span>
        <span class="chat-content">${escapeHtml(message.content)}</span>
    `;
    
    chatContainer.appendChild(messageDiv);
}

function appendStatusMessage(message) {
    const chatContainer = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'chat-status';
    messageDiv.textContent = message.content;
    chatContainer.appendChild(messageDiv);
}

function showError(message) {
    // You can implement your preferred error display method
    console.error(message);
    alert(message);
}

function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Add tournament.css styles
const style = document.createElement('style');
style.textContent = `
    .chat-container {
        height: 400px;
        overflow-y: auto;
        border: 1px solid #dee2e6;
        padding: 10px;
        margin-bottom: 10px;
    }
    
    .chat-message {
        margin-bottom: 5px;
    }
    
    .chat-timestamp {
        color: #6c757d;
        font-size: 0.8em;
        margin-right: 5px;
    }
    
    .chat-username {
        font-weight: bold;
        margin-right: 5px;
    }
    
    .chat-status {
        color: #6c757d;
        font-style: italic;
        margin-bottom: 5px;
        text-align: center;
    }
    
    .tournament-bracket {
        overflow-x: auto;
        padding: 20px;
    }
    
    .match {
        border: 1px solid #dee2e6;
        padding: 10px;
        margin: 5px;
        background: white;
    }
    
    .match-player {
        display: flex;
        justify-content: space-between;
        padding: 5px;
    }
    
    .match-winner {
        font-weight: bold;
        background: #e9ecef;
    }
`;
document.head.appendChild(style);

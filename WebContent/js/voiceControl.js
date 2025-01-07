// Voice Control for Chess Game
class VoiceControl {
    constructor() {
        this.recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
        this.recognition.continuous = false;
        this.recognition.lang = 'en-US';
        this.isListening = false;
        this.setupRecognition();
    }

    setupRecognition() {
        this.recognition.onstart = () => {
            console.log('Voice recognition started');
            this.isListening = true;
            this.updateUI();
        };

        this.recognition.onend = () => {
            console.log('Voice recognition ended');
            this.isListening = false;
            this.updateUI();
        };

        this.recognition.onresult = (event) => {
            const command = event.results[0][0].transcript.toLowerCase();
            console.log('Voice command:', command);
            this.processCommand(command);
        };

        this.recognition.onerror = (event) => {
            console.error('Voice recognition error:', event.error);
            this.isListening = false;
            this.updateUI();
        };
    }

    start() {
        if (!this.isListening) {
            this.recognition.start();
        }
    }

    stop() {
        if (this.isListening) {
            this.recognition.stop();
        }
    }

    processCommand(command) {
        // Convert spoken moves like "e2 to e4" into chess notation
        const movePattern = /([a-h][1-8])(?:\s+(?:to|moves?\s+to)\s+)?([a-h][1-8])/i;
        const match = command.match(movePattern);

        if (match) {
            const [_, from, to] = match;
            this.makeMove(from, to);
            return;
        }

        // Handle other commands
        switch (command) {
            case 'undo':
            case 'take back':
                this.undoMove();
                break;
            case 'resign':
            case 'give up':
                this.resignGame();
                break;
            case 'offer draw':
            case 'propose draw':
                this.offerDraw();
                break;
            default:
                console.log('Unknown command:', command);
        }
    }

    makeMove(from, to) {
        // Implement move logic here
        if (window.game && typeof window.game.move === 'function') {
            window.game.move(from, to);
        }
    }

    undoMove() {
        if (window.game && typeof window.game.undo === 'function') {
            window.game.undo();
        }
    }

    resignGame() {
        if (window.game && typeof window.game.resign === 'function') {
            window.game.resign();
        }
    }

    offerDraw() {
        if (window.game && typeof window.game.offerDraw === 'function') {
            window.game.offerDraw();
        }
    }

    updateUI() {
        const micButton = document.getElementById('voiceControlButton');
        if (micButton) {
            micButton.classList.toggle('active', this.isListening);
            micButton.title = this.isListening ? 'Voice control active' : 'Click to activate voice control';
        }
    }
}

// Initialize voice control when the page loads
document.addEventListener('DOMContentLoaded', () => {
    window.voiceControl = new VoiceControl();
    
    // Add voice control button to the UI
    const controlsContainer = document.querySelector('.game-controls');
    if (controlsContainer) {
        const voiceButton = document.createElement('button');
        voiceButton.id = 'voiceControlButton';
        voiceButton.className = 'btn btn-outline-primary';
        voiceButton.innerHTML = '<i class="bi bi-mic"></i>';
        voiceButton.title = 'Click to activate voice control';
        
        voiceButton.addEventListener('click', () => {
            if (window.voiceControl.isListening) {
                window.voiceControl.stop();
            } else {
                window.voiceControl.start();
            }
        });
        
        controlsContainer.appendChild(voiceButton);
    }
});

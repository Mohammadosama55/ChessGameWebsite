// Achievement WebSocket connection
let achievementSocket = null;

function connectAchievementWebSocket(userId) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}${contextPath}/websocket/achievements`;
    
    achievementSocket = new WebSocket(wsUrl);
    
    achievementSocket.onopen = function() {
        // Authenticate the WebSocket connection
        const authMessage = {
            type: 'authenticate',
            userId: userId
        };
        achievementSocket.send(JSON.stringify(authMessage));
    };
    
    achievementSocket.onmessage = function(event) {
        const notification = JSON.parse(event.data);
        
        if (notification.type === 'achievement') {
            showAchievementNotification(notification);
        }
    };
    
    achievementSocket.onclose = function() {
        // Attempt to reconnect after a delay
        setTimeout(() => connectAchievementWebSocket(userId), 5000);
    };
    
    achievementSocket.onerror = function(error) {
        console.error('WebSocket Error:', error);
    };
}

function showAchievementNotification(achievement) {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = 'achievement-notification';
    notification.innerHTML = `
        <div class="achievement-notification-content">
            <div class="achievement-icon">
                <i class="bi bi-${achievement.icon}"></i>
            </div>
            <div class="achievement-info">
                <h4>${achievement.title}</h4>
                <p>${achievement.message}</p>
                <div class="achievement-points">+${achievement.points} points</div>
            </div>
        </div>
    `;
    
    // Add to notification container
    const container = document.getElementById('achievement-notifications');
    container.appendChild(notification);
    
    // Animate in
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    // Play sound effect
    playAchievementSound();
    
    // Remove after delay
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            notification.remove();
        }, 300);
    }, 5000);
    
    // Acknowledge notification
    acknowledgeAchievement(achievement.data.id);
    
    // Update achievements page if it's open
    if (typeof updateAchievements === 'function') {
        updateAchievements();
    }
}

function acknowledgeAchievement(achievementId) {
    if (achievementSocket && achievementSocket.readyState === WebSocket.OPEN) {
        const message = {
            type: 'acknowledge',
            achievementId: achievementId
        };
        achievementSocket.send(JSON.stringify(message));
    }
}

function playAchievementSound() {
    const audio = new Audio(`${contextPath}/sounds/achievement.mp3`);
    audio.volume = 0.5;
    audio.play().catch(error => {
        console.log('Could not play achievement sound:', error);
    });
}

// Add notification styles
const styles = `
    #achievement-notifications {
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 9999;
    }
    
    .achievement-notification {
        background: rgba(0, 0, 0, 0.9);
        color: white;
        border-radius: 8px;
        padding: 15px;
        margin-bottom: 10px;
        width: 300px;
        transform: translateX(120%);
        transition: transform 0.3s ease-out;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }
    
    .achievement-notification.show {
        transform: translateX(0);
    }
    
    .achievement-notification-content {
        display: flex;
        align-items: center;
    }
    
    .achievement-icon {
        font-size: 2rem;
        margin-right: 15px;
        color: #ffd700;
    }
    
    .achievement-info h4 {
        margin: 0;
        font-size: 1rem;
        color: #ffd700;
    }
    
    .achievement-info p {
        margin: 5px 0;
        font-size: 0.9rem;
    }
    
    .achievement-points {
        font-size: 0.8rem;
        color: #ffd700;
        margin-top: 5px;
    }
`;

// Add styles to document
const styleSheet = document.createElement('style');
styleSheet.textContent = styles;
document.head.appendChild(styleSheet);

// Create notification container
document.addEventListener('DOMContentLoaded', function() {
    const container = document.createElement('div');
    container.id = 'achievement-notifications';
    document.body.appendChild(container);
});

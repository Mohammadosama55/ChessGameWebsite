package com.chessgame.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.chessgame.model.Achievement;

@ServerEndpoint("/websocket/achievements")
public class AchievementWebSocket {
    private static final Map<Integer, Set<Session>> userSessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    
    @OnOpen
    public void onOpen(Session session) {
        // User ID will be set when the client connects with authentication
    }
    
    @OnClose
    public void onClose(Session session) {
        // Remove session from all user mappings
        for (Set<Session> sessions : userSessions.values()) {
            sessions.remove(session);
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            ClientMessage clientMessage = gson.fromJson(message, ClientMessage.class);
            
            switch (clientMessage.type) {
                case "authenticate" -> {
                    int userId = clientMessage.userId;
                    userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                              .add(session);
                }
                case "acknowledge" -> {
                    // Handle achievement acknowledgment
                    // Could update a database to mark notification as seen
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void notifyAchievement(int userId, Achievement achievement) {
        Set<Session> sessions = userSessions.get(userId);
        if (sessions != null) {
            AchievementNotification notification = new AchievementNotification(achievement);
            String message = gson.toJson(notification);
            
            for (Session session : sessions) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static class ClientMessage {
        String type;
        int userId;
        int achievementId;
    }
    
    private static class AchievementNotification {
        final String type = "achievement";
        final String title;
        final String message;
        final String icon;
        final int points;
        final Map<String, Object> data;
        
        AchievementNotification(Achievement achievement) {
            this.title = "New Achievement Unlocked!";
            this.message = achievement.getName();
            this.icon = achievement.getIcon();
            this.points = achievement.getPoints();
            
            this.data = new ConcurrentHashMap<>();
            data.put("id", achievement.getId());
            data.put("name", achievement.getName());
            data.put("description", achievement.getDescription());
            data.put("points", achievement.getPoints());
            data.put("earnedDate", achievement.getEarnedDate().toString());
        }
    }
}

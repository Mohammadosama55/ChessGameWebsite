package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.websocket.Session;
import com.google.gson.Gson;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;

public class TournamentNotificationService {
    private static final Map<Integer, Set<Session>> userSessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    
    public void createNotification(int tournamentId, int userId, String type, String message) throws SQLException {
        String sql = "INSERT INTO tournament_notifications (tournament_id, user_id, type, message) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, type);
            pstmt.setString(4, message);
            
            pstmt.executeUpdate();
            
            // Send real-time notification if user is online
            sendNotification(userId, new Notification(type, message, tournamentId));
        }
    }
    
    public void createBulkNotifications(int tournamentId, List<Integer> userIds, String type, String message) 
            throws SQLException {
        String sql = "INSERT INTO tournament_notifications (tournament_id, user_id, type, message) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (int userId : userIds) {
                pstmt.setInt(1, tournamentId);
                pstmt.setInt(2, userId);
                pstmt.setString(3, type);
                pstmt.setString(4, message);
                pstmt.addBatch();
                
                // Send real-time notification if user is online
                sendNotification(userId, new Notification(type, message, tournamentId));
            }
            
            pstmt.executeBatch();
        }
    }
    
    public List<Notification> getUnreadNotifications(int userId) throws SQLException {
        String sql = "SELECT * FROM tournament_notifications " +
                    "WHERE user_id = ? AND is_read = FALSE " +
                    "ORDER BY created_at DESC";
        
        List<Notification> notifications = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(new Notification(
                        rs.getInt("notification_id"),
                        rs.getString("type"),
                        rs.getString("message"),
                        rs.getInt("tournament_id"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        
        return notifications;
    }
    
    public void markAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE tournament_notifications SET is_read = TRUE WHERE notification_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, notificationId);
            pstmt.executeUpdate();
        }
    }
    
    public void markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE tournament_notifications SET is_read = TRUE WHERE user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    public void registerSession(int userId, Session session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }
    
    public void removeSession(int userId, Session session) {
        Set<Session> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }
    
    private void sendNotification(int userId, Notification notification) {
        Set<Session> sessions = userSessions.get(userId);
        if (sessions != null) {
            String jsonNotification = gson.toJson(notification);
            sessions.forEach(session -> {
                try {
                    session.getBasicRemote().sendText(jsonNotification);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    // Notification types
    public static class NotificationType {
        public static final String TOURNAMENT_START = "TOURNAMENT_START";
        public static final String ROUND_START = "ROUND_START";
        public static final String GAME_START = "GAME_START";
        public static final String GAME_RESULT = "GAME_RESULT";
        public static final String TOURNAMENT_END = "TOURNAMENT_END";
        public static final String PAIRING_ANNOUNCEMENT = "PAIRING_ANNOUNCEMENT";
        public static final String TOURNAMENT_ANNOUNCEMENT = "TOURNAMENT_ANNOUNCEMENT";
    }
    
    // Notification model
    public static class Notification {
        private Integer notificationId;
        private String type;
        private String message;
        private int tournamentId;
        private Timestamp createdAt;
        
        public Notification(String type, String message, int tournamentId) {
            this.type = type;
            this.message = message;
            this.tournamentId = tournamentId;
        }
        
        public Notification(Integer notificationId, String type, String message, 
                          int tournamentId, Timestamp createdAt) {
            this.notificationId = notificationId;
            this.type = type;
            this.message = message;
            this.tournamentId = tournamentId;
            this.createdAt = createdAt;
        }
        
        // Getters
        public Integer getNotificationId() {
            return notificationId;
        }
        
        public String getType() {
            return type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getTournamentId() {
            return tournamentId;
        }
        
        public Timestamp getCreatedAt() {
            return createdAt;
        }
    }
}

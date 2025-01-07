package com.chessgame.service;

import com.chessgame.model.DBUtil;
import com.google.gson.Gson;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/tournament-chat")
public class TournamentChatService {
    private static final Gson gson = new Gson();
    private static final Map<Integer, Set<Session>> tournamentSessions = new ConcurrentHashMap<>();
    private static final Map<Session, UserInfo> sessionUsers = new ConcurrentHashMap<>();
    private static final int MAX_MESSAGE_HISTORY = 100;
    
    @OnOpen
    public void onOpen(Session session) {
        // User info will be set during authentication
    }
    
    @OnClose
    public void onClose(Session session) {
        UserInfo userInfo = sessionUsers.remove(session);
        if (userInfo != null) {
            Set<Session> sessions = tournamentSessions.get(userInfo.tournamentId);
            if (sessions != null) {
                sessions.remove(session);
                broadcastUserStatus(userInfo, "left");
            }
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
            ChatMessage chatMessage = gson.fromJson(message, ChatMessage.class);
            
            switch (chatMessage.type) {
                case "auth" -> handleAuthentication(session, chatMessage);
                case "message" -> handleChatMessage(session, chatMessage);
                case "command" -> handleCommand(session, chatMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }
    
    private void handleAuthentication(Session session, ChatMessage message) throws SQLException {
        // Verify user is part of tournament
        if (!isUserInTournament(message.userId, message.tournamentId)) {
            sendError(session, "User is not part of this tournament");
            return;
        }
        
        // Store user info
        UserInfo userInfo = new UserInfo(
            message.userId,
            message.tournamentId,
            getUserName(message.userId),
            getUserRole(message.userId, message.tournamentId)
        );
        sessionUsers.put(session, userInfo);
        
        // Add to tournament sessions
        tournamentSessions.computeIfAbsent(message.tournamentId, k -> ConcurrentHashMap.newKeySet())
                         .add(session);
        
        // Send message history
        sendMessageHistory(session, message.tournamentId);
        
        // Broadcast user joined
        broadcastUserStatus(userInfo, "joined");
    }
    
    private void handleChatMessage(Session session, ChatMessage message) throws SQLException {
        UserInfo userInfo = sessionUsers.get(session);
        if (userInfo == null) {
            sendError(session, "Not authenticated");
            return;
        }
        
        // Store message
        storeChatMessage(userInfo, message.content);
        
        // Broadcast message
        broadcastMessage(new ChatMessage(
            "message",
            userInfo.userId,
            userInfo.tournamentId,
            message.content,
            userInfo.username,
            userInfo.role,
            LocalDateTime.now()
        ));
    }
    
    private void handleCommand(Session session, ChatMessage message) {
        UserInfo userInfo = sessionUsers.get(session);
        if (userInfo == null) {
            sendError(session, "Not authenticated");
            return;
        }
        
        // Only moderators and organizers can use commands
        if (!userInfo.role.equals("MODERATOR") && !userInfo.role.equals("ORGANIZER")) {
            sendError(session, "Insufficient permissions");
            return;
        }
        
        String[] parts = message.content.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (command) {
            case "/mute" -> handleMuteCommand(userInfo, args);
            case "/unmute" -> handleUnmuteCommand(userInfo, args);
            case "/clear" -> handleClearCommand(userInfo);
            case "/announce" -> handleAnnounceCommand(userInfo, args);
            default -> sendError(session, "Unknown command: " + command);
        }
    }
    
    private void handleMuteCommand(UserInfo moderator, String username) {
        try {
            int userId = getUserIdByName(username);
            if (userId == -1) {
                sendError(getSessionByUserInfo(moderator), "User not found: " + username);
                return;
            }
            
            muteUser(userId, moderator.tournamentId);
            broadcastModeration(moderator.tournamentId, username + " has been muted");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void handleUnmuteCommand(UserInfo moderator, String username) {
        try {
            int userId = getUserIdByName(username);
            if (userId == -1) {
                sendError(getSessionByUserInfo(moderator), "User not found: " + username);
                return;
            }
            
            unmuteUser(userId, moderator.tournamentId);
            broadcastModeration(moderator.tournamentId, username + " has been unmuted");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void handleClearCommand(UserInfo moderator) {
        try {
            clearChatHistory(moderator.tournamentId);
            broadcastModeration(moderator.tournamentId, "Chat history has been cleared");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAnnounceCommand(UserInfo moderator, String message) {
        broadcastAnnouncement(moderator.tournamentId, message);
    }
    
    private void storeChatMessage(UserInfo userInfo, String content) throws SQLException {
        String sql = """
            INSERT INTO tournament_chat_messages 
            (tournament_id, user_id, content, sent_at)
            VALUES (?, ?, ?, ?)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userInfo.tournamentId);
            pstmt.setInt(2, userInfo.userId);
            pstmt.setString(3, content);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
        }
    }
    
    private void sendMessageHistory(Session session, int tournamentId) throws SQLException {
        String sql = """
            SELECT m.*, u.username, 
                   COALESCE(tm.role, 'PLAYER') as user_role
            FROM tournament_chat_messages m
            JOIN users u ON m.user_id = u.user_id
            LEFT JOIN tournament_moderators tm ON m.user_id = tm.user_id 
                AND m.tournament_id = tm.tournament_id
            WHERE m.tournament_id = ?
            ORDER BY m.sent_at DESC
            LIMIT ?
            """;
            
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, MAX_MESSAGE_HISTORY);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(new ChatMessage(
                        "message",
                        rs.getInt("user_id"),
                        tournamentId,
                        rs.getString("content"),
                        rs.getString("username"),
                        rs.getString("user_role"),
                        rs.getTimestamp("sent_at").toLocalDateTime()
                    ));
                }
            }
        }
        
        Collections.reverse(messages);
        for (ChatMessage message : messages) {
            sendToSession(session, message);
        }
    }
    
    private void broadcastMessage(ChatMessage message) {
        Set<Session> sessions = tournamentSessions.get(message.tournamentId);
        if (sessions != null) {
            String json = gson.toJson(message);
            for (Session session : sessions) {
                try {
                    session.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void broadcastUserStatus(UserInfo userInfo, String status) {
        ChatMessage message = new ChatMessage(
            "status",
            userInfo.userId,
            userInfo.tournamentId,
            userInfo.username + " has " + status + " the chat",
            userInfo.username,
            userInfo.role,
            LocalDateTime.now()
        );
        broadcastMessage(message);
    }
    
    private void broadcastModeration(int tournamentId, String content) {
        ChatMessage message = new ChatMessage(
            "moderation",
            0,
            tournamentId,
            content,
            "System",
            "SYSTEM",
            LocalDateTime.now()
        );
        broadcastMessage(message);
    }
    
    private void broadcastAnnouncement(int tournamentId, String content) {
        ChatMessage message = new ChatMessage(
            "announcement",
            0,
            tournamentId,
            content,
            "Announcement",
            "SYSTEM",
            LocalDateTime.now()
        );
        broadcastMessage(message);
    }
    
    private void sendError(Session session, String error) {
        ChatMessage message = new ChatMessage(
            "error",
            0,
            0,
            error,
            "System",
            "SYSTEM",
            LocalDateTime.now()
        );
        sendToSession(session, message);
    }
    
    private void sendToSession(Session session, ChatMessage message) {
        try {
            session.getBasicRemote().sendText(gson.toJson(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Session getSessionByUserInfo(UserInfo userInfo) {
        return sessionUsers.entrySet().stream()
            .filter(e -> e.getValue().equals(userInfo))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    private boolean isUserInTournament(int userId, int tournamentId) throws SQLException {
        String sql = """
            SELECT 1 FROM tournament_players 
            WHERE tournament_id = ? AND user_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    private String getUserName(int userId) throws SQLException {
        String sql = "SELECT username FROM users WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("username") : "Unknown";
            }
        }
    }
    
    private String getUserRole(int userId, int tournamentId) throws SQLException {
        String sql = """
            SELECT role FROM tournament_moderators 
            WHERE tournament_id = ? AND user_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("role") : "PLAYER";
            }
        }
    }
    
    private int getUserIdByName(String username) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }
    
    private void muteUser(int userId, int tournamentId) throws SQLException {
        String sql = """
            INSERT INTO tournament_muted_users (tournament_id, user_id)
            VALUES (?, ?)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }
    
    private void unmuteUser(int userId, int tournamentId) throws SQLException {
        String sql = """
            DELETE FROM tournament_muted_users 
            WHERE tournament_id = ? AND user_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }
    
    private void clearChatHistory(int tournamentId) throws SQLException {
        String sql = "DELETE FROM tournament_chat_messages WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.executeUpdate();
        }
    }
    
    private static class UserInfo {
        final int userId;
        final int tournamentId;
        final String username;
        final String role;
        
        UserInfo(int userId, int tournamentId, String username, String role) {
            this.userId = userId;
            this.tournamentId = tournamentId;
            this.username = username;
            this.role = role;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserInfo that = (UserInfo) o;
            return userId == that.userId && tournamentId == that.tournamentId;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(userId, tournamentId);
        }
    }
    
    private static class ChatMessage {
        String type;
        int userId;
        int tournamentId;
        String content;
        String username;
        String role;
        LocalDateTime timestamp;
        
        ChatMessage(String type, int userId, int tournamentId, String content,
                   String username, String role, LocalDateTime timestamp) {
            this.type = type;
            this.userId = userId;
            this.tournamentId = tournamentId;
            this.content = content;
            this.username = username;
            this.role = role;
            this.timestamp = timestamp;
        }
    }
}

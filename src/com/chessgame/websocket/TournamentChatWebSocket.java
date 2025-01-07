package com.chessgame.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ServerEndpoint(value = "/tournament-chat/{tournamentId}", configurator = WebSocketConfig.class)
public class TournamentChatWebSocket {
    private static final Map<Integer, Set<Session>> tournamentSessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @OnOpen
    public void onOpen(Session session, @PathParam("tournamentId") int tournamentId, EndpointConfig config) {
        // Get user info from HTTP session
        HttpSession httpSession = (HttpSession) config.getUserProperties().get("httpSession");
        String username = (String) httpSession.getAttribute("username");
        
        // Store username in session properties
        session.getUserProperties().put("username", username);
        session.getUserProperties().put("tournamentId", tournamentId);
        
        // Add session to tournament sessions
        tournamentSessions.computeIfAbsent(tournamentId, k -> ConcurrentHashMap.newKeySet()).add(session);
        
        // Broadcast join message
        broadcastMessage(tournamentId, createSystemMessage(username + " joined the chat"));
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        String username = (String) session.getUserProperties().get("username");
        int tournamentId = (int) session.getUserProperties().get("tournamentId");
        
        try {
            ChatMessage chatMessage = gson.fromJson(message, ChatMessage.class);
            
            switch (chatMessage.getType()) {
                case "CHAT":
                    broadcastMessage(tournamentId, createChatMessage(username, chatMessage.getContent()));
                    break;
                    
                case "SYSTEM":
                    // Only admins can send system messages
                    if (isAdmin(session)) {
                        broadcastMessage(tournamentId, createSystemMessage(chatMessage.getContent()));
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.getBasicRemote().sendText(
                    gson.toJson(createErrorMessage("Invalid message format"))
                );
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        String username = (String) session.getUserProperties().get("username");
        int tournamentId = (int) session.getUserProperties().get("tournamentId");
        
        // Remove session from tournament sessions
        Set<Session> sessions = tournamentSessions.get(tournamentId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                tournamentSessions.remove(tournamentId);
            }
        }
        
        // Broadcast leave message
        broadcastMessage(tournamentId, createSystemMessage(username + " left the chat"));
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void broadcastMessage(int tournamentId, OutgoingMessage message) {
        Set<Session> sessions = tournamentSessions.get(tournamentId);
        if (sessions != null) {
            String jsonMessage = gson.toJson(message);
            sessions.forEach(session -> {
                try {
                    session.getBasicRemote().sendText(jsonMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    private boolean isAdmin(Session session) {
        HttpSession httpSession = (HttpSession) session.getUserProperties().get("httpSession");
        return "ADMIN".equals(httpSession.getAttribute("userRole"));
    }
    
    private OutgoingMessage createChatMessage(String username, String content) {
        return new OutgoingMessage("CHAT", username, content, LocalDateTime.now().format(formatter));
    }
    
    private OutgoingMessage createSystemMessage(String content) {
        return new OutgoingMessage("SYSTEM", "System", content, LocalDateTime.now().format(formatter));
    }
    
    private OutgoingMessage createErrorMessage(String content) {
        return new OutgoingMessage("ERROR", "System", content, LocalDateTime.now().format(formatter));
    }
    
    // Inner classes for message handling
    private static class ChatMessage {
        private String type;
        private String content;
        
        public String getType() {
            return type;
        }
        
        public String getContent() {
            return content;
        }
    }
    
    private static class OutgoingMessage {
        private final String type;
        private final String username;
        private final String content;
        private final String timestamp;
        
        public OutgoingMessage(String type, String username, String content, String timestamp) {
            this.type = type;
            this.username = username;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}

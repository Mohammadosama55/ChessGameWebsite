package com.chessgame.service;

import com.chessgame.model.DBUtil;
import com.google.gson.Gson;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/tournament-spectator")
public class TournamentSpectatorService {
    private static final Gson gson = new Gson();
    private static final Map<Integer, Set<Session>> gameSpectators = new ConcurrentHashMap<>();
    private static final Map<Session, SpectatorInfo> spectatorSessions = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session) {
        // Session will be registered when spectator joins a game
    }
    
    @OnClose
    public void onClose(Session session) {
        SpectatorInfo info = spectatorSessions.remove(session);
        if (info != null) {
            Set<Session> spectators = gameSpectators.get(info.gameId);
            if (spectators != null) {
                spectators.remove(session);
            }
        }
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            SpectatorMessage msg = gson.fromJson(message, SpectatorMessage.class);
            
            switch (msg.type) {
                case "join" -> handleJoin(session, msg);
                case "leave" -> handleLeave(session);
                case "request_moves" -> sendMoveHistory(session, msg.gameId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }
    
    public void broadcastMove(int gameId, String move) {
        SpectatorMessage message = new SpectatorMessage("move", gameId, move);
        broadcast(gameId, message);
    }
    
    public void broadcastGameEnd(int gameId, String result) {
        SpectatorMessage message = new SpectatorMessage("game_end", gameId, result);
        broadcast(gameId, message);
    }
    
    private void handleJoin(Session session, SpectatorMessage message) throws SQLException {
        // Verify game exists and is part of a tournament
        if (!isValidTournamentGame(message.gameId)) {
            sendError(session, "Invalid game ID");
            return;
        }
        
        // Register spectator
        SpectatorInfo info = new SpectatorInfo(message.gameId);
        spectatorSessions.put(session, info);
        gameSpectators.computeIfAbsent(message.gameId, k -> ConcurrentHashMap.newKeySet())
                     .add(session);
        
        // Send current game state
        sendGameState(session, message.gameId);
    }
    
    private void handleLeave(Session session) {
        SpectatorInfo info = spectatorSessions.remove(session);
        if (info != null) {
            Set<Session> spectators = gameSpectators.get(info.gameId);
            if (spectators != null) {
                spectators.remove(session);
            }
        }
    }
    
    private boolean isValidTournamentGame(int gameId) throws SQLException {
        String sql = """
            SELECT 1 FROM tournament_games 
            WHERE game_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    private void sendGameState(Session session, int gameId) throws SQLException {
        String sql = """
            SELECT g.*, tg.tournament_id,
                   w.username as white_username,
                   b.username as black_username
            FROM games g
            JOIN tournament_games tg ON g.game_id = tg.game_id
            JOIN users w ON g.white_player_id = w.user_id
            JOIN users b ON g.black_player_id = b.user_id
            WHERE g.game_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> gameState = new HashMap<>();
                    gameState.put("gameId", rs.getInt("game_id"));
                    gameState.put("tournamentId", rs.getInt("tournament_id"));
                    gameState.put("whitePlayer", rs.getString("white_username"));
                    gameState.put("blackPlayer", rs.getString("black_username"));
                    gameState.put("currentPosition", rs.getString("current_position"));
                    gameState.put("moveHistory", rs.getString("move_history"));
                    
                    sendToSession(session, new SpectatorMessage("game_state", gameId, 
                        gson.toJson(gameState)));
                }
            }
        }
    }
    
    private void sendMoveHistory(Session session, int gameId) throws SQLException {
        String sql = "SELECT move_history FROM games WHERE game_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    sendToSession(session, new SpectatorMessage("move_history", gameId,
                        rs.getString("move_history")));
                }
            }
        }
    }
    
    private void broadcast(int gameId, SpectatorMessage message) {
        Set<Session> spectators = gameSpectators.get(gameId);
        if (spectators != null) {
            String json = gson.toJson(message);
            for (Session session : spectators) {
                try {
                    session.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void sendError(Session session, String error) {
        sendToSession(session, new SpectatorMessage("error", 0, error));
    }
    
    private void sendToSession(Session session, SpectatorMessage message) {
        try {
            session.getBasicRemote().sendText(gson.toJson(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static class SpectatorInfo {
        final int gameId;
        
        SpectatorInfo(int gameId) {
            this.gameId = gameId;
        }
    }
    
    private static class SpectatorMessage {
        String type;
        int gameId;
        String data;
        
        SpectatorMessage(String type, int gameId, String data) {
            this.type = type;
            this.gameId = gameId;
            this.data = data;
        }
    }
}

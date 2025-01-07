package com.chessgame.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.chessgame.dao.GameDAO;
import com.chessgame.model.Game;

@ServerEndpoint(value = "/game-socket")
public class GameWebSocket {
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> gameRooms = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static final GameDAO gameDAO = new GameDAO();
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket opened: " + session.getId());
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            WebSocketMessage wsMessage = gson.fromJson(message, WebSocketMessage.class);
            
            switch (wsMessage.getType()) {
                case "join":
                    handleJoinGame(wsMessage, session);
                    break;
                case "move":
                    handleMove(wsMessage, session);
                    break;
                case "chat":
                    handleChat(wsMessage, session);
                    break;
                case "resign":
                    handleResign(wsMessage, session);
                    break;
                case "loadGame":
                    handleLoadGame(wsMessage, session);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        String gameId = gameRooms.remove(session.getId());
        if (gameId != null) {
            // Notify other player about disconnection
            broadcastToGame(gameId, createMessage("playerDisconnected", null));
        }
        System.out.println("WebSocket closed: " + session.getId());
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error for session " + session.getId());
        throwable.printStackTrace();
    }
    
    private void handleJoinGame(WebSocketMessage message, Session session) throws Exception {
        String gameId = message.getGameId();
        int userId = message.getUserId();
        
        Game game = gameDAO.getGameById(Integer.parseInt(gameId));
        if (game != null) {
            gameRooms.put(session.getId(), gameId);
            
            // Determine player color
            boolean isWhite = game.getPlayer1Id() == userId;
            
            // Send game state to the joining player
            sendToSession(session, createMessage("gameState", new GameState(game.getGameState(), isWhite)));
            
            // Notify other player
            broadcastToGame(gameId, createMessage("playerJoined", 
                new PlayerInfo(userId, isWhite)), session);
        }
    }
    
    private void handleMove(WebSocketMessage message, Session session) throws Exception {
        String gameId = message.getGameId();
        Move move = gson.fromJson(gson.toJson(message.getData()), Move.class);
        
        // Update game state in database
        Game game = gameDAO.getGameById(Integer.parseInt(gameId));
        if (game != null) {
            game.setGameState(move.getFen());
            gameDAO.updateGameState(game.getGameId(), game.getGameState());
            
            // Broadcast move to all players in the game
            broadcastToGame(gameId, createMessage("move", move));
        }
    }
    
    private void handleChat(WebSocketMessage message, Session session) {
        String gameId = message.getGameId();
        ChatMessage chatMessage = gson.fromJson(gson.toJson(message.getData()), ChatMessage.class);
        
        // Broadcast chat message to all players in the game
        broadcastToGame(gameId, createMessage("chat", chatMessage));
    }
    
    private void handleResign(WebSocketMessage message, Session session) throws Exception {
        String gameId = message.getGameId();
        int userId = message.getUserId();
        
        Game game = gameDAO.getGameById(Integer.parseInt(gameId));
        if (game != null) {
            // Set winner as the opponent
            int winnerId = game.getPlayer1Id() == userId ? game.getPlayer2Id() : game.getPlayer1Id();
            gameDAO.endGame(game.getGameId(), winnerId);
            
            // Notify all players about the resignation
            broadcastToGame(gameId, createMessage("gameOver", 
                new GameResult("resignation", winnerId)));
        }
    }
    
    private void handleLoadGame(WebSocketMessage message, Session session) throws Exception {
        String gameId = message.getGameId();
        Game game = gameDAO.getGameById(Integer.parseInt(gameId));
        
        if (game != null) {
            sendToSession(session, createMessage("gameState", 
                new GameState(game.getGameState(), game.getPlayer1Id() == message.getUserId())));
        }
    }
    
    private void broadcastToGame(String gameId, String message) {
        broadcastToGame(gameId, message, null);
    }
    
    private void broadcastToGame(String gameId, String message, Session except) {
        gameRooms.forEach((sessionId, gameRoomId) -> {
            if (gameRoomId.equals(gameId)) {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen() && 
                    (except == null || !session.getId().equals(except.getId()))) {
                    sendToSession(session, message);
                }
            }
        });
    }
    
    private void sendToSession(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendError(Session session, String error) {
        sendToSession(session, createMessage("error", new ErrorMessage(error)));
    }
    
    private String createMessage(String type, Object data) {
        return gson.toJson(new WebSocketMessage(type, data));
    }
    
    // Inner classes for message handling
    private static class WebSocketMessage {
        private String type;
        private String gameId;
        private int userId;
        private Object data;
        
        public WebSocketMessage() {}
        
        public WebSocketMessage(String type, Object data) {
            this.type = type;
            this.data = data;
        }
        
        public String getType() { return type; }
        public String getGameId() { return gameId; }
        public int getUserId() { return userId; }
        public Object getData() { return data; }
    }
    
    private static class GameState {
        private String fen;
        private boolean isWhite;
        
        public GameState(String fen, boolean isWhite) {
            this.fen = fen;
            this.isWhite = isWhite;
        }
    }
    
    private static class Move {
        private String from;
        private String to;
        private String fen;
        
        public String getFen() { return fen; }
    }
    
    private static class ChatMessage {
        private String username;
        private String message;
    }
    
    private static class GameResult {
        private String type;
        private int winnerId;
        
        public GameResult(String type, int winnerId) {
            this.type = type;
            this.winnerId = winnerId;
        }
    }
    
    private static class PlayerInfo {
        private int userId;
        private boolean isWhite;
        
        public PlayerInfo(int userId, boolean isWhite) {
            this.userId = userId;
            this.isWhite = isWhite;
        }
    }
    
    private static class ErrorMessage {
        private String error;
        
        public ErrorMessage(String error) {
            this.error = error;
        }
    }
}

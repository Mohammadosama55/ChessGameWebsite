package com.chessgame.model;

import java.sql.Timestamp;

public class Game {
    private int gameId;
    private int player1Id;  // White player
    private Integer player2Id;  // Black player
    private Timestamp startTime;
    private Timestamp endTime;
    private Integer winnerId;
    private String gameState;  // FEN notation
    private String gameType;   // SINGLE_PLAYER or MULTIPLAYER
    private String status;     // IN_PROGRESS, COMPLETED, or ABANDONED
    
    // Default constructor
    public Game() {}
    
    // Constructor for new game
    public Game(int player1Id, Integer player2Id, String gameType) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.gameType = gameType;
        this.status = "IN_PROGRESS";
        // Standard chess starting position in FEN notation
        this.gameState = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    }
    
    // Getters and Setters
    public int getGameId() {
        return gameId;
    }
    
    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
    
    public int getPlayer1Id() {
        return player1Id;
    }
    
    public void setPlayer1Id(int player1Id) {
        this.player1Id = player1Id;
    }
    
    public Integer getPlayer2Id() {
        return player2Id;
    }
    
    public void setPlayer2Id(Integer player2Id) {
        this.player2Id = player2Id;
    }
    
    public Timestamp getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    
    public Timestamp getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    
    public Integer getWinnerId() {
        return winnerId;
    }
    
    public void setWinnerId(Integer winnerId) {
        this.winnerId = winnerId;
    }
    
    public String getGameState() {
        return gameState;
    }
    
    public void setGameState(String gameState) {
        this.gameState = gameState;
    }
    
    public String getGameType() {
        return gameType;
    }
    
    public void setGameType(String gameType) {
        this.gameType = gameType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getWhitePlayerId() {
        return player1Id;
    }
    
    public int getBlackPlayerId() {
        return player2Id != null ? player2Id : -1;
    }
    
    // Utility methods
    public boolean isMultiplayer() {
        return "MULTIPLAYER".equals(gameType);
    }
    
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
    
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }
    
    public boolean isAbandoned() {
        return "ABANDONED".equals(status);
    }
}

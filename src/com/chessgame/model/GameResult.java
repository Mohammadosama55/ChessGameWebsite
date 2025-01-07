package com.chessgame.model;

public class GameResult {
    private int whitePlayerId;
    private int blackPlayerId;
    private double whiteScore;  // 1.0 for win, 0.5 for draw, 0.0 for loss

    public GameResult(int whitePlayerId, int blackPlayerId, double whiteScore) {
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.whiteScore = whiteScore;
    }

    public int getWhitePlayerId() {
        return whitePlayerId;
    }

    public int getBlackPlayerId() {
        return blackPlayerId;
    }

    public double getWhiteScore() {
        return whiteScore;
    }

    public double getBlackScore() {
        return 1.0 - whiteScore;
    }
}

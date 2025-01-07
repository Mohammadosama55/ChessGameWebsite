package com.chessgame.model;

import java.sql.Timestamp;

public class TournamentMatch {
    private int matchId;
    private int roundId;
    private int whitePlayerId;
    private int blackPlayerId;
    private String result;  // "1-0" (white wins), "0-1" (black wins), "1/2-1/2" (draw), "*" (ongoing/not started)
    private Timestamp startTime;
    private Timestamp endTime;
    private String pgn;  // Portable Game Notation of the match

    public TournamentMatch() {}

    public TournamentMatch(int matchId, int roundId, int whitePlayerId, int blackPlayerId, 
            String result, Timestamp startTime, Timestamp endTime, String pgn) {
        this.matchId = matchId;
        this.roundId = roundId;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.result = result;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pgn = pgn;
    }

    // Getters and setters
    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public int getRoundId() { return roundId; }
    public void setRoundId(int roundId) { this.roundId = roundId; }

    public int getWhitePlayerId() { return whitePlayerId; }
    public void setWhitePlayerId(int whitePlayerId) { this.whitePlayerId = whitePlayerId; }

    public int getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(int blackPlayerId) { this.blackPlayerId = blackPlayerId; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getPgn() { return pgn; }
    public void setPgn(String pgn) { this.pgn = pgn; }
}

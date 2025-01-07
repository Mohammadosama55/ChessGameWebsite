package com.chessgame.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class TournamentRound {
    private int roundId;
    private int tournamentId;
    private int roundNumber;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status;  // NOT_STARTED, IN_PROGRESS, COMPLETED
    private List<TournamentMatch> matches;

    public TournamentRound() {
        this.matches = new ArrayList<>();
    }

    public TournamentRound(int roundId, int tournamentId, int roundNumber, 
            Timestamp startTime, Timestamp endTime, String status) {
        this.roundId = roundId;
        this.tournamentId = tournamentId;
        this.roundNumber = roundNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.matches = new ArrayList<>();
    }

    // Getters and setters
    public int getRoundId() { return roundId; }
    public void setRoundId(int roundId) { this.roundId = roundId; }

    public int getTournamentId() { return tournamentId; }
    public void setTournamentId(int tournamentId) { this.tournamentId = tournamentId; }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<TournamentMatch> getMatches() { return matches; }
    public void setMatches(List<TournamentMatch> matches) { this.matches = matches; }
    public void addMatch(TournamentMatch match) { this.matches.add(match); }
}

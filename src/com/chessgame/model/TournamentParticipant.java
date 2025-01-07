package com.chessgame.model;

public class TournamentParticipant {
    private int participantId;
    private int tournamentId;
    private int userId;
    private String username;
    private int rating;
    private double score;
    private int rank;
    private String status;  // REGISTERED, ACTIVE, WITHDRAWN, ELIMINATED

    public TournamentParticipant() {}

    public TournamentParticipant(int participantId, int tournamentId, int userId, 
            String username, int rating, double score, int rank, String status) {
        this.participantId = participantId;
        this.tournamentId = tournamentId;
        this.userId = userId;
        this.username = username;
        this.rating = rating;
        this.score = score;
        this.rank = rank;
        this.status = status;
    }

    // Getters and setters
    public int getParticipantId() { return participantId; }
    public void setParticipantId(int participantId) { this.participantId = participantId; }

    public int getTournamentId() { return tournamentId; }
    public void setTournamentId(int tournamentId) { this.tournamentId = tournamentId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

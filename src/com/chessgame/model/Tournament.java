package com.chessgame.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class Tournament {
    private int tournamentId;
    private String name;
    private String description;
    private Timestamp startDate;
    private Timestamp endDate;
    private String status;  // UPCOMING, IN_PROGRESS, COMPLETED
    private int maxParticipants;
    private String tournamentType;  // SWISS, ROUND_ROBIN, ELIMINATION
    private int roundsCount;
    private int timeControlMinutes;
    private int timeIncrementSeconds;
    private List<TournamentRound> rounds;
    private List<TournamentParticipant> participants;
    
    public Tournament() {
        this.rounds = new ArrayList<>();
        this.participants = new ArrayList<>();
    }
    
    // Constructor for new tournament
    public Tournament(String name, String description, Timestamp startDate, 
                     Timestamp endDate, int maxParticipants, String tournamentType,
                     int roundsCount, int timeControlMinutes, int timeIncrementSeconds) {
        this();
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxParticipants = maxParticipants;
        this.tournamentType = tournamentType;
        this.roundsCount = roundsCount;
        this.timeControlMinutes = timeControlMinutes;
        this.timeIncrementSeconds = timeIncrementSeconds;
        this.status = "UPCOMING";
    }
    
    // Getters and Setters
    public int getTournamentId() {
        return tournamentId;
    }
    
    public void setTournamentId(int tournamentId) {
        this.tournamentId = tournamentId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Timestamp getStartDate() {
        return startDate;
    }
    
    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }
    
    public Timestamp getEndDate() {
        return endDate;
    }
    
    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public String getTournamentType() {
        return tournamentType;
    }
    
    public void setTournamentType(String tournamentType) {
        this.tournamentType = tournamentType;
    }
    
    public int getRoundsCount() {
        return roundsCount;
    }
    
    public void setRoundsCount(int roundsCount) {
        this.roundsCount = roundsCount;
    }
    
    public int getTimeControlMinutes() {
        return timeControlMinutes;
    }
    
    public void setTimeControlMinutes(int timeControlMinutes) {
        this.timeControlMinutes = timeControlMinutes;
    }
    
    public int getTimeIncrementSeconds() {
        return timeIncrementSeconds;
    }
    
    public void setTimeIncrementSeconds(int timeIncrementSeconds) {
        this.timeIncrementSeconds = timeIncrementSeconds;
    }
    
    public List<TournamentRound> getRounds() {
        return rounds;
    }
    
    public void setRounds(List<TournamentRound> rounds) {
        this.rounds = rounds;
    }
    
    public List<TournamentParticipant> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<TournamentParticipant> participants) {
        this.participants = participants;
    }
    
    // Utility methods
    public boolean isUpcoming() {
        return "UPCOMING".equals(status);
    }
    
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }
    
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
    
    public boolean canRegister() {
        return isUpcoming() && participants.size() < maxParticipants;
    }
    
    public boolean isRegistered(int userId) {
        return participants.stream()
            .anyMatch(p -> p.getUserId() == userId);
    }
    
    public TournamentParticipant getParticipant(int userId) {
        return participants.stream()
            .filter(p -> p.getUserId() == userId)
            .findFirst()
            .orElse(null);
    }
    
    // Inner classes
    public static class TournamentRound {
        private int roundNumber;
        private List<TournamentPairing> pairings;
        private Timestamp startTime;
        private Timestamp endTime;
        
        public TournamentRound() {
            this.pairings = new ArrayList<>();
        }
        
        // Getters and Setters
        public int getRoundNumber() {
            return roundNumber;
        }
        
        public void setRoundNumber(int roundNumber) {
            this.roundNumber = roundNumber;
        }
        
        public List<TournamentPairing> getPairings() {
            return pairings;
        }
        
        public void setPairings(List<TournamentPairing> pairings) {
            this.pairings = pairings;
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
    }
    
    public static class TournamentPairing {
        private int player1Id;
        private int player2Id;
        private Integer gameId;
        private Integer winnerId;
        private boolean isDraw;
        
        // Getters and Setters
        public int getPlayer1Id() {
            return player1Id;
        }
        
        public void setPlayer1Id(int player1Id) {
            this.player1Id = player1Id;
        }
        
        public int getPlayer2Id() {
            return player2Id;
        }
        
        public void setPlayer2Id(int player2Id) {
            this.player2Id = player2Id;
        }
        
        public Integer getGameId() {
            return gameId;
        }
        
        public void setGameId(Integer gameId) {
            this.gameId = gameId;
        }
        
        public Integer getWinnerId() {
            return winnerId;
        }
        
        public void setWinnerId(Integer winnerId) {
            this.winnerId = winnerId;
        }
        
        public boolean isDraw() {
            return isDraw;
        }
        
        public void setDraw(boolean isDraw) {
            this.isDraw = isDraw;
        }
    }
    
    public static class TournamentParticipant {
        private int userId;
        private double score;
        private int rank;
        private List<TournamentResult> results;
        
        public TournamentParticipant() {
            this.results = new ArrayList<>();
        }
        
        // Getters and Setters
        public int getUserId() {
            return userId;
        }
        
        public void setUserId(int userId) {
            this.userId = userId;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
        
        public int getRank() {
            return rank;
        }
        
        public void setRank(int rank) {
            this.rank = rank;
        }
        
        public List<TournamentResult> getResults() {
            return results;
        }
        
        public void setResults(List<TournamentResult> results) {
            this.results = results;
        }
    }
    
    public static class TournamentResult {
        private int roundNumber;
        private int opponentId;
        private boolean isWhite;
        private double score;  // 1 for win, 0.5 for draw, 0 for loss
        
        // Getters and Setters
        public int getRoundNumber() {
            return roundNumber;
        }
        
        public void setRoundNumber(int roundNumber) {
            this.roundNumber = roundNumber;
        }
        
        public int getOpponentId() {
            return opponentId;
        }
        
        public void setOpponentId(int opponentId) {
            this.opponentId = opponentId;
        }
        
        public boolean isWhite() {
            return isWhite;
        }
        
        public void setWhite(boolean isWhite) {
            this.isWhite = isWhite;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
    }
}

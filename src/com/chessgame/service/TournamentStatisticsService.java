package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TournamentStatisticsService {
    
    public void updateGameStatistics(int gameId, int tournamentId, int whitePlayerId, int blackPlayerId,
                                   String openingEco, String openingName, int numMoves, int gameLengthSeconds,
                                   String result, String terminationType) throws SQLException {
        String sql = "INSERT INTO tournament_game_statistics " +
                    "(game_id, tournament_id, white_player_id, black_player_id, opening_eco, " +
                    "opening_name, num_moves, game_length_seconds, result, termination_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, tournamentId);
            pstmt.setInt(3, whitePlayerId);
            pstmt.setInt(4, blackPlayerId);
            pstmt.setString(5, openingEco);
            pstmt.setString(6, openingName);
            pstmt.setInt(7, numMoves);
            pstmt.setInt(8, gameLengthSeconds);
            pstmt.setString(9, result);
            pstmt.setString(10, terminationType);
            
            pstmt.executeUpdate();
            
            // Update player statistics
            updatePlayerStatistics(tournamentId, whitePlayerId, blackPlayerId, result);
        }
    }
    
    private void updatePlayerStatistics(int tournamentId, int whitePlayerId, int blackPlayerId, 
                                      String result) throws SQLException {
        // Update white player stats
        updatePlayerStats(tournamentId, whitePlayerId, result.equals("1-0"), result.equals("1/2-1/2"), 
                         result.equals("0-1"));
        
        // Update black player stats
        updatePlayerStats(tournamentId, blackPlayerId, result.equals("0-1"), result.equals("1/2-1/2"), 
                         result.equals("1-0"));
    }
    
    private void updatePlayerStats(int tournamentId, int playerId, boolean won, boolean drawn, 
                                 boolean lost) throws SQLException {
        String sql = "INSERT INTO tournament_statistics " +
                    "(tournament_id, user_id, games_played, games_won, games_drawn, games_lost) " +
                    "VALUES (?, ?, 1, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "games_played = games_played + 1, " +
                    "games_won = games_won + ?, " +
                    "games_drawn = games_drawn + ?, " +
                    "games_lost = games_lost + ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, playerId);
            pstmt.setInt(3, won ? 1 : 0);
            pstmt.setInt(4, drawn ? 1 : 0);
            pstmt.setInt(5, lost ? 1 : 0);
            pstmt.setInt(6, won ? 1 : 0);
            pstmt.setInt(7, drawn ? 1 : 0);
            pstmt.setInt(8, lost ? 1 : 0);
            
            pstmt.executeUpdate();
        }
    }
    
    public void updatePerformanceRating(int tournamentId, int userId, int performanceRating) 
            throws SQLException {
        String sql = "UPDATE tournament_statistics SET performance_rating = ? " +
                    "WHERE tournament_id = ? AND user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, performanceRating);
            pstmt.setInt(2, tournamentId);
            pstmt.setInt(3, userId);
            
            pstmt.executeUpdate();
        }
    }
    
    public void updateRatingChange(int tournamentId, int userId, int ratingChange) throws SQLException {
        String sql = "UPDATE tournament_statistics SET rating_change = ? " +
                    "WHERE tournament_id = ? AND user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, ratingChange);
            pstmt.setInt(2, tournamentId);
            pstmt.setInt(3, userId);
            
            pstmt.executeUpdate();
        }
    }
    
    public TournamentStats getTournamentStats(int tournamentId) throws SQLException {
        TournamentStats stats = new TournamentStats();
        
        // Get general tournament statistics
        String sql = "SELECT COUNT(*) as total_games, " +
                    "AVG(num_moves) as avg_moves, " +
                    "AVG(game_length_seconds) as avg_game_length, " +
                    "COUNT(CASE WHEN result = '1-0' THEN 1 END) as white_wins, " +
                    "COUNT(CASE WHEN result = '0-1' THEN 1 END) as black_wins, " +
                    "COUNT(CASE WHEN result = '1/2-1/2' THEN 1 END) as draws " +
                    "FROM tournament_game_statistics WHERE tournament_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalGames(rs.getInt("total_games"));
                    stats.setAverageMoves(rs.getDouble("avg_moves"));
                    stats.setAverageGameLength(rs.getDouble("avg_game_length"));
                    stats.setWhiteWins(rs.getInt("white_wins"));
                    stats.setBlackWins(rs.getInt("black_wins"));
                    stats.setDraws(rs.getInt("draws"));
                }
            }
        }
        
        // Get opening statistics
        sql = "SELECT opening_name, COUNT(*) as count " +
              "FROM tournament_game_statistics " +
              "WHERE tournament_id = ? " +
              "GROUP BY opening_name " +
              "ORDER BY count DESC LIMIT 5";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.getTopOpenings().put(
                        rs.getString("opening_name"),
                        rs.getInt("count")
                    );
                }
            }
        }
        
        return stats;
    }
    
    public PlayerStats getPlayerStats(int tournamentId, int userId) throws SQLException {
        String sql = "SELECT * FROM tournament_statistics WHERE tournament_id = ? AND user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerStats(
                        rs.getInt("games_played"),
                        rs.getInt("games_won"),
                        rs.getInt("games_drawn"),
                        rs.getInt("games_lost"),
                        rs.getInt("rating_change"),
                        rs.getInt("performance_rating")
                    );
                }
            }
        }
        return null;
    }
    
    // Statistics model classes
    public static class TournamentStats {
        private int totalGames;
        private double averageMoves;
        private double averageGameLength;
        private int whiteWins;
        private int blackWins;
        private int draws;
        private Map<String, Integer> topOpenings;
        
        public TournamentStats() {
            this.topOpenings = new HashMap<>();
        }
        
        // Getters and setters
        public int getTotalGames() {
            return totalGames;
        }
        
        public void setTotalGames(int totalGames) {
            this.totalGames = totalGames;
        }
        
        public double getAverageMoves() {
            return averageMoves;
        }
        
        public void setAverageMoves(double averageMoves) {
            this.averageMoves = averageMoves;
        }
        
        public double getAverageGameLength() {
            return averageGameLength;
        }
        
        public void setAverageGameLength(double averageGameLength) {
            this.averageGameLength = averageGameLength;
        }
        
        public int getWhiteWins() {
            return whiteWins;
        }
        
        public void setWhiteWins(int whiteWins) {
            this.whiteWins = whiteWins;
        }
        
        public int getBlackWins() {
            return blackWins;
        }
        
        public void setBlackWins(int blackWins) {
            this.blackWins = blackWins;
        }
        
        public int getDraws() {
            return draws;
        }
        
        public void setDraws(int draws) {
            this.draws = draws;
        }
        
        public Map<String, Integer> getTopOpenings() {
            return topOpenings;
        }
    }
    
    public static class PlayerStats {
        private final int gamesPlayed;
        private final int gamesWon;
        private final int gamesDrawn;
        private final int gamesLost;
        private final int ratingChange;
        private final int performanceRating;
        
        public PlayerStats(int gamesPlayed, int gamesWon, int gamesDrawn, int gamesLost,
                         int ratingChange, int performanceRating) {
            this.gamesPlayed = gamesPlayed;
            this.gamesWon = gamesWon;
            this.gamesDrawn = gamesDrawn;
            this.gamesLost = gamesLost;
            this.ratingChange = ratingChange;
            this.performanceRating = performanceRating;
        }
        
        // Getters
        public int getGamesPlayed() {
            return gamesPlayed;
        }
        
        public int getGamesWon() {
            return gamesWon;
        }
        
        public int getGamesDrawn() {
            return gamesDrawn;
        }
        
        public int getGamesLost() {
            return gamesLost;
        }
        
        public int getRatingChange() {
            return ratingChange;
        }
        
        public int getPerformanceRating() {
            return performanceRating;
        }
        
        public double getWinPercentage() {
            return gamesPlayed == 0 ? 0 : (gamesWon * 100.0) / gamesPlayed;
        }
        
        public double getScore() {
            return gamesWon + (gamesDrawn * 0.5);
        }
    }
}

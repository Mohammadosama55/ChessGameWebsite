package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;

public class TournamentAchievementService {
    
    public static class Achievement {
        private final int id;
        private final String name;
        private final String description;
        private final String icon;
        private final int points;
        private final LocalDateTime earnedDate;
        
        public Achievement(int id, String name, String description, String icon, int points, 
                         LocalDateTime earnedDate) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.points = points;
            this.earnedDate = earnedDate;
        }
        
        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public int getPoints() { return points; }
        public LocalDateTime getEarnedDate() { return earnedDate; }
    }
    
    // Achievement types
    public static final class AchievementType {
        public static final String TOURNAMENT_WINNER = "TOURNAMENT_WINNER";
        public static final String TOURNAMENT_PODIUM = "TOURNAMENT_PODIUM";
        public static final String PERFECT_SCORE = "PERFECT_SCORE";
        public static final String UNDEFEATED = "UNDEFEATED";
        public static final String TOURNAMENTS_WON = "TOURNAMENTS_WON";
        public static final String TOURNAMENTS_PLAYED = "TOURNAMENTS_PLAYED";
        public static final String RATING_MILESTONE = "RATING_MILESTONE";
        public static final String WINNING_STREAK = "WINNING_STREAK";
    }
    
    public void checkTournamentAchievements(int userId, int tournamentId) throws SQLException {
        // Get tournament performance
        TournamentStatisticsService.PlayerStats stats = getTournamentStatisticsService()
            .getPlayerStats(tournamentId, userId);
        
        if (stats == null) return;
        
        // Check various achievements
        checkTournamentWinner(userId, tournamentId);
        checkPerfectScore(userId, tournamentId, stats);
        checkUndefeated(userId, tournamentId, stats);
        checkTournamentsPlayed(userId);
        checkTournamentsWon(userId);
        checkWinningStreak(userId);
        checkRatingMilestones(userId);
    }
    
    private void checkTournamentWinner(int userId, int tournamentId) throws SQLException {
        String sql = "SELECT winner_id FROM tournaments WHERE tournament_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt("winner_id") == userId) {
                    grantAchievement(userId, AchievementType.TOURNAMENT_WINNER, 
                        "Tournament Victory", "Win a tournament", "trophy", 100);
                }
            }
        }
    }
    
    private void checkPerfectScore(int userId, int tournamentId, 
                                 TournamentStatisticsService.PlayerStats stats) throws SQLException {
        if (stats.getGamesPlayed() > 0 && stats.getGamesWon() == stats.getGamesPlayed()) {
            grantAchievement(userId, AchievementType.PERFECT_SCORE,
                "Perfect Performance", "Win all games in a tournament", "star", 150);
        }
    }
    
    private void checkUndefeated(int userId, int tournamentId,
                               TournamentStatisticsService.PlayerStats stats) throws SQLException {
        if (stats.getGamesPlayed() > 0 && stats.getGamesLost() == 0) {
            grantAchievement(userId, AchievementType.UNDEFEATED,
                "Undefeated", "Complete a tournament without losing", "shield", 100);
        }
    }
    
    private void checkTournamentsPlayed(int userId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT tournament_id) as count FROM tournament_participants " +
                    "WHERE user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    checkTournamentMilestone(userId, count);
                }
            }
        }
    }
    
    private void checkTournamentsWon(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM tournaments WHERE winner_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    checkWinningMilestone(userId, count);
                }
            }
        }
    }
    
    private void checkWinningStreak(int userId) throws SQLException {
        String sql = "SELECT t.tournament_id, t.end_date, t.winner_id " +
                    "FROM tournaments t " +
                    "JOIN tournament_participants tp ON t.tournament_id = tp.tournament_id " +
                    "WHERE tp.user_id = ? AND t.status = 'COMPLETED' " +
                    "ORDER BY t.end_date DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                int streak = 0;
                while (rs.next() && rs.getInt("winner_id") == userId) {
                    streak++;
                }
                
                if (streak >= 3) {
                    grantAchievement(userId, AchievementType.WINNING_STREAK,
                        "Winning Streak", "Win " + streak + " tournaments in a row", "fire", 200);
                }
            }
        }
    }
    
    private void checkRatingMilestones(int userId) throws SQLException {
        String sql = "SELECT rating FROM users WHERE user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int rating = rs.getInt("rating");
                    checkRatingMilestone(userId, rating);
                }
            }
        }
    }
    
    private void checkTournamentMilestone(int userId, int count) throws SQLException {
        if (count >= 100) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_PLAYED,
                "Tournament Veteran", "Participate in 100 tournaments", "medal", 200);
        } else if (count >= 50) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_PLAYED,
                "Tournament Expert", "Participate in 50 tournaments", "medal", 150);
        } else if (count >= 25) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_PLAYED,
                "Tournament Regular", "Participate in 25 tournaments", "medal", 100);
        } else if (count >= 10) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_PLAYED,
                "Tournament Enthusiast", "Participate in 10 tournaments", "medal", 50);
        }
    }
    
    private void checkWinningMilestone(int userId, int count) throws SQLException {
        if (count >= 50) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_WON,
                "Tournament Legend", "Win 50 tournaments", "crown", 500);
        } else if (count >= 25) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_WON,
                "Tournament Master", "Win 25 tournaments", "crown", 300);
        } else if (count >= 10) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_WON,
                "Tournament Champion", "Win 10 tournaments", "crown", 200);
        } else if (count >= 5) {
            grantAchievement(userId, AchievementType.TOURNAMENTS_WON,
                "Tournament Winner", "Win 5 tournaments", "crown", 100);
        }
    }
    
    private void checkRatingMilestone(int userId, int rating) throws SQLException {
        if (rating >= 2500) {
            grantAchievement(userId, AchievementType.RATING_MILESTONE,
                "Grandmaster", "Achieve a rating of 2500+", "star", 500);
        } else if (rating >= 2200) {
            grantAchievement(userId, AchievementType.RATING_MILESTONE,
                "Master", "Achieve a rating of 2200+", "star", 300);
        } else if (rating >= 2000) {
            grantAchievement(userId, AchievementType.RATING_MILESTONE,
                "Expert", "Achieve a rating of 2000+", "star", 200);
        } else if (rating >= 1800) {
            grantAchievement(userId, AchievementType.RATING_MILESTONE,
                "Advanced", "Achieve a rating of 1800+", "star", 100);
        }
    }
    
    private void grantAchievement(int userId, String type, String name, String description,
                                String icon, int points) throws SQLException {
        String sql = "INSERT INTO achievements (user_id, type, name, description, icon, points) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE earned_date = CURRENT_TIMESTAMP";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, type);
            pstmt.setString(3, name);
            pstmt.setString(4, description);
            pstmt.setString(5, icon);
            pstmt.setInt(6, points);
            
            pstmt.executeUpdate();
        }
    }
    
    public List<Achievement> getUserAchievements(int userId) throws SQLException {
        String sql = "SELECT * FROM achievements WHERE user_id = ? ORDER BY earned_date DESC";
        List<Achievement> achievements = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    achievements.add(new Achievement(
                        rs.getInt("achievement_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("icon"),
                        rs.getInt("points"),
                        rs.getTimestamp("earned_date").toLocalDateTime()
                    ));
                }
            }
        }
        
        return achievements;
    }
    
    public int getTotalAchievementPoints(int userId) throws SQLException {
        String sql = "SELECT SUM(points) as total FROM achievements WHERE user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        
        return 0;
    }
    
    private TournamentStatisticsService getTournamentStatisticsService() {
        return new TournamentStatisticsService();
    }
}

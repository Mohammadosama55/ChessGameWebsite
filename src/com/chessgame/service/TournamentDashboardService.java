package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.time.*;
import java.util.*;

public class TournamentDashboardService {
    private static final Duration CACHE_DURATION = Duration.ofMinutes(5);
    private final Map<Integer, CachedData<Map<String, Object>>> dashboardCache = new HashMap<>();
    
    public Map<String, Object> getDashboard(int tournamentId) throws SQLException {
        // Check cache
        CachedData<Map<String, Object>> cached = dashboardCache.get(tournamentId);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }
        
        // Generate dashboard data
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("overview", getTournamentOverview(tournamentId));
        dashboard.put("currentRound", getCurrentRoundStats(tournamentId));
        dashboard.put("playerStats", getPlayerStats(tournamentId));
        dashboard.put("gameStats", getGameStats(tournamentId));
        dashboard.put("achievements", getAchievements(tournamentId));
        
        // Cache result
        dashboardCache.put(tournamentId, new CachedData<>(dashboard));
        
        return dashboard;
    }
    
    private Map<String, Object> getTournamentOverview(int tournamentId) throws SQLException {
        String sql = """
            SELECT t.*, 
                   COUNT(DISTINCT tp.user_id) as player_count,
                   COUNT(DISTINCT tg.game_id) as total_games,
                   COUNT(DISTINCT CASE WHEN tg.status = 'COMPLETED' THEN tg.game_id END) as completed_games
            FROM tournaments t
            LEFT JOIN tournament_players tp ON t.tournament_id = tp.tournament_id
            LEFT JOIN tournament_games tg ON t.tournament_id = tg.tournament_id
            WHERE t.tournament_id = ?
            GROUP BY t.tournament_id
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                        "name", rs.getString("name"),
                        "format", rs.getString("format"),
                        "status", rs.getString("status"),
                        "currentRound", rs.getInt("current_round"),
                        "totalRounds", rs.getInt("rounds_count"),
                        "playerCount", rs.getInt("player_count"),
                        "totalGames", rs.getInt("total_games"),
                        "completedGames", rs.getInt("completed_games"),
                        "startDate", rs.getTimestamp("start_date").toLocalDateTime(),
                        "endDate", rs.getTimestamp("end_date").toLocalDateTime()
                    );
                }
            }
        }
        throw new SQLException("Tournament not found");
    }
    
    private Map<String, Object> getCurrentRoundStats(int tournamentId) throws SQLException {
        String sql = """
            SELECT tgs.*
            FROM tournament_game_statistics tgs
            JOIN tournaments t ON t.tournament_id = tgs.tournament_id
            WHERE tgs.tournament_id = ? AND tgs.round_number = t.current_round
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                        "totalGames", rs.getInt("total_games"),
                        "completedGames", rs.getInt("completed_games"),
                        "whiteWins", rs.getInt("white_wins"),
                        "blackWins", rs.getInt("black_wins"),
                        "draws", rs.getInt("draws"),
                        "avgGameLength", rs.getInt("avg_game_length")
                    );
                }
            }
        }
        return Map.of();
    }
    
    private List<Map<String, Object>> getPlayerStats(int tournamentId) throws SQLException {
        String sql = """
            SELECT ts.*, u.username,
                   RANK() OVER (ORDER BY ts.points DESC) as rank
            FROM tournament_statistics ts
            JOIN users u ON ts.user_id = u.user_id
            WHERE ts.tournament_id = ?
            ORDER BY ts.points DESC
            """;
            
        List<Map<String, Object>> stats = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.add(Map.of(
                        "rank", rs.getInt("rank"),
                        "username", rs.getString("username"),
                        "points", rs.getDouble("points"),
                        "gamesPlayed", rs.getInt("games_played"),
                        "gamesWon", rs.getInt("games_won"),
                        "gamesDrawn", rs.getInt("games_drawn"),
                        "gamesLost", rs.getInt("games_lost"),
                        "performanceRating", rs.getInt("performance_rating")
                    ));
                }
            }
        }
        return stats;
    }
    
    private Map<String, Object> getGameStats(int tournamentId) throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as total_games,
                AVG(num_moves) as avg_moves,
                MIN(num_moves) as min_moves,
                MAX(num_moves) as max_moves,
                SUM(CASE WHEN result = 'WHITE_WIN' THEN 1 ELSE 0 END) as white_wins,
                SUM(CASE WHEN result = 'BLACK_WIN' THEN 1 ELSE 0 END) as black_wins,
                SUM(CASE WHEN result = 'DRAW' THEN 1 ELSE 0 END) as draws
            FROM tournament_games tg
            JOIN games g ON tg.game_id = g.game_id
            WHERE tg.tournament_id = ? AND tg.status = 'COMPLETED'
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                        "totalGames", rs.getInt("total_games"),
                        "avgMoves", rs.getDouble("avg_moves"),
                        "minMoves", rs.getInt("min_moves"),
                        "maxMoves", rs.getInt("max_moves"),
                        "whiteWinPercentage", calculatePercentage(rs.getInt("white_wins"), 
                            rs.getInt("total_games")),
                        "blackWinPercentage", calculatePercentage(rs.getInt("black_wins"), 
                            rs.getInt("total_games")),
                        "drawPercentage", calculatePercentage(rs.getInt("draws"), 
                            rs.getInt("total_games"))
                    );
                }
            }
        }
        return Map.of();
    }
    
    private List<Map<String, Object>> getAchievements(int tournamentId) throws SQLException {
        String sql = """
            SELECT a.*, u.username,
                   COUNT(*) OVER (PARTITION BY a.achievement_id) as total_earned
            FROM achievements a
            JOIN user_achievements ua ON a.achievement_id = ua.achievement_id
            JOIN users u ON ua.user_id = u.user_id
            WHERE ua.tournament_id = ?
            ORDER BY ua.earned_at DESC
            """;
            
        List<Map<String, Object>> achievements = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    achievements.add(Map.of(
                        "achievementId", rs.getInt("achievement_id"),
                        "name", rs.getString("name"),
                        "description", rs.getString("description"),
                        "username", rs.getString("username"),
                        "totalEarned", rs.getInt("total_earned")
                    ));
                }
            }
        }
        return achievements;
    }
    
    private double calculatePercentage(int value, int total) {
        return total > 0 ? (value * 100.0) / total : 0;
    }
    
    private static class CachedData<T> {
        final T data;
        final LocalDateTime timestamp;
        
        CachedData(T data) {
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return LocalDateTime.now().isAfter(timestamp.plus(CACHE_DURATION));
        }
    }
}

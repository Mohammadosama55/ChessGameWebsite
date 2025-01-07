package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TournamentLeaderboardService {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<String, Map<Integer, List<LeaderboardEntry>>> leaderboardCache = new ConcurrentHashMap<>();
    private static final int CACHE_DURATION_MINUTES = 5;
    
    public TournamentLeaderboardService() {
        // Schedule periodic cache cleanup
        scheduler.scheduleAtFixedRate(this::cleanupCache, 1, 1, TimeUnit.HOURS);
    }
    
    public List<LeaderboardEntry> getLeaderboard(String category, int limit) throws SQLException {
        // Check cache first
        Map<Integer, List<LeaderboardEntry>> categoryCache = leaderboardCache.get(category);
        if (categoryCache != null && categoryCache.containsKey(limit)) {
            return categoryCache.get(limit);
        }
        
        // If not in cache, fetch from database
        List<LeaderboardEntry> leaderboard = fetchLeaderboard(category, limit);
        
        // Update cache
        categoryCache = leaderboardCache.computeIfAbsent(category, k -> new ConcurrentHashMap<>());
        categoryCache.put(limit, leaderboard);
        
        return leaderboard;
    }
    
    private List<LeaderboardEntry> fetchLeaderboard(String category, int limit) throws SQLException {
        String sql = switch (category) {
            case "TOURNAMENT_WINS" -> """
                SELECT u.user_id, u.username, COUNT(*) as value,
                       MAX(t.end_date) as last_achievement
                FROM users u
                JOIN tournament_statistics ts ON u.user_id = ts.user_id
                JOIN tournaments t ON ts.tournament_id = t.tournament_id
                WHERE ts.placement = 1
                GROUP BY u.user_id, u.username
                ORDER BY value DESC, last_achievement DESC
                LIMIT ?
                """;
                
            case "RATING" -> """
                SELECT u.user_id, u.username, u.rating as value,
                       u.last_rating_change as last_achievement
                FROM users u
                WHERE u.rating > 0
                ORDER BY value DESC
                LIMIT ?
                """;
                
            case "WINRATE" -> """
                SELECT u.user_id, u.username, 
                       (CAST(SUM(ts.games_won) AS FLOAT) / 
                        NULLIF(SUM(ts.games_played), 0) * 100) as value,
                       MAX(t.end_date) as last_achievement
                FROM users u
                JOIN tournament_statistics ts ON u.user_id = ts.user_id
                JOIN tournaments t ON ts.tournament_id = t.tournament_id
                GROUP BY u.user_id, u.username
                HAVING SUM(ts.games_played) >= 10
                ORDER BY value DESC
                LIMIT ?
                """;
                
            case "TOURNAMENTS_PLAYED" -> """
                SELECT u.user_id, u.username, COUNT(*) as value,
                       MAX(t.end_date) as last_achievement
                FROM users u
                JOIN tournament_statistics ts ON u.user_id = ts.user_id
                JOIN tournaments t ON ts.tournament_id = t.tournament_id
                GROUP BY u.user_id, u.username
                ORDER BY value DESC
                LIMIT ?
                """;
                
            case "PERFECT_TOURNAMENTS" -> """
                SELECT u.user_id, u.username, COUNT(*) as value,
                       MAX(t.end_date) as last_achievement
                FROM users u
                JOIN tournament_statistics ts ON u.user_id = ts.user_id
                JOIN tournaments t ON ts.tournament_id = t.tournament_id
                WHERE ts.games_won = ts.games_played
                GROUP BY u.user_id, u.username
                ORDER BY value DESC
                LIMIT ?
                """;
                
            case "ACHIEVEMENT_POINTS" -> """
                SELECT u.user_id, u.username, 
                       SUM(a.points) as value,
                       MAX(a.earned_date) as last_achievement
                FROM users u
                JOIN achievements a ON u.user_id = a.user_id
                GROUP BY u.user_id, u.username
                ORDER BY value DESC
                LIMIT ?
                """;
                
            default -> throw new IllegalArgumentException("Invalid leaderboard category: " + category);
        };
        
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getDouble("value"),
                        rs.getTimestamp("last_achievement").toLocalDateTime()
                    ));
                }
            }
        }
        return entries;
    }
    
    public Map<String, List<LeaderboardEntry>> getAllLeaderboards(int limit) throws SQLException {
        Map<String, List<LeaderboardEntry>> allLeaderboards = new HashMap<>();
        String[] categories = {
            "TOURNAMENT_WINS", "RATING", "WINRATE", "TOURNAMENTS_PLAYED",
            "PERFECT_TOURNAMENTS", "ACHIEVEMENT_POINTS"
        };
        
        for (String category : categories) {
            allLeaderboards.put(category, getLeaderboard(category, limit));
        }
        
        return allLeaderboards;
    }
    
    private void cleanupCache() {
        leaderboardCache.clear();
    }
    
    public static class LeaderboardEntry {
        private final int userId;
        private final String username;
        private final double value;
        private final LocalDateTime lastAchievement;
        
        public LeaderboardEntry(int userId, String username, double value, LocalDateTime lastAchievement) {
            this.userId = userId;
            this.username = username;
            this.value = value;
            this.lastAchievement = lastAchievement;
        }
        
        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public double getValue() { return value; }
        public LocalDateTime getLastAchievement() { return lastAchievement; }
        
        public Map<String, Object> toMap() {
            return Map.of(
                "userId", userId,
                "username", username,
                "value", value,
                "lastAchievement", lastAchievement.toString()
            );
        }
    }
}

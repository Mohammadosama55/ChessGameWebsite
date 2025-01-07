package com.chessgame.service;

import com.chessgame.model.DBUtil;
import com.google.gson.Gson;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TournamentAnalyticsService {
    private static final Gson gson = new Gson();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public void updateTournamentAnalytics(int tournamentId) {
        CompletableFuture.runAsync(() -> {
            try {
                updateBasicStats(tournamentId);
                updateOpeningStats(tournamentId);
                updatePlayerAnalytics(tournamentId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void updateBasicStats(int tournamentId) throws SQLException {
        String sql = """
            INSERT INTO tournament_analytics 
            (tournament_id, total_games, total_moves, avg_game_length, 
             white_wins, black_wins, draws, shortest_game, longest_game, avg_rating)
            SELECT 
                ?, 
                COUNT(*), 
                SUM(num_moves),
                AVG(num_moves),
                SUM(CASE WHEN result = 'WHITE_WIN' THEN 1 ELSE 0 END),
                SUM(CASE WHEN result = 'BLACK_WIN' THEN 1 ELSE 0 END),
                SUM(CASE WHEN result = 'DRAW' THEN 1 ELSE 0 END),
                MIN(num_moves),
                MAX(num_moves),
                AVG((white_rating + black_rating) / 2)
            FROM tournament_games
            WHERE tournament_id = ?
            ON DUPLICATE KEY UPDATE
                total_games = VALUES(total_games),
                total_moves = VALUES(total_moves),
                avg_game_length = VALUES(avg_game_length),
                white_wins = VALUES(white_wins),
                black_wins = VALUES(black_wins),
                draws = VALUES(draws),
                shortest_game = VALUES(shortest_game),
                longest_game = VALUES(longest_game),
                avg_rating = VALUES(avg_rating)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, tournamentId);
            pstmt.executeUpdate();
        }
    }
    
    private void updateOpeningStats(int tournamentId) throws SQLException {
        String sql = """
            INSERT INTO tournament_opening_stats 
            (tournament_id, eco_code, opening_name, frequency, 
             white_wins, black_wins, draws, avg_moves)
            SELECT 
                ?, 
                eco_code,
                opening_name,
                COUNT(*),
                SUM(CASE WHEN result = 'WHITE_WIN' THEN 1 ELSE 0 END),
                SUM(CASE WHEN result = 'BLACK_WIN' THEN 1 ELSE 0 END),
                SUM(CASE WHEN result = 'DRAW' THEN 1 ELSE 0 END),
                AVG(num_moves)
            FROM tournament_games
            WHERE tournament_id = ?
            GROUP BY eco_code, opening_name
            ON DUPLICATE KEY UPDATE
                frequency = VALUES(frequency),
                white_wins = VALUES(white_wins),
                black_wins = VALUES(black_wins),
                draws = VALUES(draws),
                avg_moves = VALUES(avg_moves)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, tournamentId);
            pstmt.executeUpdate();
        }
    }
    
    private void updatePlayerAnalytics(int tournamentId) throws SQLException {
        String sql = """
            INSERT INTO tournament_player_analytics 
            (tournament_id, user_id, avg_move_time, avg_accuracy,
             blunders, mistakes, inaccuracies, avg_centipawn_loss)
            SELECT 
                ?,
                player_id,
                AVG(move_time),
                AVG(accuracy),
                SUM(blunders),
                SUM(mistakes),
                SUM(inaccuracies),
                AVG(centipawn_loss)
            FROM (
                SELECT 
                    white_player_id as player_id,
                    white_avg_move_time as move_time,
                    white_accuracy as accuracy,
                    white_blunders as blunders,
                    white_mistakes as mistakes,
                    white_inaccuracies as inaccuracies,
                    white_centipawn_loss as centipawn_loss
                FROM tournament_games
                WHERE tournament_id = ?
                UNION ALL
                SELECT 
                    black_player_id,
                    black_avg_move_time,
                    black_accuracy,
                    black_blunders,
                    black_mistakes,
                    black_inaccuracies,
                    black_centipawn_loss
                FROM tournament_games
                WHERE tournament_id = ?
            ) player_stats
            GROUP BY player_id
            ON DUPLICATE KEY UPDATE
                avg_move_time = VALUES(avg_move_time),
                avg_accuracy = VALUES(avg_accuracy),
                blunders = VALUES(blunders),
                mistakes = VALUES(mistakes),
                inaccuracies = VALUES(inaccuracies),
                avg_centipawn_loss = VALUES(avg_centipawn_loss)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, tournamentId);
            pstmt.setInt(3, tournamentId);
            pstmt.executeUpdate();
        }
    }
    
    public Map<String, Object> getTournamentAnalytics(int tournamentId) throws SQLException {
        Map<String, Object> analytics = new HashMap<>();
        
        // Get basic stats
        String basicStatsSql = "SELECT * FROM tournament_analytics WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(basicStatsSql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    analytics.put("basicStats", new HashMap<String, Object>() {{
                        put("totalGames", rs.getInt("total_games"));
                        put("totalMoves", rs.getInt("total_moves"));
                        put("avgGameLength", rs.getInt("avg_game_length"));
                        put("whiteWins", rs.getInt("white_wins"));
                        put("blackWins", rs.getInt("black_wins"));
                        put("draws", rs.getInt("draws"));
                        put("shortestGame", rs.getInt("shortest_game"));
                        put("longestGame", rs.getInt("longest_game"));
                        put("avgRating", rs.getInt("avg_rating"));
                    }});
                }
            }
        }
        
        // Get opening stats
        String openingStatsSql = "SELECT * FROM tournament_opening_stats WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(openingStatsSql)) {
            pstmt.setInt(1, tournamentId);
            List<Map<String, Object>> openingStats = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    openingStats.add(new HashMap<String, Object>() {{
                        put("ecoCode", rs.getString("eco_code"));
                        put("openingName", rs.getString("opening_name"));
                        put("frequency", rs.getInt("frequency"));
                        put("whiteWins", rs.getInt("white_wins"));
                        put("blackWins", rs.getInt("black_wins"));
                        put("draws", rs.getInt("draws"));
                        put("avgMoves", rs.getInt("avg_moves"));
                    }});
                }
            }
            analytics.put("openingStats", openingStats);
        }
        
        // Get player analytics
        String playerAnalyticsSql = """
            SELECT pa.*, u.username 
            FROM tournament_player_analytics pa
            JOIN users u ON pa.user_id = u.user_id
            WHERE pa.tournament_id = ?
            """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(playerAnalyticsSql)) {
            pstmt.setInt(1, tournamentId);
            List<Map<String, Object>> playerAnalytics = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    playerAnalytics.add(new HashMap<String, Object>() {{
                        put("username", rs.getString("username"));
                        put("avgMoveTime", rs.getInt("avg_move_time"));
                        put("avgAccuracy", rs.getDouble("avg_accuracy"));
                        put("blunders", rs.getInt("blunders"));
                        put("mistakes", rs.getInt("mistakes"));
                        put("inaccuracies", rs.getInt("inaccuracies"));
                        put("avgCentipawnLoss", rs.getInt("avg_centipawn_loss"));
                    }});
                }
            }
            analytics.put("playerAnalytics", playerAnalytics);
        }
        
        return analytics;
    }
    
    // Schedule periodic updates for active tournaments
    public void scheduleAnalyticsUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT tournament_id FROM tournaments WHERE status = 'ACTIVE'")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        updateTournamentAnalytics(rs.getInt("tournament_id"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }
}

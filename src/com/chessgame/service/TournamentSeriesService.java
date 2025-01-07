package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class TournamentSeriesService {
    private static final Map<String, PointsSystem> POINTS_SYSTEMS = Map.of(
        "STANDARD", new StandardPointsSystem(),
        "GRAND_PRIX", new GrandPrixPointsSystem(),
        "PROGRESSIVE", new ProgressivePointsSystem()
    );
    
    public int createSeries(String name, String description, LocalDate startDate, 
                          LocalDate endDate, String pointsSystem) throws SQLException {
        String sql = """
            INSERT INTO tournament_series 
            (name, description, start_date, end_date, points_system)
            VALUES (?, ?, ?, ?, ?)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDate(3, Date.valueOf(startDate));
            pstmt.setDate(4, Date.valueOf(endDate));
            pstmt.setString(5, pointsSystem);
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create tournament series");
    }
    
    public void addTournamentToSeries(int seriesId, int tournamentId, int eventOrder, 
                                    double pointsMultiplier) throws SQLException {
        String sql = """
            INSERT INTO tournament_series_events 
            (series_id, tournament_id, event_order, points_multiplier)
            VALUES (?, ?, ?, ?)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seriesId);
            pstmt.setInt(2, tournamentId);
            pstmt.setInt(3, eventOrder);
            pstmt.setDouble(4, pointsMultiplier);
            pstmt.executeUpdate();
        }
    }
    
    public void updateSeriesStandings(int seriesId) throws SQLException {
        // Get series points system
        String pointsSystemSql = "SELECT points_system FROM tournament_series WHERE series_id = ?";
        String pointsSystem;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(pointsSystemSql)) {
            pstmt.setInt(1, seriesId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Series not found");
                }
                pointsSystem = rs.getString("points_system");
            }
        }
        
        // Get all tournaments in the series
        String tournamentsSql = """
            SELECT t.tournament_id, t.status, tse.points_multiplier,
                   ts.user_id, ts.games_won, ts.games_drawn,
                   (SELECT COUNT(*) + 1 
                    FROM tournament_statistics ts2 
                    WHERE ts2.tournament_id = t.tournament_id 
                    AND ts2.games_won > ts.games_won) as placement
            FROM tournaments t
            JOIN tournament_series_events tse ON t.tournament_id = tse.tournament_id
            JOIN tournament_statistics ts ON t.tournament_id = ts.tournament_id
            WHERE tse.series_id = ?
            AND t.status = 'COMPLETED'
            ORDER BY tse.event_order
            """;
            
        Map<Integer, SeriesPlayerStats> playerStats = new HashMap<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(tournamentsSql)) {
            pstmt.setInt(1, seriesId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int userId = rs.getInt("user_id");
                    int placement = rs.getInt("placement");
                    double multiplier = rs.getDouble("points_multiplier");
                    
                    // Calculate points based on the selected points system
                    double points = POINTS_SYSTEMS.get(pointsSystem)
                                                .calculatePoints(placement) * multiplier;
                    
                    // Update player stats
                    playerStats.computeIfAbsent(userId, k -> new SeriesPlayerStats())
                             .update(points, placement);
                }
            }
        }
        
        // Update series standings
        String updateStandingsSql = """
            INSERT INTO tournament_series_standings 
            (series_id, user_id, total_points, tournaments_played, best_placement)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                total_points = VALUES(total_points),
                tournaments_played = VALUES(tournaments_played),
                best_placement = VALUES(best_placement)
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateStandingsSql)) {
            for (Map.Entry<Integer, SeriesPlayerStats> entry : playerStats.entrySet()) {
                pstmt.setInt(1, seriesId);
                pstmt.setInt(2, entry.getKey());
                pstmt.setDouble(3, entry.getValue().totalPoints);
                pstmt.setInt(4, entry.getValue().tournamentsPlayed);
                pstmt.setInt(5, entry.getValue().bestPlacement);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    public List<Map<String, Object>> getSeriesStandings(int seriesId) throws SQLException {
        String sql = """
            SELECT ss.*, u.username
            FROM tournament_series_standings ss
            JOIN users u ON ss.user_id = u.user_id
            WHERE ss.series_id = ?
            ORDER BY ss.total_points DESC
            """;
            
        List<Map<String, Object>> standings = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seriesId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    standings.add(new HashMap<String, Object>() {{
                        put("username", rs.getString("username"));
                        put("totalPoints", rs.getDouble("total_points"));
                        put("tournamentsPlayed", rs.getInt("tournaments_played"));
                        put("bestPlacement", rs.getInt("best_placement"));
                    }});
                }
            }
        }
        return standings;
    }
    
    private static class SeriesPlayerStats {
        double totalPoints = 0;
        int tournamentsPlayed = 0;
        int bestPlacement = Integer.MAX_VALUE;
        
        void update(double points, int placement) {
            totalPoints += points;
            tournamentsPlayed++;
            bestPlacement = Math.min(bestPlacement, placement);
        }
    }
    
    private interface PointsSystem {
        double calculatePoints(int placement);
    }
    
    private static class StandardPointsSystem implements PointsSystem {
        private static final double[] POINTS = {100, 80, 65, 55, 45, 40, 35, 30, 25, 20};
        
        @Override
        public double calculatePoints(int placement) {
            return placement <= POINTS.length ? POINTS[placement - 1] : 10;
        }
    }
    
    private static class GrandPrixPointsSystem implements PointsSystem {
        private static final double[] POINTS = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        
        @Override
        public double calculatePoints(int placement) {
            return placement <= POINTS.length ? POINTS[placement - 1] : 0;
        }
    }
    
    private static class ProgressivePointsSystem implements PointsSystem {
        @Override
        public double calculatePoints(int placement) {
            return Math.max(100 - (placement - 1) * 5, 10);
        }
    }
}

package com.chessgame.dao;

import com.chessgame.model.Game;
import com.chessgame.model.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameDAO {
    
    public Game createGame(Game game) throws SQLException {
        String sql = "INSERT INTO games (player1_id, player2_id, game_type, game_state, status) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, game.getPlayer1Id());
            if (game.getPlayer2Id() != null) {
                pstmt.setInt(2, game.getPlayer2Id());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, game.getGameType());
            pstmt.setString(4, game.getGameState());
            pstmt.setString(5, game.getStatus());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating game failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    game.setGameId(generatedKeys.getInt(1));
                    return game;
                } else {
                    throw new SQLException("Creating game failed, no ID obtained.");
                }
            }
        }
    }
    
    public Game getGameById(int gameId) throws SQLException {
        String sql = "SELECT * FROM games WHERE game_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, gameId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGame(rs);
                }
            }
        }
        return null;
    }
    
    public List<Game> getGamesByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM games WHERE player1_id = ? OR player2_id = ? ORDER BY start_time DESC";
        List<Game> games = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    games.add(mapResultSetToGame(rs));
                }
            }
        }
        return games;
    }
    
    public List<Game> getGamesByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM games WHERE player1_id = ? OR player2_id = ? ORDER BY start_time DESC";
        List<Game> games = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    games.add(mapResultSetToGame(rs));
                }
            }
        }
        return games;
    }
    
    public boolean updateGameState(int gameId, String gameState) throws SQLException {
        String sql = "UPDATE games SET game_state = ? WHERE game_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, gameState);
            pstmt.setInt(2, gameId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean endGame(int gameId, Integer winnerId) throws SQLException {
        String sql = "UPDATE games SET status = 'COMPLETED', end_time = CURRENT_TIMESTAMP, winner_id = ? WHERE game_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (winnerId != null) {
                pstmt.setInt(1, winnerId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, gameId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean abandonGame(int gameId) throws SQLException {
        String sql = "UPDATE games SET status = 'ABANDONED', end_time = CURRENT_TIMESTAMP WHERE game_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, gameId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public void addGameVariation(int gameId, String position, int userId, String variation) throws SQLException {
        String sql = "INSERT INTO game_variations (game_id, position, user_id, variation) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, gameId);
            pstmt.setString(2, position);
            pstmt.setInt(3, userId);
            pstmt.setString(4, variation);
            
            pstmt.executeUpdate();
        }
    }
    
    private Game mapResultSetToGame(ResultSet rs) throws SQLException {
        Game game = new Game();
        game.setGameId(rs.getInt("game_id"));
        game.setPlayer1Id(rs.getInt("player1_id"));
        game.setPlayer2Id(rs.getObject("player2_id") != null ? rs.getInt("player2_id") : null);
        game.setStartTime(rs.getTimestamp("start_time"));
        game.setEndTime(rs.getTimestamp("end_time"));
        game.setWinnerId(rs.getObject("winner_id") != null ? rs.getInt("winner_id") : null);
        game.setGameState(rs.getString("game_state"));
        game.setGameType(rs.getString("game_type"));
        game.setStatus(rs.getString("status"));
        return game;
    }
    
    public List<Game> getActiveGames() throws SQLException {
        String sql = "SELECT * FROM games WHERE status = 'IN_PROGRESS'";
        List<Game> games = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                games.add(mapResultSetToGame(rs));
            }
        }
        return games;
    }
}

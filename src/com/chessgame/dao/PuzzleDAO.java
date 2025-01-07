package com.chessgame.dao;

import com.chessgame.model.Puzzle;
import com.chessgame.model.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PuzzleDAO {
    
    public Puzzle createPuzzle(Puzzle puzzle) throws SQLException {
        String sql = "INSERT INTO puzzles (initial_position, solution, difficulty, rating, title, description, theme) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, puzzle.getInitialPosition());
            pstmt.setString(2, puzzle.getSolution());
            pstmt.setString(3, puzzle.getDifficulty());
            pstmt.setInt(4, puzzle.getRating());
            pstmt.setString(5, puzzle.getTitle());
            pstmt.setString(6, puzzle.getDescription());
            pstmt.setString(7, puzzle.getTheme());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating puzzle failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    puzzle.setPuzzleId(generatedKeys.getInt(1));
                    return puzzle;
                } else {
                    throw new SQLException("Creating puzzle failed, no ID obtained.");
                }
            }
        }
    }
    
    public Puzzle getPuzzleById(int puzzleId) throws SQLException {
        String sql = "SELECT * FROM puzzles WHERE puzzle_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, puzzleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPuzzle(rs);
                }
            }
        }
        return null;
    }
    
    public List<Puzzle> getPuzzlesByDifficulty(String difficulty) throws SQLException {
        String sql = "SELECT * FROM puzzles WHERE difficulty = ? ORDER BY rating";
        List<Puzzle> puzzles = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, difficulty);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    puzzles.add(mapResultSetToPuzzle(rs));
                }
            }
        }
        return puzzles;
    }
    
    public List<Puzzle> getPuzzlesByTheme(String theme) throws SQLException {
        String sql = "SELECT * FROM puzzles WHERE theme = ? ORDER BY rating";
        List<Puzzle> puzzles = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, theme);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    puzzles.add(mapResultSetToPuzzle(rs));
                }
            }
        }
        return puzzles;
    }
    
    public List<Puzzle> getRandomPuzzles(int count, String difficulty) throws SQLException {
        String sql = "SELECT * FROM puzzles WHERE difficulty = ? ORDER BY RAND() LIMIT ?";
        List<Puzzle> puzzles = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, difficulty);
            pstmt.setInt(2, count);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    puzzles.add(mapResultSetToPuzzle(rs));
                }
            }
        }
        return puzzles;
    }
    
    public boolean updatePuzzleRating(int puzzleId, int newRating) throws SQLException {
        String sql = "UPDATE puzzles SET rating = ? WHERE puzzle_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newRating);
            pstmt.setInt(2, puzzleId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean deletePuzzle(int puzzleId) throws SQLException {
        String sql = "DELETE FROM puzzles WHERE puzzle_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, puzzleId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public List<String> getAllThemes() throws SQLException {
        String sql = "SELECT DISTINCT theme FROM puzzles ORDER BY theme";
        List<String> themes = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                themes.add(rs.getString("theme"));
            }
        }
        return themes;
    }
    
    public void updateUserPuzzleProgress(int userId, int puzzleId, boolean completed, int attempts) throws SQLException {
        String sql = "INSERT INTO user_puzzle_progress (user_id, puzzle_id, completed, attempts, completed_at) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE completed = ?, attempts = ?, completed_at = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            Timestamp now = new Timestamp(System.currentTimeMillis());
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, puzzleId);
            pstmt.setBoolean(3, completed);
            pstmt.setInt(4, attempts);
            pstmt.setTimestamp(5, completed ? now : null);
            pstmt.setBoolean(6, completed);
            pstmt.setInt(7, attempts);
            pstmt.setTimestamp(8, completed ? now : null);
            
            pstmt.executeUpdate();
        }
    }
    
    private Puzzle mapResultSetToPuzzle(ResultSet rs) throws SQLException {
        Puzzle puzzle = new Puzzle();
        puzzle.setPuzzleId(rs.getInt("puzzle_id"));
        puzzle.setInitialPosition(rs.getString("initial_position"));
        puzzle.setSolution(rs.getString("solution"));
        puzzle.setDifficulty(rs.getString("difficulty"));
        puzzle.setRating(rs.getInt("rating"));
        puzzle.setTitle(rs.getString("title"));
        puzzle.setDescription(rs.getString("description"));
        puzzle.setTheme(rs.getString("theme"));
        return puzzle;
    }
}

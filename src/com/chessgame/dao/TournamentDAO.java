package com.chessgame.dao;

import com.chessgame.model.Tournament;
import com.chessgame.model.Tournament.*;
import com.chessgame.model.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class TournamentDAO {
    private final Gson gson = new Gson();
    
    public Tournament createTournament(Tournament tournament) throws SQLException {
        String sql = "INSERT INTO tournaments (name, description, start_date, end_date, " +
                    "max_participants, tournament_type, rounds_count, time_control_minutes, " +
                    "time_increment_seconds, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, tournament.getName());
            pstmt.setString(2, tournament.getDescription());
            pstmt.setTimestamp(3, tournament.getStartDate());
            pstmt.setTimestamp(4, tournament.getEndDate());
            pstmt.setInt(5, tournament.getMaxParticipants());
            pstmt.setString(6, tournament.getTournamentType());
            pstmt.setInt(7, tournament.getRoundsCount());
            pstmt.setInt(8, tournament.getTimeControlMinutes());
            pstmt.setInt(9, tournament.getTimeIncrementSeconds());
            pstmt.setString(10, tournament.getStatus());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating tournament failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    tournament.setTournamentId(generatedKeys.getInt(1));
                    return tournament;
                } else {
                    throw new SQLException("Creating tournament failed, no ID obtained.");
                }
            }
        }
    }
    
    public Tournament getTournamentById(int tournamentId) throws SQLException {
        String sql = "SELECT * FROM tournaments WHERE tournament_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Tournament tournament = mapResultSetToTournament(rs);
                    loadTournamentDetails(tournament);
                    return tournament;
                }
            }
        }
        return null;
    }
    
    public List<Tournament> getAllTournaments() throws SQLException {
        String sql = "SELECT * FROM tournaments ORDER BY start_date DESC";
        List<Tournament> tournaments = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                tournaments.add(mapResultSetToTournament(rs));
            }
        }
        return tournaments;
    }
    
    public List<Tournament> getTournamentsByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM tournaments WHERE status = ? ORDER BY start_date";
        List<Tournament> tournaments = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tournaments.add(mapResultSetToTournament(rs));
                }
            }
        }
        return tournaments;
    }
    
    public void registerParticipant(int tournamentId, int userId) throws SQLException {
        String sql = "INSERT INTO tournament_participants (tournament_id, user_id, score) VALUES (?, ?, 0)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, userId);
            
            pstmt.executeUpdate();
        }
    }
    
    public void updateParticipantScore(int tournamentId, int userId, double score) throws SQLException {
        String sql = "UPDATE tournament_participants SET score = ? WHERE tournament_id = ? AND user_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, score);
            pstmt.setInt(2, tournamentId);
            pstmt.setInt(3, userId);
            
            pstmt.executeUpdate();
        }
    }
    
    public void createRound(int tournamentId, TournamentRound round) throws SQLException {
        String sql = "INSERT INTO tournament_rounds (tournament_id, round_number, start_time, end_time) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, round.getRoundNumber());
            pstmt.setTimestamp(3, round.getStartTime());
            pstmt.setTimestamp(4, round.getEndTime());
            
            pstmt.executeUpdate();
            
            // Create pairings for this round
            for (TournamentPairing pairing : round.getPairings()) {
                createPairing(tournamentId, round.getRoundNumber(), pairing);
            }
        }
    }
    
    public void createPairing(int tournamentId, int roundNumber, TournamentPairing pairing) throws SQLException {
        String sql = "INSERT INTO tournament_pairings (tournament_id, round_number, player1_id, player2_id) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, roundNumber);
            pstmt.setInt(3, pairing.getPlayer1Id());
            pstmt.setInt(4, pairing.getPlayer2Id());
            
            pstmt.executeUpdate();
        }
    }
    
    public void updatePairingResult(int tournamentId, int roundNumber, int player1Id, int player2Id,
                                  Integer winnerId, boolean isDraw) throws SQLException {
        String sql = "UPDATE tournament_pairings SET winner_id = ?, is_draw = ? " +
                    "WHERE tournament_id = ? AND round_number = ? AND player1_id = ? AND player2_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (winnerId != null) {
                pstmt.setInt(1, winnerId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setBoolean(2, isDraw);
            pstmt.setInt(3, tournamentId);
            pstmt.setInt(4, roundNumber);
            pstmt.setInt(5, player1Id);
            pstmt.setInt(6, player2Id);
            
            pstmt.executeUpdate();
        }
    }
    
    public void updateTournamentStatus(int tournamentId, String status) throws SQLException {
        String sql = "UPDATE tournaments SET status = ? WHERE tournament_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, tournamentId);
            
            pstmt.executeUpdate();
        }
    }
    
    private Tournament mapResultSetToTournament(ResultSet rs) throws SQLException {
        Tournament tournament = new Tournament();
        tournament.setTournamentId(rs.getInt("tournament_id"));
        tournament.setName(rs.getString("name"));
        tournament.setDescription(rs.getString("description"));
        tournament.setStartDate(rs.getTimestamp("start_date"));
        tournament.setEndDate(rs.getTimestamp("end_date"));
        tournament.setMaxParticipants(rs.getInt("max_participants"));
        tournament.setTournamentType(rs.getString("tournament_type"));
        tournament.setRoundsCount(rs.getInt("rounds_count"));
        tournament.setTimeControlMinutes(rs.getInt("time_control_minutes"));
        tournament.setTimeIncrementSeconds(rs.getInt("time_increment_seconds"));
        tournament.setStatus(rs.getString("status"));
        return tournament;
    }
    
    private void loadTournamentDetails(Tournament tournament) throws SQLException {
        loadParticipants(tournament);
        loadRounds(tournament);
    }
    
    private void loadParticipants(Tournament tournament) throws SQLException {
        String sql = "SELECT * FROM tournament_participants WHERE tournament_id = ? ORDER BY score DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournament.getTournamentId());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TournamentParticipant participant = new TournamentParticipant();
                    participant.setUserId(rs.getInt("user_id"));
                    participant.setScore(rs.getDouble("score"));
                    participant.setRank(rs.getInt("rank"));
                    loadParticipantResults(tournament.getTournamentId(), participant);
                    tournament.getParticipants().add(participant);
                }
            }
        }
    }
    
    private void loadParticipantResults(int tournamentId, TournamentParticipant participant) throws SQLException {
        String sql = "SELECT * FROM tournament_results WHERE tournament_id = ? AND user_id = ? ORDER BY round_number";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, participant.getUserId());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TournamentResult result = new TournamentResult();
                    result.setRoundNumber(rs.getInt("round_number"));
                    result.setOpponentId(rs.getInt("opponent_id"));
                    result.setWhite(rs.getBoolean("is_white"));
                    result.setScore(rs.getDouble("score"));
                    participant.getResults().add(result);
                }
            }
        }
    }
    
    private void loadRounds(Tournament tournament) throws SQLException {
        String sql = "SELECT * FROM tournament_rounds WHERE tournament_id = ? ORDER BY round_number";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournament.getTournamentId());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TournamentRound round = new TournamentRound();
                    round.setRoundNumber(rs.getInt("round_number"));
                    round.setStartTime(rs.getTimestamp("start_time"));
                    round.setEndTime(rs.getTimestamp("end_time"));
                    loadPairings(tournament.getTournamentId(), round);
                    tournament.getRounds().add(round);
                }
            }
        }
    }
    
    private void loadPairings(int tournamentId, TournamentRound round) throws SQLException {
        String sql = "SELECT * FROM tournament_pairings WHERE tournament_id = ? AND round_number = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, round.getRoundNumber());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TournamentPairing pairing = new TournamentPairing();
                    pairing.setPlayer1Id(rs.getInt("player1_id"));
                    pairing.setPlayer2Id(rs.getInt("player2_id"));
                    pairing.setGameId(rs.getObject("game_id") != null ? rs.getInt("game_id") : null);
                    pairing.setWinnerId(rs.getObject("winner_id") != null ? rs.getInt("winner_id") : null);
                    pairing.setDraw(rs.getBoolean("is_draw"));
                    round.getPairings().add(pairing);
                }
            }
        }
    }
}

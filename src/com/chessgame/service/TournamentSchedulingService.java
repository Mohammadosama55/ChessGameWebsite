package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class TournamentSchedulingService {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Duration DEFAULT_ROUND_DURATION = Duration.ofHours(2);
    private static final Duration MIN_BREAK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_CONCURRENT_GAMES = 100;
    
    public TournamentSchedulingService() {
        // Schedule periodic checks for round transitions
        scheduler.scheduleAtFixedRate(this::checkRoundTransitions, 1, 1, TimeUnit.MINUTES);
    }
    
    public void scheduleTournament(int tournamentId, TournamentSchedule schedule) throws SQLException {
        // Validate schedule
        validateSchedule(schedule);
        
        // Store schedule
        storeTournamentSchedule(tournamentId, schedule);
        
        // Schedule initial round
        scheduleNextRound(tournamentId);
    }
    
    private void validateSchedule(TournamentSchedule schedule) {
        if (schedule.startDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        
        if (schedule.endDate.isBefore(schedule.startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        
        if (schedule.roundDuration.compareTo(DEFAULT_ROUND_DURATION) < 0) {
            throw new IllegalArgumentException("Round duration must be at least " + 
                                             DEFAULT_ROUND_DURATION.toHours() + " hours");
        }
        
        if (schedule.breakDuration.compareTo(MIN_BREAK_DURATION) < 0) {
            throw new IllegalArgumentException("Break duration must be at least " + 
                                             MIN_BREAK_DURATION.toMinutes() + " minutes");
        }
    }
    
    private void storeTournamentSchedule(int tournamentId, TournamentSchedule schedule) 
            throws SQLException {
        String sql = """
            UPDATE tournaments 
            SET start_date = ?, end_date = ?, 
                round_duration = ?, break_duration = ?,
                rounds_per_day = ?, current_round = 0,
                status = 'SCHEDULED'
            WHERE tournament_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(schedule.startDate));
            pstmt.setTimestamp(2, Timestamp.valueOf(schedule.endDate));
            pstmt.setLong(3, schedule.roundDuration.toMinutes());
            pstmt.setLong(4, schedule.breakDuration.toMinutes());
            pstmt.setInt(5, schedule.roundsPerDay);
            pstmt.setInt(6, tournamentId);
            pstmt.executeUpdate();
        }
    }
    
    private void checkRoundTransitions() {
        try {
            // Get active tournaments
            List<TournamentInfo> activeTournaments = getActiveTournaments();
            
            for (TournamentInfo tournament : activeTournaments) {
                // Check if current round should end
                if (shouldEndRound(tournament)) {
                    endRound(tournament.tournamentId);
                    
                    // Schedule next round if available
                    if (hasMoreRounds(tournament)) {
                        scheduleNextRound(tournament.tournamentId);
                    } else {
                        endTournament(tournament.tournamentId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private boolean shouldEndRound(TournamentInfo tournament) throws SQLException {
        // Check if all games in the current round are completed
        String sql = """
            SELECT COUNT(*) 
            FROM tournament_games
            WHERE tournament_id = ?
            AND round_number = ?
            AND status != 'COMPLETED'
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournament.tournamentId);
            pstmt.setInt(2, tournament.currentRound);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // All games completed, check if round duration has elapsed
                    return LocalDateTime.now().isAfter(tournament.roundStartTime
                        .plus(tournament.roundDuration));
                }
            }
        }
        return false;
    }
    
    private void endRound(int tournamentId) throws SQLException {
        // Update game statistics
        updateGameStatistics(tournamentId);
        
        // Update player statistics
        updatePlayerStatistics(tournamentId);
        
        // Update tournament round
        String sql = "UPDATE tournaments SET current_round = current_round + 1 WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.executeUpdate();
        }
    }
    
    private void scheduleNextRound(int tournamentId) throws SQLException {
        // Get tournament info
        TournamentInfo tournament = getTournamentInfo(tournamentId);
        
        // Calculate round start time
        LocalDateTime roundStartTime = calculateNextRoundStartTime(tournament);
        
        // Create pairings for the round
        List<Pairing> pairings = createPairings(tournament);
        
        // Schedule games
        scheduleGames(tournament, pairings, roundStartTime);
        
        // Update tournament round start time
        updateRoundStartTime(tournamentId, roundStartTime);
    }
    
    private LocalDateTime calculateNextRoundStartTime(TournamentInfo tournament) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baseTime = tournament.roundStartTime != null ?
            tournament.roundStartTime.plus(tournament.roundDuration).plus(tournament.breakDuration) :
            tournament.startDate;
        
        // Ensure we don't schedule rounds outside of tournament hours
        while (baseTime.isBefore(now) || !isWithinTournamentHours(baseTime, tournament)) {
            baseTime = baseTime.plusDays(1)
                             .withHour(tournament.startDate.getHour())
                             .withMinute(0)
                             .withSecond(0);
        }
        
        return baseTime;
    }
    
    private boolean isWithinTournamentHours(LocalDateTime time, TournamentInfo tournament) {
        int startHour = tournament.startDate.getHour();
        int endHour = startHour + (tournament.roundsPerDay * 
            (int) (tournament.roundDuration.plus(tournament.breakDuration).toHours()));
        
        int timeHour = time.getHour();
        return timeHour >= startHour && timeHour < endHour;
    }
    
    private List<Pairing> createPairings(TournamentInfo tournament) throws SQLException {
        // Get available players
        List<Player> players = getAvailablePlayers(tournament.tournamentId);
        
        // Create pairings based on tournament format
        return switch (tournament.format) {
            case SINGLE_ELIMINATION -> createSingleEliminationPairings(players);
            case DOUBLE_ELIMINATION -> createDoubleEliminationPairings(players);
            case SWISS -> createSwissPairings(players, tournament);
            case ROUND_ROBIN -> createRoundRobinPairings(players, tournament);
        };
    }
    
    private void scheduleGames(TournamentInfo tournament, List<Pairing> pairings, 
                             LocalDateTime roundStartTime) throws SQLException {
        String sql = """
            INSERT INTO tournament_games 
            (tournament_id, round_number, match_number,
             white_player_id, black_player_id,
             scheduled_start, scheduled_end, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'SCHEDULED')
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < pairings.size(); i++) {
                Pairing pairing = pairings.get(i);
                
                pstmt.setInt(1, tournament.tournamentId);
                pstmt.setInt(2, tournament.currentRound + 1);
                pstmt.setInt(3, i + 1);
                pstmt.setInt(4, pairing.whitePlayer.userId);
                pstmt.setInt(5, pairing.blackPlayer.userId);
                pstmt.setTimestamp(6, Timestamp.valueOf(roundStartTime));
                pstmt.setTimestamp(7, Timestamp.valueOf(
                    roundStartTime.plus(tournament.roundDuration)));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    private void updateRoundStartTime(int tournamentId, LocalDateTime startTime) throws SQLException {
        String sql = "UPDATE tournaments SET round_start_time = ? WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(startTime));
            pstmt.setInt(2, tournamentId);
            pstmt.executeUpdate();
        }
    }
    
    private void endTournament(int tournamentId) throws SQLException {
        String sql = "UPDATE tournaments SET status = 'COMPLETED' WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.executeUpdate();
        }
        
        // Update final statistics and rankings
        updateFinalStatistics(tournamentId);
        
        // Award achievements
        awardTournamentAchievements(tournamentId);
    }
    
    private List<TournamentInfo> getActiveTournaments() throws SQLException {
        String sql = """
            SELECT tournament_id, format, current_round, round_start_time,
                   start_date, end_date, round_duration, break_duration, rounds_per_day
            FROM tournaments
            WHERE status = 'ACTIVE'
            """;
            
        List<TournamentInfo> tournaments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tournaments.add(new TournamentInfo(
                    rs.getInt("tournament_id"),
                    TournamentFormat.valueOf(rs.getString("format")),
                    rs.getInt("current_round"),
                    rs.getTimestamp("round_start_time") != null ?
                        rs.getTimestamp("round_start_time").toLocalDateTime() : null,
                    rs.getTimestamp("start_date").toLocalDateTime(),
                    rs.getTimestamp("end_date").toLocalDateTime(),
                    Duration.ofMinutes(rs.getLong("round_duration")),
                    Duration.ofMinutes(rs.getLong("break_duration")),
                    rs.getInt("rounds_per_day")
                ));
            }
        }
        return tournaments;
    }
    
    private TournamentInfo getTournamentInfo(int tournamentId) throws SQLException {
        String sql = """
            SELECT format, current_round, round_start_time,
                   start_date, end_date, round_duration, break_duration, rounds_per_day
            FROM tournaments
            WHERE tournament_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new TournamentInfo(
                        tournamentId,
                        TournamentFormat.valueOf(rs.getString("format")),
                        rs.getInt("current_round"),
                        rs.getTimestamp("round_start_time") != null ?
                            rs.getTimestamp("round_start_time").toLocalDateTime() : null,
                        rs.getTimestamp("start_date").toLocalDateTime(),
                        rs.getTimestamp("end_date").toLocalDateTime(),
                        Duration.ofMinutes(rs.getLong("round_duration")),
                        Duration.ofMinutes(rs.getLong("break_duration")),
                        rs.getInt("rounds_per_day")
                    );
                }
            }
        }
        throw new SQLException("Tournament not found");
    }
    
    private static class TournamentSchedule {
        final LocalDateTime startDate;
        final LocalDateTime endDate;
        final Duration roundDuration;
        final Duration breakDuration;
        final int roundsPerDay;
        
        TournamentSchedule(LocalDateTime startDate, LocalDateTime endDate,
                          Duration roundDuration, Duration breakDuration, int roundsPerDay) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.roundDuration = roundDuration;
            this.breakDuration = breakDuration;
            this.roundsPerDay = roundsPerDay;
        }
    }
    
    private static class TournamentInfo {
        final int tournamentId;
        final TournamentFormat format;
        final int currentRound;
        final LocalDateTime roundStartTime;
        final LocalDateTime startDate;
        final LocalDateTime endDate;
        final Duration roundDuration;
        final Duration breakDuration;
        final int roundsPerDay;
        
        TournamentInfo(int tournamentId, TournamentFormat format, int currentRound,
                      LocalDateTime roundStartTime, LocalDateTime startDate, 
                      LocalDateTime endDate, Duration roundDuration, 
                      Duration breakDuration, int roundsPerDay) {
            this.tournamentId = tournamentId;
            this.format = format;
            this.currentRound = currentRound;
            this.roundStartTime = roundStartTime;
            this.startDate = startDate;
            this.endDate = endDate;
            this.roundDuration = roundDuration;
            this.breakDuration = breakDuration;
            this.roundsPerDay = roundsPerDay;
        }
    }
    
    private static class Player {
        final int userId;
        final String username;
        final int rating;
        final double points;
        
        Player(int userId, String username, int rating, double points) {
            this.userId = userId;
            this.username = username;
            this.rating = rating;
            this.points = points;
        }
    }
    
    private static class Pairing {
        final Player whitePlayer;
        final Player blackPlayer;
        
        Pairing(Player whitePlayer, Player blackPlayer) {
            this.whitePlayer = whitePlayer;
            this.blackPlayer = blackPlayer;
        }
    }
    
    private enum TournamentFormat {
        SINGLE_ELIMINATION,
        DOUBLE_ELIMINATION,
        SWISS,
        ROUND_ROBIN
    }
}

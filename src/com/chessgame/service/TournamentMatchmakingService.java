package com.chessgame.service;

import com.chessgame.model.DBUtil;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TournamentMatchmakingService {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Integer, MatchmakingPool> tournamentPools = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> playerMatches = new ConcurrentHashMap<>();
    
    public TournamentMatchmakingService() {
        // Schedule periodic cleanup of inactive pools
        scheduler.scheduleAtFixedRate(this::cleanupInactivePools, 1, 1, TimeUnit.HOURS);
        
        // Schedule periodic match creation
        scheduler.scheduleAtFixedRate(this::createMatches, 10, 10, TimeUnit.SECONDS);
    }
    
    public void registerPlayer(int tournamentId, int userId, int rating) {
        MatchmakingPool pool = tournamentPools.computeIfAbsent(
            tournamentId, 
            id -> new MatchmakingPool()
        );
        pool.addPlayer(new Player(userId, rating));
    }
    
    public void unregisterPlayer(int tournamentId, int userId) {
        MatchmakingPool pool = tournamentPools.get(tournamentId);
        if (pool != null) {
            pool.removePlayer(userId);
        }
    }
    
    private void createMatches() {
        for (Map.Entry<Integer, MatchmakingPool> entry : tournamentPools.entrySet()) {
            int tournamentId = entry.getKey();
            MatchmakingPool pool = entry.getValue();
            
            try {
                List<Match> matches = pool.createMatches();
                for (Match match : matches) {
                    createTournamentGame(tournamentId, match);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void createTournamentGame(int tournamentId, Match match) throws SQLException {
        // Check if players haven't already played each other
        if (havePlayedBefore(tournamentId, match.white.userId, match.black.userId)) {
            return;
        }
        
        String sql = """
            INSERT INTO tournament_games 
            (tournament_id, white_player_id, black_player_id, 
             white_rating, black_rating, status)
            VALUES (?, ?, ?, ?, ?, 'PENDING')
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, match.white.userId);
            pstmt.setInt(3, match.black.userId);
            pstmt.setInt(4, match.white.rating);
            pstmt.setInt(5, match.black.rating);
            
            pstmt.executeUpdate();
            
            // Record that these players have been matched
            recordMatch(match.white.userId, match.black.userId);
        }
    }
    
    private boolean havePlayedBefore(int tournamentId, int player1Id, int player2Id) throws SQLException {
        String sql = """
            SELECT COUNT(*) 
            FROM tournament_games
            WHERE tournament_id = ?
            AND ((white_player_id = ? AND black_player_id = ?)
                 OR (white_player_id = ? AND black_player_id = ?))
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, player1Id);
            pstmt.setInt(3, player2Id);
            pstmt.setInt(4, player2Id);
            pstmt.setInt(5, player1Id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    private void recordMatch(int player1Id, int player2Id) {
        playerMatches.computeIfAbsent(player1Id, k -> ConcurrentHashMap.newKeySet()).add(player2Id);
        playerMatches.computeIfAbsent(player2Id, k -> ConcurrentHashMap.newKeySet()).add(player1Id);
    }
    
    private void cleanupInactivePools() {
        tournamentPools.entrySet().removeIf(entry -> {
            try {
                return !isTournamentActive(entry.getKey());
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    private boolean isTournamentActive(int tournamentId) throws SQLException {
        String sql = "SELECT status FROM tournaments WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && "ACTIVE".equals(rs.getString("status"));
            }
        }
    }
    
    private static class Player {
        final int userId;
        final int rating;
        int gamesPlayed;
        LocalDateTime lastGameTime;
        
        Player(int userId, int rating) {
            this.userId = userId;
            this.rating = rating;
            this.gamesPlayed = 0;
            this.lastGameTime = null;
        }
    }
    
    private static class Match {
        final Player white;
        final Player black;
        final int ratingDifference;
        
        Match(Player white, Player black) {
            this.white = white;
            this.black = black;
            this.ratingDifference = Math.abs(white.rating - black.rating);
        }
    }
    
    private static class MatchmakingPool {
        private final Map<Integer, Player> players = new ConcurrentHashMap<>();
        private final Duration MIN_WAIT_BETWEEN_GAMES = Duration.ofMinutes(5);
        private final int MAX_RATING_DIFFERENCE = 200;
        private final int MAX_GAMES_DIFFERENCE = 2;
        
        void addPlayer(Player player) {
            players.put(player.userId, player);
        }
        
        void removePlayer(int userId) {
            players.remove(userId);
        }
        
        List<Match> createMatches() {
            List<Match> matches = new ArrayList<>();
            List<Player> availablePlayers = getAvailablePlayers();
            
            while (availablePlayers.size() >= 2) {
                Optional<Match> match = findBestMatch(availablePlayers);
                if (match.isPresent()) {
                    matches.add(match.get());
                    availablePlayers.remove(match.get().white);
                    availablePlayers.remove(match.get().black);
                } else {
                    break;
                }
            }
            
            // Update player statistics for matched players
            for (Match match : matches) {
                match.white.gamesPlayed++;
                match.black.gamesPlayed++;
                match.white.lastGameTime = LocalDateTime.now();
                match.black.lastGameTime = LocalDateTime.now();
            }
            
            return matches;
        }
        
        private List<Player> getAvailablePlayers() {
            LocalDateTime now = LocalDateTime.now();
            return players.values().stream()
                .filter(p -> p.lastGameTime == null || 
                           Duration.between(p.lastGameTime, now).compareTo(MIN_WAIT_BETWEEN_GAMES) >= 0)
                .sorted(Comparator.comparingInt(p -> p.gamesPlayed))
                .collect(Collectors.toList());
        }
        
        private Optional<Match> findBestMatch(List<Player> availablePlayers) {
            Match bestMatch = null;
            int bestScore = Integer.MAX_VALUE;
            
            for (int i = 0; i < availablePlayers.size(); i++) {
                for (int j = i + 1; j < availablePlayers.size(); j++) {
                    Player p1 = availablePlayers.get(i);
                    Player p2 = availablePlayers.get(j);
                    
                    int ratingDiff = Math.abs(p1.rating - p2.rating);
                    int gamesDiff = Math.abs(p1.gamesPlayed - p2.gamesPlayed);
                    
                    // Skip if rating difference is too high
                    if (ratingDiff > MAX_RATING_DIFFERENCE) {
                        continue;
                    }
                    
                    // Skip if games played difference is too high
                    if (gamesDiff > MAX_GAMES_DIFFERENCE) {
                        continue;
                    }
                    
                    // Calculate match score (lower is better)
                    int score = ratingDiff + (gamesDiff * 50);
                    
                    if (score < bestScore) {
                        // Randomly assign colors
                        Player white = Math.random() < 0.5 ? p1 : p2;
                        Player black = white == p1 ? p2 : p1;
                        bestMatch = new Match(white, black);
                        bestScore = score;
                    }
                }
            }
            
            return Optional.ofNullable(bestMatch);
        }
    }
}

package com.chessgame.service;

import com.chessgame.model.DBUtil;
import com.chessgame.model.TournamentFormat;
import com.google.gson.Gson;
import java.sql.*;
import java.time.*;
import java.util.*;

public class TournamentBracketService {
    private static final Gson gson = new Gson();
    
    public Map<String, Object> generateBracket(int tournamentId) throws SQLException {
        TournamentFormat format = getTournamentFormat(tournamentId);
        
        return switch (format) {
            case SINGLE_ELIMINATION -> generateSingleEliminationBracket(tournamentId);
            case DOUBLE_ELIMINATION -> generateDoubleEliminationBracket(tournamentId);
            case SWISS -> generateSwissBracket(tournamentId);
            case ROUND_ROBIN -> generateRoundRobinBracket(tournamentId);
        };
    }
    
    private TournamentFormat getTournamentFormat(int tournamentId) throws SQLException {
        String sql = "SELECT format FROM tournaments WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return TournamentFormat.valueOf(rs.getString("format"));
                }
            }
        }
        throw new SQLException("Tournament not found");
    }
    
    private Map<String, Object> generateSingleEliminationBracket(int tournamentId) throws SQLException {
        List<Round> rounds = new ArrayList<>();
        Map<Integer, List<Match>> matchesByRound = getMatchesByRound(tournamentId);
        
        // Calculate number of rounds needed
        int playerCount = getPlayerCount(tournamentId);
        int roundCount = (int) Math.ceil(Math.log(playerCount) / Math.log(2));
        
        // Generate rounds
        for (int roundNum = 1; roundNum <= roundCount; roundNum++) {
            List<Match> roundMatches = matchesByRound.getOrDefault(roundNum, new ArrayList<>());
            Round round = new Round(roundNum, roundMatches);
            rounds.add(round);
        }
        
        return Map.of(
            "type", "SINGLE_ELIMINATION",
            "rounds", rounds,
            "statistics", generateBracketStatistics(tournamentId)
        );
    }
    
    private Map<String, Object> generateDoubleEliminationBracket(int tournamentId) throws SQLException {
        List<Round> winnersBracket = new ArrayList<>();
        List<Round> losersBracket = new ArrayList<>();
        Map<Integer, List<Match>> matchesByRound = getMatchesByRound(tournamentId);
        
        // Split matches into winners and losers brackets
        for (Map.Entry<Integer, List<Match>> entry : matchesByRound.entrySet()) {
            int roundNum = entry.getKey();
            List<Match> matches = entry.getValue();
            
            List<Match> winnersMatches = matches.stream()
                .filter(m -> !m.isLosersBracket)
                .toList();
            
            List<Match> losersMatches = matches.stream()
                .filter(m -> m.isLosersBracket)
                .toList();
            
            if (!winnersMatches.isEmpty()) {
                winnersBracket.add(new Round(roundNum, winnersMatches));
            }
            if (!losersMatches.isEmpty()) {
                losersBracket.add(new Round(roundNum, losersMatches));
            }
        }
        
        return Map.of(
            "type", "DOUBLE_ELIMINATION",
            "winnersBracket", winnersBracket,
            "losersBracket", losersBracket,
            "statistics", generateBracketStatistics(tournamentId)
        );
    }
    
    private Map<String, Object> generateSwissBracket(int tournamentId) throws SQLException {
        List<Round> rounds = new ArrayList<>();
        Map<Integer, List<Match>> matchesByRound = getMatchesByRound(tournamentId);
        Map<Integer, PlayerStats> playerStats = getPlayerStats(tournamentId);
        
        // Generate rounds
        for (Map.Entry<Integer, List<Match>> entry : matchesByRound.entrySet()) {
            int roundNum = entry.getKey();
            List<Match> matches = entry.getValue();
            
            // Add player statistics to matches
            for (Match match : matches) {
                if (match.whitePlayer != null) {
                    match.whitePlayerStats = playerStats.get(match.whitePlayer.userId);
                }
                if (match.blackPlayer != null) {
                    match.blackPlayerStats = playerStats.get(match.blackPlayer.userId);
                }
            }
            
            rounds.add(new Round(roundNum, matches));
        }
        
        return Map.of(
            "type", "SWISS",
            "rounds", rounds,
            "standings", generateSwissStandings(tournamentId),
            "statistics", generateBracketStatistics(tournamentId)
        );
    }
    
    private Map<String, Object> generateRoundRobinBracket(int tournamentId) throws SQLException {
        List<Round> rounds = new ArrayList<>();
        Map<Integer, List<Match>> matchesByRound = getMatchesByRound(tournamentId);
        Map<Integer, PlayerStats> playerStats = getPlayerStats(tournamentId);
        
        // Generate rounds
        for (Map.Entry<Integer, List<Match>> entry : matchesByRound.entrySet()) {
            int roundNum = entry.getKey();
            List<Match> matches = entry.getValue();
            
            // Add player statistics to matches
            for (Match match : matches) {
                if (match.whitePlayer != null) {
                    match.whitePlayerStats = playerStats.get(match.whitePlayer.userId);
                }
                if (match.blackPlayer != null) {
                    match.blackPlayerStats = playerStats.get(match.blackPlayer.userId);
                }
            }
            
            rounds.add(new Round(roundNum, matches));
        }
        
        return Map.of(
            "type", "ROUND_ROBIN",
            "rounds", rounds,
            "crosstable", generateCrosstable(tournamentId),
            "statistics", generateBracketStatistics(tournamentId)
        );
    }
    
    private Map<Integer, List<Match>> getMatchesByRound(int tournamentId) throws SQLException {
        String sql = """
            SELECT tg.*, 
                   w.user_id as white_id, w.username as white_name, w.rating as white_rating,
                   b.user_id as black_id, b.username as black_name, b.rating as black_rating
            FROM tournament_games tg
            LEFT JOIN users w ON tg.white_player_id = w.user_id
            LEFT JOIN users b ON tg.black_player_id = b.user_id
            WHERE tg.tournament_id = ?
            ORDER BY tg.round_number, tg.match_number
            """;
            
        Map<Integer, List<Match>> matches = new HashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int roundNum = rs.getInt("round_number");
                    
                    Player whitePlayer = rs.getObject("white_id") != null ? new Player(
                        rs.getInt("white_id"),
                        rs.getString("white_name"),
                        rs.getInt("white_rating")
                    ) : null;
                    
                    Player blackPlayer = rs.getObject("black_id") != null ? new Player(
                        rs.getInt("black_id"),
                        rs.getString("black_name"),
                        rs.getInt("black_rating")
                    ) : null;
                    
                    Match match = new Match(
                        rs.getInt("game_id"),
                        roundNum,
                        rs.getInt("match_number"),
                        whitePlayer,
                        blackPlayer,
                        rs.getString("result"),
                        rs.getBoolean("is_losers_bracket")
                    );
                    
                    matches.computeIfAbsent(roundNum, k -> new ArrayList<>()).add(match);
                }
            }
        }
        return matches;
    }
    
    private Map<Integer, PlayerStats> getPlayerStats(int tournamentId) throws SQLException {
        String sql = """
            SELECT user_id, games_played, games_won, games_drawn, games_lost,
                   performance_rating
            FROM tournament_statistics
            WHERE tournament_id = ?
            """;
            
        Map<Integer, PlayerStats> stats = new HashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getInt("user_id"), new PlayerStats(
                        rs.getInt("games_played"),
                        rs.getInt("games_won"),
                        rs.getInt("games_drawn"),
                        rs.getInt("games_lost"),
                        rs.getInt("performance_rating")
                    ));
                }
            }
        }
        return stats;
    }
    
    private List<Map<String, Object>> generateSwissStandings(int tournamentId) throws SQLException {
        String sql = """
            SELECT ts.*, u.username
            FROM tournament_statistics ts
            JOIN users u ON ts.user_id = u.user_id
            WHERE ts.tournament_id = ?
            ORDER BY ts.points DESC, ts.performance_rating DESC
            """;
            
        List<Map<String, Object>> standings = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    standings.add(Map.of(
                        "userId", rs.getInt("user_id"),
                        "username", rs.getString("username"),
                        "points", rs.getDouble("points"),
                        "gamesPlayed", rs.getInt("games_played"),
                        "performanceRating", rs.getInt("performance_rating")
                    ));
                }
            }
        }
        return standings;
    }
    
    private Map<String, Object> generateCrosstable(int tournamentId) throws SQLException {
        List<Integer> players = getPlayerIds(tournamentId);
        Map<String, String> results = getGameResults(tournamentId);
        
        Map<String, Object> crosstable = new HashMap<>();
        crosstable.put("players", players);
        crosstable.put("results", results);
        
        return crosstable;
    }
    
    private List<Integer> getPlayerIds(int tournamentId) throws SQLException {
        String sql = """
            SELECT user_id 
            FROM tournament_players 
            WHERE tournament_id = ?
            ORDER BY initial_rating DESC
            """;
            
        List<Integer> players = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    players.add(rs.getInt("user_id"));
                }
            }
        }
        return players;
    }
    
    private Map<String, String> getGameResults(int tournamentId) throws SQLException {
        String sql = """
            SELECT white_player_id, black_player_id, result
            FROM tournament_games
            WHERE tournament_id = ?
            """;
            
        Map<String, String> results = new HashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getInt("white_player_id") + "-" + rs.getInt("black_player_id");
                    results.put(key, rs.getString("result"));
                }
            }
        }
        return results;
    }
    
    private Map<String, Object> generateBracketStatistics(int tournamentId) throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as total_games,
                SUM(CASE WHEN result = 'WHITE_WIN' THEN 1 ELSE 0 END) as white_wins,
                SUM(CASE WHEN result = 'BLACK_WIN' THEN 1 ELSE 0 END) as black_wins,
                SUM(CASE WHEN result = 'DRAW' THEN 1 ELSE 0 END) as draws,
                AVG(num_moves) as avg_game_length
            FROM tournament_games
            WHERE tournament_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                        "totalGames", rs.getInt("total_games"),
                        "whiteWins", rs.getInt("white_wins"),
                        "blackWins", rs.getInt("black_wins"),
                        "draws", rs.getInt("draws"),
                        "avgGameLength", rs.getDouble("avg_game_length")
                    );
                }
            }
        }
        return Map.of();
    }
    
    private int getPlayerCount(int tournamentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tournament_players WHERE tournament_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
    
    private static class Player {
        final int userId;
        final String username;
        final int rating;
        
        Player(int userId, String username, int rating) {
            this.userId = userId;
            this.username = username;
            this.rating = rating;
        }
    }
    
    private static class PlayerStats {
        final int gamesPlayed;
        final int gamesWon;
        final int gamesDrawn;
        final int gamesLost;
        final int performanceRating;
        
        PlayerStats(int gamesPlayed, int gamesWon, int gamesDrawn, int gamesLost, 
                   int performanceRating) {
            this.gamesPlayed = gamesPlayed;
            this.gamesWon = gamesWon;
            this.gamesDrawn = gamesDrawn;
            this.gamesLost = gamesLost;
            this.performanceRating = performanceRating;
        }
    }
    
    private static class Match {
        final int gameId;
        final int roundNumber;
        final int matchNumber;
        final Player whitePlayer;
        final Player blackPlayer;
        final String result;
        final boolean isLosersBracket;
        PlayerStats whitePlayerStats;
        PlayerStats blackPlayerStats;
        
        Match(int gameId, int roundNumber, int matchNumber, Player whitePlayer, 
              Player blackPlayer, String result, boolean isLosersBracket) {
            this.gameId = gameId;
            this.roundNumber = roundNumber;
            this.matchNumber = matchNumber;
            this.whitePlayer = whitePlayer;
            this.blackPlayer = blackPlayer;
            this.result = result;
            this.isLosersBracket = isLosersBracket;
        }
    }
    
    private static class Round {
        final int roundNumber;
        final List<Match> matches;
        
        Round(int roundNumber, List<Match> matches) {
            this.roundNumber = roundNumber;
            this.matches = matches;
        }
    }
}

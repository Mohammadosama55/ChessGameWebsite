package com.chessgame.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.chessgame.model.GameResult;

public class RatingCalculationService {
    private static final int K_FACTOR = 32;  // Standard K-factor for rating calculations
    private static final int PROVISIONAL_K_FACTOR = 40;  // Higher K-factor for provisional ratings
    private static final int PROVISIONAL_GAMES_THRESHOLD = 20;  // Number of games before rating is no longer provisional
    
    public Map<Integer, Integer> calculateTournamentRatingChanges(
            Map<Integer, Integer> playerRatings,
            Map<Integer, Integer> gamesPlayed,
            List<GameResult> results) {
        
        Map<Integer, Integer> ratingChanges = new HashMap<>();
        Map<Integer, Double> performanceScores = new HashMap<>();
        
        // Calculate performance scores for each player
        for (GameResult result : results) {
            updatePerformanceScores(performanceScores, result);
        }
        
        // Calculate rating changes
        for (Map.Entry<Integer, Double> entry : performanceScores.entrySet()) {
            int playerId = entry.getKey();
            double score = entry.getValue();
            int games = gamesPlayed.get(playerId);
            
            double expectedScore = 0;
            int playerRating = playerRatings.get(playerId);
            
            // Calculate expected score against each opponent
            for (GameResult result : results) {
                if (result.involves(playerId)) {
                    int opponentId = result.getOpponent(playerId);
                    int opponentRating = playerRatings.get(opponentId);
                    expectedScore += calculateExpectedScore(playerRating, opponentRating);
                }
            }
            
            // Calculate rating change
            int kFactor = games < PROVISIONAL_GAMES_THRESHOLD ? PROVISIONAL_K_FACTOR : K_FACTOR;
            int ratingChange = (int) Math.round(kFactor * (score - expectedScore));
            ratingChanges.put(playerId, ratingChange);
        }
        
        return ratingChanges;
    }
    
    public int calculatePerformanceRating(int playerRating, List<GameResult> results, int playerId) {
        if (results.isEmpty()) {
            return playerRating;
        }
        
        double score = 0;
        int gamesPlayed = 0;
        int totalOpponentRating = 0;
        
        for (GameResult result : results) {
            if (result.involves(playerId)) {
                gamesPlayed++;
                int opponentRating = result.getOpponentRating(playerId);
                totalOpponentRating += opponentRating;
                score += result.getScore(playerId);
            }
        }
        
        if (gamesPlayed == 0) {
            return playerRating;
        }
        
        double percentage = score / gamesPlayed;
        double averageOpponentRating = totalOpponentRating / (double) gamesPlayed;
        
        // Performance Rating = Average Opponent Rating + Rating Difference
        return (int) Math.round(averageOpponentRating + ratingDifferenceFromPercentage(percentage));
    }
    
    private double calculateExpectedScore(int playerRating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10, (opponentRating - playerRating) / 400.0));
    }
    
    private double ratingDifferenceFromPercentage(double percentage) {
        // Convert percentage to rating difference using FIDE conversion table
        if (percentage >= 1.0) return 800;
        if (percentage <= 0.0) return -800;
        
        // Logistic function to approximate FIDE table
        return -400 * Math.log10(1.0 / percentage - 1.0);
    }
    
    private void updatePerformanceScores(Map<Integer, Double> scores, GameResult result) {
        int whiteId = result.getWhitePlayerId();
        int blackId = result.getBlackPlayerId();
        
        scores.merge(whiteId, result.getWhiteScore(), Double::sum);
        scores.merge(blackId, result.getBlackScore(), Double::sum);
    }
    
    // Game result model
    public static class GameResult {
        private final int whitePlayerId;
        private final int blackPlayerId;
        private final int whiteRating;
        private final int blackRating;
        private final double whiteScore;  // 1 for win, 0.5 for draw, 0 for loss
        
        public GameResult(int whitePlayerId, int blackPlayerId, int whiteRating, int blackRating, 
                         String result) {
            this.whitePlayerId = whitePlayerId;
            this.blackPlayerId = blackPlayerId;
            this.whiteRating = whiteRating;
            this.blackRating = blackRating;
            
            switch (result) {
                case "1-0":
                    this.whiteScore = 1.0;
                    break;
                case "0-1":
                    this.whiteScore = 0.0;
                    break;
                case "1/2-1/2":
                default:
                    this.whiteScore = 0.5;
            }
        }
        
        public boolean involves(int playerId) {
            return playerId == whitePlayerId || playerId == blackPlayerId;
        }
        
        public int getOpponent(int playerId) {
            return playerId == whitePlayerId ? blackPlayerId : whitePlayerId;
        }
        
        public int getOpponentRating(int playerId) {
            return playerId == whitePlayerId ? blackRating : whiteRating;
        }
        
        public double getScore(int playerId) {
            return playerId == whitePlayerId ? whiteScore : 1.0 - whiteScore;
        }
        
        public int getWhitePlayerId() {
            return whitePlayerId;
        }
        
        public int getBlackPlayerId() {
            return blackPlayerId;
        }
        
        public double getWhiteScore() {
            return whiteScore;
        }
        
        public double getBlackScore() {
            return 1.0 - whiteScore;
        }
    }
    
    // Rating category thresholds
    public static class RatingCategory {
        public static final int BEGINNER = 0;
        public static final int NOVICE = 1000;
        public static final int INTERMEDIATE = 1400;
        public static final int ADVANCED = 1800;
        public static final int EXPERT = 2000;
        public static final int MASTER = 2200;
        public static final int GRANDMASTER = 2500;
        
        public static String getCategory(int rating) {
            if (rating >= GRANDMASTER) return "Grandmaster";
            if (rating >= MASTER) return "Master";
            if (rating >= EXPERT) return "Expert";
            if (rating >= ADVANCED) return "Advanced";
            if (rating >= INTERMEDIATE) return "Intermediate";
            if (rating >= NOVICE) return "Novice";
            return "Beginner";
        }
    }
    
    // Title requirements
    public static class TitleRequirements {
        private static final int GAMES_REQUIRED = 30;
        private static final int NORM_PERFORMANCE = 2600;  // For GM norm
        private static final int TITLE_THRESHOLD = 2500;   // For GM title
        
        public static boolean isEligibleForTitle(int rating, int gamesPlayed, 
                                               List<Integer> normPerformances) {
            if (gamesPlayed < GAMES_REQUIRED) {
                return false;
            }
            
            if (rating < TITLE_THRESHOLD) {
                return false;
            }
            
            // Need at least 3 norm performances
            int normCount = 0;
            for (int performance : normPerformances) {
                if (performance >= NORM_PERFORMANCE) {
                    normCount++;
                }
            }
            
            return normCount >= 3;
        }
    }
}

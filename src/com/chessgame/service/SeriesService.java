package com.chessgame.service;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.chessgame.model.Series;
import com.chessgame.model.Game;
import com.chessgame.dao.SeriesDAO;
import com.chessgame.dao.GameDAO;

public class SeriesService {
    private final SeriesDAO seriesDAO;
    private final GameDAO gameDAO;
    
    public SeriesService() {
        this.seriesDAO = new SeriesDAO();
        this.gameDAO = new GameDAO();
    }
    
    public Series createSeries(int player1Id, int player2Id, int numberOfGames) throws SQLException {
        Series series = new Series();
        series.setPlayer1Id(player1Id);
        series.setPlayer2Id(player2Id);
        series.setNumberOfGames(numberOfGames);
        series.setStatus("ONGOING");
        
        return seriesDAO.createSeries(series);
    }
    
    public Game createNextGame(int seriesId) throws SQLException {
        Series series = seriesDAO.getSeriesById(seriesId);
        if (series == null || series.getStatus().equals("COMPLETED")) {
            throw new IllegalStateException("Series not found or already completed");
        }
        
        List<Game> games = gameDAO.getGamesBySeries(seriesId);
        if (games.size() >= series.getNumberOfGames()) {
            throw new IllegalStateException("All games in series have been played");
        }
        
        // Alternate colors for each game
        boolean isPlayer1White = games.size() % 2 == 0;
        int whiteId = isPlayer1White ? series.getPlayer1Id() : series.getPlayer2Id();
        int blackId = isPlayer1White ? series.getPlayer2Id() : series.getPlayer1Id();
        
        Game game = new Game();
        game.setWhitePlayerId(whiteId);
        game.setBlackPlayerId(blackId);
        game.setSeriesId(seriesId);
        game.setStatus("ONGOING");
        
        return gameDAO.createGame(game);
    }
    
    public void updateSeriesStatus(int seriesId) throws SQLException {
        Series series = seriesDAO.getSeriesById(seriesId);
        List<Game> games = gameDAO.getGamesBySeries(seriesId);
        
        int player1Wins = 0;
        int player2Wins = 0;
        int draws = 0;
        
        for (Game game : games) {
            if (game.getStatus().equals("COMPLETED")) {
                Integer winnerId = game.getWinnerId();
                if (winnerId == null) {
                    draws++;
                } else if (winnerId == series.getPlayer1Id()) {
                    player1Wins++;
                } else if (winnerId == series.getPlayer2Id()) {
                    player2Wins++;
                }
            }
        }
        
        // Update series status if all games are completed
        if (games.size() == series.getNumberOfGames() && 
            games.stream().allMatch(g -> g.getStatus().equals("COMPLETED"))) {
            
            series.setStatus("COMPLETED");
            if (player1Wins > player2Wins) {
                series.setWinnerId(series.getPlayer1Id());
            } else if (player2Wins > player1Wins) {
                series.setWinnerId(series.getPlayer2Id());
            }
            
            seriesDAO.updateSeries(series);
        }
    }
    
    public Map<String, Object> getSeriesStats(int seriesId) throws SQLException {
        Series series = seriesDAO.getSeriesById(seriesId);
        List<Game> games = gameDAO.getGamesBySeries(seriesId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", series.getNumberOfGames());
        stats.put("gamesPlayed", games.size());
        stats.put("player1Id", series.getPlayer1Id());
        stats.put("player2Id", series.getPlayer2Id());
        
        int player1Wins = 0;
        int player2Wins = 0;
        int draws = 0;
        
        for (Game game : games) {
            if (game.getStatus().equals("COMPLETED")) {
                Integer winnerId = game.getWinnerId();
                if (winnerId == null) {
                    draws++;
                } else if (winnerId == series.getPlayer1Id()) {
                    player1Wins++;
                } else if (winnerId == series.getPlayer2Id()) {
                    player2Wins++;
                }
            }
        }
        
        stats.put("player1Wins", player1Wins);
        stats.put("player2Wins", player2Wins);
        stats.put("draws", draws);
        stats.put("status", series.getStatus());
        stats.put("winnerId", series.getWinnerId());
        
        return stats;
    }
}

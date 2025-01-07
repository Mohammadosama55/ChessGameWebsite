package com.chessgame.service;

import com.chessgame.model.DBUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.sql.*;
import java.util.*;

public class TournamentExportService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public void exportToJSON(int tournamentId, HttpServletResponse response) throws SQLException, IOException {
        Map<String, Object> tournamentData = getTournamentData(tournamentId);
        
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=tournament_" + tournamentId + ".json");
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(gson.toJson(tournamentData));
        }
    }
    
    public void exportToExcel(int tournamentId, HttpServletResponse response) throws SQLException, IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create Tournament Info sheet
            Sheet infoSheet = workbook.createSheet("Tournament Info");
            Map<String, Object> tournamentData = getTournamentData(tournamentId);
            createTournamentInfoSheet(infoSheet, tournamentData);
            
            // Create Players sheet
            Sheet playersSheet = workbook.createSheet("Players");
            createPlayersSheet(playersSheet, (List<Map<String, Object>>) tournamentData.get("players"));
            
            // Create Games sheet
            Sheet gamesSheet = workbook.createSheet("Games");
            createGamesSheet(gamesSheet, (List<Map<String, Object>>) tournamentData.get("games"));
            
            // Create Statistics sheet
            Sheet statsSheet = workbook.createSheet("Statistics");
            createStatisticsSheet(statsSheet, tournamentData);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=tournament_" + tournamentId + ".xlsx");
            
            workbook.write(response.getOutputStream());
        }
    }
    
    public void exportToPGN(int tournamentId, HttpServletResponse response) throws SQLException, IOException {
        Map<String, Object> tournamentData = getTournamentData(tournamentId);
        List<Map<String, Object>> games = (List<Map<String, Object>>) tournamentData.get("games");
        
        response.setContentType("application/x-chess-pgn");
        response.setHeader("Content-Disposition", "attachment; filename=tournament_" + tournamentId + ".pgn");
        
        try (PrintWriter writer = response.getWriter()) {
            // Write tournament header
            writer.println("[Event \"" + tournamentData.get("name") + "\"]");
            writer.println("[Site \"Chess Game Website\"]");
            writer.println("[Date \"" + tournamentData.get("startDate") + "\"]");
            
            // Write each game
            for (Map<String, Object> game : games) {
                writer.println();
                writer.println("[Event \"" + tournamentData.get("name") + "\"]");
                writer.println("[Site \"Chess Game Website\"]");
                writer.println("[Date \"" + game.get("date") + "\"]");
                writer.println("[White \"" + game.get("whitePlayer") + "\"]");
                writer.println("[Black \"" + game.get("blackPlayer") + "\"]");
                writer.println("[Result \"" + game.get("result") + "\"]");
                writer.println("[WhiteElo \"" + game.get("whiteRating") + "\"]");
                writer.println("[BlackElo \"" + game.get("blackRating") + "\"]");
                writer.println("[ECO \"" + game.get("eco") + "\"]");
                writer.println("[Opening \"" + game.get("opening") + "\"]");
                writer.println();
                writer.println(game.get("pgn"));
                writer.println();
            }
        }
    }
    
    private Map<String, Object> getTournamentData(int tournamentId) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        
        try (Connection conn = DBUtil.getConnection()) {
            // Get tournament info
            String sql = "SELECT * FROM tournaments WHERE tournament_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, tournamentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        data.put("id", rs.getInt("tournament_id"));
                        data.put("name", rs.getString("name"));
                        data.put("description", rs.getString("description"));
                        data.put("startDate", rs.getDate("start_date"));
                        data.put("endDate", rs.getDate("end_date"));
                        data.put("type", rs.getString("tournament_type"));
                        data.put("status", rs.getString("status"));
                    }
                }
            }
            
            // Get players
            sql = "SELECT u.user_id, u.username, ts.* FROM tournament_participants tp " +
                  "JOIN users u ON tp.user_id = u.user_id " +
                  "LEFT JOIN tournament_statistics ts ON tp.tournament_id = ts.tournament_id " +
                  "AND tp.user_id = ts.user_id " +
                  "WHERE tp.tournament_id = ?";
            
            List<Map<String, Object>> players = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, tournamentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> player = new HashMap<>();
                        player.put("id", rs.getInt("user_id"));
                        player.put("username", rs.getString("username"));
                        player.put("gamesPlayed", rs.getInt("games_played"));
                        player.put("gamesWon", rs.getInt("games_won"));
                        player.put("gamesDrawn", rs.getInt("games_drawn"));
                        player.put("gamesLost", rs.getInt("games_lost"));
                        player.put("performanceRating", rs.getInt("performance_rating"));
                        player.put("ratingChange", rs.getInt("rating_change"));
                        players.add(player);
                    }
                }
            }
            data.put("players", players);
            
            // Get games
            sql = "SELECT g.*, tgs.*, " +
                  "w.username as white_player, b.username as black_player " +
                  "FROM tournament_game_statistics tgs " +
                  "JOIN games g ON tgs.game_id = g.game_id " +
                  "JOIN users w ON tgs.white_player_id = w.user_id " +
                  "JOIN users b ON tgs.black_player_id = b.user_id " +
                  "WHERE tgs.tournament_id = ?";
            
            List<Map<String, Object>> games = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, tournamentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> game = new HashMap<>();
                        game.put("id", rs.getInt("game_id"));
                        game.put("whitePlayer", rs.getString("white_player"));
                        game.put("blackPlayer", rs.getString("black_player"));
                        game.put("result", rs.getString("result"));
                        game.put("date", rs.getTimestamp("created_at"));
                        game.put("eco", rs.getString("opening_eco"));
                        game.put("opening", rs.getString("opening_name"));
                        game.put("moves", rs.getInt("num_moves"));
                        game.put("length", rs.getInt("game_length_seconds"));
                        game.put("pgn", rs.getString("pgn"));
                        games.add(game);
                    }
                }
            }
            data.put("games", games);
        }
        
        return data;
    }
    
    private void createTournamentInfoSheet(Sheet sheet, Map<String, Object> data) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Tournament Information");
        
        Row nameRow = sheet.createRow(1);
        nameRow.createCell(0).setCellValue("Name");
        nameRow.createCell(1).setCellValue((String) data.get("name"));
        
        Row typeRow = sheet.createRow(2);
        typeRow.createCell(0).setCellValue("Type");
        typeRow.createCell(1).setCellValue((String) data.get("type"));
        
        Row dateRow = sheet.createRow(3);
        dateRow.createCell(0).setCellValue("Dates");
        dateRow.createCell(1).setCellValue(data.get("startDate") + " to " + data.get("endDate"));
        
        Row statusRow = sheet.createRow(4);
        statusRow.createCell(0).setCellValue("Status");
        statusRow.createCell(1).setCellValue((String) data.get("status"));
    }
    
    private void createPlayersSheet(Sheet sheet, List<Map<String, Object>> players) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Username", "Games Played", "Won", "Drawn", "Lost", "Performance", "Rating Change"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        
        int rowNum = 1;
        for (Map<String, Object> player : players) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) player.get("username"));
            row.createCell(1).setCellValue((Integer) player.get("gamesPlayed"));
            row.createCell(2).setCellValue((Integer) player.get("gamesWon"));
            row.createCell(3).setCellValue((Integer) player.get("gamesDrawn"));
            row.createCell(4).setCellValue((Integer) player.get("gamesLost"));
            row.createCell(5).setCellValue((Integer) player.get("performanceRating"));
            row.createCell(6).setCellValue((Integer) player.get("ratingChange"));
        }
    }
    
    private void createGamesSheet(Sheet sheet, List<Map<String, Object>> games) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"White", "Black", "Result", "Opening", "Moves", "Length (min)"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        
        int rowNum = 1;
        for (Map<String, Object> game : games) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) game.get("whitePlayer"));
            row.createCell(1).setCellValue((String) game.get("blackPlayer"));
            row.createCell(2).setCellValue((String) game.get("result"));
            row.createCell(3).setCellValue((String) game.get("opening"));
            row.createCell(4).setCellValue((Integer) game.get("moves"));
            row.createCell(5).setCellValue(((Integer) game.get("length")) / 60.0);
        }
    }
    
    private void createStatisticsSheet(Sheet sheet, Map<String, Object> data) {
        List<Map<String, Object>> games = (List<Map<String, Object>>) data.get("games");
        
        // Calculate statistics
        int totalGames = games.size();
        int whiteWins = 0;
        int blackWins = 0;
        int draws = 0;
        int totalMoves = 0;
        Map<String, Integer> openings = new HashMap<>();
        
        for (Map<String, Object> game : games) {
            String result = (String) game.get("result");
            if ("1-0".equals(result)) whiteWins++;
            else if ("0-1".equals(result)) blackWins++;
            else draws++;
            
            totalMoves += (Integer) game.get("moves");
            String opening = (String) game.get("opening");
            openings.merge(opening, 1, Integer::sum);
        }
        
        // Write statistics
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Tournament Statistics");
        
        Row totalRow = sheet.createRow(1);
        totalRow.createCell(0).setCellValue("Total Games");
        totalRow.createCell(1).setCellValue(totalGames);
        
        Row avgMovesRow = sheet.createRow(2);
        avgMovesRow.createCell(0).setCellValue("Average Moves per Game");
        avgMovesRow.createCell(1).setCellValue(totalGames > 0 ? totalMoves / (double) totalGames : 0);
        
        Row whiteWinsRow = sheet.createRow(3);
        whiteWinsRow.createCell(0).setCellValue("White Wins");
        whiteWinsRow.createCell(1).setCellValue(whiteWins);
        
        Row blackWinsRow = sheet.createRow(4);
        blackWinsRow.createCell(0).setCellValue("Black Wins");
        blackWinsRow.createCell(1).setCellValue(blackWins);
        
        Row drawsRow = sheet.createRow(5);
        drawsRow.createCell(0).setCellValue("Draws");
        drawsRow.createCell(1).setCellValue(draws);
        
        // Top openings
        Row openingsHeader = sheet.createRow(7);
        openingsHeader.createCell(0).setCellValue("Most Popular Openings");
        
        int rowNum = 8;
        for (Map.Entry<String, Integer> entry : 
             openings.entrySet().stream()
                     .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                     .limit(5)
                     .toList()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
    }
}

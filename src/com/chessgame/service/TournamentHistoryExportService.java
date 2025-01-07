package com.chessgame.service;

import com.chessgame.model.DBUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TournamentHistoryExportService {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    
    public void exportTournamentHistory(int userId, String format, OutputStream outputStream) 
            throws SQLException, IOException {
        switch (format.toUpperCase()) {
            case "JSON" -> exportAsJson(userId, outputStream);
            case "EXCEL" -> exportAsExcel(userId, outputStream);
            case "PGN" -> exportAsPgn(userId, outputStream);
            case "ZIP" -> exportAsZip(userId, outputStream);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    private void exportAsJson(int userId, OutputStream outputStream) throws SQLException, IOException {
        Map<String, Object> history = new HashMap<>();
        history.put("tournaments", getTournamentHistory(userId));
        history.put("statistics", getPlayerStatistics(userId));
        history.put("achievements", getPlayerAchievements(userId));
        
        try (Writer writer = new OutputStreamWriter(outputStream)) {
            gson.toJson(history, writer);
        }
    }
    
    private void exportAsExcel(int userId, OutputStream outputStream) throws SQLException, IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            
            // Create sheets
            createTournamentSheet(workbook, headerStyle, dateStyle, userId);
            createGamesSheet(workbook, headerStyle, dateStyle, userId);
            createStatsSheet(workbook, headerStyle, userId);
            createAchievementsSheet(workbook, headerStyle, dateStyle, userId);
            
            workbook.write(outputStream);
        }
    }
    
    private void exportAsPgn(int userId, OutputStream outputStream) throws SQLException, IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream))) {
            List<Map<String, Object>> games = getTournamentGames(userId);
            
            for (Map<String, Object> game : games) {
                // Write PGN headers
                writer.println("[Event \"" + game.get("tournamentName") + "\"]");
                writer.println("[Site \"Chess Game Website\"]");
                writer.println("[Date \"" + game.get("date") + "\"]");
                writer.println("[White \"" + game.get("whitePlayer") + "\"]");
                writer.println("[Black \"" + game.get("blackPlayer") + "\"]");
                writer.println("[Result \"" + game.get("result") + "\"]");
                writer.println("[WhiteElo \"" + game.get("whiteRating") + "\"]");
                writer.println("[BlackElo \"" + game.get("blackRating") + "\"]");
                writer.println("[ECO \"" + game.get("ecoCode") + "\"]");
                writer.println("[Opening \"" + game.get("openingName") + "\"]");
                writer.println();
                
                // Write moves
                writer.println(game.get("moves"));
                writer.println();
                writer.println();
            }
        }
    }
    
    private void exportAsZip(int userId, OutputStream outputStream) throws SQLException, IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            // Add JSON export
            zipOut.putNextEntry(new ZipEntry("tournament_history.json"));
            exportAsJson(userId, zipOut);
            zipOut.closeEntry();
            
            // Add Excel export
            zipOut.putNextEntry(new ZipEntry("tournament_history.xlsx"));
            exportAsExcel(userId, zipOut);
            zipOut.closeEntry();
            
            // Add PGN export
            zipOut.putNextEntry(new ZipEntry("tournament_games.pgn"));
            exportAsPgn(userId, zipOut);
            zipOut.closeEntry();
        }
    }
    
    private List<Map<String, Object>> getTournamentHistory(int userId) throws SQLException {
        String sql = """
            SELECT t.*, ts.*, 
                   COUNT(DISTINCT tg.game_id) as total_games,
                   u.username as organizer_name
            FROM tournaments t
            JOIN tournament_statistics ts ON t.tournament_id = ts.tournament_id
            LEFT JOIN tournament_games tg ON t.tournament_id = tg.tournament_id
            JOIN users u ON t.organizer_id = u.user_id
            WHERE ts.user_id = ?
            GROUP BY t.tournament_id
            ORDER BY t.start_date DESC
            """;
            
        List<Map<String, Object>> tournaments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tournaments.add(new HashMap<String, Object>() {{
                        put("tournamentId", rs.getInt("tournament_id"));
                        put("name", rs.getString("name"));
                        put("startDate", rs.getTimestamp("start_date").toLocalDateTime());
                        put("endDate", rs.getTimestamp("end_date").toLocalDateTime());
                        put("status", rs.getString("status"));
                        put("organizer", rs.getString("organizer_name"));
                        put("totalGames", rs.getInt("total_games"));
                        put("gamesWon", rs.getInt("games_won"));
                        put("gamesDrawn", rs.getInt("games_drawn"));
                        put("gamesLost", rs.getInt("games_lost"));
                        put("performanceRating", rs.getInt("performance_rating"));
                        put("placement", rs.getInt("placement"));
                    }});
                }
            }
        }
        return tournaments;
    }
    
    private List<Map<String, Object>> getTournamentGames(int userId) throws SQLException {
        String sql = """
            SELECT tg.*, t.name as tournament_name,
                   w.username as white_player, w.rating as white_rating,
                   b.username as black_player, b.rating as black_rating
            FROM tournament_games tg
            JOIN tournaments t ON tg.tournament_id = t.tournament_id
            JOIN users w ON tg.white_player_id = w.user_id
            JOIN users b ON tg.black_player_id = b.user_id
            WHERE tg.white_player_id = ? OR tg.black_player_id = ?
            ORDER BY tg.start_time DESC
            """;
            
        List<Map<String, Object>> games = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    games.add(new HashMap<String, Object>() {{
                        put("tournamentName", rs.getString("tournament_name"));
                        put("date", rs.getDate("start_time").toLocalDate());
                        put("whitePlayer", rs.getString("white_player"));
                        put("blackPlayer", rs.getString("black_player"));
                        put("whiteRating", rs.getInt("white_rating"));
                        put("blackRating", rs.getInt("black_rating"));
                        put("result", rs.getString("result"));
                        put("moves", rs.getString("moves"));
                        put("ecoCode", rs.getString("eco_code"));
                        put("openingName", rs.getString("opening_name"));
                    }});
                }
            }
        }
        return games;
    }
    
    private Map<String, Object> getPlayerStatistics(int userId) throws SQLException {
        String sql = """
            SELECT 
                COUNT(DISTINCT ts.tournament_id) as tournaments_played,
                SUM(ts.games_won) as total_wins,
                SUM(ts.games_drawn) as total_draws,
                SUM(ts.games_lost) as total_losses,
                AVG(ts.performance_rating) as avg_performance,
                COUNT(CASE WHEN ts.placement = 1 THEN 1 END) as tournament_wins,
                MIN(t.start_date) as first_tournament,
                MAX(t.end_date) as last_tournament
            FROM tournament_statistics ts
            JOIN tournaments t ON ts.tournament_id = t.tournament_id
            WHERE ts.user_id = ?
            """;
            
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new HashMap<String, Object>() {{
                        put("tournamentsPlayed", rs.getInt("tournaments_played"));
                        put("totalWins", rs.getInt("total_wins"));
                        put("totalDraws", rs.getInt("total_draws"));
                        put("totalLosses", rs.getInt("total_losses"));
                        put("avgPerformance", rs.getDouble("avg_performance"));
                        put("tournamentWins", rs.getInt("tournament_wins"));
                        put("firstTournament", rs.getTimestamp("first_tournament"));
                        put("lastTournament", rs.getTimestamp("last_tournament"));
                    }};
                }
            }
        }
        return new HashMap<>();
    }
    
    private List<Map<String, Object>> getPlayerAchievements(int userId) throws SQLException {
        String sql = """
            SELECT *
            FROM achievements
            WHERE user_id = ?
            ORDER BY earned_date DESC
            """;
            
        List<Map<String, Object>> achievements = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    achievements.add(new HashMap<String, Object>() {{
                        put("name", rs.getString("name"));
                        put("description", rs.getString("description"));
                        put("type", rs.getString("type"));
                        put("points", rs.getInt("points"));
                        put("earnedDate", rs.getTimestamp("earned_date").toLocalDateTime());
                    }});
                }
            }
        }
        return achievements;
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        return style;
    }
    
    private void createTournamentSheet(Workbook workbook, CellStyle headerStyle, 
                                     CellStyle dateStyle, int userId) throws SQLException {
        Sheet sheet = workbook.createSheet("Tournaments");
        List<Map<String, Object>> tournaments = getTournamentHistory(userId);
        
        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Tournament", "Start Date", "End Date", "Status", "Organizer", 
                          "Games Played", "Wins", "Draws", "Losses", "Performance", "Placement"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add data
        int rowNum = 1;
        for (Map<String, Object> tournament : tournaments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) tournament.get("name"));
            
            Cell startDate = row.createCell(1);
            startDate.setCellValue(tournament.get("startDate").toString());
            startDate.setCellStyle(dateStyle);
            
            Cell endDate = row.createCell(2);
            endDate.setCellValue(tournament.get("endDate").toString());
            endDate.setCellStyle(dateStyle);
            
            row.createCell(3).setCellValue((String) tournament.get("status"));
            row.createCell(4).setCellValue((String) tournament.get("organizer"));
            row.createCell(5).setCellValue((Integer) tournament.get("totalGames"));
            row.createCell(6).setCellValue((Integer) tournament.get("gamesWon"));
            row.createCell(7).setCellValue((Integer) tournament.get("gamesDrawn"));
            row.createCell(8).setCellValue((Integer) tournament.get("gamesLost"));
            row.createCell(9).setCellValue((Integer) tournament.get("performanceRating"));
            row.createCell(10).setCellValue((Integer) tournament.get("placement"));
        }
        
        // Autosize columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createGamesSheet(Workbook workbook, CellStyle headerStyle, 
                                CellStyle dateStyle, int userId) throws SQLException {
        Sheet sheet = workbook.createSheet("Games");
        List<Map<String, Object>> games = getTournamentGames(userId);
        
        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Tournament", "Date", "White", "Black", "White Rating", 
                          "Black Rating", "Result", "ECO", "Opening"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add data
        int rowNum = 1;
        for (Map<String, Object> game : games) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) game.get("tournamentName"));
            
            Cell date = row.createCell(1);
            date.setCellValue(game.get("date").toString());
            date.setCellStyle(dateStyle);
            
            row.createCell(2).setCellValue((String) game.get("whitePlayer"));
            row.createCell(3).setCellValue((String) game.get("blackPlayer"));
            row.createCell(4).setCellValue((Integer) game.get("whiteRating"));
            row.createCell(5).setCellValue((Integer) game.get("blackRating"));
            row.createCell(6).setCellValue((String) game.get("result"));
            row.createCell(7).setCellValue((String) game.get("ecoCode"));
            row.createCell(8).setCellValue((String) game.get("openingName"));
        }
        
        // Autosize columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createStatsSheet(Workbook workbook, CellStyle headerStyle, 
                                int userId) throws SQLException {
        Sheet sheet = workbook.createSheet("Statistics");
        Map<String, Object> stats = getPlayerStatistics(userId);
        
        // Create headers and data
        int rowNum = 0;
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            Cell header = row.createCell(0);
            header.setCellValue(entry.getKey());
            header.setCellStyle(headerStyle);
            row.createCell(1).setCellValue(entry.getValue().toString());
        }
        
        // Autosize columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    private void createAchievementsSheet(Workbook workbook, CellStyle headerStyle, 
                                       CellStyle dateStyle, int userId) throws SQLException {
        Sheet sheet = workbook.createSheet("Achievements");
        List<Map<String, Object>> achievements = getPlayerAchievements(userId);
        
        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Achievement", "Description", "Type", "Points", "Earned Date"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add data
        int rowNum = 1;
        for (Map<String, Object> achievement : achievements) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) achievement.get("name"));
            row.createCell(1).setCellValue((String) achievement.get("description"));
            row.createCell(2).setCellValue((String) achievement.get("type"));
            row.createCell(3).setCellValue((Integer) achievement.get("points"));
            
            Cell earnedDate = row.createCell(4);
            earnedDate.setCellValue(achievement.get("earnedDate").toString());
            earnedDate.setCellStyle(dateStyle);
        }
        
        // Autosize columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}

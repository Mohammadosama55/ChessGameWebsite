package com.chessgame.controller;

import com.chessgame.dao.GameDAO;
import com.chessgame.model.Game;
import com.chessgame.service.ExpertService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;

@WebServlet("/analysis/*")
public class AnalysisServlet extends HttpServlet {
    private GameDAO gameDAO;
    private ExpertService expertService;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        gameDAO = new GameDAO();
        expertService = new ExpertService();
        gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("userId");
        
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Show list of games for analysis
                List<Game> games = gameDAO.getGamesByUser(userId);
                request.setAttribute("games", games);
                request.getRequestDispatcher("/jsp/analysis.jsp").forward(request, response);
            } else {
                // Show specific game analysis
                int gameId = Integer.parseInt(pathInfo.substring(1));
                Game game = gameDAO.getGameById(gameId);
                
                if (game != null && (game.getWhitePlayerId() == userId || game.getBlackPlayerId() == userId)) {
                    request.setAttribute("game", game);
                    request.getRequestDispatcher("/jsp/analysis.jsp").forward(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("userId");
        
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        String action = request.getParameter("action");
        int gameId = Integer.parseInt(request.getParameter("gameId"));
        
        try {
            Game game = gameDAO.getGameById(gameId);
            if (game == null || (game.getWhitePlayerId() != userId && game.getBlackPlayerId() != userId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            switch (action) {
                case "addComment":
                    String comment = request.getParameter("comment");
                    String position = request.getParameter("position");
                    
                    // Get expert analysis
                    Map<String, Object> analysis = expertService.analyzeGame(gameId);
                    response.setContentType("application/json");
                    response.getWriter().write(gson.toJson(analysis));
                    break;
                    
                case "addVariation":
                    String variation = request.getParameter("variation");
                    String startPosition = request.getParameter("startPosition");
                    gameDAO.addGameVariation(gameId, startPosition, userId, variation);
                    break;
                    
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
            }
            
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"success\"}");
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

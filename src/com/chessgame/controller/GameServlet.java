package com.chessgame.controller;

import com.chessgame.dao.GameDAO;
import com.chessgame.model.Game;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import com.google.gson.Gson;

@WebServlet("/game/*")
public class GameServlet extends HttpServlet {
    private GameDAO gameDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        gameDAO = new GameDAO();
        gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Show game page
                request.getRequestDispatcher("/jsp/chessboard.jsp").forward(request, response);
            } else if (pathInfo.equals("/list")) {
                // List user's games
                HttpSession session = request.getSession();
                Integer userId = (Integer) session.getAttribute("userId");
                
                if (userId != null) {
                    List<Game> games = gameDAO.getGamesByUserId(userId);
                    request.setAttribute("games", games);
                    request.getRequestDispatcher("/jsp/games-list.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login");
                }
            } else {
                // Get specific game
                String gameId = pathInfo.substring(1);
                Game game = gameDAO.getGameById(Integer.parseInt(gameId));
                
                if (game != null) {
                    request.setAttribute("game", game);
                    request.getRequestDispatcher("/jsp/chessboard.jsp").forward(request, response);
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
        
        try {
            switch (action) {
                case "create":
                    handleCreateGame(request, response, userId);
                    break;
                case "move":
                    handleMove(request, response);
                    break;
                case "resign":
                    handleResign(request, response, userId);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    private void handleCreateGame(HttpServletRequest request, HttpServletResponse response, int userId)
            throws SQLException, IOException {
        String gameType = request.getParameter("gameType");
        String opponent = request.getParameter("opponent");
        
        Game game = new Game(userId, 
                            opponent != null ? Integer.parseInt(opponent) : null,
                            gameType);
        
        game = gameDAO.createGame(game);
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(game));
    }
    
    private void handleMove(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int gameId = Integer.parseInt(request.getParameter("gameId"));
        String gameState = request.getParameter("gameState");
        
        boolean success = gameDAO.updateGameState(gameId, gameState);
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(success));
    }
    
    private void handleResign(HttpServletRequest request, HttpServletResponse response, int userId)
            throws SQLException, IOException {
        int gameId = Integer.parseInt(request.getParameter("gameId"));
        Game game = gameDAO.getGameById(gameId);
        
        if (game != null) {
            // Set the winner as the opponent
            int winnerId = game.getPlayer1Id() == userId ? game.getPlayer2Id() : game.getPlayer1Id();
            boolean success = gameDAO.endGame(gameId, winnerId);
            
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(success));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}

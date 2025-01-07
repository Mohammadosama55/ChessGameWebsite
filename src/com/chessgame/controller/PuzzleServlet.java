package com.chessgame.controller;

import com.chessgame.dao.PuzzleDAO;
import com.chessgame.model.Puzzle;
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

@WebServlet("/puzzle/*")
public class PuzzleServlet extends HttpServlet {
    private PuzzleDAO puzzleDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        puzzleDAO = new PuzzleDAO();
        gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Show puzzle list page
                List<String> themes = puzzleDAO.getAllThemes();
                request.setAttribute("themes", themes);
                request.getRequestDispatcher("/jsp/puzzle-list.jsp").forward(request, response);
            } else if (pathInfo.equals("/random")) {
                // Get random puzzle
                String difficulty = request.getParameter("difficulty");
                List<Puzzle> puzzles = puzzleDAO.getRandomPuzzles(1, difficulty);
                
                if (!puzzles.isEmpty()) {
                    request.setAttribute("puzzle", puzzles.get(0));
                    request.getRequestDispatcher("/jsp/puzzle-solve.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/puzzle");
                }
            } else if (pathInfo.startsWith("/theme/")) {
                // Get puzzles by theme
                String theme = pathInfo.substring(7);
                List<Puzzle> puzzles = puzzleDAO.getPuzzlesByTheme(theme);
                request.setAttribute("puzzles", puzzles);
                request.setAttribute("currentTheme", theme);
                request.getRequestDispatcher("/jsp/puzzle-list.jsp").forward(request, response);
            } else {
                // Get specific puzzle
                int puzzleId = Integer.parseInt(pathInfo.substring(1));
                Puzzle puzzle = puzzleDAO.getPuzzleById(puzzleId);
                
                if (puzzle != null) {
                    request.setAttribute("puzzle", puzzle);
                    request.getRequestDispatcher("/jsp/puzzle-solve.jsp").forward(request, response);
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
                case "verify":
                    handleVerifySolution(request, response, userId);
                    break;
                case "create":
                    handleCreatePuzzle(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    private void handleVerifySolution(HttpServletRequest request, HttpServletResponse response, int userId)
            throws SQLException, IOException {
        int puzzleId = Integer.parseInt(request.getParameter("puzzleId"));
        String[] moves = request.getParameter("moves").split(",");
        
        Puzzle puzzle = puzzleDAO.getPuzzleById(puzzleId);
        boolean isCorrect = puzzle != null && puzzle.checkSolution(moves);
        
        // Update user progress
        puzzleDAO.updateUserPuzzleProgress(userId, puzzleId, isCorrect, 
            Integer.parseInt(request.getParameter("attempts")));
        
        // Send response
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(new VerificationResult(isCorrect)));
    }
    
    private void handleCreatePuzzle(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        // Only allow admins to create puzzles
        HttpSession session = request.getSession();
        if (!"ADMIN".equals(session.getAttribute("userRole"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        Puzzle puzzle = new Puzzle(
            request.getParameter("initialPosition"),
            request.getParameter("solution"),
            request.getParameter("difficulty"),
            request.getParameter("title"),
            request.getParameter("theme")
        );
        puzzle.setDescription(request.getParameter("description"));
        
        puzzle = puzzleDAO.createPuzzle(puzzle);
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(puzzle));
    }
    
    private static class VerificationResult {
        private final boolean correct;
        
        public VerificationResult(boolean correct) {
            this.correct = correct;
        }
    }
}

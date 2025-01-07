package com.chessgame.controller;

import com.chessgame.dao.TournamentDAO;
import com.chessgame.model.Tournament;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import com.google.gson.Gson;

@WebServlet("/tournament/*")
public class TournamentServlet extends HttpServlet {
    private TournamentDAO tournamentDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        tournamentDAO = new TournamentDAO();
        gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Show tournament list page
                List<Tournament> upcomingTournaments = tournamentDAO.getTournamentsByStatus("UPCOMING");
                List<Tournament> inProgressTournaments = tournamentDAO.getTournamentsByStatus("IN_PROGRESS");
                List<Tournament> completedTournaments = tournamentDAO.getTournamentsByStatus("COMPLETED");
                
                request.setAttribute("upcomingTournaments", upcomingTournaments);
                request.setAttribute("inProgressTournaments", inProgressTournaments);
                request.setAttribute("completedTournaments", completedTournaments);
                request.getRequestDispatcher("/jsp/tournament-list.jsp").forward(request, response);
            } else if (pathInfo.startsWith("/create")) {
                // Show tournament creation page
                HttpSession session = request.getSession();
                if (!"ADMIN".equals(session.getAttribute("userRole"))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                request.getRequestDispatcher("/jsp/tournament-create.jsp").forward(request, response);
            } else {
                // Show specific tournament
                int tournamentId = Integer.parseInt(pathInfo.substring(1));
                Tournament tournament = tournamentDAO.getTournamentById(tournamentId);
                
                if (tournament != null) {
                    request.setAttribute("tournament", tournament);
                    request.getRequestDispatcher("/jsp/tournament-view.jsp").forward(request, response);
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
                    handleCreateTournament(request, response);
                    break;
                case "register":
                    handleRegisterParticipant(request, response, userId);
                    break;
                case "start":
                    handleStartTournament(request, response);
                    break;
                case "updateResult":
                    handleUpdateResult(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    private void handleCreateTournament(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        // Only allow admins to create tournaments
        HttpSession session = request.getSession();
        if (!"ADMIN".equals(session.getAttribute("userRole"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        Tournament tournament = new Tournament(
            request.getParameter("name"),
            request.getParameter("description"),
            Timestamp.valueOf(request.getParameter("startDate")),
            Timestamp.valueOf(request.getParameter("endDate")),
            Integer.parseInt(request.getParameter("maxParticipants")),
            request.getParameter("tournamentType"),
            Integer.parseInt(request.getParameter("roundsCount")),
            Integer.parseInt(request.getParameter("timeControlMinutes")),
            Integer.parseInt(request.getParameter("timeIncrementSeconds"))
        );
        
        tournament = tournamentDAO.createTournament(tournament);
        
        response.sendRedirect(request.getContextPath() + "/tournament/" + tournament.getTournamentId());
    }
    
    private void handleRegisterParticipant(HttpServletRequest request, HttpServletResponse response, int userId)
            throws SQLException, IOException {
        int tournamentId = Integer.parseInt(request.getParameter("tournamentId"));
        Tournament tournament = tournamentDAO.getTournamentById(tournamentId);
        
        if (tournament != null && tournament.canRegister() && !tournament.isRegistered(userId)) {
            tournamentDAO.registerParticipant(tournamentId, userId);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\": true}");
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private void handleStartTournament(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        // Only allow admins to start tournaments
        HttpSession session = request.getSession();
        if (!"ADMIN".equals(session.getAttribute("userRole"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        int tournamentId = Integer.parseInt(request.getParameter("tournamentId"));
        Tournament tournament = tournamentDAO.getTournamentById(tournamentId);
        
        if (tournament != null && tournament.isUpcoming()) {
            tournamentDAO.updateTournamentStatus(tournamentId, "IN_PROGRESS");
            // Create first round
            Tournament.TournamentRound round = new Tournament.TournamentRound();
            round.setRoundNumber(1);
            round.setStartTime(new Timestamp(System.currentTimeMillis()));
            // Generate pairings based on tournament type
            generatePairings(tournament, round);
            tournamentDAO.createRound(tournamentId, round);
            
            response.setContentType("application/json");
            response.getWriter().write("{\"success\": true}");
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private void handleUpdateResult(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int tournamentId = Integer.parseInt(request.getParameter("tournamentId"));
        int roundNumber = Integer.parseInt(request.getParameter("roundNumber"));
        int player1Id = Integer.parseInt(request.getParameter("player1Id"));
        int player2Id = Integer.parseInt(request.getParameter("player2Id"));
        String result = request.getParameter("result");
        
        Integer winnerId = null;
        boolean isDraw = false;
        
        if ("draw".equals(result)) {
            isDraw = true;
        } else {
            winnerId = Integer.parseInt(result);
        }
        
        tournamentDAO.updatePairingResult(tournamentId, roundNumber, player1Id, player2Id, winnerId, isDraw);
        
        // Update participant scores
        if (isDraw) {
            tournamentDAO.updateParticipantScore(tournamentId, player1Id, 0.5);
            tournamentDAO.updateParticipantScore(tournamentId, player2Id, 0.5);
        } else if (winnerId != null) {
            tournamentDAO.updateParticipantScore(tournamentId, winnerId, 1.0);
            tournamentDAO.updateParticipantScore(tournamentId, 
                winnerId == player1Id ? player2Id : player1Id, 0.0);
        }
        
        response.setContentType("application/json");
        response.getWriter().write("{\"success\": true}");
    }
    
    private void generatePairings(Tournament tournament, Tournament.TournamentRound round) {
        List<Tournament.TournamentParticipant> participants = tournament.getParticipants();
        
        switch (tournament.getTournamentType()) {
            case "SWISS":
                generateSwissPairings(participants, round);
                break;
            case "ROUND_ROBIN":
                generateRoundRobinPairings(participants, round);
                break;
            case "ELIMINATION":
                generateEliminationPairings(participants, round);
                break;
        }
    }
    
    private void generateSwissPairings(List<Tournament.TournamentParticipant> participants,
                                     Tournament.TournamentRound round) {
        // Sort participants by score
        participants.sort((p1, p2) -> Double.compare(p2.getScore(), p1.getScore()));
        
        // Create pairings
        for (int i = 0; i < participants.size() - 1; i += 2) {
            Tournament.TournamentPairing pairing = new Tournament.TournamentPairing();
            pairing.setPlayer1Id(participants.get(i).getUserId());
            pairing.setPlayer2Id(participants.get(i + 1).getUserId());
            round.getPairings().add(pairing);
        }
        
        // Handle odd number of participants
        if (participants.size() % 2 != 0) {
            Tournament.TournamentPairing pairing = new Tournament.TournamentPairing();
            pairing.setPlayer1Id(participants.get(participants.size() - 1).getUserId());
            pairing.setPlayer2Id(0); // Bye
            round.getPairings().add(pairing);
        }
    }
    
    private void generateRoundRobinPairings(List<Tournament.TournamentParticipant> participants,
                                          Tournament.TournamentRound round) {
        int n = participants.size();
        int roundIndex = round.getRoundNumber() - 1;
        
        // If odd number of participants, add a dummy player
        if (n % 2 != 0) {
            Tournament.TournamentParticipant dummy = new Tournament.TournamentParticipant();
            dummy.setUserId(0);
            participants.add(dummy);
            n++;
        }
        
        for (int i = 0; i < n/2; i++) {
            int player1Index = (roundIndex + i) % (n - 1);
            int player2Index = (n - 1 - i + roundIndex) % (n - 1);
            
            if (i == 0) {
                player2Index = n - 1;
            }
            
            Tournament.TournamentPairing pairing = new Tournament.TournamentPairing();
            pairing.setPlayer1Id(participants.get(player1Index).getUserId());
            pairing.setPlayer2Id(participants.get(player2Index).getUserId());
            round.getPairings().add(pairing);
        }
    }
    
    private void generateEliminationPairings(List<Tournament.TournamentParticipant> participants,
                                           Tournament.TournamentRound round) {
        // Sort participants by score for seeding
        participants.sort((p1, p2) -> Double.compare(p2.getScore(), p1.getScore()));
        
        // Create pairings
        for (int i = 0; i < participants.size() - 1; i += 2) {
            Tournament.TournamentPairing pairing = new Tournament.TournamentPairing();
            pairing.setPlayer1Id(participants.get(i).getUserId());
            pairing.setPlayer2Id(participants.get(i + 1).getUserId());
            round.getPairings().add(pairing);
        }
        
        // Handle bye if odd number of participants
        if (participants.size() % 2 != 0) {
            Tournament.TournamentPairing pairing = new Tournament.TournamentPairing();
            pairing.setPlayer1Id(participants.get(participants.size() - 1).getUserId());
            pairing.setPlayer2Id(0); // Bye
            round.getPairings().add(pairing);
        }
    }
}

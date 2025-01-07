package com.chessgame.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.chessgame.dao.TournamentDAO;
import com.chessgame.dao.UserDAO;
import com.chessgame.model.Tournament;
import com.chessgame.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class TournamentServiceTest {
    
    private TournamentService tournamentService;
    
    @Mock
    private TournamentDAO tournamentDAO;
    
    @Mock
    private UserDAO userDAO;
    
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        tournamentService = new TournamentService(tournamentDAO, userDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testCreateTournament() throws SQLException {
        Tournament tournament = new Tournament();
        tournament.setName("Test Tournament");
        tournament.setStartDate(LocalDateTime.now().plusDays(1));
        tournament.setMaxParticipants(8);

        when(tournamentDAO.createTournament(any(Tournament.class)))
            .thenReturn(tournament);

        Tournament created = tournamentService.createTournament(tournament);
        assertNotNull(created);
        assertEquals("Test Tournament", created.getName());
        verify(tournamentDAO).createTournament(tournament);
    }

    @Test
    void testRegisterParticipant() throws SQLException {
        Tournament tournament = new Tournament();
        tournament.setId(1);
        tournament.setMaxParticipants(8);
        
        User user = new User();
        user.setId(1);
        
        when(tournamentDAO.getTournamentById(1)).thenReturn(tournament);
        when(userDAO.getUserById(1)).thenReturn(user);
        when(tournamentDAO.registerParticipant(1, 1)).thenReturn(true);

        boolean result = tournamentService.registerParticipant(1, 1);
        assertTrue(result);
    }

    @Test
    void testGetActiveTournaments() throws SQLException {
        List<Tournament> tournaments = Arrays.asList(
            new Tournament(), new Tournament()
        );
        
        when(tournamentDAO.getActiveTournaments()).thenReturn(tournaments);

        List<Tournament> active = tournamentService.getActiveTournaments();
        assertEquals(2, active.size());
    }

    @Test
    void testStartTournament() throws SQLException {
        Tournament tournament = new Tournament();
        tournament.setId(1);
        tournament.setStatus("PENDING");
        
        when(tournamentDAO.getTournamentById(1)).thenReturn(tournament);
        when(tournamentDAO.updateTournament(any(Tournament.class))).thenReturn(tournament);

        Tournament started = tournamentService.startTournament(1);
        assertEquals("ACTIVE", started.getStatus());
    }

    @Test
    void testGetTournamentParticipants() throws SQLException {
        List<User> participants = Arrays.asList(
            new User(), new User(), new User()
        );
        
        when(tournamentDAO.getTournamentParticipants(1)).thenReturn(participants);

        List<User> result = tournamentService.getTournamentParticipants(1);
        assertEquals(3, result.size());
    }

    @Test
    void testEndTournament() throws SQLException {
        Tournament tournament = new Tournament();
        tournament.setId(1);
        tournament.setStatus("ACTIVE");
        
        when(tournamentDAO.getTournamentById(1)).thenReturn(tournament);
        when(tournamentDAO.updateTournament(any(Tournament.class))).thenReturn(tournament);

        Tournament ended = tournamentService.endTournament(1);
        assertEquals("COMPLETED", ended.getStatus());
    }

    @Test
    void testGetTournamentResults() throws SQLException {
        Tournament tournament = new Tournament();
        tournament.setId(1);
        
        List<User> winners = Arrays.asList(
            new User(), new User(), new User()
        );
        
        when(tournamentDAO.getTournamentWinners(1)).thenReturn(winners);

        List<User> results = tournamentService.getTournamentResults(1);
        assertEquals(3, results.size());
    }
}

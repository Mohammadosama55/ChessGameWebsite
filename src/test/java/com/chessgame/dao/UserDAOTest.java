package com.chessgame.dao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.SQLException;
import com.chessgame.model.User;
import com.chessgame.util.DBUtil;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {
    private static UserDAO userDAO;
    private static Connection conn;

    @BeforeAll
    static void setUp() throws SQLException {
        userDAO = new UserDAO();
        conn = DBUtil.getConnection();
        // Setup test database
        setupTestDatabase();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    private static void setupTestDatabase() throws SQLException {
        // Create test tables
        try (var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "username VARCHAR(50) UNIQUE NOT NULL," +
                        "password VARCHAR(255) NOT NULL," +
                        "email VARCHAR(100) UNIQUE NOT NULL," +
                        "rating INT DEFAULT 1200)");
        }
    }

    @Test
    @Order(1)
    void testCreateUser() throws SQLException {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("test@example.com");
        user.setRating(1200);

        User createdUser = userDAO.createUser(user);
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("testuser", createdUser.getUsername());
    }

    @Test
    @Order(2)
    void testGetUserById() throws SQLException {
        User user = userDAO.getUserById(1);
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }

    @Test
    @Order(3)
    void testGetUserByUsername() throws SQLException {
        User user = userDAO.getUserByUsername("testuser");
        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    @Order(4)
    void testUpdateUserRating() throws SQLException {
        User user = userDAO.getUserByUsername("testuser");
        int newRating = 1500;
        user.setRating(newRating);
        
        User updatedUser = userDAO.updateUser(user);
        assertNotNull(updatedUser);
        assertEquals(newRating, updatedUser.getRating());
    }

    @Test
    @Order(5)
    void testGetUserByEmail() throws SQLException {
        User user = userDAO.getUserByEmail("test@example.com");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }

    @Test
    @Order(6)
    void testInvalidUserCredentials() throws SQLException {
        User user = userDAO.getUserByUsername("nonexistentuser");
        assertNull(user);
    }

    @Test
    @Order(7)
    void testDeleteUser() throws SQLException {
        User user = userDAO.getUserByUsername("testuser");
        assertTrue(userDAO.deleteUser(user.getId()));
        
        User deletedUser = userDAO.getUserById(user.getId());
        assertNull(deletedUser);
    }
}

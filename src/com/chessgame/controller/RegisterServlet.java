package com.chessgame.controller;

import com.chessgame.dao.UserDAO;
import com.chessgame.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            // Validate input
            if (username == null || username.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                request.setAttribute("error", "All fields are required");
                request.getRequestDispatcher("/jsp/register.jsp").forward(request, response);
                return;
            }

            // Check if passwords match
            if (!password.equals(confirmPassword)) {
                request.setAttribute("error", "Passwords do not match");
                request.getRequestDispatcher("/jsp/register.jsp").forward(request, response);
                return;
            }

            // Check if username exists
            if (userDAO.isUsernameExists(username)) {
                request.setAttribute("error", "Username already exists");
                request.getRequestDispatcher("/jsp/register.jsp").forward(request, response);
                return;
            }

            // Check if email exists
            if (userDAO.isEmailExists(email)) {
                request.setAttribute("error", "Email already exists");
                request.getRequestDispatcher("/jsp/register.jsp").forward(request, response);
                return;
            }

            // Create new user
            User user = new User(username, email, password);
            boolean success = userDAO.createUser(user);

            if (success) {
                request.setAttribute("success", "Registration successful! Please login.");
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                request.setAttribute("error", "Registration failed");
                request.getRequestDispatcher("/jsp/register.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "An error occurred during registration");
            request.getRequestDispatcher("/jsp/register.jsp").forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/jsp/register.jsp").forward(request, response);
    }
}

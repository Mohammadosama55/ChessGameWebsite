package com.chessgame.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("")  // Map to root context
public class IndexServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("userId") != null) {
            // User is logged in, redirect to dashboard
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else {
            // User is not logged in, show welcome page
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }
}

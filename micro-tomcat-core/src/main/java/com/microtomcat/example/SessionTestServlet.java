package com.microtomcat.example;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import java.io.IOException;

public class SessionTestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        Integer count = (Integer) session.getAttribute("count");
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        session.setAttribute("count", count);
        
        response.setContentType("text/html");
        response.getWriter().println("<h1>Session Test</h1>");
        response.getWriter().println("<p>Session ID: " + session.getId() + "</p>");
        response.getWriter().println("<p>Visit count: " + count + "</p>");
    }
} 